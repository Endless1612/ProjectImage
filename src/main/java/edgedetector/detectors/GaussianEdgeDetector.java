package edgedetector.detectors;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import edgedetector.imagederivatives.ConvolutionKernel;
import edgedetector.imagederivatives.ImageConvolution;
import edgedetector.util.Grayscale;
import edgedetector.util.Threshold;

public class GaussianEdgeDetector implements EdgeDetector {

    private static final double[][] X_KERNEL = {{-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}};
    private static final double[][] Y_KERNEL = {{1, 2, 1},
            {0, 0, 0},
            {-1, -2, -1}};

    private boolean[][] edges;
    private int rows;
    private int columns;

    @Override
    public File detectEdges(File imageFile) throws IOException {
        BufferedImage originalImage = ImageIO.read(imageFile);
        int[][] pixels = Grayscale.imgToGrayPixels(originalImage);

        // Step 1: Gaussian Smoothing (Blur)
        ImageConvolution gaussianConvolution = new ImageConvolution(pixels, ConvolutionKernel.GAUSSIAN_KERNEL);
        int[][] smoothedImage = gaussianConvolution.getConvolvedImage();

        // Step 2: Apply Sobel Operator (Gradient Calculation)
        ImageConvolution x_ic = new ImageConvolution(smoothedImage, X_KERNEL);
        ImageConvolution y_ic = new ImageConvolution(smoothedImage, Y_KERNEL);

        int[][] x_imageConvolution = x_ic.getConvolvedImage();
        int[][] y_imageConvolution = y_ic.getConvolvedImage();

        rows = x_imageConvolution.length;
        columns = x_imageConvolution[0].length;

        // Step 3: Compute gradient magnitude
        int[][] mag = new int[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                mag[i][j] = (int) Math.hypot(x_imageConvolution[i][j], y_imageConvolution[i][j]);
            }
        }

        // Step 4: Thresholding
        edges = new boolean[rows][columns];
        int threshold = Threshold.calcThresholdEdges(mag);  // Automatically calculate a threshold
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                edges[i][j] = mag[i][j] >= threshold;
            }
        }

        // Step 5: Create output image
        BufferedImage edgeImage = Threshold.applyThresholdReversed(edges);
        File result = new File("gaussian_edge_result.png");
        ImageIO.write(edgeImage, "png", result);
        return result;
    }

    public boolean[][] getEdges() {
        return edges;
    }
}