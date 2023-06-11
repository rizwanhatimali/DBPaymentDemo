async function authorizePayment(paymentId, token) {
	console.log("payment-id=" + paymentId);
	console.log("token=" + token);

	try {
		const response = await fetch("/api/payment/" + paymentId + ":authorize", {
			method: "POST",
			headers: {
				Accept: "application/json",
				"Content-Type": "application/json",
			},
			body: JSON.stringify({
				token: token
			})
		});

		const responseText = await response.text();

		if (!response.ok) {
			throw new RestError(response.status, responseText);
		}

		const paymentStatusUrlWithPaymentId = responseText + "&paymentId=" + paymentId;
		console.log("payment status URL: " + paymentStatusUrlWithPaymentId);
		window.location.href = paymentStatusUrlWithPaymentId;
	} catch (error) {
		createErrorAlert(error);
	}
}
