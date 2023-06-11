async function capturePayment(paymentId) {
    try {
        const response = await fetch("/api/payment/" + paymentId + ":capture", {
            method: "POST",
            headers: {
                Accept: "application/json",
                "Content-Type": "application/json",
            }
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

$("#captureButton").click(function () {
    capturePayment(this.getAttribute("payment-id"));
})
