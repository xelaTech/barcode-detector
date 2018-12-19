package com.xelatech.barcode.localizer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class ImageDisplay extends JPanel {
	BufferedImage image;

	private static JPanel imagePanel;

	private static JFrame frame;

	private ImageDisplay(final String imageFile) {
		try {
			/**
			 * ImageIO.read() returns a BufferedImage object, decoding the supplied file
			 * with an ImageReader, chosen automatically from registered files The File is
			 * wrapped in an ImageInputStream object, so we don't need one. Null is
			 * returned, If no registered ImageReader claims to be able to read the
			 * resulting stream.
			 */
			this.image = ImageIO.read(new File(imageFile));
		} catch (final IOException e) {
			// Let us know what happened
			System.out.println("Error reading dir: " + e.getMessage());
		}

	}

	private ImageDisplay(final Mat openCVImage) {
		try {
			this.image = ImageDisplay.getBufImg(openCVImage);
		} catch (final IOException e) {
			// Let us know what happened
			System.out.println("Error converting openCV image: " + e.getMessage());
		}
	}

	private ImageDisplay(final BufferedImage img) {
		this.image = img;
	}

	protected static BufferedImage getBufImg(final Mat image) throws IOException {
		// converts image in an openCV Mat object into a Java BufferedImage
		final MatOfByte bytemat = new MatOfByte();
		Imgcodecs.imencode(".jpg", image, bytemat);
		final InputStream in = new ByteArrayInputStream(bytemat.toArray());
		final BufferedImage img = ImageIO.read(in);
		return img;
	}

	@Override
	public Dimension getPreferredSize() {
		// set our preferred size if we succeeded in loading image
		if (this.image == null) {
			return new Dimension(100, 100);
		} else {
			return new Dimension(this.image.getWidth(null), this.image.getHeight(null));
		}
	}

	@Override
	public void paint(final Graphics g) {
		// Draw our image on the screen with Graphic's "drawImage()" method
		g.drawImage(this.image, 0, 0, null);
	}

	public static void showImageFrame(final String imageFile) {
		// convenience function that displays a frame with the image in the parameters
		ImageDisplay.displayFrame(new ImageDisplay(imageFile), imageFile);
	}

	public static void showImageFrame(final Mat openCVImage, final String title) {
		// convenience function that displays a frame with the image in the parameters
		ImageDisplay.displayFrame(new ImageDisplay(openCVImage), title);
	}

	public static ImageDisplay getImageFrame(final Mat openCVImage, final String title) {
		// convenience function that displays a frame with the image in the parameters
		final ImageDisplay window = new ImageDisplay(openCVImage);
		ImageDisplay.displayFrame(window, title);
		return window;
	}

	public void updateImage(final Mat img, final String title) {
		try {
			this.image = ImageDisplay.getBufImg(img);
		} catch (final IOException e) {
			// Let us know what happened
			System.out.println("Error converting openCV image: " + e.getMessage());
		}
		ImageDisplay.frame.setTitle(title);
		this.repaint();
	}

	public static void showImageFrameGrid(final Mat openCVImage, final String title) {
		// convenience function that displays a frame with the image in the parameters
		final Mat displayImg = openCVImage.clone();
		if (openCVImage.channels() < 3) {
			Imgproc.cvtColor(openCVImage, displayImg, Imgproc.COLOR_GRAY2BGR);
		}

		final int rows = displayImg.rows();
		final int cols = displayImg.cols();
		// draw rows
		for (int i = 0; i < rows; i += 10) {
			Imgproc.line(displayImg, new Point(0, i), new Point(cols - 1, i), new Scalar(0, 128, 255));
		}

		for (int i = 0; i < cols; i += 10) {
			Imgproc.line(displayImg, new Point(i, 0), new Point(i, rows - 1), new Scalar(0, 128, 255));
		}

		if (displayImg.rows() > 750) {
			Imgproc.resize(displayImg, displayImg, new Size(1000, 750));
		}

		ImageDisplay.displayFrame(new ImageDisplay(displayImg), title);
	}

	public static void showImageFrame(final BufferedImage img, final String title) {
		// convenience function that displays a frame with the image in the parameters
		ImageDisplay.displayFrame(new ImageDisplay(img), title);
	}

	private static void displayFrame(final ImageDisplay img, final String title) {
		// internal function that displays a frame with the image in the parameters

		ImageDisplay.frame = new JFrame(title);
		ImageDisplay.imagePanel = new JPanel();
		ImageDisplay.imagePanel.add(img);
		final JScrollPane scroll = new JScrollPane(ImageDisplay.imagePanel);
		ImageDisplay.frame.add(scroll);
		ImageDisplay.frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		ImageDisplay.frame.pack();
		ImageDisplay.frame.setVisible(true);
	}

	public void close() {
		ImageDisplay.frame.dispose();
	}
}
