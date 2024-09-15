package edgedetector.util;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class Threshold {

   /**
    * Calculates threshold as the mean of the |G| matrix for edge detection algorithms.
    * @param magnitude the magnitude of the gradient for each pixel in the image
    * @return the calculated threshold based on the mean value of the magnitudes
    */
   public static int calcThresholdEdges(int[][] magnitude) {
      return (int) Statistics.calcMean(magnitude);
   }

   /**
    * Returns BufferedImage where color at (i, j) is black if pixel intensity >
    * threshold; white otherwise.
    * @param pixels the pixel values of the image
    * @param threshold the threshold value for binarization
    * @return the thresholded BufferedImage
    */
   public static BufferedImage applyThreshold(int[][] pixels, int threshold) {
      int height = pixels.length;
      int width = pixels[0].length;

      BufferedImage thresholdedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
      WritableRaster raster = thresholdedImage.getRaster();

      int[] black = {0, 0, 0};
      int[] white = {255, 255, 255};

      // Cache-efficient for both BufferedImage and int[][]
      for (int row = 0; row < height; row++) {
         for (int col = 0; col < width; col++) {
            raster.setPixel(col, row, pixels[row][col] > threshold ? white : black);
         }
      }
      return thresholdedImage;
   }

   /**
    * Applies a threshold to the image based on boolean pixel values.
    * @param pixels the binary values representing edges
    * @return a BufferedImage with white for true (edge) and black for false (non-edge)
    */
   public static BufferedImage applyThreshold(boolean[][] pixels) {
      int height = pixels.length;
      int width = pixels[0].length;

      BufferedImage thresholdedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
      WritableRaster raster = thresholdedImage.getRaster();

      int[] black = {0, 0, 0};
      int[] white = {255, 255, 255};

      // Cache-efficient for both BufferedImage and int[][]
      for (int row = 0; row < height; row++) {
         for (int col = 0; col < width; col++) {
            raster.setPixel(col, row, pixels[row][col] ? white : black);
         }
      }
      return thresholdedImage;
   }

   /**
    * Applies a threshold to the image and reverses the pixel values.
    * @param pixels the binary values representing edges
    * @return a BufferedImage with black for true (edge) and white for false (non-edge)
    */
   public static BufferedImage applyThresholdReversed(boolean[][] pixels) {
      int height = pixels.length;
      int width = pixels[0].length;

      BufferedImage thresholdedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
      WritableRaster raster = thresholdedImage.getRaster();

      int[] black = {0, 0, 0};
      int[] white = {255, 255, 255};

      // Cache-efficient for both BufferedImage and int[][]
      for (int row = 0; row < height; row++) {
         for (int col = 0; col < width; col++) {
            raster.setPixel(col, row, pixels[row][col] ? black : white);
         }
      }
      return thresholdedImage;
   }

   /**
    * Applies a threshold to distinguish weak and strong edges using different colors.
    * @param weakEdges the weak edge binary values
    * @param strongEdges the strong edge binary values
    * @return a BufferedImage with blue for weak edges and green for strong edges
    */
   public static BufferedImage applyThresholdWeakStrongCanny(boolean[][] weakEdges, boolean[][] strongEdges) {
      int height = weakEdges.length;
      int width = weakEdges[0].length;

      BufferedImage thresholdedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
      WritableRaster raster = thresholdedImage.getRaster();

      int[] white = {255, 255, 255};
      int[] blue = {0, 0, 255};
      int[] green = {0, 255, 0};

      // Cache-efficient for both BufferedImage and int[][]
      for (int row = 0; row < height; row++) {
         for (int col = 0; col < width; col++) {
            if (strongEdges[row][col]) {
               raster.setPixel(col, row, green);
            } else if (weakEdges[row][col]) {
               raster.setPixel(col, row, blue);
            } else {
               raster.setPixel(col, row, white);
            }
         }
      }

      return thresholdedImage;
   }

   /**
    * Applies a threshold and keeps the original image color for edges.
    * @param edges the binary values representing edges
    * @param originalImage the original image to maintain its color
    * @return a new BufferedImage with white for non-edges and original color for edges
    */
   public static BufferedImage applyThresholdOriginal(boolean[][] edges, BufferedImage originalImage) {
      int height = edges.length;
      int width = edges[0].length;

      BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
      WritableRaster raster_new = newImage.getRaster();
      WritableRaster raster_old = originalImage.getRaster();

      int[] white = {255, 255, 255};
      int[] arr = new int[3];
      int min;

      for (int row = 0; row < height; row++) {
         for (int col = 0; col < width; col++) {
            if (!edges[row][col]) {
               raster_new.setPixel(col, row, white);
            } else {
               // Get original pixel color
               raster_old.getPixel(col, row, arr);

               // Scale to max intensity
               min = 255;
               for (int i : arr) {
                  if (i < min) {
                     min = i;
                  }
               }
               double scale = 255.0 / (255.0 - min);
               for (int i = 0; i < 3; i++) {
                  arr[i] = 255 - (int) (scale * (255.0 - arr[i]));
               }
               raster_new.setPixel(col, row, arr);
            }
         }
      }
      return newImage;
   }
}