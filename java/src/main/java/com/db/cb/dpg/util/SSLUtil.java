package com.db.cb.dpg.util;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public final class SSLUtil {

	public static SSLContext getSSLContext(final String keystoreFile, final String keystorePass) {
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			KeyManager[] sslKeyManagers = KeyStoreUtil.getKeyManagerFactory(keystoreFile, keystorePass).getKeyManagers();
			sslContext.init(sslKeyManagers, new TrustManager[]{new DummyTrustManager()}, new SecureRandom());
			return sslContext;
		} catch (Exception e) {
			throw new RuntimeException("Cannot create SSLContext", e);
		}
	}

	private static class DummyTrustManager implements X509TrustManager {

		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public void checkClientTrusted(X509Certificate[] certs, String authType) {
		}

		public void checkServerTrusted(X509Certificate[] certs, String authType) {
		}
	}
}
