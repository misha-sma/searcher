package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Util {
	public static String loadText(File file) {
		FileInputStream input;
		try {
			input = new FileInputStream(file);
			byte[] bytes = new byte[(int) file.length()];
			input.read(bytes);
			input.close();
			String text = new String(bytes);
			return text;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String loadText(String fileName) {
		return loadText(new File(fileName));
	}

	public static void writeText2File(String text, String path) {
		try {
			FileOutputStream output = new FileOutputStream(path);
			output.write(text.getBytes("UTF8"));
			output.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
