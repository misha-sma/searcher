package misha_sma;

import java.util.List;

import misha_sma.util.Pair;

public class SearchResults {
	private List<Pair<String, String>> urls;
	private int totalHitsCount;
	private int totalTabsCount;

	public SearchResults(List<Pair<String, String>> urls, int totalHitsCount, int totalTabsCount) {
		this.urls = urls;
		this.totalHitsCount = totalHitsCount;
		this.totalTabsCount = totalTabsCount;
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

	public int getTotalTabsCount() {
		return totalTabsCount;
	}

	public void setTotalTabsCount(int totalTabsCount) {
		this.totalTabsCount = totalTabsCount;
	}
}
