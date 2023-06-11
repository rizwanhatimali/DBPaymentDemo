"use strict";

let cryptoObject = window.crypto /*native*/ || window.msCrypto /*IE11 native*/ || window.msrCrypto; /*polyfill*/;

let encryptAlgorithm = { name: "RSA-OAEP", hash: { name: "SHA-256" } };

/**
 * Copies the bytes from a binary string onto an array starting from the
 * position `start`.
 */
function copyToArray(binString, array, start) {
    for (let i = 0; i < binString.length; i++) {
        array[start + i] = binString.charCodeAt(i);
    }
    return binString.length;
}

function generate_token(lengthBytes) {
    let random = new Uint8Array(lengthBytes);
    cryptoObject.getRandomValues(random);
    return random;
}

function generate_plain_E2E(passwordStr, tokenStr) {
    /**
     * Encodes an integer value in 4 big-endian bytes.
     * Used for length-prefixing.
     */
    function int2Bytes(n) {
        return String.fromCharCode(
            (n & 0xFF000000) >> 24,
            (n & 0x00FF0000) >> 16,
            (n & 0x0000FF00) >> 8,
            (n & 0x000000FF) >> 0
        );
    }
    
    // The plaintext block to be encrypted is made of four elements:
    // [token_length (4 bytes), token (32 bytes), password_length (4 bytes),
    // password (n bytes)]
    
    var plainE2EBlock;
    
    let randomTokenBin = atob(tokenStr);
    
    let plainE2EBlock_len = 4 + randomTokenBin.length + 4 + passwordStr.length;
    plainE2EBlock = new Uint8Array(plainE2EBlock_len);
    let start = 0;
    start += copyToArray(int2Bytes(randomTokenBin.length), plainE2EBlock, start);
    start += copyToArray(randomTokenBin, plainE2EBlock, start);
    start += copyToArray(int2Bytes(passwordStr.length), plainE2EBlock, start);
    start += copyToArray(passwordStr, plainE2EBlock, start);
    
    return btoa(String.fromCharCode.apply(null, plainE2EBlock));
}

function parseDER(binString, start) {
	var tag = binString.charCodeAt(start);
	var firstLengthByte = binString.charCodeAt(start+1);
	if (firstLengthByte & 0x80) {
		// Long definite length encoding.
		// E.g. 0x82 0x01 0x22 means the next 2 bytes encode the length, which is 0x0122 = 290.
		var length = 0;
		for (var i = 0; i < firstLengthByte - 0x80; i++) {
			length = (length * 0x100) + binString.charCodeAt(start + 2 + i);
		}
		var dataStart = start + 2 + i;
	} else {
		// Short length encoding.
		var length = firstLengthByte;
		var dataStart = start + 2;
	}
	var dataEnd = dataStart + length;
	
	if (tag == 0x02) {
		while (dataStart < dataEnd && binString.charCodeAt(dataStart) === 0x00)
			dataStart += 1;
		var obj = {"type": "number", "value": btoa(binString.slice(dataStart, dataEnd))};
	} else if (tag == 0x03) {
		if (binString.charCodeAt(dataStart) !== 0x00) throw "Unused bits in Bit String are not supported";
		var obj = {"type": "bit string", "value": parseDER(binString, dataStart+1)};
	} else if (tag == 0x05) {
		var obj = {"type": "null", "value": null};
	} else if (tag == 0x06) {
		var obj = {"type": "object identifier", "value": binString.slice(dataStart, dataEnd)};
	} else if (tag == 0x30) {
		var obj = {"type": "sequence", "value": []};
		while (dataStart < dataEnd) {
			var item = parseDER(binString, dataStart);
			obj.value.push(item);
			dataStart = item.end;
		}
	} else {
		throw "Unknown tag at position " + start + ": " + tag
	}
	obj.start = start;
	obj.end = dataEnd;
	return obj;
}

function E2E_encrypt(b64PlainE2EBlock, b64PubKey) {
    let plainBin = atob(b64PlainE2EBlock);
    let plainArray = new Uint8Array(plainBin.length);
    copyToArray(plainBin, plainArray, 0);
	
	// The base64 pubkey is in SPKI format, but IE11 crypto.subtle only supports JWK.
	let parsedPubKey = parseDER(atob(b64PubKey), 0);
	let algorithmB64 = btoa(parsedPubKey.value[0].value[0].value);
	if (algorithmB64 !== "KoZIhvcNAQEB") {
		throw "Unsupported public key format, should be 1.2.840.113549.1.1.1 rsaEncryption (PKCS #1) (KoZIhvcNAQEB), got " + algorithmB64;
	}
	function toUrlSafe(b64) {
		return b64.replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "")
	}
	let jwkPubKey = {
        kty: "RSA",
        ext: true,
        n: toUrlSafe(parsedPubKey.value[1].value.value[0].value),
        e: toUrlSafe(parsedPubKey.value[1].value.value[1].value)
    };
	
    return cryptoObject.subtle.importKey("jwk", jwkPubKey, encryptAlgorithm, true, ["encrypt"]).then(function (publicKey) {
        return cryptoObject.subtle.encrypt(encryptAlgorithm, publicKey, plainArray);
    }, function (error) {
        alert("Public Key import error: " + error);
    }).then(function (encrypted) {
        return btoa(String.fromCharCode.apply(null, new Uint8Array(encrypted)));
    }, function (error) {
        alert("Failed to encrypt: " + error);
    });
}

export {E2E_encrypt, generate_plain_E2E, generate_token};

// Example usage:
//var base64PubKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsW5ku3tvXXTkQJXIHHBmK/brmT88ehhLEvrSvpYDnj/GZj9OjSDIRQpN+X00chSEjI7rnL6AqwCHLPMwy4Wn/y9FeDNEI3Wcorl7II8PXkAWlxG083pFc/CmG/R+Vr10lqE57LFsYJtyMUaA4tGfpdmEUfsHWW4Oh7bbcKA3iWod1lrz+EYkbMNhLa6r3AAApwZD+WzJqXjj3O4sXwU9HZ5R1j67+lNrjOmGQ+9dG5sqlKgUxs+UsLtrwvAXXWvsSIzbN62+fMI6JKV7Tam44dPYa/A1X7Yp9RBfGbZy/ms96/gxwRHYp23uUB4kxlND6zUg1ZV79Wzk160pYtojqwIDAQAB";
//var token = "12345678901234567890123456789012";
//var b64PlainE2EBlock = generate_plain_E2E("my password", token);
//E2E_encrypt(b64PlainE2EBlock, base64PubKey).then(function(result) { console.log(result); });