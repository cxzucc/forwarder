package com.cxzucc.forwarder;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import okhttp3.Protocol;

public class ForwarderResponseEntity implements Serializable {
	private static final long serialVersionUID = -1714410125479633860L;

	private int code;
	private Map<String, List<String>> headers;
	private String body;
	private String message;
	private Protocol protocol;

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public Map<String, List<String>> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, List<String>> headers) {
		this.headers = headers;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}
}
