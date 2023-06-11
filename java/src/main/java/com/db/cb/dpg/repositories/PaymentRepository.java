package com.db.cb.dpg.repositories;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.db.cb.dpg.model.Payment;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PaymentRepository {

	private final Map<String, Payment> payments = new ConcurrentHashMap<>();

	public Payment save(final Payment payment) {
		payments.put(payment.getPaymentId(), payment);
		return payments.get(payment.getPaymentId());
	}

	public Optional<Payment> findById(final String paymentId) {
		return Optional.ofNullable(payments.get(paymentId));
	}

}
