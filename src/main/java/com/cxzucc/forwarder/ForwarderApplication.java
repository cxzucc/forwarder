package com.cxzucc.forwarder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.cxzucc.forwarder.util.OkHttpClient;

import okhttp3.Request;
import okhttp3.Response;

@SpringBootApplication
public class ForwarderApplication {
	private static Set<String> IP_SET = new HashSet<>();

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
		if (IP_SET.size() > 1000) {
			IP_SET = new HashSet<>();
		}

		if (!IP_SET.add(DateTime.now().toString("yyyyMMdd") + ip)) {
			System.out.println("IP重复");
			return false;
		}

		String loginUrl = "http://101.37.105.154/v1/tools/login?orderNo=" + orderNo + "&ip=" + ip + "&port=10088";

		Request request = new Request.Builder().get().url(loginUrl).build();
		try (Response response = OkHttpClient.execute(request, 3000, 3000)) {
			if (response.isSuccessful()) {
				String body = response.body().string();
				if ("true".equals(body)) {
					return true;
				}
			}
		} catch (Exception e) {
		}

		return false;
	}

	public static boolean logout(String orderNo) {
		String logoutUrl = "http://101.37.105.154/v1/tools/logout?orderNo=" + orderNo;
		Request request = new Request.Builder().get().url(logoutUrl).build();
		try (Response response = OkHttpClient.execute(request, 3000, 3000)) {
			if (response.isSuccessful()) {
				String body = response.body().string();
				if ("true".equals(body)) {
					return true;
				}
			}
		} catch (Exception e) {
		}

		return false;
	}

	public static boolean testConnection() {
		Request request = new Request.Builder().get().url("http://101.37.105.154/v1/tools/echo_headers").build();

		for (int i = 0; i < 3; i++) {
			try (Response response = OkHttpClient.execute(request, 3000, 3000)) {
				if (response.isSuccessful()) {
					return true;
				}
			} catch (Exception e) {
			}
		}

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
					// System.out.println(inetAddr.toString() +
					// ",isAnyLocalAddress=" + inetAddr.isAnyLocalAddress()
					// + ",isSiteLocalAddress=" + inetAddr.isSiteLocalAddress()
					// + ",isLinkLocalAddress="
					// + inetAddr.isLinkLocalAddress() + ",isLoopbackAddress=" +
					// inetAddr.isLoopbackAddress()
					// + ",isMCGlobal=" + inetAddr.isMCGlobal() +
					// ",isMCLinkLocal=" + inetAddr.isMCLinkLocal()
					// + ",isMCNodeLocal=" + inetAddr.isMCNodeLocal() +
					// ",isMCOrgLocal=" + inetAddr.isMCOrgLocal()
					// + ",isMCSiteLocal=" + inetAddr.isMCSiteLocal() +
					// ",isMulticastAddress="
					// + inetAddr.isMulticastAddress());
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
