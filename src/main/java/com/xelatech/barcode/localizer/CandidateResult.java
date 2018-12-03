package com.xelatech.barcode.localizer;


import java.awt.image.BufferedImage;

import org.opencv.core.Mat;
import org.opencv.core.Point;


public class CandidateResult
{
  public Mat ROI;

  public Point[] ROICoords;

  public BufferedImage candidate;


  public String getROICoords()
  {
    final StringBuffer result = new StringBuffer("");

    for(final Point p : this.ROICoords)
    {
      result.append("(" + (Math.round(p.x * 1000) / 1000.0) + "," + (Math.round(p.y * 1000) / 1000.0) + "), ");
    }
    return result.toString();
  }
}
