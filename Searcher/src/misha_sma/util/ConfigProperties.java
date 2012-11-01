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

	public static final int PORT;
	public static final String HTML_FORMAT = "html";
	public static final String PATH_2_LUCENE_INDEX;
	public static final String PATH_2_SUGGESTING_INDEX;
	public static final Set<String> STOP_WORDS = new HashSet<String>(510);

	static {
		Properties props = new Properties();
		try {
			FileInputStream input = new FileInputStream("config/config.properties");
			props.load(input);
		} catch (IOException e) {
			logger.error("Error: Can't load config.properties!", e);
		}

		PORT = Integer.parseInt(props.getProperty("port"));
		PATH_2_LUCENE_INDEX = validatePath(props.getProperty("path2LuceneIndex"));
		PATH_2_SUGGESTING_INDEX = validatePath(props.getProperty("path2SuggestingIndex"));

		String stopWordsStr = Util.loadText("config/stopwords.txt");
		StringTokenizer tokenizer = new StringTokenizer(stopWordsStr);
		while (tokenizer.hasMoreTokens()) {
			STOP_WORDS.add(tokenizer.nextToken());
		}

		logger.info(PATH_2_LUCENE_INDEX + "  " + PATH_2_SUGGESTING_INDEX);
	}

	private ConfigProperties() {
	}

	private static String validatePath(String path) {
		return path == null ? null : path.endsWith("/") || path.endsWith("\\") ? path.substring(0, path.length() - 1)
				: path;
	}
}
