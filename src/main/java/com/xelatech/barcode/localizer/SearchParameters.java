package com.xelatech.barcode.localizer;


import org.opencv.core.Size;


/**
 * Contains settings for miscellaneous parameters used in searching for barcodes. Instances are returned by the factory
 * methods for small and large barcode searches.
 */
class SearchParameters
{
  /**
   * Threshold below which normalized variance is considered to be low enough for angles in that area to be mostly
   * unidirectional (used only for 1D barcodes).
   */
  static final double THRESHOLD_VARIANCE = 75;

  Size elementSize;

  Size largeElementSize;

  /**
   * Image with more rows than MAX_ROWS is scaled down to make finding barcode quicker.
   */
  final int MAX_ROWS = 500;

  /**
   * Threshold for ratio of contour area to bounding rectangle area - used to see if contour shape is roughly
   * rectangular.
   */
  double THRESHOLD_AREA_RATIO = 0.4;

  /**
   * Multipliers to calculate threshold values as a function of image size
   */
  double THRESHOLD_MIN_AREA_MULTIPLIER;

  double THRESHOLD_MIN_GRADIENT_EDGES_MULTIPLIER;

  double RECT_HEIGHT_MULTIPLIER;

  double RECT_WIDTH_MULTIPLIER;

  /**
   * Size of rectangular window to calculate variance, probability etc around each pixel. each window of RECT_HEIGHT X
   * RECT_WIDTH size becomes a square of side PROB_MAT_TILE_SIZE.
   */
  protected int PROB_MAT_TILE_SIZE = 2;

  protected int RECT_WIDTH;

  protected int RECT_HEIGHT;

  /**
   * Min area for candidate region to be considered as a barcode.
   */
  protected double THRESHOLD_MIN_AREA;

  /**
   * Used to expand candidate barcode region by looking for quiet zone around the barcode.
   */
  int NUM_BLANKS_THRESHOLD;

  int MATRIX_NUM_BLANKS_THRESHOLD;

  protected int tileSize;

  protected double scaleFactor;

  /**
   * Sets to true if these are parameters optimized for very small matrix code.
   */
  boolean isVerySmallMatrix = false;


  private SearchParameters()
  {
  }


  /**
   * Returns normal parameters used in barcode search.
   *
   * @return
   */
  static SearchParameters getNormalParameters()
  {
    final SearchParameters params = new SearchParameters();

    params.THRESHOLD_MIN_AREA_MULTIPLIER = 0.02;
    params.THRESHOLD_MIN_GRADIENT_EDGES_MULTIPLIER = 0.3;

    params.NUM_BLANKS_THRESHOLD = 20;
    params.MATRIX_NUM_BLANKS_THRESHOLD = 10;

    params.RECT_HEIGHT_MULTIPLIER = 0.1;
    params.RECT_WIDTH_MULTIPLIER = 0.1;

    // params.RECT_HEIGHT = 10;
    // params.RECT_WIDTH = params.RECT_HEIGHT * 6;

    params.elementSize = new Size(10, 10);
    params.largeElementSize = new Size(12, 12);
    // params.tileSize = params.RECT_HEIGHT;
    // params.scale_factor = params.PROB_MAT_TILE_SIZE / (params.RECT_HEIGHT * 1.0);

    return params;
  }


  /**
   * Returns parameters used when searching for barcodes that are small relative to image size used as one of the
   * TRY_HARDER options.
   *
   * @return
   */
  static SearchParameters getSmallParameters()
  {
    final SearchParameters params = new SearchParameters();

    params.THRESHOLD_MIN_AREA_MULTIPLIER = 0.02;
    params.THRESHOLD_MIN_GRADIENT_EDGES_MULTIPLIER = 0.3;

    params.NUM_BLANKS_THRESHOLD = 10;
    params.MATRIX_NUM_BLANKS_THRESHOLD = 5;

    params.RECT_HEIGHT_MULTIPLIER = 0.02;
    params.RECT_WIDTH_MULTIPLIER = 0.02;

    // params.RECT_HEIGHT = 10;
    // params.RECT_WIDTH = params.RECT_HEIGHT * 6;

    params.elementSize = new Size(10, 10);
    params.largeElementSize = new Size(12, 12);
    // params.tileSize = params.RECT_HEIGHT;
    // params.scale_factor = params.PROB_MAT_TILE_SIZE / (params.RECT_HEIGHT * 1.0);

    return params;
  }


  /**
   * Returns parameters used when searching for barcodes that are large relative to image size. It has some success with
   * localizing blurry barcodes also though they probably will not decode correctly used with one of the TRY_HARDER
   * options.
   *
   * @return
   */
  static SearchParameters getLargeParameters()
  {
    final SearchParameters params = new SearchParameters();

    params.THRESHOLD_MIN_AREA_MULTIPLIER = 0.02;
    params.THRESHOLD_MIN_GRADIENT_EDGES_MULTIPLIER = 0.2;

    params.NUM_BLANKS_THRESHOLD = 40;
    params.MATRIX_NUM_BLANKS_THRESHOLD = 20;

    params.RECT_HEIGHT_MULTIPLIER = 0.1;
    params.RECT_WIDTH_MULTIPLIER = 0.1;

    // params.RECT_HEIGHT = 20;
    // params.RECT_WIDTH = params.RECT_HEIGHT * 6;

    params.elementSize = new Size(20, 20);
    params.largeElementSize = new Size(24, 24);
    // params.tileSize = params.RECT_HEIGHT;
    // params.scale_factor = params.PROB_MAT_TILE_SIZE / (params.RECT_HEIGHT * 1.0);

    return params;
  }


  /**
   * Returns parameters used when searching for 1D barcodes that are small relative to image size used as one of the
   * TRY_HARDER options.
   *
   * @return
   */
  static SearchParameters getVerySmallLinearParameters()
  {
    final SearchParameters params = new SearchParameters();

    // params.THRESHOLD_MIN_AREA_MULTIPLIER = 0.001;
    params.THRESHOLD_MIN_GRADIENT_EDGES_MULTIPLIER = 0.3;

    params.NUM_BLANKS_THRESHOLD = 5;
    params.MATRIX_NUM_BLANKS_THRESHOLD = 3;

    params.RECT_HEIGHT_MULTIPLIER = 0.01;
    params.RECT_WIDTH_MULTIPLIER = 0.01;

    // params.RECT_HEIGHT = 5;
    // params.RECT_WIDTH = params.RECT_HEIGHT * 6;

    params.elementSize = new Size(params.PROB_MAT_TILE_SIZE * 1.5, params.PROB_MAT_TILE_SIZE * 1.5);
    params.largeElementSize = new Size(params.PROB_MAT_TILE_SIZE * 2, params.PROB_MAT_TILE_SIZE * 2);
    // params.elementSize = new Size(5, 5);
    // params.largeElementSize = new Size(8, 8);

    // params.tileSize = params.RECT_HEIGHT;
    // params.scale_factor = params.PROB_MAT_TILE_SIZE / (params.RECT_HEIGHT * 1.0);

    return params;
  }


  /**
   * Returns parameters used when searching for matrix barcodes that are small relative to image size used as one of the
   * TRY_HARDER options.
   *
   * @return
   */
  static SearchParameters getVerySmallMatrixParameters()
  {
    final SearchParameters params = new SearchParameters();

    params.isVerySmallMatrix = true;
    params.THRESHOLD_MIN_GRADIENT_EDGES_MULTIPLIER = 0.3;

    params.RECT_HEIGHT = params.RECT_WIDTH = 10;

    // Set small element size to 50% bigger than tile height/width to erode small elements away.
    params.elementSize = new Size(params.PROB_MAT_TILE_SIZE * 1.5, params.PROB_MAT_TILE_SIZE * 1.5);
    params.largeElementSize = new Size(params.PROB_MAT_TILE_SIZE * 2, params.PROB_MAT_TILE_SIZE * 2);
    params.tileSize = params.RECT_HEIGHT;
    params.scaleFactor = params.PROB_MAT_TILE_SIZE / (params.RECT_HEIGHT * 1.0);

    return params;
  }


  /**
   * Sets parameters that are specific to the image being processed based on the size of the image(potentially after it
   * is pre-processed and rescaled).
   *
   * @param rows
   * @param cols
   * @return
   */
  public SearchParameters setImageSpecificParameters(final int rows, final int cols)
  {
    if(this.isVerySmallMatrix == true)
    {
      this.THRESHOLD_MIN_AREA = 250;
    }
    else
    {
      this.THRESHOLD_MIN_AREA = 250;
      // this.THRESHOLD_MIN_AREA = this.THRESHOLD_MIN_AREA_MULTIPLIER * cols * rows;
      this.RECT_HEIGHT = (int)(this.RECT_HEIGHT_MULTIPLIER * rows);
      this.RECT_WIDTH = this.RECT_HEIGHT;
      // this.RECT_WIDTH = (int)(this.RECT_WIDTH_MULTIPLIER * cols);

      this.tileSize = this.RECT_HEIGHT;
      this.scaleFactor = this.PROB_MAT_TILE_SIZE / (this.RECT_HEIGHT * 1.0);
    }

    return this;
  }

}
