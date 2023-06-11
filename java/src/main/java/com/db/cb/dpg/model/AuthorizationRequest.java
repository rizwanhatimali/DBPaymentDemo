package com.db.cb.dpg.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthorizationRequest {
	@JsonProperty("token")
	private String token;

	public AuthorizationRequest token(String token) {
		this.token = token;
		return this;
	}

	@NotNull
	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
    }
}

