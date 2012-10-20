import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

import util.Util;

public class Indexator {

	public static final int THREADS_COUNT = 10;
	public static final int REQUESTS_COUNT = 10000;
	public static final int TIMEOUT = 10000;

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
				//req.setHeader("Accept-Language", "ru-ru,ru;q=0.8,en-us;q=0.5,en;q=0.3");
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

				if (entity != null) {
					String html = EntityUtils.toString(entity);
					Util.writeText2File(html, "/home/misha-sma/searcherProjects/html/" + Math.random() + ".html");
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

	public static void main(String[] args) {
		List<String> urls = loadUrls();
		pool.submit(new SendUrl("http://www.yandex.ru"));
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
}
