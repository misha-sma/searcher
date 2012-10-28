package misha_sma.test;

import java.io.File;
import java.io.IOException;

import misha_sma.util.ConfigProperties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

public class TestIndexWriter {
	public static final String URL = "url";
	public static final String FULLTEXT = "fulltext";
	public static final String HASH = "hash";
	public static final String TIME = "time";

	public static final Version VERSION = Version.LUCENE_40;

	private static Directory directory;
	private static Analyzer analyzer;

	public static void main(String[] args) {
		analyzer = new StandardAnalyzer(VERSION);
		File fileDir = new File(ConfigProperties.PATH_2_LUCENE_INDEX);
		if (fileDir.isDirectory() && fileDir.listFiles().length == 0 || !fileDir.isDirectory()) {
			fileDir.mkdirs();
			createDirectory(fileDir);
			clearIndex();
		} else {
			createDirectory(fileDir);
		}

		Document doc = new Document();
		StringField fldUrl = new StringField(URL, "uuuuu", Store.YES);
		StringField fldHash = new StringField(HASH, "hhhhh", Store.YES);
		LongField fldTime = new LongField(TIME, 123123123, Store.YES);

		doc.add(fldUrl);
		doc.add(fldHash);
		doc.add(fldTime);

		IndexWriter iwriter = null;
		try {
			IndexWriterConfig iwConfig = new IndexWriterConfig(VERSION, analyzer);
			iwConfig.setOpenMode(OpenMode.APPEND);
			iwriter = new IndexWriter(directory, iwConfig);
			System.out.println("11111111111");
			iwriter.addDocument(doc);
			System.out.println("222222222222");
			iwriter.commit();
			System.out.println("3333333");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (iwriter != null) {
				try {
					iwriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void createDirectory(File fileDir) {
		try {
			directory = NIOFSDirectory.open(fileDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void clearIndex() {
		try {
			IndexWriterConfig iwConfig = new IndexWriterConfig(VERSION, analyzer);
			iwConfig.setOpenMode(OpenMode.CREATE);
			IndexWriter iwriter = new IndexWriter(directory, iwConfig);
			iwriter.close();
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (LockObtainFailedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
