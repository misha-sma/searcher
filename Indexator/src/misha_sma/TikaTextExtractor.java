package misha_sma;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.BoilerpipeContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class TikaTextExtractor {
	private static final Logger logger = Logger.getLogger(TikaTextExtractor.class);

	public static String extractText(String path) {
		ParseContext context = new ParseContext();
		Detector detector = new DefaultDetector();
		Parser parser = new AutoDetectParser(detector);
		context.set(Parser.class, parser);
		OutputStream output = new ByteArrayOutputStream();
		URL url;
		File file = new File(path);
		InputStream input = null;
		try {
			if (file.isFile()) {
				url = file.toURI().toURL();
			} else {
				url = new URL(path);
			}
			Metadata metadata = new Metadata();
			input = TikaInputStream.get(url, metadata);
			ContentHandler handler = new BoilerpipeContentHandler(new OutputStreamWriter(output, "UTF-8"));
			parser.parse(input, handler, metadata, context);
			return output.toString();
		} catch (MalformedURLException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		} catch (SAXException e) {
			logger.error(e);
		} catch (TikaException e) {
			logger.error(e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					logger.error(e);
				}
			}
		}
		return "";
	}

}
