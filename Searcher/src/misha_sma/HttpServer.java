package misha_sma;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import misha_sma.util.ConfigProperties;
import misha_sma.util.Pair;
import misha_sma.util.Util;

public class HttpServer {
	private static final Logger logger = Logger.getLogger(HttpServer.class);

	public static final String HOME_PAGE;
	public static final String HOME_PAGE_BEGIN;
	public static final String HOME_PAGE_END;
	public static byte[] faviconBytes;

	public static int HITS_COUNT = 10;

	static {
		HOME_PAGE = Util.loadText("web/index.html");
		int endBodyIndex = HOME_PAGE.indexOf("</body>");
		HOME_PAGE_BEGIN = HOME_PAGE.substring(0, endBodyIndex);
		HOME_PAGE_END = HOME_PAGE.substring(endBodyIndex);
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
					try {
						query = URLDecoder.decode(query, "UTF8");
					} catch (UnsupportedEncodingException e) {
						logger.error(e);
					}
					int pageIndex = url.indexOf("&page=");
					int page = 1;
					if (pageIndex > 0) {
						andIndex = url.indexOf('&', pageIndex + 6);
						page = andIndex > 0 ? Integer.parseInt(url.substring(pageIndex + 6, andIndex)) : Integer
								.parseInt(url.substring(pageIndex + 6));
					}
					logger.info("query=" + query + "  page=" + page);
					// RUSSIAN URL ENCODING
					SearchResults results = SearchManager.getInstance().search(query, page, HITS_COUNT);
					StringBuilder builder = new StringBuilder(HOME_PAGE_BEGIN);
					for (Pair<String, String> pair : results.getUrls()) {
						builder.append("<a href=\"").append(pair.getLeft()).append("\">").append(pair.getLeft())
								.append("</a><br>\n").append(pair.getRight()).append("<br><br>\n");
					}
					if (results.getTotalTabsCount() > 1) {
						String baseUrl = "/?query=" + query + "&page=";
						for (int i = 1; i <= results.getTotalTabsCount(); ++i) {
							builder.append("<a href=\"").append(baseUrl).append(i).append("\">").append(i)
									.append("</a>   ");
						}
						builder.append("<br><br>\n");
					}
					builder.append(HOME_PAGE_END);
					writeHomePageResponse(builder.toString());
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
					+ "Content-Type: text/html\r\n" + "Connection: close\r\n\r\n";
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
		logger.info("Server started!!!");
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
