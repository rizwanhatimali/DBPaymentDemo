package com.db.cb.dpg.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Objects;

import javax.net.ssl.KeyManagerFactory;

public final class KeyStoreUtil {
	public static final String ALIAS = "1";

	public static KeyStore getKeyStore(final String keystoreFile, final String keystorePass) {
		try (InputStream stream = new FileInputStream(keystoreFile)) {
			KeyStore keystore = KeyStore.getInstance("PKCS12");
			keystore.load(stream, keystorePass.toCharArray());
			return keystore;
		} catch (Exception e) {
			throw new RuntimeException("Cannot load keystore", e);
		}
	}

	public static KeyManagerFactory getKeyManagerFactory(final String keystoreFile, final String keystorePass) {
		try {
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
			keyManagerFactory.init(KeyStoreUtil.getKeyStore(keystoreFile, keystorePass), keystorePass.toCharArray());
			return keyManagerFactory;
		} catch (Exception e) {
			throw new RuntimeException("Cannot create KeyManagerFactory", e);
		}
	}

	public static PrivateKey getPrivateKey(final String keystoreFile, final String keystorePass) {
		try {
			PrivateKey key = (PrivateKey) KeyStoreUtil.getKeyStore(keystoreFile, keystorePass).getKey(ALIAS, keystorePass.toCharArray());
			return Objects.requireNonNull(key, "Private key not found for alias: " + ALIAS);
		} catch (Exception e) {
			throw new RuntimeException("Cannot get private key", e);
		}
	}
}
