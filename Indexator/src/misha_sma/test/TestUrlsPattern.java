package misha_sma.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestUrlsPattern {
	public static final String DOMENS = "(com|edu|gov|mil|net|org|info|su|af|al|dz|as|ad|ao|ai|aq|ag|ar|am|aw|au|at|az|bs|bh|bd|bb|by|be|bz|bj|bm|bt|bo|ba|bw|bv|br|io|bn|bg|bf|bi|kh|cm|ca|cv|ky|cf|td|cl|cn|cx|cc|co|km|cg|ck|cr|ci|hr|cu|cy|cz|dk|dj|dm|do|tp|ec|eg|sv|gq|er|ee|et|fk|fo|fj|fi|fr|fx|gf|pf|tf|ga|gm|ge|de|gh|gi|gr|gl|gd|gp|gu|gt|gg|gn|gw|gy|ht|hm|hn|hk|hu|is|in|id|ir|iq|ie|im|il|it|jm|jp|je|jo|kz|ke|ki|kp|kr|kw|kg|la|lv|lb|ls|lr|ly|li|lt|lu|mo|mk|mg|mw|my|mv|ml|mt|mh|mq|mr|mu|yt|mx|fm|md|mc|mn|ms|ma|mz|mm|na|nr|np|nl|an|nc|nz|ni|ne|ng|nu|nf|mp|no|om|pk|pw|pa|pg|py|pe|ph|pn|pl|pt|pr|qa|re|ro|ru|rw|kn|lc|vc|ws|sm|st|sa|sn|sc|sl|sg|sk|si|sb|so|za|gs|es|lk|sh|no|sd|sr|sj|sz|se|ch|sy|tw|tj|tz|th|tg|tk|to|tt|tn|tr|tm|tc|tv|ug|ua|ae|uk|us|um|uy|uz|vu|va|ve|vn|vg|vi|wf|eh|ye|yu|zr|zm|zw)";
	public static final String REGEXP_URL = "((http|https)\\u003A\\u002F{2})?[\\p{Alnum}\\u002E\\u002D\\u005F]{4,255}[\\u002E]"
			+ DOMENS
			+ "((\\u002F)[\\p{Alnum}\\u002D\\u0026\\u002B\\u005F\\u003F\\u003D\\u0023\\u0025\\u002F\\u002E]*)?";
	public static final Pattern URL_PATTERN = Pattern.compile(REGEXP_URL);

	public static void main(String[] args) {
		String url1 = "http://www.google.com";
		String url2 = "http://google.com";
		String url3 = "www.google.com";
		String url4 = "google.com";
		String url5 = "ftp://www.google.com";
		String url6 = "ftp://google.com";
		String url7 = "https://www.google.com";
		String url8 = "https://google.com";

		Matcher matcher = URL_PATTERN.matcher(url1);
		System.out.println("1=" + matcher.matches());

		matcher = URL_PATTERN.matcher(url2);
		System.out.println("2=" + matcher.matches());

		matcher = URL_PATTERN.matcher(url3);
		System.out.println("3=" + matcher.matches());

		matcher = URL_PATTERN.matcher(url4);
		System.out.println("4=" + matcher.matches());

		matcher = URL_PATTERN.matcher(url5);
		System.out.println("5=" + matcher.matches());

		matcher = URL_PATTERN.matcher(url6);
		System.out.println("6=" + matcher.matches());

		matcher = URL_PATTERN.matcher(url7);
		System.out.println("7=" + matcher.matches());

		matcher = URL_PATTERN.matcher(url8);
		System.out.println("8=" + matcher.matches());
	}
}
