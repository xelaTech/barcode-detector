package com.xelatech.barcode.localizer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class Test {
	public static void main(final String[] args) {
		final long startTime = System.currentTimeMillis();

		System.out.println("PdfBoxBarcodeScannerTest starts...");

		try {
//			final URL url = Thread.currentThread().getContextClassLoader().getResource("barcodes/test_4.pdf");
			final URL url = new URL("file://" + args[0]);
			PDDocumentBarcodeScanner scanner = new PDDocumentBarcodeScanner(new File(url.toURI()));

			// scanner.scan(1);
			scanner.scan();

			System.out.println("PDF scanned in " + (System.currentTimeMillis() - startTime) + " ms");

			scanner.displayResults();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
