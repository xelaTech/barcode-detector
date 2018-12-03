package com.xelatech.barcode.localizer;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;


public class MatrixBarcode extends Barcode
{
  /**
   * Used in histogram calculation.
   */
  private static final int DUMMY_ANGLE = 255;

  private static final Scalar ZERO_SCALAR = new Scalar(0);

  private static Mat hist = new MatOfInt(ImageInfo.bins, 1);

  private static Mat histIdx = new Mat();

  /**
   * Empty matrix to use as mask for histogram calculation.
   */
  private static Mat mask = new Mat();

  private static Mat angles;

  /**
   * Empty matrix required as parameter in contour finding. Not used anywhere else.
   */
  private static final Mat hierarchy = new Mat();

  private static final Map<Integer, Scalar> scalarDict = new HashMap<>();

  static
  {
    // create a hashmap with Scalar objects used during histogram calculation
    // done so that we can reuse these objects instead of creating and destroying them
    for(int r = 1; r <= 181; r += ImageInfo.BIN_WIDTH)
    {
      MatrixBarcode.scalarDict.put(r, new Scalar(r));
    }

    // Add objects used when trimming angles to 0-360 range.
    MatrixBarcode.scalarDict.put(170, new Scalar(170));
    MatrixBarcode.scalarDict.put(180, new Scalar(180));
    MatrixBarcode.scalarDict.put(-180, new Scalar(-180));
    MatrixBarcode.scalarDict.put(360, new Scalar(360));
    MatrixBarcode.scalarDict.put(MatrixBarcode.DUMMY_ANGLE, new Scalar(MatrixBarcode.DUMMY_ANGLE));
  }


  public MatrixBarcode(final String filename, final boolean debug, final TryHarderFlags flag) throws IOException
  {
    super(filename, flag);
    this.debugging = debug;
    this.imgDetails.searchType = CodeType.MATRIX;
  }


  public MatrixBarcode(final String image_name, final Mat img, final TryHarderFlags flag) throws IOException
  {
    super(img, flag);
    this.fileName = image_name;
    this.imgDetails.searchType = CodeType.MATRIX;
    this.debugging = false;
  }


  @Override
  public List<CandidateResult> locateBarcode() throws IOException
  {
    this.calcGradientDirectionAndMagnitude();

    for(int tileSize = this.searchParams.tileSize; (tileSize < this.rows) && (tileSize < this.cols); tileSize *= 4)
    {
      // find areas with low variance in gradient direction
      this.imgDetails.probabilities = this.calcProbabilityMatrix(tileSize);

      // connectComponents();
      final List<MatOfPoint> contours = new ArrayList<>();
      // findContours modifies source image so probabilities pass it a clone of img_details.probabilities
      // img_details.probabilities will be used again shortly to expand the bsrcode region
      Imgproc.findContours(this.imgDetails.probabilities.clone(), contours, MatrixBarcode.hierarchy, Imgproc.RETR_LIST,
          Imgproc.CHAIN_APPROX_SIMPLE);

      final int areaMultiplier = (this.searchParams.RECT_HEIGHT * this.searchParams.RECT_WIDTH)
          / (this.searchParams.PROB_MAT_TILE_SIZE * this.searchParams.PROB_MAT_TILE_SIZE);

      // Pictures were downsampled during probability calculation, so we multiply it by the tile size to get area in the
      // original picture.
      for(int i = 0; i < contours.size(); i++)
      {
        final double area = Imgproc.contourArea(contours.get(i));
        if((area * areaMultiplier) < this.searchParams.THRESHOLD_MIN_AREA)
        {
          continue;
        }

        final RotatedRect minRect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i).toArray()));
        final double boundingRectArea = minRect.size.width * minRect.size.height;

        if(this.debugging == true)
        {
          System.out
              .println("Area is " + (area * areaMultiplier) + " MIN_AREA is " + this.searchParams.THRESHOLD_MIN_AREA);
          System.out.println("area ratio is " + ((area / boundingRectArea)));
        }

        // check if contour is of a rectangular object
        if((area / boundingRectArea) > this.searchParams.THRESHOLD_AREA_RATIO)
        {
          final MatrixBarcodeCandidate cb = new MatrixBarcodeCandidate(this.imgDetails, minRect, this.searchParams);
          if(this.debugging == true)
          {
            cb.markCandidateRegion(new Scalar(0, 255, 128), this.imgDetails.srcScaled);
          }

          // Rotates candidate region to straighten it based on the angle of the enclosing RotatedRect.
          final CandidateResult ROI = cb.normalizeCandidateRegion(Barcode.USE_ROTATED_RECT_ANGLE);
          if(this.postProcessResizeBarcode)
          {
            ROI.ROI = this.scale(ROI.ROI);
          }

          ROI.candidate = ImageDisplay.getBufImg(ROI.ROI);
          this.candidateBarcodes.add(ROI);

          if(this.debugging == true)
          {
            cb.markCandidateRegion(new Scalar(0, 0, 255), this.imgDetails.srcScaled);
          }
        }
      }

      if(this.debugging == true)
      {
        ImageDisplay.showImageFrameGrid(this.imgDetails.srcScaled, this.fileName + " with candidate regions");
      }
    }

    return this.candidateBarcodes;
  }


  /**
   * Calculates magnitudes and directions of gradients in the image. Results are stored in appropriate matrices in
   * img_details object.
   */
  private void calcGradientDirectionAndMagnitude()
  {
    Imgproc.Scharr(this.imgDetails.srcGrayscale, this.imgDetails.scharrX, CvType.CV_32F, 1, 0);
    Imgproc.Scharr(this.imgDetails.srcGrayscale, this.imgDetails.scharrY, CvType.CV_32F, 0, 1);

    // Calculate angle using Core.phase function - quicker than using atan2 manually.
    Core.phase(this.imgDetails.scharrX, this.imgDetails.scharrY, this.imgDetails.gradientDirection, true);

    // Convert angles from 180-360 to 0-180 range and set angles from 170-180 to 0.
    Core.inRange(this.imgDetails.gradientDirection, MatrixBarcode.scalarDict.get(180),
        MatrixBarcode.scalarDict.get(360), this.imgDetails.mask);
    Core.add(this.imgDetails.gradientDirection, MatrixBarcode.scalarDict.get(-180), this.imgDetails.gradientDirection,
        this.imgDetails.mask);
    Core.inRange(this.imgDetails.gradientDirection, MatrixBarcode.scalarDict.get(170),
        MatrixBarcode.scalarDict.get(180), this.imgDetails.mask);

    this.imgDetails.gradientDirection.setTo(MatrixBarcode.ZERO_SCALAR, this.imgDetails.mask);

    // convert type after modifying angle so that angles above 360 don't get truncated
    this.imgDetails.gradientDirection.convertTo(this.imgDetails.gradientDirection, CvType.CV_8U);

    if(this.debugging == true)
    {
      Barcode.writeMat("angles.csv", this.imgDetails.gradientDirection);
    }

    // calculate magnitude of gradient, normalize and threshold
    Core.magnitude(this.imgDetails.scharrX, this.imgDetails.scharrY, this.imgDetails.gradientMagnitude);
    Core.normalize(this.imgDetails.gradientMagnitude, this.imgDetails.gradientMagnitude, 0, 255, Core.NORM_MINMAX,
        CvType.CV_8U);
    Imgproc.threshold(this.imgDetails.gradientMagnitude, this.imgDetails.gradientMagnitude, 50, 255,
        Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

    // set angle to DUMMY_ANGLE = 255 at all points where gradient magnitude is 0 i.e. where there are no edges
    // these angles will be ignored in the histogram calculation since that counts only up to 180
    Core.inRange(this.imgDetails.gradientMagnitude, MatrixBarcode.ZERO_SCALAR, MatrixBarcode.ZERO_SCALAR,
        this.imgDetails.mask);
    this.imgDetails.gradientDirection.setTo(MatrixBarcode.scalarDict.get(MatrixBarcode.DUMMY_ANGLE),
        this.imgDetails.mask);
    // add 1 to gradient directions so that gradients of 0 can be located
    Core.add(this.imgDetails.gradientDirection, new Scalar(1), this.imgDetails.gradientDirection);

    // calculate integral image for edge density
    this.imgDetails.edgeDensity = this.calcEdgeDensityIntegralImage();

    // calculate histograms for each tile
    this.calcHistograms();

    if(this.debugging == true)
    {
      Barcode.writeMat("magnitudes.csv", this.imgDetails.gradientMagnitude);
      Barcode.writeMat("angles_modified.csv", this.imgDetails.gradientDirection);
    }
  }


  /**
   * Calculate probability of a barcode region in each tile based on HOG data for each tile.
   *
   * @param tileSize
   * @return
   */
  private Mat calcProbabilityMatrix(final int tileSize)
  {
    // Calculate probabilities for each pixel from window around it, normalize and threshold.
    final Mat probabilities = this.calcProbabilityTilings(tileSize);

    final double debugProbThreshold = Imgproc.threshold(probabilities, probabilities, 128, 255, Imgproc.THRESH_BINARY);

    if(this.debugging == true)
    {
      System.out.println("Probability threshold is " + debugProbThreshold);

      Barcode.writeMat("probabilities.csv", probabilities);

      ImageDisplay.showImageFrameGrid(this.imgDetails.gradientMagnitude, "Magnitudes");
      ImageDisplay.showImageFrameGrid(probabilities, "histogram probabilities");
    }

    return probabilities;
  }


  /**
   * Calculates probability of each tile being in a 2D barcode region.
   *
   * @param tileSize
   * @return
   */
  private Mat calcProbabilityTilings(final int tileSize)
  {
    // Tiles must be square.
    assert (this.searchParams.RECT_HEIGHT == this.searchParams.RECT_WIDTH) : "RECT_HEIGHT and RECT_WIDTH must be equal in searchParams imageSpecificParams";

    final int probMatTileSize = (int)(tileSize
        * (this.searchParams.PROB_MAT_TILE_SIZE / (1.0 * this.searchParams.tileSize)));
    final int threshold_min_gradient_edges = (int)(tileSize * tileSize
        * this.searchParams.THRESHOLD_MIN_GRADIENT_EDGES_MULTIPLIER);

    int right_col, bottom_row;
    int prob_mat_right_col, prob_mat_bottom_row;

    // Used to hold sub-matrices into probability matrix that represent window around current point.
    Mat probWindow;

    int num_edges;
    double prob;
    int max_angle_idx, second_highest_angle_index, max_angle_count, second_highest_angle_count, angle_diff;

    this.imgDetails.probabilities.setTo(MatrixBarcode.ZERO_SCALAR);

    for(int i = 0, row_offset = 0; i < this.rows; i += tileSize, row_offset += probMatTileSize)
    {
      // First do bounds checking for bottom right of tiles.
      bottom_row = java.lang.Math.min((i + tileSize), this.rows);
      prob_mat_bottom_row = java.lang.Math.min((row_offset + probMatTileSize), this.imgDetails.probMatRows);

      for(int j = 0, col_offset = 0; j < this.cols; j += tileSize, col_offset += probMatTileSize)
      {
        // then calculate the column locations of the rectangle and set them to -1
        // if they are outside the matrix bounds
        right_col = java.lang.Math.min((j + tileSize), this.cols);
        prob_mat_right_col = java.lang.Math.min((col_offset + probMatTileSize), this.imgDetails.probMatCols);

        // calculate number of edges in the tile using the already calculated integral image
        num_edges = (int)this.calculateRectSum(this.imgDetails.edgeDensity, i, bottom_row, j, right_col);

        if(num_edges < threshold_min_gradient_edges)
        {
          // if gradient density is below the threshold level, prob of matrix code in this tile is 0
          continue;
        }

        for(int r = 0; r < ImageInfo.bins; r++)
        {
          this.imgDetails.histArray[r] = (int)this.calculateRectSum(this.imgDetails.histIntegrals.get(r), i, bottom_row,
              j, right_col);
        }

        MatrixBarcode.hist = Converters.vector_int_to_Mat(Arrays.asList(this.imgDetails.histArray));
        // Mat imgWindow = img_details.gradient_direction.submat(i, bottom_row, j, right_col);
        // Imgproc.calcHist(Arrays.asList(imgWindow), mChannels, histMask, hist, mHistSize, mRanges, false);
        Core.sortIdx(MatrixBarcode.hist, MatrixBarcode.histIdx, Core.SORT_EVERY_COLUMN + Core.SORT_DESCENDING);

        max_angle_idx = (int)MatrixBarcode.histIdx.get(0, 0)[0];
        max_angle_count = (int)MatrixBarcode.hist.get(max_angle_idx, 0)[0];

        second_highest_angle_index = (int)MatrixBarcode.histIdx.get(1, 0)[0];
        second_highest_angle_count = (int)MatrixBarcode.hist.get(second_highest_angle_index, 0)[0];

        angle_diff = Math.abs(max_angle_idx - second_highest_angle_index);

        // formula below is modified from Szentandrasi, Herout, Dubska paper pp. 4
        prob = 0;
        if(angle_diff != 1)
        {
          prob = (2.0 * Math.min(max_angle_count, second_highest_angle_count))
              / (max_angle_count + second_highest_angle_count);
        }

        probWindow = this.imgDetails.probabilities.submat(row_offset, prob_mat_bottom_row, col_offset,
            prob_mat_right_col);
        probWindow.setTo(new Scalar((int)(prob * 255)));

      } // for j
    } // for i

    return this.imgDetails.probabilities;
  }


  /**
   * Calculates number of edges in the image and returns it as an integral image.
   *
   * @return
   */
  private Mat calcEdgeDensityIntegralImage()
  {
    // First set all non-zero gradient magnitude points (i.e. all edges) to 1. Then calculate the integral image from
    // the above. Now the number of edges can be calculated in any tile in the matrix using the integral image.
    final Mat temp = new Mat();

    Imgproc.threshold(this.imgDetails.gradientMagnitude, temp, 1, 1, Imgproc.THRESH_BINARY);
    Imgproc.integral(temp, this.imgDetails.edgeDensity);

    return this.imgDetails.edgeDensity;
  }


  private void calcHistograms()
  {
    Mat target;
    MatrixBarcode.angles = this.imgDetails.gradientDirection.clone();

    for(int binRange = 1, integralIndex = 0; binRange < 181; binRange += ImageInfo.BIN_WIDTH, integralIndex++)
    {
      target = this.imgDetails.histIntegrals.get(integralIndex);

      this.imgDetails.gradientDirection.copyTo(MatrixBarcode.angles);
      Core.inRange(this.imgDetails.gradientDirection, MatrixBarcode.scalarDict.get(binRange),
          MatrixBarcode.scalarDict.get(binRange + ImageInfo.BIN_WIDTH), MatrixBarcode.mask);
      Core.bitwise_not(MatrixBarcode.mask, MatrixBarcode.mask);
      MatrixBarcode.angles.setTo(MatrixBarcode.ZERO_SCALAR, MatrixBarcode.mask);
      final Mat temp = new Mat();

      Imgproc.threshold(MatrixBarcode.angles, temp, 0, 1, Imgproc.THRESH_BINARY);
      Imgproc.integral(temp, target);
    }
  }

}
