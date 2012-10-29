package misha_sma;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import misha_sma.util.ConfigProperties;
import misha_sma.util.Pair;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.DefaultEncoder;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.search.vectorhighlight.FragListBuilder;
import org.apache.lucene.search.vectorhighlight.FragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.ScoreOrderFragmentsBuilder;
import org.apache.lucene.search.vectorhighlight.SimpleFragListBuilder;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

public class SearchManager {
	private static final Logger logger = Logger.getLogger(SearchManager.class);

	private volatile static SearchManager instance;

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

	public static final String URL = "url";
	public static final String FULLTEXT = "fulltext";
	public static final String HASH = "hash";
	public static final String TIME = "time";

	public static final Version VERSION = Version.LUCENE_40;
	public static final int MAX_QUERY_LENGTH = 256;
	public static final int MAX_HITS_COUNT = 5000;
	public static final int DEFAULT_ROWS_HITS_COUNT = 10;
	public static final int SNIPPET_LENGTH = 100;
	public static final String[] PRE_TAGS = { "<font style=\"background-color: yellow;\">" };
	public static final String[] POST_TAGS = { "</font>" };

	private final Set<String> selectFields = new HashSet<String>();

	private Directory directory;
	private Analyzer analyzer;
	private DirectoryReader dirReader;
	private IndexSearcher isearcher;

	private SearchManager() {
		selectFields.add(URL);
		CharArraySet stopWords = new CharArraySet(VERSION, ConfigProperties.STOP_WORDS, true);
		analyzer = new StandardAnalyzer(VERSION, stopWords);
		File fileDir = new File(ConfigProperties.PATH_2_LUCENE_INDEX);
		if (fileDir.isDirectory() && fileDir.listFiles().length == 0 || !fileDir.isDirectory()) {
			fileDir.mkdirs();
			createDirectory(fileDir);
			clearIndex();
		} else {
			createDirectory(fileDir);
		}
		try {
			dirReader = DirectoryReader.open(directory);
		} catch (IOException e) {
			logger.error("Error while opening DirectoryReader!!!", e);
		}
		isearcher = new IndexSearcher(dirReader);
	}

	private void createDirectory(File fileDir) {
		try {
			directory = NIOFSDirectory.open(fileDir);
		} catch (IOException e) {
			logger.error("Error while creating directory!!!", e);
		}
	}

	public void clearIndex() {
		try {
			IndexWriterConfig iwConfig = new IndexWriterConfig(VERSION, analyzer);
			iwConfig.setOpenMode(OpenMode.CREATE);
			IndexWriter iwriter = new IndexWriter(directory, iwConfig);
			iwriter.close();
		} catch (IOException e) {
			logger.error("Error while clear lucene index!!!", e);
		}
	}

	public void closeIndex() {
		try {
			dirReader.close();
			directory.close();
		} catch (IOException e) {
			logger.error("Error while closing lucene index!!!", e);
		}
		analyzer.close();
	}

	public void search(String query, int number, int rows) throws ParseException, IOException {
		query = queryValidate(query);
		if (query == null) {
			return;
		}

		Query fulltextQuery = (new QueryParser(VERSION, FULLTEXT, analyzer)).parse(query);
		number = number < 1 ? 1 : number;
		int aprioriHitsCount = Math.min(MAX_HITS_COUNT, rows * number);
		TopDocs topDocs = isearcher.search(fulltextQuery, aprioriHitsCount);
		ScoreDoc[] hits = topDocs.scoreDocs;
		rows = rows <= 0 ? DEFAULT_ROWS_HITS_COUNT : rows;
		List<Pair<String, String>> urls = new ArrayList<Pair<String, String>>(rows);

		FastVectorHighlighter fastHighlighter = new FastVectorHighlighter(true, true);
		FragListBuilder fragListBuilder = new SimpleFragListBuilder();
		FragmentsBuilder fragmentsBuilder = new ScoreOrderFragmentsBuilder();
		Encoder encoder = new DefaultEncoder();
		FieldQuery fieldQuery = fastHighlighter.getFieldQuery(fulltextQuery, dirReader);

		for (int i = (number - 1) * rows; i < Math.min(hits.length, number * rows); ++i) {
			int luceneId = hits[i].doc;
			Document doc = dirReader.document(luceneId, selectFields);
			String url = doc.get(URL);
			String snippet = fastHighlighter.getBestFragment(fieldQuery, dirReader, luceneId, FULLTEXT, SNIPPET_LENGTH,
					fragListBuilder, fragmentsBuilder, PRE_TAGS, POST_TAGS, encoder);
			logger.info("url=" + url + "  snippet=" + snippet);
		}

		int realHitsCount = Math.min(topDocs.totalHits, MAX_HITS_COUNT);
		int totalTabsCount = realHitsCount / rows;
		totalTabsCount += realHitsCount % rows == 0 ? 0 : 1;
		System.out.println("totalHitsCount=" + realHitsCount + "  totalTabsCount=" + totalTabsCount);
	}

	protected String queryValidate(String query) {
		if (query == null) {
			return null;
		}
		if (query.trim().isEmpty()) {
			return null;
		}

		query = query.trim();
		query = query.length() > MAX_QUERY_LENGTH ? query.substring(0, MAX_QUERY_LENGTH) : query;

		// ~ * ? in begin query handler
		int i = 0;
		while (i < query.length()
				&& (query.charAt(i) == '~' || query.charAt(i) == '*' || query.charAt(i) == '?' || query.charAt(i) == ' ')) {
			++i;
		}
		if (i == query.length()) {
			return null;
		} else {
			query = query.substring(i, query.length());
		}

		// quotes handler
		int quotesCounter = 0;
		for (i = 0; i < query.length(); ++i) {
			quotesCounter += query.charAt(i) == '"' ? 1 : 0;
		}
		if (quotesCounter % 2 == 1) {
			query = query.replace('"', ' ');
		}

		// wild cards handler
		StringBuilder builder = new StringBuilder();
		StringTokenizer tokenizer = new StringTokenizer(query);
		while (tokenizer.hasMoreTokens()) {
			String word = tokenizer.nextToken();
			if (word.startsWith("\"*") || word.startsWith("\"?")) {
				builder.append('\"').append(word.substring(2));
			} else if (word.startsWith("*") || word.startsWith("?")) {
				builder.append(word.substring(1));
			} else {
				builder.append(word);
			}
			builder.append(' ');
		}
		query = builder.toString().trim();

		// braces handler
		query = checkBraces(query, '(', ')');
		query = checkBraces(query, '[', ']');
		query = checkBraces(query, '{', '}');

		return query;
	}

	private String checkBraces(String query, char openBrace, char closeBrace) {
		StringBuilder bracesBuilder = new StringBuilder();
		boolean isValid = true;
		for (int i = 0; i < query.length(); ++i) {
			int c = query.charAt(i);
			if (c == openBrace) {
				bracesBuilder.append(c);
			}
			if (c == closeBrace) {
				if (bracesBuilder.length() == 0) {
					isValid = false;
					break;
				}
				bracesBuilder.deleteCharAt(bracesBuilder.length() - 1);
			}
		}
		isValid = isValid ? bracesBuilder.length() == 0 : false;
		if (!isValid) {
			query = query.replace(openBrace, ' ');
			query = query.replace(closeBrace, ' ');
		}
		return query;
	}
}
