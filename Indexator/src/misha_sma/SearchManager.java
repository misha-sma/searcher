package misha_sma;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import misha_sma.util.ConfigProperties;
import misha_sma.util.Util;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
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
	public static final String COUNT = "count";

	public static final Version version = Version.LUCENE_40;

	private Directory directory;
	private Analyzer analyzer;

	private static Lock luceneWriteLock = new ReentrantLock();

	private SearchManager() {
		CharArraySet stopWords = new CharArraySet(version, ConfigProperties.STOP_WORDS, true);
		analyzer = new StandardAnalyzer(version, stopWords);
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
			directory = FSDirectory.open(fileDir);
		} catch (IOException e) {
			logger.error(e);
		}
	}

	public void clearIndex() {
		try {
			IndexWriterConfig iwConfig = new IndexWriterConfig(version, analyzer);
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

}
