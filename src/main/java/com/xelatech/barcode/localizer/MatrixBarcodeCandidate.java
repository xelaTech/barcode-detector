package com.xelatech.barcode.localizer;


import java.util.Arrays;
import java.util.Collections;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;


/**
 * CandidateRegion is a RotatedRect which contains a candidate region for the barcode.
 */
public class MatrixBarcodeCandidate extends BarcodeCandidate
{
  public MatrixBarcodeCandidate(final ImageInfo img_details, final RotatedRect minRect, final SearchParameters params)
  {
    super(img_details, minRect, params);

    final int factor = params.RECT_HEIGHT / params.PROB_MAT_TILE_SIZE;
    final Point candidateCentre = new Point(minRect.center.x * factor, minRect.center.y * factor);
    final Size candidateSize = new Size(minRect.size.width * factor, minRect.size.height * factor);
    final RotatedRect candidateRect = new RotatedRect(candidateCentre, candidateSize, minRect.angle);
    this.candidateRegion = candidateRect;
  }


  /**
   * Angle is the rotation angle or USE_ROTATED_RECT_ANGLE for this function to estimate rotation angle from the rect
   * parameter returns Mat containing cropped area(region of interest) with just the barcode. The barcode region is from
   * the *original* image, not the scaled image the cropped area is also rotated as necessary to be horizontal or
   * vertical rather than skewed. Some parts of this function are from
   * http://felix.abecassis.me/2011/10/opencv-rotation-deskewing/ and
   * http://stackoverflow.com/questions/22041699/rotate-an-image-without-cropping-in-opencv-in-c
   */
  public CandidateResult normalizeCandidateRegion(final double angle)
  {
    // Scale candidate region back up to original size to return cropped part from *original* image.
    // Need the 1.0 there to force floating-point arithmetic from integer values.
    final double scaleFactor = this.imgDetails.srcOriginal.rows() / (1.0 * this.imgDetails.srcGrayscale.rows());

    // Expand the region found - this helps capture the entire code including the border zone.
    this.candidateRegion.size.width += 2 * this.params.RECT_WIDTH;
    this.candidateRegion.size.height += 2 * this.params.RECT_HEIGHT;

    // Calculate location of rectangle in original image and its corner points.
    final RotatedRect scaledRegion = new RotatedRect(this.candidateRegion.center, this.candidateRegion.size,
        this.candidateRegion.angle);
    scaledRegion.center.x = scaledRegion.center.x * scaleFactor;
    scaledRegion.center.y = scaledRegion.center.y * scaleFactor;
    scaledRegion.size.height *= scaleFactor;
    scaledRegion.size.width *= scaleFactor;

    scaledRegion.points(this.imgDetails.scaledCorners);

    // Lets get the coordinates of the ROI in the original image and save it.
    final CandidateResult result = new CandidateResult();
    result.ROICoords = Arrays.copyOf(this.imgDetails.scaledCorners, 4);

    // Get the bounding rectangle of the ROI by sorting its corner points. Do it manually because RotatedRect can
    // generate corner points outside the Mat area.
    Arrays.sort(this.imgDetails.scaledCorners, BarcodeCandidate.get_x_comparator());
    int leftCol = (int)this.imgDetails.scaledCorners[0].x;
    int rightCol = (int)this.imgDetails.scaledCorners[3].x;
    leftCol = (leftCol < 0) ? 0 : leftCol;
    rightCol = (rightCol > (this.imgDetails.srcOriginal.cols() - 1)) ? this.imgDetails.srcOriginal.cols() - 1
        : rightCol;

    Arrays.sort(this.imgDetails.scaledCorners, BarcodeCandidate.get_y_comparator());
    int topRow = (int)this.imgDetails.scaledCorners[0].y;
    int bottomRow = (int)this.imgDetails.scaledCorners[3].y;
    topRow = (topRow < 0) ? 0 : topRow;
    bottomRow = (bottomRow > (this.imgDetails.srcOriginal.rows() - 1)) ? this.imgDetails.srcOriginal.rows() - 1
        : bottomRow;

    final Mat ROI = this.imgDetails.srcOriginal.submat(topRow, bottomRow, leftCol, rightCol);

    // Create a container that is a square with side = diagonal of ROI. This is large enough to accommodate the ROI
    // region with rotation without cropping it.
    final int origRows = bottomRow - topRow;
    final int origCols = rightCol - leftCol;
    final int diagonal = (int)Math.sqrt((origRows * origRows) + (origCols * origCols));

    final int newWidth = diagonal + 1;
    final int newHeight = diagonal + 1;

    final int offsetX = (newWidth - origCols) / 2;
    final int offsetY = (newHeight - origRows) / 2;

    final Mat enlargedROIContainer = new Mat(newWidth, newHeight, this.imgDetails.srcOriginal.type());
    enlargedROIContainer.setTo(BarcodeCandidate.ZERO_SCALAR);

    // Copy ROI to the center of container and rotate it.
    ROI.copyTo(enlargedROIContainer.rowRange(offsetY, offsetY + origRows).colRange(offsetX, offsetX + origCols));
    final Point enlargedCenteredROIContainer = new Point(enlargedROIContainer.rows() / 2.0,
        enlargedROIContainer.cols() / 2.0);
    final Mat rotated = Mat.zeros(enlargedROIContainer.size(), enlargedROIContainer.type());

    double rotationAngle;
    if(angle == Barcode.USE_ROTATED_RECT_ANGLE)
    {
      rotationAngle = this.estimateBarcodeOrientation();
    }
    else
    {
      rotationAngle = angle;
    }

    // Perform the Affine transformation.
    this.imgDetails.rotationMatrix = Imgproc.getRotationMatrix2D(enlargedCenteredROIContainer, rotationAngle, 1.0);

    // Convert type so that matrix multiplication works properly.
    this.imgDetails.rotationMatrix.convertTo(this.imgDetails.rotationMatrix, CvType.CV_32F);

    this.imgDetails.newCornerCoord.setTo(BarcodeCandidate.ZERO_SCALAR);

    // Convert scaledCorners to contain locations of corners in enlarged_ROI_container Mat.
    this.imgDetails.scaledCorners[0] = new Point(offsetX, offsetY);
    this.imgDetails.scaledCorners[1] = new Point(offsetX, offsetY + origRows);
    this.imgDetails.scaledCorners[2] = new Point(offsetX + origCols, offsetY);
    this.imgDetails.scaledCorners[3] = new Point(offsetX + origCols, offsetY + origRows);

    // Calculate the new location for each corner point of the rectangle ROI after rotation.
    for(int r = 0; r < 4; r++)
    {
      this.imgDetails.coord.put(0, 0, this.imgDetails.scaledCorners[r].x);
      this.imgDetails.coord.put(1, 0, this.imgDetails.scaledCorners[r].y);
      Core.gemm(this.imgDetails.rotationMatrix, this.imgDetails.coord, 1, this.imgDetails.delta, 0,
          this.imgDetails.newCornerCoord);
      BarcodeCandidate.updatePoint(this.imgDetails.newCornerPoints.get(r), this.imgDetails.newCornerCoord.get(0, 0)[0],
          this.imgDetails.newCornerCoord.get(1, 0)[0]);
    }

    rotated.setTo(BarcodeCandidate.ZERO_SCALAR);
    Imgproc.warpAffine(enlargedROIContainer, rotated, this.imgDetails.rotationMatrix, enlargedROIContainer.size(),
        Imgproc.INTER_CUBIC);

    // Sort rectangles points in order by first sorting all 4 points based on x. Then sort the first two based on y and
    // then the next two based on y. This leaves the array in order top-left, bottom-left, top-right, bottom-right.
    Collections.sort(this.imgDetails.newCornerPoints, BarcodeCandidate.get_x_comparator());
    Collections.sort(this.imgDetails.newCornerPoints.subList(0, 2), BarcodeCandidate.get_y_comparator());
    Collections.sort(this.imgDetails.newCornerPoints.subList(2, 4), BarcodeCandidate.get_y_comparator());

    // Calculate height and width of rectangular region.
    final double height = this.length(this.imgDetails.newCornerPoints.get(1), this.imgDetails.newCornerPoints.get(0));
    final double width = this.length(this.imgDetails.newCornerPoints.get(2), this.imgDetails.newCornerPoints.get(0));

    // Create destination points for warpPerspective to map to.
    BarcodeCandidate.updatePoint(this.imgDetails.transformedPoints.get(0), 0, 0);
    BarcodeCandidate.updatePoint(this.imgDetails.transformedPoints.get(1), 0, height);
    BarcodeCandidate.updatePoint(this.imgDetails.transformedPoints.get(2), width, 0);
    BarcodeCandidate.updatePoint(this.imgDetails.transformedPoints.get(3), width, height);

    final Mat perspectiveTransform = Imgproc.getPerspectiveTransform(
        Converters.vector_Point2f_to_Mat(this.imgDetails.newCornerPoints),
        Converters.vector_Point2f_to_Mat(this.imgDetails.transformedPoints));
    final Mat perspectiveOut = Mat.zeros((int)height + 2, (int)width + 2, CvType.CV_32F);
    Imgproc.warpPerspective(rotated, perspectiveOut, perspectiveTransform, perspectiveOut.size(), Imgproc.INTER_CUBIC);

    result.ROI = perspectiveOut;

    return result;
  }
}
