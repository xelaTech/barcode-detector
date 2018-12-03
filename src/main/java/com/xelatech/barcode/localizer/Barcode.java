package com.xelatech.barcode.localizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * Parent class containing common methods and definitions for 1D and 2D barcode
 * searches.
 */
public abstract class Barcode {
	/**
	 * Flag to indicate what kind of searches to perform on image to locate barcode.
	 */
	// protected int sizeFlag = TryHarderFlags.VERY_SMALL_MATRIX.value();
	protected int sizeFlag = TryHarderFlags.VERY_SMALL_LINEAR.value();

	protected boolean postProcessResizeBarcode = true;

	protected static double USE_ROTATED_RECT_ANGLE = /* 361 */-9;

	/**
	 * Filename of barcode image file.
	 */
	protected String fileName;

	/**
	 * Flag if we want to show intermediate steps for debugging.
	 */
	boolean debugging;

	/**
	 * Various parameters and thresholds used during the search.
	 */
	protected SearchParameters searchParams;

	protected ImageInfo imgDetails;

	protected int rows;

	protected int cols;

	List<CandidateResult> candidateBarcodes = new ArrayList<>();

	static enum CodeType {
		LINEAR, MATRIX
	};

	public Barcode(final String filename, final TryHarderFlags flag) throws IOException {
		this.fileName = filename;
		this.imgDetails = new ImageInfo(this.loadImage());

		this.rows = this.imgDetails.srcOriginal.rows();
		this.cols = this.imgDetails.srcOriginal.cols();

		this.setBarcodeSize(flag);

		this.debugging = false;
	}

	/**
	 * Used in mobile implementation to avoid recreating Mat objects repeatedly.
	 *
	 * @param img
	 * @param flag
	 * @throws IOException
	 */
	public Barcode(final Mat img, final TryHarderFlags flag) throws IOException {
		this.imgDetails = new ImageInfo(img);

		this.rows = this.imgDetails.srcOriginal.rows();
		this.cols = this.imgDetails.srcOriginal.cols();

		this.setBarcodeSize(flag);

		this.debugging = false;
	}

	public String getName() {
		return this.fileName;
	}

	public static boolean updateImage(final Barcode barcode, final Mat img, final String imgFileName) {
		barcode.fileName = imgFileName;
		return Barcode.updateImage(barcode, img);
	}

	/**
	 * Used for video or camera feed when all images are of the same size.
	 *
	 * @param barcode
	 * @param img
	 * @return
	 */
	public static boolean updateImage(final Barcode barcode, final Mat img) {
		final int orig_rows = barcode.imgDetails.srcOriginal.rows();
		final int orig_cols = barcode.imgDetails.srcOriginal.cols();

		final int new_rows = img.rows();
		final int new_cols = img.cols();

		if ((orig_rows != new_rows) || (orig_cols != new_cols)) {
			return false;
		}

		barcode.candidateBarcodes.clear();
		barcode.imgDetails.srcOriginal = img;
		Imgproc.resize(barcode.imgDetails.srcOriginal, barcode.imgDetails.srcScaled,
				barcode.imgDetails.srcScaled.size(), 0, 0, Imgproc.INTER_AREA);
		Imgproc.cvtColor(barcode.imgDetails.srcScaled, barcode.imgDetails.srcGrayscale, Imgproc.COLOR_RGB2GRAY);

		return true;
	}

	public void setBarcodeSize(final TryHarderFlags size) {
		// At least one of the size flags must be set so it chooses NORMAL as the
		// default if nothing is set.
		this.sizeFlag = size.value();
		this.setSearchParameters(size);
	}

	public void doPostProcessResizeBarcode(final boolean postProcess) {
		this.postProcessResizeBarcode = postProcess;
	}

	protected void setSearchParameters(final TryHarderFlags flags) {
		// Should not be used when multiple size flags are set. It will set the search
		// parameters to one of them and ignore the others
		if ((this.sizeFlag & TryHarderFlags.SMALL.value()) != 0) {
			this.searchParams = SearchParameters.getSmallParameters();
		}

		if ((this.sizeFlag & TryHarderFlags.LARGE.value()) != 0) {
			this.searchParams = SearchParameters.getLargeParameters();
		}

		if ((this.sizeFlag & TryHarderFlags.NORMAL.value()) != 0) {
			this.searchParams = SearchParameters.getNormalParameters();
		}

		if ((this.sizeFlag & TryHarderFlags.VERY_SMALL_LINEAR.value()) != 0) {
			this.searchParams = SearchParameters.getVerySmallLinearParameters();
		}

		if ((this.sizeFlag & TryHarderFlags.VERY_SMALL_MATRIX.value()) != 0) {
			this.searchParams = SearchParameters.getVerySmallMatrixParameters();
		}

		this.preprocessImage();
	}

	/**
	 * Actual locateBarcode algorithm must be implemented in child class.
	 *
	 * @return
	 * @throws IOException
	 */
	public abstract List<CandidateResult> locateBarcode() throws IOException;

	/**
	 * Pre-process image to convert to grayscale and do morph black hat. It also
	 * resizes image if it is above a specified size and sets the search parameters
	 * based on image size.
	 */
	protected void preprocessImage() {
		// Shrink the image if it is above a certain size. It reduces image size for
		// large images which helps with
		// processing speed and reducing sensitivity to barcode size within the image.
		if (this.rows > this.searchParams.MAX_ROWS) {
			this.cols = (int) (this.cols * ((this.searchParams.MAX_ROWS * 1.0) / this.rows));
			this.rows = this.searchParams.MAX_ROWS;
			this.imgDetails.srcScaled = new Mat(this.rows, this.cols, CvType.CV_32F);
			Imgproc.resize(this.imgDetails.srcOriginal, this.imgDetails.srcScaled, this.imgDetails.srcScaled.size(), 0,
					0, Imgproc.INTER_AREA);
		}

		if (this.imgDetails.srcScaled == null) {
			this.imgDetails.srcScaled = this.imgDetails.srcOriginal.clone();
		}

		this.searchParams.setImageSpecificParameters(this.rows, this.cols);

		// Do pre-processing to increase contrast.
		this.imgDetails.initializeMats(this.rows, this.cols, this.searchParams);

		Imgproc.cvtColor(this.imgDetails.srcScaled, this.imgDetails.srcGrayscale, Imgproc.COLOR_RGB2GRAY);
	}

	/**
	 * Resizes candidate image to have at least MIN_COLS columns and MIN_ROWS rows
	 * called when RESIZE_BEFORE_DECODE is set - seems to help ZXing decode barcode.
	 *
	 * @param candidate
	 * @return
	 */
	protected Mat scale(final Mat candidate) {
		final int MIN_COLS = 200;
		final int MIN_ROWS = 200;

		int num_rows = candidate.rows();
		int num_cols = candidate.cols();

		if ((num_cols > MIN_COLS) && (num_rows > MIN_ROWS)) {
			return candidate;
		}

		if (num_cols < MIN_COLS) {
			num_rows = (int) ((num_rows * MIN_COLS) / (1.0 * num_cols));
			num_cols = MIN_COLS;
		}

		if (num_rows < MIN_ROWS) {
			num_cols = (int) ((num_cols * MIN_ROWS) / (1.0 * num_rows));
			num_rows = MIN_ROWS;
		}

		final Mat result = Mat.zeros(num_rows, num_cols, candidate.type());

		Imgproc.resize(candidate, result, result.size(), 0, 0, Imgproc.INTER_CUBIC);

		return result;
	}

	/**
	 * Connect large components by doing morph close followed by morph open use
	 * larger element size for erosion to remove small elements joined by dilation.
	 */
	protected void connectComponents() {
		int shape = Imgproc.MORPH_ELLIPSE;
		if (this.searchParams.isVerySmallMatrix == true) {
			// Test out slightly different process for small codes in a large image.
			shape = Imgproc.MORPH_RECT;
		}

		final Mat small_elemSE = Imgproc.getStructuringElement(shape, this.searchParams.elementSize);
		final Mat large_elemSE = Imgproc.getStructuringElement(shape, this.searchParams.largeElementSize);

		Imgproc.dilate(this.imgDetails.probabilities, this.imgDetails.probabilities, small_elemSE);
		Imgproc.erode(this.imgDetails.probabilities, this.imgDetails.probabilities, large_elemSE);

		Imgproc.erode(this.imgDetails.probabilities, this.imgDetails.probabilities, small_elemSE);
		Imgproc.dilate(this.imgDetails.probabilities, this.imgDetails.probabilities, large_elemSE);
	}

	/**
	 * Calculates sum of values within a rectangle from a given integral image.
	 *
	 * @param integral
	 * @param top_row
	 * @param bottom_row
	 * @param left_col
	 * @param right_col
	 * @return
	 */
	protected double calculateRectSum(final Mat integral, final int top_row, int bottom_row, final int left_col,
			int right_col) {
		// if the right col or bottom row falls outside the image bounds, sets it to max
		// col and max row
		// in actuality, top_row - 1 and left_col - 1 are used - see p. 185 of Learning
		// OpenCV ed. 1 by Gary Bradski for an
		// explanation
		// if top_row or left_col are outside image boundaries, it uses 0 for their
		// value
		// this is useful when one part of the rectangle lies outside the image bounds
		final int numRows = integral.rows();
		final int numCols = integral.cols();

		// Do bounds checking on provided parameters.
		bottom_row = java.lang.Math.min(bottom_row, numRows);
		right_col = java.lang.Math.min(right_col, numCols);

		final double bottom_right = integral.get(bottom_row, right_col)[0];
		final double top_right = (top_row < 0) ? 0 : integral.get(top_row, right_col)[0];
		final double top_left = ((left_col < 0) || (top_row < 0)) ? 0 : integral.get(top_row, left_col)[0];
		final double bottom_left = (left_col < 0) ? 0 : integral.get(bottom_row, left_col)[0];

		return ((bottom_right - bottom_left - top_right) + top_left);
	}

	/**
	 * Writes the contents of a Mat object to disk.
	 *
	 * @param filename
	 * @param img
	 */
	protected static void writeMat(final String filename, final Mat img) {
		try {
			final PrintStream original = new PrintStream(System.out);
			final PrintStream printStream = new PrintStream(new FileOutputStream(new File(filename)));
			System.setOut(printStream);
			System.out.println(img.dump());
			System.setOut(original);
		} catch (final IOException ioe) {
		}
	}

	protected Mat loadImage() throws IOException {
		final File file = new File(this.fileName);
		if (file.isFile() == false) {
			throw new IOException(this.fileName + " isn't a file.");
		}

		return Imgcodecs.imread(this.fileName, Imgcodecs.IMREAD_COLOR);
	}

}
