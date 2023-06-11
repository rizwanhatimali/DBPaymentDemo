async function runPspSecureFieldsIntegrationIfNecessary() {
	console.log("payment-session=" + $("#payment-session").val());
	console.log("Running PspSecureFieldsIntegration...");

	const pspUrlResponse = await fetch("/api/psp-url");
	const pspUrlResponseText = await pspUrlResponse.text();

	if (!pspUrlResponse.ok) {
		throw new RestError(pspUrlResponse.status, pspUrlResponseText);
	}

	const pspSandboxIntegrationJSUrl = pspUrlResponseText + "/web-ifs/assets/1.1/integration.js";
	$.getScript(pspSandboxIntegrationJSUrl, function () {
		const options = integrateCreditCardFieldsWithPsp();
		const payButton = document.querySelector("#payButton");

		integration.initialize(options).then(function (ifsInstance) {
			ifsInstance.on("submitRequest", paymentAuthorization(ifsInstance));
			payButton.addEventListener("click", paymentAuthorization(ifsInstance), false);
		});
	});
}

function integrateCreditCardFieldsWithPsp() {
	const paymentSession = JSON.parse($("#payment-session").val());

	const allFields = {
		holder: {
			selector: "#paymentCardName",
			placeholder: "Cardholder Name",
			required: false
		},
		number: {
			selector: "#paymentCardNumber",
			placeholder: "0000 0000 0000 0000"
		},
		expiry: {
			selector: "#paymentCardExpiration",
			placeholder: "MM/YY"
		},
		code: {
			selector: "#paymentCardCVV",
			placeholder: "nnn"
		}
	};

	const options = {
		clientSession: paymentSession.sessionId,
		clientConfiguration: paymentSession.sessionConfig,
		acs_window: {
			width: 700,
			height: 600
		}
	};

	options.fields = allFields;
	options.type = "creditCard";

	for (const fieldKey in allFields) {
		let field = allFields[fieldKey];
		let foundField = document.querySelector(field.selector);
		if (foundField) {
			foundField.innerHTML = "";
		}
	}

	var styles = {
		"input": {
			"font-size": "16px",
			"color": "#444444",
			"font-family": "monospace"
		},
		".ifs-valid": {
			"color": "Green"
		},
		".ifs-invalid": {
			"color": "Crimson"
		},
		".ifs-not-accepted": {
			"color": "DarkGoldenRod"
		}
	};
	options.styles = styles;

	console.log(options);

	return options;
}

function paymentAuthorization(ifsInstnc) {
	return event => {
		if (event instanceof Event) event.preventDefault();
		if (!ifsInstnc.isActive() || !ifsInstnc.isValid()) return;

		ifsInstnc.createToken(function(createTokenErr, createTokenResponse) {
			if (createTokenErr) {
				console.error("failed to create token: " + createTokenErr);
				createErrorAlert(new TokenError(createTokenErr));
				return;
			}

			console.log("createTokenResponse.token: " + createTokenResponse.token);
			console.log("createTokenResponse.type: " + createTokenResponse.type);

			const paymentSession = JSON.parse($("#payment-session").val());

			authorizePayment(paymentSession.paymentId, createTokenResponse.token);
		});
	};
}
