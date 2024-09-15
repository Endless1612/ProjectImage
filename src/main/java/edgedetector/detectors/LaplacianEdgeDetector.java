package edgedetector.detectors;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import edgedetector.imagederivatives.ConvolutionKernel;
import edgedetector.imagederivatives.ImageConvolution;
import edgedetector.util.Grayscale;
import edgedetector.util.Threshold;

public class LaplacianEdgeDetector implements EdgeDetector {

    private boolean[][] edges;
    private int threshold;
    private int rows;
    private int columns;

    // Laplacian Kernel
    private static final double[][] LAPLACIAN_KERNEL = {
            {-1, -1, -1},
            {-1,  8, -1},
            {-1, -1, -1}
    };

    @Override
    public File detectEdges(File imageFile) throws IOException {
        BufferedImage originalImage = ImageIO.read(imageFile);
        int[][] pixels = Grayscale.imgToGrayPixels(originalImage);

        // Gaussian smoothing
        ImageConvolution gaussianConvolution = new ImageConvolution(pixels, ConvolutionKernel.GAUSSIAN_KERNEL);
        int[][] smoothedImage = gaussianConvolution.getConvolvedImage();

        // Apply Laplacian Kernel to detect edges
        ImageConvolution laplacianConvolution = new ImageConvolution(smoothedImage, LAPLACIAN_KERNEL);
        int[][] convolvedImage = laplacianConvolution.getConvolvedImage();

        rows = convolvedImage.length;
        columns = convolvedImage[0].length;

        // Calculate threshold
        threshold = Threshold.calcThresholdEdges(convolvedImage);

        // Thresholding
        edges = new boolean[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                edges[i][j] = Math.abs(convolvedImage[i][j]) > threshold;
            }
        }

        // Create output image
        BufferedImage edgeImage = Threshold.applyThresholdReversed(edges);
        File result = new File("laplacian_edge_result.png");
        ImageIO.write(edgeImage, "png", result);
        return result;
    }

    public boolean[][] getEdges() {
        return edges;
    }
}