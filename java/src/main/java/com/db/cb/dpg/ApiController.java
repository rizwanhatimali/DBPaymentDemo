package com.db.cb.dpg;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.db.cb.dpg.model.AuthorizationRequest;
import com.db.cb.dpg.model.CheckoutRequest;
import com.db.cb.dpg.model.CheckoutResponse;
import com.db.cb.dpg.model.Payment;
import com.db.cb.dpg.repositories.PaymentRepository;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController {

	private final String pspUrl;
	private final PaymentRepository paymentRepository;
	private final DpgService dpgService;

	public ApiController(@Value("${demo-app.external-checkout-url}") final String pspUrl,
						 final PaymentRepository paymentRepository,
						 final DpgService dpgService) {
		this.pspUrl = pspUrl;
		this.paymentRepository = paymentRepository;
		this.dpgService = dpgService;
	}

	@PostMapping(value = "/checkout", consumes = "application/json", produces = "application/json")
	public ResponseEntity<CheckoutResponse> checkout(@Valid @RequestBody final CheckoutRequest checkoutRequest) {
		log.info("Received checkout request={}", checkoutRequest);
		var payment = Payment.builder()
				.paymentId("merchant-" + String.format("%020d", System.nanoTime()))
				.productId(checkoutRequest.getProductId())
				.amount(checkoutRequest.getAmount())
				.currency(checkoutRequest.getCurrency())
				.build();
		var persistedPayment = paymentRepository.save(payment);
		var dpgPaymentId = dpgService.createPayment(persistedPayment);
		var paymentWithDpgPaymentId = paymentRepository.save(persistedPayment.withDpgPaymentId(dpgPaymentId));
		return ResponseEntity.ok(dpgService.checkoutPayment(paymentWithDpgPaymentId));
	}

	@GetMapping("/psp-url")
	public ResponseEntity<String> getPspUrl() {
		return ResponseEntity.ok(pspUrl);
	}

	@PostMapping("/payment/{payment-id}:authorize")
	public ResponseEntity<String> authorizePayment(
			@PathVariable("payment-id") final String paymentId,
			@Valid @RequestBody final AuthorizationRequest authorizationRequest
	) {
		var payment = paymentRepository.findById(paymentId).orElseThrow(() -> new RuntimeException(
				String.format("Payment-id=%s not found", paymentId)));
		var transactionId = dpgService.preAuthorizePayment(payment, authorizationRequest);
		log.info("Successfully authorize paymentId={}, transactionId={}", paymentId, transactionId);
		return ResponseEntity.ok("/payment-status.html?status=PREAUTHORIZED");
	}

	@PostMapping("/payment/{payment-id}:capture")
	public ResponseEntity<String> capturePayment(@PathVariable("payment-id") final String paymentId) {
		var payment = paymentRepository.findById(paymentId).orElseThrow(() -> new RuntimeException(
				String.format("Payment-id=%s not found", paymentId)));
		dpgService.capturePayment(payment);
		return ResponseEntity.ok("/payment-status.html?status=CAPTURED");
	}
}
