package com.db.cb.dpg.model;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {
	@NotNull
	private String productId;
	@NotNull
	private String amount;
	@NotNull
	private String currency;
}

