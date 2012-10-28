package misha_sma.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;

public class Util {
	private static final Logger logger = Logger.getLogger(Util.class);

	private Util() {
	}

	public static String loadText(File file) {
		try {
			FileInputStream input = new FileInputStream(file);
			byte[] bytes = new byte[(int) file.length()];
			input.read(bytes);
			input.close();
			return new String(bytes);
		} catch (FileNotFoundException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		}
		return null;
	}

	public static String loadText(String fileName) {
		return loadText(new File(fileName));
	}
}
