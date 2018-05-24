package com.cxzucc.forwarder.util;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import okhttp3.ConnectionPool;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpClient {
	public static final ConnectionPool CONNECTION_POOL = new ConnectionPool(50, 10, TimeUnit.MINUTES);

	private static okhttp3.OkHttpClient okHttpClient = new okhttp3.OkHttpClient.Builder()
			.hostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String arg0, SSLSession arg1) {
					return true;
				}
			}).connectionPool(CONNECTION_POOL).connectTimeout(3, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
			.writeTimeout(10, TimeUnit.SECONDS).build();

	public static Response execute(Request request, long connectTimeout, long readTimeout) throws IOException {
		return okHttpClient.newBuilder().connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
				.readTimeout(readTimeout, TimeUnit.MILLISECONDS).build().newCall(request).execute();
	}
}
