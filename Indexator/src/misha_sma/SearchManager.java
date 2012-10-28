package misha_sma;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
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

	private final Set<String> selectFields = new HashSet<String>();

	private Directory directory;
	private Analyzer analyzer;

	private static Lock luceneWriteLock = new ReentrantLock();

	private SearchManager() {
		selectFields.add(URL);
		selectFields.add(TIME);
		selectFields.add(HASH);
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
	}

	private void createDirectory(File fileDir) {
		try {
			directory = NIOFSDirectory.open(fileDir);
		} catch (IOException e) {
			logger.error(e);
		}
	}

	public void clearIndex() {
		try {
			IndexWriterConfig iwConfig = new IndexWriterConfig(VERSION, analyzer);
			iwConfig.setOpenMode(OpenMode.CREATE);
			IndexWriter iwriter = new IndexWriter(directory, iwConfig);
			iwriter.close();
		} catch (CorruptIndexException e) {
			logger.error(e);
		} catch (LockObtainFailedException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
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

		IndexWriter iwriter = null;
		try {
			IndexWriterConfig iwConfig = new IndexWriterConfig(VERSION, analyzer);
			iwConfig.setOpenMode(OpenMode.APPEND);
			luceneWriteLock.lock();
			iwriter = new IndexWriter(directory, iwConfig);
			iwriter.addDocument(doc);
		} finally {
			if (iwriter != null) {
				iwriter.close();
			}
			luceneWriteLock.unlock();
		}
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

		IndexWriter iwriter = null;
		try {
			IndexWriterConfig iwConfig = new IndexWriterConfig(VERSION, analyzer);
			iwConfig.setOpenMode(OpenMode.APPEND);
			luceneWriteLock.lock();
			iwriter = new IndexWriter(directory, iwConfig);
			iwriter.updateDocument(new Term(URL, url), doc);
		} finally {
			if (iwriter != null) {
				iwriter.close();
			}
			luceneWriteLock.unlock();
		}
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

	public Map<String, Pair<String, Long>> loadUrlsMap() {
		long initTime = System.currentTimeMillis();
		Map<String, Pair<String, Long>> urlMap = new HashMap<String, Pair<String, Long>>();
		try {
			DirectoryReader dirReader = DirectoryReader.open(directory);
			for (int id = 0; id < dirReader.maxDoc(); ++id) {
				Document doc = dirReader.document(id, selectFields);
				if (doc == null) {
					logger.error("Error!!! Document with id=" + id + " is null!");
					continue;
				}

				String url = doc.get(URL);
				String hash = doc.get(HASH);
				Long time = (Long) doc.getField(TIME).numericValue();
				urlMap.put(url, new Pair<String, Long>(hash, time));
			}
		} catch (IOException e) {
			logger.error("Error while load urls map!!!", e);
		}
		logger.info("LOAD URLS TIME=" + (System.currentTimeMillis() - initTime));
		return urlMap;
	}
}
