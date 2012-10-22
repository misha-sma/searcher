package misha_sma.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

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

	public static void writeText2File(String text, String path) {
		try {
			FileOutputStream output = new FileOutputStream(path);
			output.write(text.getBytes("UTF8"));
			output.close();
		} catch (FileNotFoundException e) {
			logger.error(e);
		} catch (UnsupportedEncodingException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		}
	}

	public static void writeBytes2File(byte[] bytes, String path) {
		try {
			FileOutputStream output = new FileOutputStream(path);
			output.write(bytes);
			output.close();
		} catch (FileNotFoundException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		}
	}

	public static String runTika(String[] args, File folder) {
		String text = null;
		try {
			Process process = Runtime.getRuntime().exec(args, null, folder);
			Thread threadError = readProcessErrorOutput(process.getErrorStream());
			text = readTikaOutput(process.getInputStream());
			threadError.join();
			int status = process.waitFor();
			if (status == 0) {
				logger.info("PROCESS STATUS=" + status);
			} else {
				logger.error("Process exit with status " + status + " !!!");
			}
		} catch (IOException e) {
			logger.error(e);
		} catch (InterruptedException e) {
			logger.error(e);
		}
		return text;
	}

	public static void runProcess(String[] args, File folder) {
		try {
			Process process = Runtime.getRuntime().exec(args, null, folder);
			Thread thread = readProcessOutput(process.getInputStream());
			Thread threadError = readProcessErrorOutput(process.getErrorStream());
			thread.join();
			threadError.join();
			int status = process.waitFor();
			if (status == 0) {
				logger.info("PROCESS STATUS=" + status);
			} else {
				logger.error("Process exit with status " + status + " !!!");
			}
		} catch (IOException e) {
			logger.error(e);
		} catch (InterruptedException e) {
			logger.error(e);
		}
	}

	public static String readTikaOutput(final InputStream is) throws InterruptedException {
		final StringBuilder builder = new StringBuilder();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line = null;
		try {
			while ((line = br.readLine()) != null) {
				builder.append(line).append('\n');
			}
		} catch (IOException e) {
			logger.error("IOException while reading process output", e);
		} finally {
			try {
				br.close();
				isr.close();
				is.close();
			} catch (IOException e) {
				logger.error("IOException while closing reading process output", e);
			}
		}
		return builder.toString();
	}

	public static Thread readProcessOutput(final InputStream is) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				try {
					while ((line = br.readLine()) != null) {
						logger.info(line);
					}
				} catch (IOException e) {
					logger.error("IOException while reading process output", e);
				} finally {
					try {
						br.close();
						isr.close();
						is.close();
					} catch (IOException e) {
						logger.error("IOException while closing reading process output", e);
					}
				}
			}
		});
		thread.start();
		return thread;
	}

	public static Thread readProcessErrorOutput(final InputStream is) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				try {
					while ((line = br.readLine()) != null) {
						logger.error(line);
					}
				} catch (IOException e) {
					logger.error("IOException while reading error process output", e);
				} finally {
					try {
						br.close();
						isr.close();
						is.close();
					} catch (IOException e) {
						logger.error("IOException while closing reading error process output", e);
					}
				}
			}
		});
		thread.start();
		return thread;
	}
}
