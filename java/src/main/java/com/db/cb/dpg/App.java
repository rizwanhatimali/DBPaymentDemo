package com.db.cb.dpg;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.MessageDigest;
import java.security.Signature;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.openapitools.configuration.ApiKeyRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import com.db.cb.dpg.util.KeyStoreUtil;
import com.db.cb.dpg.util.SSLUtil;

import feign.Client;
import feign.Request;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableFeignClients(basePackages = {"com.db.cb.dpg.contract.api"})
@Slf4j
public class App {

	@Value("${demo-app.tls-keystore-file}")
	private String tlsKeystoreFile;
	@Value("${demo-app.tls-keystore-pass}")
	private String tlsKeystorePass;
	@Value("${demo-app.cs-keystore-file}")
	private String csKeystoreFile;
	@Value("${demo-app.cs-keystore-pass}")
	private String csKeystorePass;
	@Value("${demo-app.public-key-id}")
	private String publicKeyId;
	@Value("${demo-app.header-date-name}")
	private String headerDateName;
	@Value("${demo-app.marketplace-api-key}")
	private String marketplaceApiKey;
	@Value("${demo-app.proxy-host}")
	private String proxyHost;
	@Value("${demo-app.proxy-port}")
	private int proxyPort;
	public static void main(String[] args) {
		SpringApplication.run(App.class);
	}

	@Bean
	public Client feignClient() {
		System.setProperty("jdk.tls.maxHandshakeMessageSize", "65536");

		if (proxyHost.isBlank()) {
			log.info("Connecting to the internet directly");
			return new Client.Default(
				SSLUtil.getSSLContext(tlsKeystoreFile, tlsKeystorePass).getSocketFactory(),
				(h, s) -> true
			);
		}
		else {
			log.info("Connecting to the internet through proxy {}:{}", proxyHost, proxyPort);
			return new Client.Proxied(
				SSLUtil.getSSLContext(tlsKeystoreFile, tlsKeystorePass).getSocketFactory(),
				(h, s) -> true,
				new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort))
			);
		}
	}

	@Bean
	public RequestInterceptor apiKeyRequestInterceptor() {
		return new ApiKeyRequestInterceptor("header", "Api-Key", marketplaceApiKey);
	}

	@Bean
	public RequestInterceptor securityRequestInterceptor() {
		return request -> {
			if (request.method().equals(Request.HttpMethod.POST.name()) && request.body() == null) {
				request.header("Content-Type", "application/json");
				request.body("{}");
			}
			String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
			String payloadDigest = request.body() != null ? digest(request.body()) : null;
			String path = (request.feignTarget().url() + request.path()).replaceAll("^https?://[^/]+/", "/");
			Map<String, String> signHeadersMap = createSignHeadersMap(
					headerDateName.toLowerCase(),
					date,
					request.method(),
					path,
					payloadDigest
			);
			String signHeadersNames = String.join(" ", signHeadersMap.keySet());
			String signHeadersData =
					signHeadersMap.entrySet().stream()
							.map(header -> header.getKey() + ": " + header.getValue())
							.collect(Collectors.joining("\n"));
			String signHeadersSignature = sign(signHeadersData);
			String signature =
					String.format("keyId=\"%s\",algorithm=\"%s\",headers=\"%s\",signature=\"%s\"",
							publicKeyId,
							"rsa-sha256",
							signHeadersNames,
							signHeadersSignature);

			request
					.header(headerDateName, date)
					.header("X-Request-ID", UUID.randomUUID().toString())
					.header("Signature", signature)
					.header("Digest", payloadDigest);
		};
	}

	private String digest(byte[] input) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(input);
			return "SHA-256=" + Base64.getEncoder().encodeToString(hash);
		} catch (Exception e) {
			log.error("Some error occurs", e);
			return null;
		}
	}

	private String sign(String stringToSign) {
		try {
			Signature signature = Signature.getInstance("SHA256withRSA");
			signature.initSign(KeyStoreUtil.getPrivateKey(csKeystoreFile, csKeystorePass));
			signature.update(stringToSign.getBytes(UTF_8));
			byte[] signedString = signature.sign();
			return Base64.getEncoder().encodeToString(signedString);
		} catch (Exception e) {
			log.error("Cannot sign", e);
			return null;
		}
	}

	private static Map<String, String> createSignHeadersMap(String headerDateName,
															String date,
															String requestMethod,
															String requestTarget,
															String digest) {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("(request-target)", requestMethod.toLowerCase() + " " + requestTarget);
		map.put(headerDateName, date);
		if (digest != null) {
			map.put("digest", digest);
		}
		return map;
	}
}
