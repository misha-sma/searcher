package misha_sma;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import misha_sma.util.ConfigProperties;
import misha_sma.util.Util;

public class HttpServer {
	private static final Logger logger = Logger.getLogger(HttpServer.class);

	public static final String HOME_PAGE;
	public static byte[] faviconBytes;

	static {
		HOME_PAGE = Util.loadText("web/index.html");
		File file = new File("web/favicon.ico");
		try {
			FileInputStream input = new FileInputStream(file);
			faviconBytes = new byte[(int) file.length()];
			input.read(faviconBytes);
			input.close();
		} catch (FileNotFoundException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		}
	}

	private static class SocketProcessor implements Runnable {
		private Socket socket;
		private InputStream is;
		private OutputStream os;

		private SocketProcessor(Socket socket) throws Throwable {
			this.socket = socket;
			this.is = socket.getInputStream();
			this.os = socket.getOutputStream();
		}

		public void run() {
			try {
				String headers = readInputHeaders();
				String url = getUrl(headers);
				if (url.equals("favicon.ico")) {
					writeFaviconResponse();
				} else if (url.startsWith("?query=")) {
					int andIndex = url.indexOf('&');
					String query = andIndex > 0 ? url.substring(7, andIndex) : url.substring(7);
					logger.info("query=" + query);
					// RUSSIAN URL ENCODING
					SearchManager.getInstance().search(query, 1, 10);
				} else {
					writeHomePageResponse(HOME_PAGE);
				}
			} catch (Throwable t) {
				logger.error(t);
			} finally {
				try {
					socket.close();
				} catch (Throwable t) {
					logger.error(t);
				}
			}
			logger.info("----------------------Client processing finished---------------------------");
		}

		private void writeHomePageResponse(String html) throws Throwable {
			String response = "HTTP/1.1 200 OK\r\n" + "Server: misha-sma-Server/2012\r\n"
					+ "Content-Type: text/html\r\n" + "Content-Length: " + html.length() + "\r\n"
					+ "Connection: close\r\n\r\n";
			String result = response + html;
			os.write(result.getBytes());
			os.flush();
		}

		private void writeFaviconResponse() throws Throwable {
			String response = "HTTP/1.1 200 OK\r\n" + "Server: misha-sma-Server/2012\r\n"
					+ "Content-Type: image/vnd.microsoft.icon\r\n" + "Content-Length: " + faviconBytes.length + "\r\n"
					+ "Connection: close\r\n\r\n";
			os.write(response.getBytes());
			os.write(faviconBytes);
			os.flush();
		}

		private String readInputHeaders() throws Throwable {
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null && !line.trim().isEmpty()) {
				logger.debug("line=" + line);
				builder.append(line).append('\n');
			}
			return builder.toString();
		}
	}

	public static void main(String[] args) throws Throwable {
		ServerSocket serverSocket = new ServerSocket(ConfigProperties.PORT);
		while (true) {
			Socket socket = serverSocket.accept();
			logger.info("---------------Client accepted------------------------");
			new Thread(new SocketProcessor(socket)).start();
		}
	}

	public static String getUrl(String headers) {
		String url = "";
		StringTokenizer tokenizer = new StringTokenizer(headers);
		while (tokenizer.hasMoreTokens()) {
			String word = tokenizer.nextToken();
			if (word.equals("GET")) {
				url = tokenizer.nextToken();
			}
		}
		url = url.startsWith("/") ? url.substring(1) : url;
		url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
		return url;
	}

}
