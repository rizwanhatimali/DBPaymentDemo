package com.db.cb.dpg.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.With;

@Builder
@Getter
@ToString
public final class Payment {
	private final String paymentId;
	@With
	private final String dpgPaymentId;
	private final String productId;
	private final String amount;
	private final String currency;
}