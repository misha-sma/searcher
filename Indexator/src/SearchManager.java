public class SearchManager {
	private SearchManager() {
	}

	private static SearchManager instance;

	public static SearchManager getInstance() {
		if (instance == null) {
			synchronized (SearchManager.class) {
				if (instance == null) {
					instance = new SearchManager();
				}
			}
		}
		return instance;
	}
}
