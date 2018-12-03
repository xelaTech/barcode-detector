package com.xelatech.barcode.localizer.test;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.oned.Code128Reader;
import com.xelatech.barcode.localizer.Barcode;
import com.xelatech.barcode.localizer.CandidateResult;
import com.xelatech.barcode.localizer.ImageDisplay;
import com.xelatech.barcode.localizer.LinearBarcode;
import com.xelatech.barcode.localizer.MatrixBarcode;
import com.xelatech.barcode.localizer.TryHarderFlags;

public class SimpleBarcodeTester {

	private static String lineSeparator = System.getProperty("line.separator");

	private static boolean IS_VIDEO = false;

	private static boolean IS_CAMERA = false;

	private static boolean SHOW_INTERMEDIATE_STEPS = false;

	private static boolean showImages = true;

	private static String imgFile;

	private static VideoCapture video;

	private static int CV_CAP_PROP_FPS = 5;

	private static int CV_CAP_PROP_POS_FRAMES = 1;

	private static int CV_FRAME_COUNT = 7;

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public static void main(final String[] args) {
		Map<CharSequence, BarcodeLocation> results = new HashMap<CharSequence, BarcodeLocation>();
		SimpleBarcodeTester.show_usage_syntax();
		SimpleBarcodeTester.parse_args(args);
		if (SimpleBarcodeTester.IS_VIDEO && SimpleBarcodeTester.IS_CAMERA) {
			SimpleBarcodeTester.IS_VIDEO = false;
		}

		if (!SimpleBarcodeTester.IS_VIDEO && !SimpleBarcodeTester.IS_CAMERA) {
			SimpleBarcodeTester.processImage();

			if (IS_VIDEO) {
				video = new VideoCapture(imgFile);
				results = processVideo(imgFile);
				if (results.size() == 0)
					System.out.println("No results found");
			}

			if (IS_CAMERA) {
				video = new VideoCapture(0);
				video.open(0);
				if (video.isOpened())
					System.out.println("Camera is open");
				results = processCamera("Camera feed");
				if (results.size() == 0)
					System.out.println("No results found");
			}
		}

		final Mat image = new Mat();
		for (final CharSequence result : results.keySet()) {
			final BarcodeLocation resultLoc = results.get(result);
			System.out.print("Found " + result + " Location - " + resultLoc.toString());
			System.out.print(SimpleBarcodeTester.lineSeparator + SimpleBarcodeTester.lineSeparator);
			if (SimpleBarcodeTester.showImages) {
				// video.set(CV_CAP_PROP_POS_FRAMES, resultLoc.frame);
				// video.read(image);
				final Point[] rectPoints = resultLoc.coords;
				final Scalar colour = new Scalar(0, 0, 255);
				for (int j = 0; j < 3; j++) {
					// Imgproc.line(image, rectPoints[j], rectPoints[j + 1], colour, 2,
					// Imgproc.LINE_AA, 0);
				}
				// Imgproc.line(image, rectPoints[3], rectPoints[0], colour, 2, Imgproc.LINE_AA,
				// 0);
				ImageDisplay.showImageFrame(image, "Barcode text - " + result);
			}
		}
		/*
		 * if(IS_CAMERA) video.release();
		 */
	}

	private static Map<CharSequence, BarcodeLocation> processVideo(final String filename) {
		double frames_per_second;
		int frame_count;
		final Mat image = new Mat();

		Barcode barcode = null;
		ImageDisplay videoDisp = null;
		final Map<CharSequence, BarcodeLocation> foundCodes = new HashMap<CharSequence, BarcodeLocation>();

		try {
			frames_per_second = video.get(SimpleBarcodeTester.CV_CAP_PROP_FPS);
			frame_count = (int) video.get(SimpleBarcodeTester.CV_FRAME_COUNT);

			System.out.println("FPS is " + frames_per_second);
			System.out.println("Frame count is " + frame_count);
			video.read(image);
			if (SimpleBarcodeTester.showImages) {
				videoDisp = ImageDisplay.getImageFrame(image, "Video Frames");
			}
			for (int i = 0; i < frame_count; i += (frames_per_second / 3.0)) {
				video.set(SimpleBarcodeTester.CV_CAP_PROP_POS_FRAMES, i);
				video.read(image);
				final String imgName = filename + "_Frame_" + i;

				if (barcode == null) {
					barcode = new MatrixBarcode(imgName, image, TryHarderFlags.VERY_SMALL_MATRIX);
				} else {
					if (!Barcode.updateImage(barcode, image, imgName)) {
						barcode = new MatrixBarcode(imgName, image, TryHarderFlags.VERY_SMALL_MATRIX);
					}
				}

				// locateBarcode() returns a List<CandidateResult> with all possible candidate
				// barcode regions from within the image. These images then get passed to a
				// decoder(we use ZXing here but could be any decoder)

				final List<CandidateResult> results = barcode.locateBarcode();

				final Map<CharSequence, BarcodeLocation> frame_results = SimpleBarcodeTester
						.decodeBarcodeFromVideo(results, i);
				foundCodes.putAll(frame_results);
				System.out.print("Processed frame " + i + "- Found " + frame_results.size() + " results\r");

				for (final BarcodeLocation bl : frame_results.values()) {
					final Point[] rectPoints = bl.coords;
					final Scalar colour = new Scalar(255, 0, 0);
					for (int j = 0; j < 3; j++) {
						Imgproc.line(image, rectPoints[j], rectPoints[j + 1], colour, 2, Imgproc.LINE_AA, 0);
					}
					Imgproc.line(image, rectPoints[3], rectPoints[0], colour, 2, Imgproc.LINE_AA, 0);
				}
				if (SimpleBarcodeTester.showImages) {
					videoDisp.updateImage(image, "Video frame " + i);
				}

			}
			if (SimpleBarcodeTester.showImages) {
				videoDisp.close();
			}
		} catch (final IOException ioe) {
			System.out.println("IO Exception when finding barcode " + ioe.getMessage());
		}

		return foundCodes;
	}

	private static Map<CharSequence, BarcodeLocation> processCamera(final String caption) {
		double frames_per_second;
		int frame_count;
		final Mat image = new Mat();
		Barcode barcode = null;
		final Map<CharSequence, BarcodeLocation> foundCodes = new HashMap<CharSequence, BarcodeLocation>();

		try {
			frames_per_second = video.get(SimpleBarcodeTester.CV_CAP_PROP_FPS);
			frame_count = (int) video.get(SimpleBarcodeTester.CV_FRAME_COUNT);

			System.out.println("FPS is " + frames_per_second);
			System.out.println("Frame count is " + frame_count);
			video.read(image);
			final ImageDisplay videoDisp = ImageDisplay.getImageFrame(image, "Video Frames");

			final long end_time = System.currentTimeMillis() + 240000;
			int i = 0;
			while (System.currentTimeMillis() < end_time) {
				i += 1;
				video.read(image);
				final String imgName = caption + "_" + System.currentTimeMillis();

				if (barcode == null) {
					barcode = new MatrixBarcode(imgName, image, TryHarderFlags.VERY_SMALL_MATRIX);
				} else {
					if (!Barcode.updateImage(barcode, image, imgName)) {
						barcode = new MatrixBarcode(imgName, image, TryHarderFlags.VERY_SMALL_MATRIX);
					}
				}

				// locateBarcode() returns a List<CandidateResult> with all possible candidate
				// barcode regions from within the image. These images then get passed to a
				// decoder(we use ZXing here but could be any decoder)

				final List<CandidateResult> results = barcode.locateBarcode();

				final Map<CharSequence, BarcodeLocation> frame_results = SimpleBarcodeTester
						.decodeBarcodeFromVideo(results, i);
				foundCodes.putAll(frame_results);
				System.out.println("Processed frame " + i + "- Found " + frame_results.size() + " results");

				for (final BarcodeLocation bl : frame_results.values()) {
					final Point[] rectPoints = bl.coords;
					final Scalar colour = new Scalar(255, 0, 0);
					for (int j = 0; j < 3; j++) {
						Imgproc.line(image, rectPoints[j], rectPoints[j + 1], colour, 2, Imgproc.LINE_AA, 0);
					}
					Imgproc.line(image, rectPoints[3], rectPoints[0], colour, 2, Imgproc.LINE_AA, 0);
				}
				videoDisp.updateImage(image, "Video frame " + i);

			}
		} catch (final IOException ioe) {
			System.out.println("IO Exception when finding barcode " + ioe.getMessage());
		}
		return foundCodes;
	}

	private static Map<CharSequence, BarcodeLocation> decodeBarcodeFromVideo(final List<CandidateResult> candidateCodes,
			final int frameNumber) {
		// Decodes barcode using ZXing and either print the barcode's content or says no
		// barcode found
		Result result = null;
		final Map<CharSequence, BarcodeLocation> results = new HashMap<CharSequence, BarcodeLocation>();

		for (final CandidateResult cr : candidateCodes) {
			final BufferedImage candidate = cr.candidate;
			final LuminanceSource source = new BufferedImageLuminanceSource(candidate);
			final BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
			final Reader reader = new MultiFormatReader();

			final Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
			hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

			try {
				result = reader.decode(bitmap, hints);
				results.put(result.getText(), new BarcodeLocation(cr.ROICoords, frameNumber));
			} catch (final ReaderException re) {
			}
		}

		return results;
	}

	private static void processImage() {
		try {
			// Instantiate a class of type MatrixBarcode with the image filename.
			// Barcode barcode = new MatrixBarcode(SimpleBarcodeTester.imgFile,
			// SimpleBarcodeTester.SHOW_INTERMEDIATE_STEPS,
			// TryHarderFlags.VERY_SMALL_MATRIX);

			Barcode barcode = new LinearBarcode(SimpleBarcodeTester.imgFile,
					SimpleBarcodeTester.SHOW_INTERMEDIATE_STEPS, TryHarderFlags.VERY_SMALL_LINEAR);

			// locateBarcode() returns a List<CandidateResult> with all possible candidate
			// barcode regions from within the image. These images then get passed to a
			// decoder(we use ZXing here but could be any decoder).
			final List<CandidateResult> results = barcode.locateBarcode();

			System.out.println(
					"Decoding " + SimpleBarcodeTester.imgFile + " " + results.size() + " candidate(s) codes found.");

			SimpleBarcodeTester.decodeBarcode(results, barcode.getName(), "Localizer");
		} catch (final IOException ioe) {
			System.out.println("IO Exception when finding barcode " + ioe.getMessage());
		}
	}

	/**
	 * Decodes barcode using ZXing and either print the barcode text or says no
	 * barcode found.
	 * 
	 * @param candidateCodes
	 * @param filename
	 * @param caption
	 */
	private static void decodeBarcode(final List<CandidateResult> candidateCodes, final String filename,
			final String caption) {
		BufferedImage decodedBarcode = null;
		String title = null;
		// Result result = null;
		Result[] results = null;

		for (final CandidateResult cr : candidateCodes) {
			final BufferedImage candidate = cr.candidate;
			decodedBarcode = null;
			final LuminanceSource source = new BufferedImageLuminanceSource(candidate);
			final BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
			final Reader reader = new MultiFormatReader();

			final Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
			hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

			// try
			// {
			// result = reader.decode(bitmap, hints);

			results = extractBarcode(candidate, hints);

			decodedBarcode = candidate;
			// title = filename + " " + caption + " - barcode text " + result.getText() + "
			// " + cr.getROICoords();
			// }
			// catch(final ReaderException re)
			// {
			// }

			if (results != null) {
				for (final Result result : results) {
					// System.out.println("\nBarcode format = " + result.getBarcodeFormat() + ",
					// value = " + result.getText());
					title = filename + " " + caption + " - barcode text " + result.getText() + " " + cr.getROICoords();

					// if(decodedBarcode == null)
					// {
					// title = filename + " - no barcode found - " + cr.getROICoords();
					// if(SimpleBarcodeTester.showImages)
					// {
					// ImageDisplay.showImageFrame(candidate, title);
					// }
					// }
					// else
					// {
					if (SimpleBarcodeTester.showImages) {
						ImageDisplay.showImageFrame(decodedBarcode, title);
					}

					System.out.println("\nFound in " + filename + ", barcode format = " + result.getBarcodeFormat()
							+ ", value = " + result.getText());
					// }
				}
			} else {
				title = filename + " - no barcode found - " + cr.getROICoords();
				if (SimpleBarcodeTester.showImages) {
					ImageDisplay.showImageFrame(candidate, title);
				}
			}
		}
	}

	public static Result[] extractBarcode(final BufferedImage bufferedImage, final Map<DecodeHintType, Object> hints) {
		Result[] results = null;

		// for(int d = 0; d < 360; d += 2)
		// {
		final LuminanceSource source = new BufferedImageLuminanceSource(
				bufferedImage/* SimpleBarcodeTester.rotateImage(bufferedImage, d) */);
		final BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

		try {
			final Result result = new MultiFormatReader().decode(bitmap, hints);
			results = new Result[1];
			results[0] = result;
		} catch (final Exception e) {
			try {
				final Result result = new Code128Reader().decode(bitmap, hints);
				results = new Result[1];
				results[0] = result;
			} catch (NotFoundException e1) {
				// e1.printStackTrace();
			} catch (FormatException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		// }

		return results;
	}

	public static BufferedImage rotateImage(final BufferedImage inputImage, final int degree) {
		AffineTransform at = new AffineTransform();

		// Translate it to the center of the component.
		at.translate(inputImage.getWidth() / 2, inputImage.getHeight() / 2);

		// Do the actual rotation.
		at.rotate(Math.toRadians(degree));

		// Just a scale because this image is big.
		at.scale(0.5, 0.5);

		// Translate the object so that you rotate it around the center.
		at.translate(-inputImage.getWidth() / 2, -inputImage.getHeight() / 2);

		BufferedImage copy = PDPageBarcodeScanner.copyImage(inputImage, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = copy.createGraphics();
		g.drawRenderedImage(copy, at);
		g.dispose();

		return copy;
	}

	private static void show_usage_syntax() {
		System.out.println("");
		System.out.println("Barcode localizer");
		System.out.println("");
		System.out.println("Usage: BarcodeTester <imagefile> [-matrix] [-oracle] ");
		System.out.println("<imagefile> must be JPEG or PNG");
		System.out.println("[-debug] - shows images for intermediate steps and saves intermediate files");
		System.out.println("[-video] - <imagefile> is a video");
		System.out.println("[-camera] - capture from camera");
		System.out.println("[-noimages] - do not display any images, overrides -debug command");
		System.out.println("");
	}

	private static void parse_args(final String[] args) {
		int ctr = 0;
		String arg;

		while (ctr < args.length) {
			arg = args[ctr++];

			if (arg.equalsIgnoreCase("-debug")) {
				SimpleBarcodeTester.SHOW_INTERMEDIATE_STEPS = true;
				continue;
			}

			if (arg.equalsIgnoreCase("-video")) {
				SimpleBarcodeTester.IS_VIDEO = true;
				continue;
			}

			if (arg.equalsIgnoreCase("-camera")) {
				SimpleBarcodeTester.IS_CAMERA = true;
				continue;
			}

			if (arg.equalsIgnoreCase("-noimages")) {
				SimpleBarcodeTester.showImages = false;
				SimpleBarcodeTester.SHOW_INTERMEDIATE_STEPS = false;
				continue;
			}
			// must be filename if we got here
			SimpleBarcodeTester.imgFile = arg;
		}
	}

	private static class BarcodeLocation {
		int frame;

		Point[] coords;

		private BarcodeLocation(final Point[] coords, final int frame) {
			this.frame = frame;
			this.coords = coords;
		}

		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer("Frame " + this.frame);

			for (final Point p : this.coords) {
				sb.append("(" + (int) (p.x) + "," + (int) (p.y) + "), ");
			}

			return sb.toString();
		}
	}

}
