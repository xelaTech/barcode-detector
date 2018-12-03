package com.xelatech.barcode.localizer;


import java.util.Comparator;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


class BarcodeCandidate
{
  protected ImageInfo imgDetails;

  protected RotatedRect candidateRegion;

  protected int num_blanks;

  protected SearchParameters params;

  /**
   * Threshold for number of blanks around barcode.
   */
  protected int threshold;

  private static Compare_x x_comparator = null;

  private static Compare_y y_comparator = null;

  protected static final Scalar ZERO_SCALAR = new Scalar(0);


  protected BarcodeCandidate(final ImageInfo imgDetails, final RotatedRect minRect, final SearchParameters params)
  {
    this.imgDetails = imgDetails;
    this.candidateRegion = minRect;

    this.params = params;

    // Set threshold for number of blanks around barcode based on whether it is linear or 2D code.
    this.threshold = (imgDetails.searchType == Barcode.CodeType.LINEAR) ? params.NUM_BLANKS_THRESHOLD
        : params.MATRIX_NUM_BLANKS_THRESHOLD;
  }


  /**
   * Convenience function to draw outline of candidate region on image.
   *
   * @param colour
   * @param img
   */
  protected void markCandidateRegion(final Scalar colour, final Mat img)
  {
    final Point rectPoints[] = new Point[4];
    this.candidateRegion.points(rectPoints);

    // Draw a rectangle.
    for(int j = 0; j < 3; j++)
    {
      Imgproc.line(img, rectPoints[j], rectPoints[j + 1], colour, 2, Imgproc.LINE_AA, 0);
    }

    Imgproc.line(img, rectPoints[3], rectPoints[0], colour, 2, Imgproc.LINE_AA, 0);
  }


  /**
   * Uses angle of orientation of enclosing rotated rectangle to rotate barcode and make it horizontal (only relevant
   * for 1D barcodes currently).
   *
   * @return
   */
  protected double estimateBarcodeOrientation()
  {
    // Get angle and size from the bounding box.
    double orientation = this.candidateRegion.angle + 90;
    final Size rect_size = new Size(this.candidateRegion.size.width, this.candidateRegion.size.height);

    // Find orientation for barcode.
    if(rect_size.width < rect_size.height)
    {
      orientation += 90;
    }

    // Rotate 90 degrees from its orientation to straighten it out.
    return(orientation - 90);
  }


  /**
   * Returns length of line segment between (x1, y1) and (x2, y2).
   *
   * @param p1
   * @param p2
   * @return
   */
  protected double length(final Point p1, final Point p2)
  {
    return Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
  }


  protected static void updatePoint(final Point p, final double newX, final double newY)
  {
    p.x = newX;
    p.y = newY;
  }


  protected static Compare_x get_x_comparator()
  {
    // factory method to return one instance of a Compare_x object
    if(BarcodeCandidate.x_comparator == null)
    {
      BarcodeCandidate.x_comparator = new Compare_x();
    }
    return BarcodeCandidate.x_comparator;
  }


  protected static Compare_y get_y_comparator()
  {
    // factory method to return one instance of a Compare_x object
    if(BarcodeCandidate.y_comparator == null)
    {
      BarcodeCandidate.y_comparator = new Compare_y();
    }
    return BarcodeCandidate.y_comparator;
  }

  protected static class Compare_x implements Comparator<Point>
  {
    // Comparator class to compare x coordinate of Point objects
    @Override
    public int compare(final Point a, final Point b)
    {
      if(a.x == b.x)
      {
        return 0;
      }
      if(a.x > b.x)
      {
        return 1;
      }
      return -1;
    }
  }

  protected static class Compare_y implements Comparator<Point>
  {
    // Comparator class to compare x coordinate of Point objects
    @Override
    public int compare(final Point a, final Point b)
    {
      if(a.y == b.y)
      {
        return 0;
      }
      if(a.y > b.y)
      {
        return 1;
      }
      return -1;
    }
  }
}
