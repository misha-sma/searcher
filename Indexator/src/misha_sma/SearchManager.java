package misha_sma;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import misha_sma.util.ConfigProperties;
import misha_sma.util.Pair;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
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
	public static final int MEMORY = 64;

	private final Set<String> selectFieldsSet = new HashSet<String>();
	private final Set<String> selectFieldsMap = new HashSet<String>();
	private final Set<String> selectFieldsHashMap = new HashSet<String>();

	private Directory directory;
	private Analyzer analyzer;
	private IndexWriter iwriter;

	private SearchManager() {
		selectFieldsSet.add(URL);
		selectFieldsMap.add(URL);
		selectFieldsMap.add(TIME);
		selectFieldsMap.add(HASH);
		selectFieldsHashMap.add(URL);
		selectFieldsHashMap.add(HASH);
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
		IndexWriterConfig iwConfig = new IndexWriterConfig(VERSION, analyzer);
		iwConfig.setOpenMode(OpenMode.APPEND);
		iwConfig.setRAMBufferSizeMB(MEMORY);
		try {
			iwriter = new IndexWriter(directory, iwConfig);
		} catch (IOException e) {
			logger.error("Error while opening IndexWriter!!!", e);
		}
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
			iwriter.close();
		} catch (IOException e) {
			logger.error("Error while close IndexWriter!!!", e);
		}
	}

	public void addUrlToIndex(String url, String fulltext, String hash, long time) throws IOException {
		Document doc = new Document();
		StringField fldUrl = new StringField(URL, url, Store.YES);
		StringField fldHash = new StringField(HASH, hash, Store.YES);
		LongField fldTime = new LongField(TIME, time, Store.YES);
		Field fldFulltext = createFulltextField(fulltext);

		doc.add(fldUrl);
		doc.add(fldHash);
		doc.add(fldTime);
		doc.add(fldFulltext);

		iwriter.addDocument(doc);
	}

	public void updateUrl(String url, String fulltext, String hash, long time) throws IOException {
		Document doc = new Document();
		StringField fldUrl = new StringField(URL, url, Store.YES);
		StringField fldHash = new StringField(HASH, hash, Store.YES);
		LongField fldTime = new LongField(TIME, time, Store.YES);
		Field fldFulltext = createFulltextField(fulltext);

		doc.add(fldUrl);
		doc.add(fldHash);
		doc.add(fldTime);
		doc.add(fldFulltext);

		iwriter.updateDocument(new Term(URL, url), doc);
	}

	private Field createFulltextField(String fulltext) {
		FieldType type = new FieldType();
		type.setIndexed(true);
		type.setOmitNorms(true);
		type.setStored(true);
		type.setStoreTermVectorOffsets(true);
		type.setStoreTermVectorPositions(true);
		type.setStoreTermVectors(true);
		return new Field(FULLTEXT, fulltext, type);
	}

	public void optimizeIndex() {
		long initTime = System.currentTimeMillis();
		try {
			iwriter.forceMerge(1);
		} catch (IOException e) {
			logger.error("Error while optimizing index!!!", e);
		}
		logger.info("Optimizing time=" + (System.currentTimeMillis() - initTime) + " ms");
	}

	public Map<String, Pair<String, Long>> loadUrlsMap() {
		long initTime = System.currentTimeMillis();
		Map<String, Pair<String, Long>> urlMap = new HashMap<String, Pair<String, Long>>();
		try {
			DirectoryReader dirReader = DirectoryReader.open(directory);
			for (int id = 0; id < dirReader.maxDoc(); ++id) {
				Document doc = dirReader.document(id, selectFieldsMap);
				if (doc == null) {
					logger.error("Error!!! Document with id=" + id + " is null!");
					continue;
				}

				String url = doc.get(URL);
				String hash = doc.get(HASH);
				Long time = (Long) doc.getField(TIME).numericValue();
				urlMap.put(url, new Pair<String, Long>(hash, time));
			}
			dirReader.close();
		} catch (IOException e) {
			logger.error("Error while load urls map!!!", e);
		}
		logger.info("LOAD URLS TIME=" + (System.currentTimeMillis() - initTime));
		return urlMap;
	}

	public Map<String, String> loadUrlsHashMap() {
		long initTime = System.currentTimeMillis();
		Map<String, String> urlsMap = null;
		try {
			DirectoryReader dirReader = DirectoryReader.open(directory);
			IndexSearcher isearcher = new IndexSearcher(dirReader);
			NumericRangeQuery<Long> query = NumericRangeQuery.newLongRange(TIME, null, System.currentTimeMillis()
					- ConfigProperties.UPDATE_TIME_INTERVAL, false, false);
			TopDocs topDocs = isearcher.search(query, dirReader.maxDoc());
			logger.info("Searched " + topDocs.totalHits + " urls");
			urlsMap = new HashMap<String, String>(topDocs.totalHits);
			for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
				Document doc = dirReader.document(scoreDoc.doc, selectFieldsHashMap);
				if (doc == null) {
					logger.error("Error!!! Document with id=" + scoreDoc.doc + " is null!");
					continue;
				}

				String url = doc.get(URL);
				String hash = doc.get(HASH);
				urlsMap.put(url, hash);
			}
			dirReader.close();
		} catch (IOException e) {
			logger.error("Error while load urls hashmap!!!", e);
		}
		logger.info("LOAD URLS TIME=" + (System.currentTimeMillis() - initTime));
		return urlsMap;
	}

	public Set<String> loadUrlsSet() {
		long initTime = System.currentTimeMillis();
		Set<String> urlsSet = new HashSet<String>();
		try {
			DirectoryReader dirReader = DirectoryReader.open(directory);
			for (int id = 0; id < dirReader.maxDoc(); ++id) {
				Document doc = dirReader.document(id, selectFieldsSet);
				if (doc == null) {
					logger.error("Error!!! Document with id=" + id + " is null!");
					continue;
				}

				String url = doc.get(URL);
				urlsSet.add(url);
			}
			dirReader.close();
		} catch (IOException e) {
			logger.error("Error while load urls set!!!", e);
		}
		logger.info("LOAD URLS TIME=" + (System.currentTimeMillis() - initTime));
		return urlsSet;
	}
}
