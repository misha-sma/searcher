package misha_sma;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import misha_sma.util.ConfigProperties;
import misha_sma.util.Pair;
import misha_sma.util.Util;

public class Indexator {
	public static final int RANDOM_COUNT = 1000;
	public static final int URLS_COUNT = 20;
	public static final int THREADS_COUNT = 1;
	public static final int TIMEOUT = 10000;
	public static final long DELTA_INDEXING_TIME = 10000;
	public static final String HREF = "href=\"";
	public static final String HTTP = "http";
	public static final String WWW = "www";
	public static final Set<String> EXTENSIONS = new HashSet<String>();
	public static final int MAX_EXTENSION_LENGTH = 4;
	private static int currentUrlsCount = 0;

	public static final String DOMENS = "(com|edu|gov|mil|net|org|info|su|af|al|dz|as|ad|ao|ai|aq|ag|ar|am|aw|au|at|az|bs|bh|bd|bb|by|be|bz|bj|bm|bt|bo|ba|bw|bv|br|io|bn|bg|bf|bi|kh|cm|ca|cv|ky|cf|td|cl|cn|cx|cc|co|km|cg|ck|cr|ci|hr|cu|cy|cz|dk|dj|dm|do|tp|ec|eg|sv|gq|er|ee|et|fk|fo|fj|fi|fr|fx|gf|pf|tf|ga|gm|ge|de|gh|gi|gr|gl|gd|gp|gu|gt|gg|gn|gw|gy|ht|hm|hn|hk|hu|is|in|id|ir|iq|ie|im|il|it|jm|jp|je|jo|kz|ke|ki|kp|kr|kw|kg|la|lv|lb|ls|lr|ly|li|lt|lu|mo|mk|mg|mw|my|mv|ml|mt|mh|mq|mr|mu|yt|mx|fm|md|mc|mn|ms|ma|mz|mm|na|nr|np|nl|an|nc|nz|ni|ne|ng|nu|nf|mp|no|om|pk|pw|pa|pg|py|pe|ph|pn|pl|pt|pr|qa|re|ro|ru|rw|kn|lc|vc|ws|sm|st|sa|sn|sc|sl|sg|sk|si|sb|so|za|gs|es|lk|sh|no|sd|sr|sj|sz|se|ch|sy|tw|tj|tz|th|tg|tk|to|tt|tn|tr|tm|tc|tv|ug|ua|ae|uk|us|um|uy|uz|vu|va|ve|vn|vg|vi|wf|eh|ye|yu|zr|zm|zw)";
	public static final String REGEXP_URL = "((http|https)\\u003A\\u002F{2})?[\\p{Alnum}\\u002E\\u002D\\u005F]{4,255}[\\u002E]"
			+ DOMENS
			+ "((\\u002F)[\\p{Alnum}\\u002D\\u0026\\u002B\\u005F\\u003F\\u003D\\u0023\\u0025\\u002F\\u002E]*)?";
	public static final Pattern URL_PATTERN = Pattern.compile(REGEXP_URL);

	public static Map<String, Pair<String, Long>> urlsMap;
	public static Set<String> waitedUrls = new HashSet<String>();
	public static final Object GLOBAL_SYNCHRONIZE_OBJECT = new Object();

	private final static ExecutorService pool = Executors.newFixedThreadPool(THREADS_COUNT);

	private static final Logger logger = Logger.getLogger(Indexator.class);

	private static double avgTikaTime = 0;

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
				httpclient.getParams().setParameter("http.protocol.max-redirects", 15);
				httpclient.getParams().setParameter("http.protocol.reject-relative-redirect", false);
				httpclient.getParams().setParameter(AllClientPNames.SO_TIMEOUT, TIMEOUT);
				logger.info("==Start executing request to " + url);
				long initTime = System.currentTimeMillis();
				HttpResponse rsp = httpclient.execute(req);
				logger.info("==End executing request Time=" + (System.currentTimeMillis() - initTime));
				HttpEntity entity = rsp.getEntity();

				StatusLine statusLine = rsp.getStatusLine();
				logger.info(statusLine);
				int status = statusLine.getStatusCode();
				if (status != 200) {
					return;
				}
				Header[] headers = rsp.getAllHeaders();
				// logger.info("---------------HEADERS-------------------------");
				// for (int i = 0; i < headers.length; i++) {
				// logger.info(headers[i]);
				// }
				// logger.info("----------------------------------------");
				String mimeType = "text/html";
				for (Header header : headers) {
					if (header.getName().toLowerCase().equals("content-type")) {
						mimeType = header.getValue().toLowerCase();
						break;
					}
				}

				if (entity != null) {
					boolean isBinary = isBinary(mimeType);
					String extension = isBinary ? "" : ".html";
					int num = (int) (Math.random() * RANDOM_COUNT);
					String name = System.currentTimeMillis() + "_" + num + extension;
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

					logger.info("==Start run tika");
					initTime = System.currentTimeMillis();
					// String[] args = new String[5];
					// args[0] = "java";
					// args[1] = "-jar";
					// args[2] = "lib/tika-app-1.2.jar";
					// args[3] = "-T";
					// args[4] = fullName;
					// String text = Util.runTika(args, new File("./"));

					String text = TikaTextExtractor.extractText(fullName);

					long tikaTime = System.currentTimeMillis() - initTime;
					logger.info("==End run tika Time=" + tikaTime);
					avgTikaTime += tikaTime;
					Util.writeText2File(text, textName);

					// find urls
					List<String> urls = isBinary ? findUrlsBinary(text) : findUrls(html, url);
					logger.info("urls=" + urls);
					String hash = Util.getMD5(text);
					logger.info("hash=" + hash);
					if (hash == null) {
						logger.error("Error!!! Hash is null!!!");
						return;
					}
					synchronized (GLOBAL_SYNCHRONIZE_OBJECT) {
						long currentTime = System.currentTimeMillis();
						if (urlsMap.containsKey(url)) {
							Pair<String, Long> value = urlsMap.get(url);
							if (currentTime - value.getRight() > DELTA_INDEXING_TIME && !hash.equals(value.getLeft())) {
								SearchManager.getInstance().updateUrl(url, text, hash, currentTime);
								urlsMap.put(url, new Pair<String, Long>(hash, currentTime));
								logger.info("==Update url " + url + " in lucene");
								++currentUrlsCount;
							}
						} else {
							SearchManager.getInstance().addUrlToIndex(url, text, hash, currentTime);
							urlsMap.put(url, new Pair<String, Long>(hash, currentTime));
							logger.info("==Add url " + url + " in lucene");
							++currentUrlsCount;
						}
						waitedUrls.remove(url);
						if (currentUrlsCount >= URLS_COUNT) {
							SearchManager.getInstance().closeIndex();
							logger.info("END INDEXING!!!");
							logger.info("average tika time=" + avgTikaTime / URLS_COUNT);
							System.exit(0);
						}
						for (String parsedUrl : urls) {
							if (waitedUrls.contains(parsedUrl)) {
								continue;
							}
							if (urlsMap.containsKey(parsedUrl)) {
								Pair<String, Long> value = urlsMap.get(parsedUrl);
								currentTime = System.currentTimeMillis();
								if (currentTime - value.getRight() > DELTA_INDEXING_TIME) {
									waitedUrls.add(parsedUrl);
									pool.submit(new SendUrl(parsedUrl));
								}
							} else {
								waitedUrls.add(parsedUrl);
								pool.submit(new SendUrl(parsedUrl));
							}
						}
					}
				}
			} catch (ClientProtocolException e) {
				logger.error(e);
			} catch (IOException e) {
				logger.error(e);
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

	public static List<String> findUrlsBinary(String text) {
		List<String> urls = new ArrayList<String>();
		Matcher matcher = URL_PATTERN.matcher(text);
		while (matcher.find()) {
			String url = matcher.group();
			url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
			if (url.startsWith(WWW)) {
				url = "http://" + url;
			} else if (!url.startsWith(HTTP)) {
				url = "http://www." + url;
			}
			try {
				String urlTrue = URLDecoder.decode(url, "UTF8");
				urls.add(urlTrue);
			} catch (UnsupportedEncodingException e) {
				logger.error(e);
			}
		}
		return urls;
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
				url = url.trim();
				url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
				url = url.startsWith(WWW) ? HTTP + "://" + url : url;
				url = url.startsWith("//") ? HTTP + ":" + url : url;
				url = url.startsWith("/") ? baseUrl + url : url;
				if (url.startsWith(HTTP) && checkExtension(url)) {
					try {
						String urlTrue = URLDecoder.decode(url, "UTF8");
						urls.add(urlTrue);
					} catch (UnsupportedEncodingException e) {
						logger.error(e);
					}
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
		urlsMap = SearchManager.getInstance().loadUrlsMap();
		pool.submit(new SendUrl("http://ru.wikipedia.org/wiki/кассини-Гюйгенс"));
		// pool.submit(new
		// SendUrl("http://en.wikipedia.org/wiki/Cassini%E2%80%93Huygens"));
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
			try {
				String urlTrue = URLDecoder.decode(url, "UTF8");
				urls.add(urlTrue);
			} catch (UnsupportedEncodingException e) {
				logger.error(e);
			}
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
