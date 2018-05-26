package com.cxzucc.forwarder.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cxzucc.forwarder.ForwarderRequestEntity;
import com.cxzucc.forwarder.ForwarderResponseEntity;
import com.cxzucc.forwarder.util.OkHttpClient;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;

@RestController
@RequestMapping("/forwarder")
public class ForwarderController {
	Logger logger = LoggerFactory.getLogger(this.getClass());

	private Map<String, MediaType> map = new HashMap<>();

	@PostMapping
	public ResponseEntity<ForwarderResponseEntity> forward(@RequestBody ForwarderRequestEntity requestEntity) {
		String url = requestEntity.getUrl();
		String method = requestEntity.getMethod();
		Map<String, List<String>> headers = requestEntity.getHeaders();
		String body = requestEntity.getBody();
		String contentType = requestEntity.getContentType();

		long connectTimeout = requestEntity.getConnectTimeout();
		long readTimeout = requestEntity.getReadTimeout();
		int retryTimes = requestEntity.getRetryTimes();

		Request.Builder builder = new Request.Builder().url(url);
		if (headers != null) {
			for (Entry<String, List<String>> entry : headers.entrySet()) {
				for (String value : entry.getValue()) {
					builder.addHeader(entry.getKey(), value);
				}
			}
		}

		Request request = null;
		if ("GET".equals(method)) {
			request = builder.get().build();
		} else {
			MediaType mediaType = null;
			if (contentType != null) {
				mediaType = map.get(contentType);
				if (mediaType == null) {
					mediaType = MediaType.parse(contentType);
					map.put(contentType, mediaType);
				}
			}

			okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(mediaType, body);
			request = builder.post(requestBody).build();
		}

		long startTime = System.currentTimeMillis();
		for (int i = 0; i < retryTimes; i++) {
			try {
				Response response = OkHttpClient.execute(request, connectTimeout, readTimeout);

				if (response == null) {
					break;
				}

				ForwarderResponseEntity responseEntity = new ForwarderResponseEntity();
				responseEntity.setCode(response.code());
				responseEntity.setHeaders(response.headers().toMultimap());

				if (response.body() != null) {
					responseEntity.setBody(response.body().string());
				}

				responseEntity.setMessage(response.message());
				responseEntity.setProtocol(response.protocol());

				long endTime = System.currentTimeMillis();
				logger.error("req {} success, retry {} times, cost {}", url, i + 1, (endTime - startTime));
				return ResponseEntity.ok(responseEntity);
			} catch (Exception e) {
			}
		}

		long endTime = System.currentTimeMillis();
		logger.error("req {} fail, retry {} times, cost {}", url, retryTimes, (endTime - startTime));

		ForwarderResponseEntity responseEntity = new ForwarderResponseEntity();
		responseEntity.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseEntity);
	}
}