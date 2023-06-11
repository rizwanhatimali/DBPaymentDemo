package com.db.cb.dpg.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckoutResponse {
	@JsonProperty("paymentId")
	private String paymentId;

	@JsonProperty("sessionId")
	private String sessionId;

	@JsonProperty("sessionConfig")
	private String sessionConfig;

	public CheckoutResponse paymentId(String paymentId) {
		this.paymentId = paymentId;
		return this;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	public CheckoutResponse sessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	@NotNull
	@Size(min = 1, max = 255)
	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public CheckoutResponse sessionConfig(String sessionConfig) {
		this.sessionConfig = sessionConfig;
		return this;
	}

	@NotNull
	@Size(min = 1, max = 255)
	public String getSessionConfig() {
		return sessionConfig;
	}

	public void setSessionConfig(String sessionConfig) {
		this.sessionConfig = sessionConfig;
    }
}

