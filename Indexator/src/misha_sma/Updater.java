package misha_sma;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import misha_sma.util.Util;

public class Updater {
	public static final int RANDOM_COUNT = 1000;
	public static final int THREADS_COUNT = 8;
	public static final int TIMEOUT = 10000;
	public static Map<String, String> urlsHashMap;
	public static final Object GLOBAL_SYNCHRONIZE_OBJECT = new Object();
	private final static ExecutorService pool = Executors.newFixedThreadPool(THREADS_COUNT);

	private static final Logger logger = Logger.getLogger(Updater.class);

	private static double avgTikaTime = 0;
	private static volatile double avgAllTime = 0;
	private static long totalTime = System.currentTimeMillis();

	private static class SendUrl implements Runnable {
		private String url;

		private SendUrl(String url) {
			this.url = url;
		}

		@Override
		public void run() {
			long initTimeAll = System.currentTimeMillis();
			boolean isOk = false;
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

					String text = TikaTextExtractor.extractText(fullName);

					long tikaTime = System.currentTimeMillis() - initTime;
					logger.info("==End run tika Time=" + tikaTime);
					avgTikaTime += tikaTime;
					Util.writeText2File(text, textName);

					String hash = Util.getMD5(text);
					logger.info("hash=" + hash);
					if (hash == null) {
						logger.error("Error!!! Hash is null!!!");
						return;
					}
					synchronized (GLOBAL_SYNCHRONIZE_OBJECT) {
						String oldHash = urlsHashMap.get(url);
						if (!oldHash.equals(hash)) {
							SearchManager.getInstance().updateUrl(url, text, hash, System.currentTimeMillis());
							logger.info("==Update url " + url + " in lucene");
						}
						isOk = true;
					}
				}
			} catch (ClientProtocolException e) {
				logger.error(e);
			} catch (IOException e) {
				logger.error(e);
			} finally {
				if (!isOk) {
					synchronized (GLOBAL_SYNCHRONIZE_OBJECT) {
						SearchManager.getInstance().deleteUrl(url);
					}
				}
				httpclient.getConnectionManager().shutdown();
				long timeAll = System.currentTimeMillis() - initTimeAll;
				avgAllTime += timeAll;
			}
		}
	}

	public static boolean isBinary(String mimeType) {
		return !(mimeType.contains("html") || mimeType.contains("text"));
	}

	public static void main(String[] args) {
		urlsHashMap = SearchManager.getInstance().loadUrlsHashMap();
		List<Future<?>> futures = new LinkedList<Future<?>>();
		for (String url : urlsHashMap.keySet()) {
			Future<?> future = pool.submit(new SendUrl(url));
			futures.add(future);
		}

		try {
			for (Future<?> future : futures) {
				future.get();
			}
		} catch (InterruptedException e) {
			logger.error("Error while futures get()!!!", e);
		} catch (ExecutionException e) {
			logger.error("Error while futures get()!!!", e);
		}

		SearchManager.getInstance().optimizeIndex();
		SearchManager.getInstance().closeIndex();
		logger.info("END UPDATING!!!");
		logger.info("average tika time=" + avgTikaTime / urlsHashMap.size() + " ms");
		logger.info("average all time=" + avgAllTime / urlsHashMap.size() + " ms");
		logger.info("Total time=" + (System.currentTimeMillis() - totalTime) + " ms");
	}

}
