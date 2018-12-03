package com.xelatech.barcode.localizer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.datamatrix.DataMatrixReader;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.oned.Code128Reader;

/**
 * free to use or copy
 * 
 * @author David KELLER <david.keller.fr@gmail.com>
 */
public class PDDocumentBarcodeScanner {
	private PDDocument pdDocument;

	private int maximumBlankPixelDelimiterCount = 20;

	private List<PDPageBarcodeScanner> pageScannerList;

	public PDDocumentBarcodeScanner(final File file) throws IOException {
		final FileInputStream pdfInputStream = new FileInputStream(file);
		this.pdDocument = PDDocument.load(pdfInputStream);
		pdfInputStream.close();

		this.pageScannerList = new ArrayList<PDPageBarcodeScanner>();
	}

	/**
	 * Scans all pages of the PDF document.
	 *
	 * @throws IOException
	 */
	public void scan() throws IOException {
		int numberOfPages = this.pdDocument.getNumberOfPages();
		for (int pageNumber = 0; pageNumber < numberOfPages; ++pageNumber) {
			final PDPageBarcodeScanner pageScanner = new PDPageBarcodeScanner(this.pdDocument.getPage(pageNumber),
					pageNumber, this.maximumBlankPixelDelimiterCount);
			// final PDPageBarcodeScannerInit pageScanner = new
			// PDPageBarcodeScannerInit(this.pdDocument.getPage(pageNumber),
			// pageNumber, this.maximumBlankPixelDelimiterCount);

			this.pageScannerList.add(pageScanner);
			pageScanner.scan();
		}
	}

	/**
	 * Scans page with the given number.
	 * 
	 * @param pageNumber number of the page to be scanned.
	 * @throws IOException
	 */
	public void scan(int pageNumber) throws IOException {
		final PDPageBarcodeScanner pageScanner = new PDPageBarcodeScanner(this.pdDocument.getPage(pageNumber),
				pageNumber, this.maximumBlankPixelDelimiterCount);
		// final PDPageBarcodeScannerInit pageScanner = new
		// PDPageBarcodeScannerInit(this.pdDocument.getPage(pageNumber),
		// pageNumber, this.maximumBlankPixelDelimiterCount);

		this.pageScannerList.add(pageScanner);
		pageScanner.scan();
	}

	public void displayResults() {
		for (final PDPageBarcodeScanner pageScanner : this.pageScannerList) {
			pageScanner.displayResults();
		}
	}

	private class PDPageBarcodeScanner {
		private static final String WS_PATH = "/tmp/barcode";

		protected PDPage pdPage;

		protected int pageNumber;

		protected int maximumBlankPixelDelimiterCount = 20;

		// protected List<Result> results = new ArrayList<Result>();
		// protected Set<Result> results = new HashSet<Result>();
		protected Map<String, Result> results = new HashMap<String, Result>();

		private Map<DecodeHintType, Object> decodeHintTypes = new Hashtable<DecodeHintType, Object>();

		private Reader specificReader;

		protected GenericMultipleBarcodeReader reader;

		private int imageCounter = 1;

		private int subImageCounter = 1;

		public int getPageNumber() {
			return this.pageNumber;
		}

		public PDPage getPdPage() {
			return this.pdPage;
		}

		public int getMaximumBlankPixelDelimiterCount() {
			return this.maximumBlankPixelDelimiterCount;
		}

		public void setMaximumBlankPixelDelimiterCount(final int maximumBlankPixelDelimiterCount) {
			this.maximumBlankPixelDelimiterCount = maximumBlankPixelDelimiterCount;
		}

		public List<Result> getResultList() {
			List<Result> resultList = Collections.emptyList();
			resultList.addAll(this.results.values());

			return resultList;
		}

		public PDPageBarcodeScanner(final PDPage pdPage, final int pageNumber,
				final int maximumBlankPixelDelimiterCount) {
			decodeHintTypes.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
			// decodeHintTypes.put(DecodeHintType.POSSIBLE_FORMATS,
			// Arrays.asList(BarcodeFormat.CODE_128, BarcodeFormat.DATA_MATRIX));

			this.pdPage = pdPage;
			this.pageNumber = pageNumber;
			this.maximumBlankPixelDelimiterCount = maximumBlankPixelDelimiterCount;

			// Build default barcode reader.
			this.specificReader = new DataMatrixReader();
			// this.specificReader = new Code128Reader();
			this.reader = new GenericMultipleBarcodeReader(this.specificReader);
		}

		public void scan() throws IOException {
			final PDResources pdResources = this.pdPage.getResources();

			for (final COSName key : pdResources.getXObjectNames()) {
				final PDXObject xobject = pdResources.getXObject(key);
				if (xobject instanceof PDImageXObject) {
					final PDImageXObject imageObject = (PDImageXObject) xobject;
					final String suffix = imageObject.getSuffix();
					if (suffix != null) {
						// if ("jpx".equals(suffix))
						// {
						// suffix = "JPEG2000";
						// }

						final BufferedImage image = imageObject.getImage();

						try {
							// String writerNames[] = ImageIO.getWriterFormatNames();
							// ImageIO.write(image, "png",
							// new File(PdPageBarcodeScanner.WS_PATH + "/test_4_p_" + this.pageNumber + "."
							// + "png"));
						} catch (Exception e) {
							e.printStackTrace();
						}

						this.extractBarcodeArrayByAreas(image, this.maximumBlankPixelDelimiterCount);
					}
				}
			}
		}

		public void displayResults() {
			for (final Result result : this.results.values()) {
				System.out.println("\nPage = " + (this.pageNumber + 1) + ", barcode format = "
						+ result.getBarcodeFormat() + ", value = " + result.getText());
			}
		}

		/**
		 * Get area list by color.
		 * 
		 * @param in
		 * @param out                             contains the initial
		 *                                        <code>BufferedImage</code> with
		 *                                        rectangle marking the bar code area
		 *                                        for debugging purposes.
		 * @param redColor
		 * @param greenColor
		 * @param blueColor
		 * @param maximumBlankPixelDelimiterCount define the same area until number of
		 *                                        blank pixel is not greater than
		 *                                        maximumBlankPixelDelimiterCount
		 * @return
		 * @throws IOException
		 */
		public List<Rectangle> getAllAreaByColor(final BufferedImage in, final BufferedImage out, final int redColor,
				final int greenColor, final int blueColor, final int maximumBlankPixelDelimiterCount,
				final boolean debug) throws IOException {
			final int w = in.getWidth();
			final int h = in.getHeight();

			Graphics2D gc = null;
			if (out != null) {
				gc = out.createGraphics();
				gc.setColor(new Color(1f, 0f, 0f));
			}

			final int maximumBlankPixelDelimiterCountDouble = maximumBlankPixelDelimiterCount * 2;

			final List<Rectangle> areaList = new ArrayList<Rectangle>();
			int pixel;
			for (int x = 0; x < w; x++) {
				for (int y = 0; y < h; y++) {
					pixel = in.getRGB(x, y);
					final int alpha = ((pixel >> 24) & 0xFF);
					final int red = ((pixel >> 16) & 0xFF);
					final int green = ((pixel >> 8) & 0xFF);
					final int blue = (pixel & 0xFF);

					if ((red == redColor) && (green == greenColor) && (blue == blueColor)) {
						final Rectangle rect = new Rectangle(x - maximumBlankPixelDelimiterCount,
								y - maximumBlankPixelDelimiterCount, maximumBlankPixelDelimiterCountDouble,
								maximumBlankPixelDelimiterCountDouble);

						boolean isInArea = false;
						for (final Rectangle rectangle : areaList) {
							if (rectangle.contains(x, y) == true) {
								rectangle.add(rect);
								isInArea = true;
								break;
							}
						}

						if (isInArea == true) {
							continue;
						}

						// Get pixel colors.
						pixel = pixel & 0x00000000;
						pixel = pixel | (alpha << 24);
						pixel = pixel | (0 << 16);
						pixel = pixel | (255 << 8);
						pixel = pixel | (0);

						isInArea = false;
						for (final Rectangle rectangle : areaList) {
							final Rectangle intersection = rectangle.intersection(rect);
							if ((intersection.width > 0) && (intersection.height > 0)) {
								isInArea = true;
								rectangle.add(rect);

								break;
							}
						}

						if (isInArea == false) {
							areaList.add(rect);
						}

						while (isInArea) {
							Rectangle rectToRemove = null;
							isInArea = false;
							for (final Rectangle rectangle : areaList) {
								for (final Rectangle rec2 : areaList) {
									if (rec2 == rectangle) {
										continue;
									}

									final Rectangle intersection = rectangle.intersection(rec2);
									if ((intersection.width > 0) && (intersection.height > 0)) {
										if (debug == true) {
											System.out.println(rectangle + " intersect " + rec2);
										}

										isInArea = true;
										rectangle.add(rec2);
										rectToRemove = rec2;

										break;
									}

									if (isInArea == true) {
										break;
									}
								}
							}

							if (rectToRemove != null) {
								areaList.remove(rectToRemove);
							}
						}

						if (out != null) {
							out.setRGB(x, y, pixel);
							gc.draw(rect);
						}
					}
				}
			}

			return areaList;
		}

		public List<Rectangle> getAreaList(final BufferedImage image, final int maximumBlankPixelDelimiterCount)
				throws IOException {
			// Use 'out' image for debug purposes.
			BufferedImage out = copyImage(image, BufferedImage.TYPE_INT_ARGB);

			List<Rectangle> areas = getAllAreaByColor(image, out/* null */, 0, 0, 0, maximumBlankPixelDelimiterCount,
					false);

			ImageIO.write(out, "png",
					new File(PDPageBarcodeScanner.WS_PATH + "/test_4_img_" + imageCounter++ + "." + "png"));

			return areas;
		}

		public void extractBarcodeArrayByAreas(final BufferedImage image, final int maximumBlankPixelDelimiterCount)
				throws IOException {
			final BufferedImage blackAndWhiteImage = getThresholdImage(image);

			// ImageIO.write(blackAndWhiteImage, "png", new
			// File(PdPageBarcodeScanner.WS_PATH + "/test_3_bw-img.png"));

			final List<Rectangle> areaList = getAreaList(blackAndWhiteImage, maximumBlankPixelDelimiterCount);

			for (final Rectangle rectangle : areaList) {
				// Verify bounds before crop image
				if (rectangle.x < 0) {
					rectangle.x = 0;
				}

				if (rectangle.y < 0) {
					rectangle.y = 0;
				}

				if ((rectangle.y + rectangle.height) > image.getHeight()) {
					rectangle.height = image.getHeight() - rectangle.y;
				}

				if ((rectangle.x + rectangle.width) > image.getWidth()) {
					rectangle.width = image.getWidth() - rectangle.x;
				}

				// Crop image to extract barcodes.
				final BufferedImage croppedImage = copyImage(
						image.getSubimage(rectangle.x, rectangle.y, rectangle.width, rectangle.height),
						BufferedImage.TYPE_INT_ARGB);

				// ImageIO.write(croppedImage, "png", new File(PdPageBarcodeScanner.WS_PATH +
				// "/test_4_sub-img_"
				// + (PdPageBarcodeScanner.imageCounter - 1) + "_" +
				// PdPageBarcodeScanner.subImageCounter++ + "." + "png"));

				// for(int d = 0; d < 360; d += 2)
				// {
				// Result[] barcodes =
				// PDPageBarcodeScanner.extractBarcode(PDPageBarcodeScanner.rotateImage(croppedImage,
				// d));
				// if(barcodes != null)
				// {
				// this.addResults(barcodes);
				//
				// System.out.println("Found " + barcodes.length + " barcode(s) @ " + d + "
				// degree.");
				//
				// break;
				// }
				// }

				// ZXing library can not deal with DataMatrix in all orientations, so we have to
				// rotate the image and ask ZXing
				// four times to find DataMatrix.
				this.addResults(extractBarcode(croppedImage));
				this.addResults(extractBarcode(rotate90ToLeftImage(croppedImage, BufferedImage.TYPE_INT_ARGB)));
				this.addResults(extractBarcode(rotate90ToRightImage(croppedImage, BufferedImage.TYPE_INT_ARGB)));
				this.addResults(extractBarcode(rotate180Image(croppedImage, BufferedImage.TYPE_INT_ARGB)));

				// for(int d = 0; d < 360; d += 2)
				// {
				// Result[] barcodes =
				// this.extractMultipleBarcode(PdPageBarcodeScanner.rotateImage(croppedImage,
				// d));
				// if(barcodes != null)
				// {
				// this.addResults(barcodes);
				//
				// System.out.println("Found " + barcodes.length + " barcode(s) @ " + d + "
				// degree.");
				//
				// break;
				// }
				// }

				this.addResults(this.extractMultipleBarcode(croppedImage));
				this.addResults(
						this.extractMultipleBarcode(rotate90ToLeftImage(croppedImage, BufferedImage.TYPE_INT_ARGB)));
				this.addResults(
						this.extractMultipleBarcode(rotate90ToRightImage(croppedImage, BufferedImage.TYPE_INT_ARGB)));
				this.addResults(this.extractMultipleBarcode(rotate180Image(croppedImage, BufferedImage.TYPE_INT_ARGB)));
			}
		}

		private void addResults(final Result[] results) {
			if (results == null) {
				return;
			}

			for (final Result result : results) {
				if (this.results.containsKey(result.getText()) == false) {
					this.results.put(result.getText(), result);
				}
			}
		}

		public Result[] extractMultipleBarcode(final BufferedImage bufferedImage) {
			Result[] results = null;

			try {
				final LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
				final BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
				results = this.reader.decodeMultiple(bitmap, decodeHintTypes);
			} catch (final NotFoundException e) {
				// e.printStackTrace();
			}

			return results;
		}

		public Result[] extractBarcode(final BufferedImage bufferedImage) {
			Result[] results = null;

			final LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
			final BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

			try {
				final Result result = new MultiFormatReader().decode(bitmap, decodeHintTypes);
				results = new Result[1];
				results[0] = result;
			} catch (final Exception e) {
				try {
					final Result result = new Code128Reader().decode(bitmap, decodeHintTypes);
					results = new Result[1];
					results[0] = result;
				} catch (NotFoundException e1) {
					// e1.printStackTrace();
				} catch (FormatException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

			return results;
		}

		/**
		 * @see http://forum.codecall.net/topic/69182-java-image-rotation/
		 */
		public BufferedImage rotate180Image(final BufferedImage inputImage, final int imageType) {
			final int width = inputImage.getWidth();
			final int height = inputImage.getHeight();
			final BufferedImage returnImage = new BufferedImage(width, height, imageType);

			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					returnImage.setRGB(width - x - 1, height - y - 1, inputImage.getRGB(x, y));
				}
			}

			return returnImage;
		}

		/**
		 * @see http://forum.codecall.net/topic/69182-java-image-rotation/
		 */
		public BufferedImage rotate90ToRightImage(final BufferedImage inputImage, final int imageType) {
			final int width = inputImage.getWidth();
			final int height = inputImage.getHeight();
			final BufferedImage returnImage = new BufferedImage(height, width, imageType);

			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					returnImage.setRGB(height - y - 1, x, inputImage.getRGB(x, y));
				}
			}

			return returnImage;
		}

		/**
		 * @see http://forum.codecall.net/topic/69182-java-image-rotation/
		 */
		public BufferedImage rotate90ToLeftImage(final BufferedImage inputImage, final int imageType) {
			final int width = inputImage.getWidth();
			final int height = inputImage.getHeight();
			final BufferedImage returnImage = new BufferedImage(height, width, imageType);

			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					returnImage.setRGB(y, width - x - 1, inputImage.getRGB(x, y));
				}
			}

			return returnImage;
		}

		public BufferedImage rotateImage(final BufferedImage inputImage, final int degree) {
			AffineTransform at = new AffineTransform();

			// Translate it to the center of the component.
			at.translate(inputImage.getWidth() / 2, inputImage.getHeight() / 2);

			// Do the actual rotation.
			at.rotate(Math.toRadians(degree));

			// Just a scale because this image is big.
			at.scale(0.5, 0.5);

			// Translate the object so that you rotate it around the center.
			at.translate(-inputImage.getWidth() / 2, -inputImage.getHeight() / 2);

			BufferedImage copy = copyImage(inputImage, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g = copy.createGraphics();
			g.drawRenderedImage(copy, at);
			g.dispose();

			return copy;
		}

		public BufferedImage getBlackAndWhiteImage(final BufferedImage image) throws IOException {
			final BufferedImage imageBW = new BufferedImage(image.getWidth(), image.getHeight(),
					BufferedImage.TYPE_BYTE_BINARY);
			final Graphics2D g = imageBW.createGraphics();
			g.drawRenderedImage(image, null);
			g.dispose();

			return imageBW;
		}

		public BufferedImage getThresholdImage(final BufferedImage image) throws IOException {
			final float saturationMin = 0.10f;
			final float brightnessMin = 0.80f;
			final BufferedImage thresholdImage = copyImage(image, BufferedImage.TYPE_INT_ARGB);

			computeBlackAndWhite(image, thresholdImage, saturationMin, brightnessMin);

			return thresholdImage;
		}

		public void computeBlackAndWhite(final BufferedImage in, final BufferedImage out, final float saturationMin,
				final float brightnessMin) throws IOException {
			final int w = in.getWidth();
			final int h = in.getHeight();
			int pixel;
			final float[] hsb = new float[3];

			for (int x = 0; x < w; x++) {
				for (int y = 0; y < h; y++) {
					pixel = in.getRGB(x, y);

					final int alpha = ((pixel >> 24) & 0xFF);
					int red = ((pixel >> 16) & 0xFF);
					int green = ((pixel >> 8) & 0xFF);
					int blue = (pixel & 0xFF);

					Color.RGBtoHSB(red, green, blue, hsb);

					if ((hsb[2] < brightnessMin) || ((hsb[2] >= brightnessMin) && (hsb[1] >= saturationMin))
					// (red + green + blue > iThreshold *3 )
					) {
						red = 0;
						green = 0;
						blue = 0;
					} else {
						red = 255;
						green = 255;
						blue = 255;
					}
					pixel = pixel & 0x00000000;
					pixel = pixel | (alpha << 24);
					pixel = pixel | (red << 16);
					pixel = pixel | (green << 8);
					pixel = pixel | (blue);

					out.setRGB(x, y, pixel);
				}
			}
		}

		public final BufferedImage copyImage(final BufferedImage bi, final int type) {
			final BufferedImage result = new BufferedImage(bi.getWidth(), bi.getHeight(), type);
			final Graphics2D g = result.createGraphics();
			g.drawRenderedImage(bi, null);
			g.dispose();

			return result;
		}
	}
}
