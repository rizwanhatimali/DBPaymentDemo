async function checkout() {
	$("#error-alert").fadeOut();

	try {
		const response = await fetch("/api/checkout", {
			method: "POST",
			headers: {
				Accept: "application/json",
				"Content-Type": "application/json",
			},
			body: JSON.stringify({
				productId: $("#productId").val(),
				amount: $("#amount").val(),
				currency: $("#currency").val(),
			}),
		});

		const responseJson = await response.text();
		console.log(responseJson);

		if (!response.ok) {
			throw new RestError(response.status, responseJson);
		}

		await runPspSecureFieldsIntegrationIfNecessary();

		$("#checkoutButton").hide();
		$("#payButton").show();

		const slideDownDelayInMs = 1000;

		$("#payment-session").val(responseJson);

		$("#creditCardDetails").slideDown(slideDownDelayInMs);

	} catch (error) {
		createErrorAlert(error);
	}
}

$("#checkoutButton").click(checkout);
