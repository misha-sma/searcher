package misha_sma;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import misha_sma.util.ConfigProperties;
import misha_sma.util.Util;

public class Indexator {
	public static final int RANDOM_COUNT = 1000;
	public static final int THREADS_COUNT = 10;
	public static final int REQUESTS_COUNT = 10000;
	public static final int TIMEOUT = 10000;
	public static final String HREF = "href=\"";
	public static final String HTTP = "http";
	public static final String WWW = "www";
	public static final Set<String> EXTENSIONS = new HashSet<String>();
	public static final int MAX_EXTENSION_LENGTH = 4;

	private final static ExecutorService pool = Executors.newFixedThreadPool(THREADS_COUNT);

	private static class SendUrl implements Runnable {
		private String url;

		private SendUrl(String url) {
			this.url = url;
		}

		@Override
		public void run() {
			HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT);
			HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT);
			DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
			try {
				HttpGet req = new HttpGet(url);
				req.setHeader("User-agent",
						"Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:15.0) Gecko/20100101 Firefox/15.0.1");
				// req.setHeader("Accept-Language",
				// "ru-ru,ru;q=0.8,en-us;q=0.5,en;q=0.3");
				httpclient.getParams().setParameter("http.protocol.max-redirects", 15);
				httpclient.getParams().setParameter("http.protocol.reject-relative-redirect", false);
				httpclient.getParams().setParameter(AllClientPNames.SO_TIMEOUT, TIMEOUT);
				System.out.println("executing request to " + url);

				HttpResponse rsp = httpclient.execute(req);
				HttpEntity entity = rsp.getEntity();

				System.out.println("----------------------------------------");
				System.out.println(rsp.getStatusLine());
				Header[] headers = rsp.getAllHeaders();
				for (int i = 0; i < headers.length; i++) {
					System.out.println(headers[i]);
				}
				System.out.println("----------------------------------------");
				String mimeType = "text/html";
				for (Header header : headers) {
					if (header.getName().toLowerCase().equals("content-type")) {
						mimeType = header.getValue().toLowerCase();
						break;
					}
				}

				if (entity != null) {
					boolean isBinary = isBinary(mimeType);
					String extension = isBinary ? "bin" : "html";
					int num = (int) (Math.random() * RANDOM_COUNT);
					String name = System.currentTimeMillis() + "_" + num + "." + extension;
					String fullName = ConfigProperties.PATH_2_HTML + "/" + name;
					String textName = ConfigProperties.PATH_2_FULLTEXT + "/" + name;
					String html = "";
					if (isBinary) {
						byte[] bytes = EntityUtils.toByteArray(entity);
						Util.writeBytes2File(bytes, fullName);
					} else {
						html = EntityUtils.toString(entity);
						Util.writeText2File(html, fullName);
					}

					String[] args = new String[5];
					args[0] = "java";
					args[1] = "-jar";
					args[2] = "lib/tika-app-1.2.jar";
					args[3] = "-T";
					args[4] = fullName;
					String text = Util.runTika(args, new File("./"));
					Util.writeText2File(text, textName);
					// StringTokenizer tokenizer = new StringTokenizer(text);
					// StringBuilder builder = new StringBuilder();
					// while (tokenizer.hasMoreTokens()) {
					// String word = tokenizer.nextToken();
					// builder.append(word).append(' ');
					// }
					// put builder.toString() to lucene

					// find urls
					if (isBinary) {

					} else {
						List<String> urls = findUrls(html, url);
						System.out.println("urls=" + urls);
						for (String url : urls) {
							// pool.submit(new SendUrl(url));
						}
					}

				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				httpclient.getConnectionManager().shutdown();
			}
		}
	}

	public static boolean isBinary(String mimeType) {
		return !(mimeType.contains("html") || mimeType.contains("text"));
	}

	public static String getExtension(String mimeType) {
		return mimeType.contains("html") ? "html" : mimeType.contains("pdf") ? "pdf" : mimeType.contains("doc") ? "doc"
				: "unknown";
	}

	public static List<String> findUrls(String html, String originalUrl) {
		List<String> urls = new ArrayList<String>();
		String body = cutBody(html);
		if (body == null) {
			return urls;
		}
		String baseUrl = getBaseUrl(originalUrl);
		int hrefIndex = body.indexOf(HREF);
		int quoteIndex = hrefIndex > 0 ? body.indexOf('"', hrefIndex + HREF.length()) : -1;
		while (hrefIndex > 0 && quoteIndex > hrefIndex) {
			String url = body.substring(hrefIndex + HREF.length(), quoteIndex);
			if (!url.startsWith("#")) {
				url = url.trim().toLowerCase();
				url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
				url = url.startsWith(WWW) || url.startsWith("//") ? HTTP + "://" + url : url;
				url = url.startsWith("/") ? baseUrl + url : url;
				if (url.startsWith(HTTP) && checkExtension(url)) {
					urls.add(url);
				}
			}
			hrefIndex = body.indexOf(HREF, quoteIndex);
			quoteIndex = hrefIndex > 0 ? body.indexOf('"', hrefIndex + HREF.length()) : -1;
		}
		return urls;
	}

	public static boolean checkExtension(String url) {
		if (getBaseUrl(url).equals(url)) {
			return true;
		}
		int dotIndex = url.lastIndexOf('.');
		if (dotIndex > 0) {
			String extension = url.substring(dotIndex + 1);
			if (extension.length() > MAX_EXTENSION_LENGTH) {
				return true;
			}
			return EXTENSIONS.contains(extension);
		}
		return false;
	}

	public static String getBaseUrl(String url) {
		int slashIndex = url.indexOf('/', 10);
		if (slashIndex > 0) {
			return url.substring(0, slashIndex);
		}
		return url;
	}

	public static String cutBody(String html) {
		int beginIndex = html.indexOf("<body");
		int endIndex = html.lastIndexOf("</body>");
		if (beginIndex > 0 && endIndex > beginIndex) {
			return html.substring(beginIndex, endIndex);
		}
		return null;
	}

	public static void main(String[] args) {
		loadExtensions();
		List<String> urls = loadUrls();
		pool.submit(new SendUrl("http://en.wikipedia.org/wiki/Cassini%E2%80%93Huygens"));
		// pool.submit(new
		// SendUrl("http://descanso.jpl.nasa.gov/DPSummary/Descanso3--Cassini2.pdf"));
		// for (String url : urls) {
		// pool.submit(new SendUrl(url));
		// }
	}

	public static List<String> loadUrls() {
		List<String> urls = new ArrayList<String>();
		String urlsStr = Util.loadText("config/init_urls.txt");
		StringTokenizer tokenizer = new StringTokenizer(urlsStr);
		while (tokenizer.hasMoreTokens()) {
			String url = tokenizer.nextToken();
			urls.add(url);
		}
		return urls;
	}

	public static void loadExtensions() {
		String extensionsStr = Util.loadText("config/extensions.txt");
		StringTokenizer tokenizer = new StringTokenizer(extensionsStr);
		while (tokenizer.hasMoreTokens()) {
			String url = tokenizer.nextToken();
			EXTENSIONS.add(url);
		}
	}
}
