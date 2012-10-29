package misha_sma.test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class TestEncodeUrls {
	public static void main(String[] args) {
//		String url = "http://ru.wikipedia.org/wiki/Оттава";
//		try {
//			String urlTrue = URLEncoder.encode(url, "UTF8");
//			System.out.println(urlTrue);
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
		
		//String url = "http://en.wikipedia.org/wiki/Cassini%E2%80%93Huygens";
	//	String url = "http://ru.wikipedia.org/wiki/Заглавная_страница";
//		String url = "http://ru.wikipedia.org/wiki/%D0%97%D0%B0%D0%B3%D0%BB%D0%B0%D0%B2%D0%BD%D0%B0%D1%8F_%D1%81%D1%82%D1%80%D0%B0%D0%BD%D0%B8%D1%86%D0%B0";
		String url = "http://localhost:8080/%26%231075%3B%26%231102%3B%26%231081%3B%26%231075%3B%26%231077%3B%26%231085%3B%26%231089%3B";
		try {
			String urlTrue = URLDecoder.decode(url, "UTF8");
			System.out.println(urlTrue);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
}
