package com.xelatech.barcode.localizer;


import java.util.ArrayList;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;


/**
 * Container class for source image and various intermediate images created while processing src_original to search for
 * a barcode.
 */
class ImageInfo
{
  Barcode.CodeType searchType;

  Mat srcOriginal;

  Mat srcScaled;

  Mat srcGrayscale;

  Mat probabilities;

  Mat gradientDirection;

  Mat gradientMagnitude;

  Mat scharrX;

  Mat scharrY;

  Mat mask;

  /**
   * Matrices used in MatrixBarcodeCandidate class.
   */
  Mat rotationMatrix;

  Mat delta = Mat.zeros(3, 3, CvType.CV_32F);

  Mat newCornerCoord = Mat.zeros(2, 1, CvType.CV_32F);

  Mat coord = Mat.ones(3, 1, CvType.CV_32F);

  List<Point> newCornerPoints = new ArrayList<>(4);

  List<Point> transformedPoints = new ArrayList<>(4);

  Point[] scaledCorners = new Point[4];

  /**
   * bin width for histogram calculation.
   */
  protected static final int BIN_WIDTH = 15;

  protected static final int bins = 180 / ImageInfo.BIN_WIDTH;

  int probMatRows, probMatCols;

  Mat edgeDensity;

  List<Mat> histograms = new ArrayList<>();

  List<Mat> histIntegrals = new ArrayList<>();

  Integer[] histArray = new Integer[ImageInfo.bins];


  ImageInfo(final Mat src)
  {
    this.srcOriginal = src;
    this.gradientDirection = new Mat();
    this.gradientMagnitude = new Mat();

    this.scharrX = new Mat();
    this.scharrY = new Mat();
    this.mask = new Mat();

    // Create List of points for MatrixBarcodeCandidate.
    for(int r = 0; r < 4; r++)
    {
      this.newCornerPoints.add(new Point());
      this.transformedPoints.add(new Point());
    }
  }


  protected void initializeMats(final int rows, final int cols, final SearchParameters searchParams)
  {
    this.probabilities = Mat.zeros((int)((rows * searchParams.scaleFactor) + 1),
        (int)((cols * searchParams.scaleFactor) + 1), CvType.CV_8U);
    this.srcGrayscale = new Mat(rows, cols, CvType.CV_32F);
    this.probMatRows = this.probabilities.rows();
    this.probMatCols = this.probabilities.cols();
    this.edgeDensity = Mat.zeros((int)(rows / (1.0 * searchParams.tileSize)),
        (int)(cols / (1.0 * searchParams.tileSize)), CvType.CV_16U);

    // Create Mat objects to contain integral histograms.
    for(int r = 0; r < ImageInfo.bins; r++)
    {
      this.histograms.add(Mat.zeros((int)((rows / (1.0 * searchParams.tileSize)) + 1),
          (int)((cols / (1.0 * searchParams.tileSize)) + 1), CvType.CV_32F));
      this.histIntegrals.add(Mat.zeros((int)((rows / (1.0 * searchParams.tileSize)) + 1),
          (int)((cols / (1.0 * searchParams.tileSize)) + 1), CvType.CV_32FC1));
    }
  }

}
