package com.cxzucc.forwarder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ForwarderApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(ForwarderApplication.class, args);

		File file = new File("D:/config.txt");
		if (!file.exists()) {
			file = new File("C:/config.txt");
		}

		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		String line = bufferedReader.readLine();
		if (line != null) {
			bufferedReader.close();

			String[] temp = line.split(",");
			String orderNo = temp[0];
			String adslName = temp[1];
			String adslPass = temp[2];
			boolean isLoginSuccess = true;

			while (true) {
				try {
					if (testConnection()) {
						if (logout(orderNo)) {
							System.out.println("登出成功");
							if (isLoginSuccess) {
								Thread.sleep(TimeUnit.MINUTES.toMillis(3));
							}
						} else {
							System.out.println("登出失败");
							Thread.sleep(10000);
							continue;
						}
					}

					cutAdsl("宽带连接");
					if (connAdsl("宽带连接", adslName, adslPass)) {
						Thread.sleep(1000);
						if (testConnection()) {
							InetAddress inetAddress = getWebSiteAddress();
							if (inetAddress != null) {
								System.out.println("ip = " + inetAddress.getHostAddress());
								if (login(orderNo, inetAddress.getHostAddress())) {
									System.out.println("登入成功");
									isLoginSuccess = true;
									Thread.sleep(TimeUnit.MINUTES.toMillis(7));
								} else {
									System.out.println("登入失败");
									isLoginSuccess = false;
								}
							} else {
								System.out.println("获取IP失败");
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			System.err.println("配置文件不存在");
			bufferedReader.close();
		}
	}

	public static boolean login(String orderNo, String ip) {
		String loginUrl = "http://101.37.105.154/v1/tools/login?orderNo=" + orderNo + "&ip=" + ip + "&port=10088";

		HttpGet httpGet = new HttpGet(loginUrl);
		httpGet.setConfig(RequestConfig.custom().setConnectionRequestTimeout(3000).setConnectTimeout(3000)
				.setSocketTimeout(30000).build());

		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build();
				CloseableHttpResponse response = httpClient.execute(httpGet)) {
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				String responseBody = EntityUtils.toString(response.getEntity());
				if ("true".equals(responseBody)) {
					return true;
				}
			}
		} catch (Exception e) {
		}

		return false;
	}

	public static boolean logout(String orderNo) {
		String logoutUrl = "http://101.37.105.154/v1/tools/logout?orderNo=" + orderNo;
		HttpGet httpGet = new HttpGet(logoutUrl);
		httpGet.setConfig(RequestConfig.custom().setConnectionRequestTimeout(3000).setConnectTimeout(3000)
				.setSocketTimeout(30000).build());

		try (CloseableHttpClient httpClient = HttpClientBuilder.create().build();
				CloseableHttpResponse response = httpClient.execute(httpGet)) {
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				String responseBody = EntityUtils.toString(response.getEntity());
				if ("true".equals(responseBody)) {
					return true;
				}
			}
		} catch (Exception e) {
		}

		return false;
	}

	public static boolean testConnection() {
		HttpGet httpGet = new HttpGet("http://101.37.105.154/v1/tools/echo_headers");
		httpGet.setConfig(RequestConfig.custom().setConnectionRequestTimeout(5000).setConnectTimeout(5000)
				.setSocketTimeout(5000).build());

		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		for (int i = 0; i < 3; i++) {
			try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					EntityUtils.consumeQuietly(response.getEntity());
					HttpClientUtils.closeQuietly(httpClient);
					return true;
				}
			} catch (Exception e) {
			}
		}

		HttpClientUtils.closeQuietly(httpClient);
		return false;
	}

	public static InetAddress getWebSiteAddress() {
		try {
			// 遍历所有的网络接口
			for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces
					.hasMoreElements();) {
				NetworkInterface iface = ifaces.nextElement();
				// 在所有的接口下再遍历IP
				for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
					InetAddress inetAddr = inetAddrs.nextElement();
					if (inetAddr.isAnyLocalAddress() == false && inetAddr.isSiteLocalAddress() == false
							&& inetAddr.isLinkLocalAddress() == false && inetAddr.isLoopbackAddress() == false
							&& inetAddr.isMCGlobal() == false && inetAddr.isMCLinkLocal() == false
							&& inetAddr.isMCNodeLocal() == false && inetAddr.isMCOrgLocal() == false
							&& inetAddr.isMCSiteLocal() == false && inetAddr.isMulticastAddress() == false) {
						return inetAddr;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 执行CMD命令,并返回String字符串
	 */
	public static String executeCmd(String strCmd) throws Exception {
		Process p = Runtime.getRuntime().exec("cmd /c " + strCmd);
		StringBuilder sbCmd = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		while ((line = br.readLine()) != null) {
			sbCmd.append(line + "\n");
		}
		return sbCmd.toString();
	}

	/**
	 * 连接ADSL
	 */
	public static boolean connAdsl(String adslTitle, String adslName, String adslPass) throws Exception {
		System.out.println("正在建立连接.");
		String adslCmd = "rasdial " + adslTitle + " " + adslName + " " + adslPass;
		String tempCmd = executeCmd(adslCmd);
		// 判断是否连接成功
		if (tempCmd.indexOf("已连接") > 0) {
			System.out.println("已成功建立连接.");
			return true;
		} else {
			System.err.println(tempCmd);
			System.err.println("建立连接失败");
			return false;
		}
	}

	/**
	 * 断开ADSL
	 */
	public static boolean cutAdsl(String adslTitle) throws Exception {
		String cutAdsl = "rasdial " + adslTitle + " /disconnect";
		String result = executeCmd(cutAdsl);

		if (result.indexOf("没有连接") != -1) {
			System.err.println(adslTitle + "连接不存在!");
			return false;
		} else {
			System.out.println("连接已断开");
			return true;
		}
	}
}
