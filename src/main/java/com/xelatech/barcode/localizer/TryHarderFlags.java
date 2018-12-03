package com.xelatech.barcode.localizer;


/**
 * Enum to control how hard the localizer tries to find a barcode, default set to NORMAL.
 */
public enum TryHarderFlags
{
  /*
   * NORMAL, SMALL, LARGE and V_SMALL control the size of the window used to find the barcode
   * Eg. if the barcode is small compared to the image size, SMALL has a better chance of finding it
   * ALL_SIZES will try all of the sizes
   * POSTPROCESS_RESIZE_BARCODE enlarges the captured region after localizing it.
   * This can sometimes help the reader decode it more easily.
   */
  NORMAL(1), SMALL(2), LARGE(4), VERY_SMALL_LINEAR(8), VERY_SMALL_MATRIX(16), ALL(255);

  private int val;


  TryHarderFlags(final int val)
  {
    this.val = val;
  }


  int value()
  {
    return this.val;
  }
}
