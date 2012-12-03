package misha_sma;

public class Url4Sorting implements Comparable<Url4Sorting> {
	private String url;
	private long time;

	public Url4Sorting(String url, long time) {
		this.url = url;
		this.time = time;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	@Override
	public int compareTo(Url4Sorting url) {
		return time > url.time ? 1 : time < url.time ? -1 : 0;
	}

	@Override
	public String toString() {
		return "url=" + url + "  time=" + time;
	}

}
