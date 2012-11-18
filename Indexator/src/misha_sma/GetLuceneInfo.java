package misha_sma;

import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.Logger;

import misha_sma.util.Pair;

public class GetLuceneInfo {
	private static final Logger logger = Logger.getLogger(GetLuceneInfo.class);

	public static void main(String[] args) {
		Map<String, Pair<String, Long>> urlsMap = SearchManager.getInstance().loadUrlsMap();
		SearchManager.getInstance().closeIndex();
		logger.info("Urls count=" + urlsMap.size());
		Url4Sorting[] urlsArray = new Url4Sorting[urlsMap.size()];
		int i = 0;
		for (String url : urlsMap.keySet()) {
			Pair<String, Long> value = urlsMap.get(url);
			String hash = value.getLeft();
			Long time = value.getRight();
			urlsArray[i] = new Url4Sorting(url, hash, time);
			++i;
		}
		Arrays.sort(urlsArray);
		for (Url4Sorting url : urlsArray) {
			logger.info(url);
		}
	}
}
