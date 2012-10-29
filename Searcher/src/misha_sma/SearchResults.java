package misha_sma;

import java.util.List;

import misha_sma.util.Pair;

public class SearchResults {
	private List<Pair<String, String>> urls;
	private int totalHitsCount;

	public SearchResults(List<Pair<String, String>> urls, int totalHitsCount) {
		this.urls = urls;
		this.totalHitsCount = totalHitsCount;
	}

	public List<Pair<String, String>> getUrls() {
		return urls;
	}

	public void setUrls(List<Pair<String, String>> urls) {
		this.urls = urls;
	}

	public int getTotalHitsCount() {
		return totalHitsCount;
	}

	public void setTotalHitsCount(int totalHitsCount) {
		this.totalHitsCount = totalHitsCount;
	}
}
