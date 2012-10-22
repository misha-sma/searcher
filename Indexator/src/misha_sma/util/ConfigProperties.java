package misha_sma.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

public class ConfigProperties {
	private static final Logger logger = Logger.getLogger(ConfigProperties.class);

	public static final String HTML_FORMAT = "html";
	public static final String PATH_2_LUCENE_INDEX;
	public static final String PATH_2_HTML;
	public static final String PATH_2_FULLTEXT;
	public static final Set<String> STOP_WORDS = new HashSet<String>(510);

	static {
		Properties props = new Properties();
		try {
			FileInputStream input = new FileInputStream("config/config.properties");
			props.load(input);
		} catch (IOException e) {
			logger.error("Error: Can't load config.properties!", e);
		}

		PATH_2_LUCENE_INDEX = validatePath(props.getProperty("path2LuceneIndex"));
		PATH_2_HTML = validatePath(props.getProperty("path2Html"));
		PATH_2_FULLTEXT = validatePath(props.getProperty("path2Text"));

		String stopWordsStr = Util.loadText("config/stopwords.txt");
		StringTokenizer tokenizer = new StringTokenizer(stopWordsStr);
		while (tokenizer.hasMoreTokens()) {
			STOP_WORDS.add(tokenizer.nextToken());
		}

		System.out.println(PATH_2_LUCENE_INDEX + "  " + PATH_2_HTML + "  " + PATH_2_FULLTEXT);
	}

	private ConfigProperties() {
	}

	private static String validatePath(String path) {
		return path == null ? null : path.endsWith("/") || path.endsWith("\\") ? path.substring(0, path.length() - 1)
				: path;
	}
}
