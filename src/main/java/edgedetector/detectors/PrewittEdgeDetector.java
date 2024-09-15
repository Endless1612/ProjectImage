package edgedetector.detectors;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class PrewittEdgeDetector implements EdgeDetector {

    // Prewitt Kernels
    private static final int[][] PREWITT_X = {
            {-1, 0, 1},
            {-1, 0, 1},
            {-1, 0, 1}
    };

    private static final int[][] PREWITT_Y = {
            {1, 1, 1},
            {0, 0, 0},
            {-1, -1, -1}
    };

    @Override
    public File detectEdges(File inputFile) throws IOException {
        BufferedImage image = ImageIO.read(inputFile);
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage grayImage = convertToGrayscale(image);
        double[][] gradientX = applyConvolution(grayImage, PREWITT_X);
        double[][] gradientY = applyConvolution(grayImage, PREWITT_Y);

        double[][] gradientMagnitude = calculateGradientMagnitude(gradientX, gradientY);

        BufferedImage resultImage = createBinaryImage(gradientMagnitude, 100.0);

        File outputFile = new File("prewitt_output.png");
        ImageIO.write(resultImage, "png", outputFile);
        return outputFile;
    }

    // Convert the image to grayscale
    private BufferedImage convertToGrayscale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (r + g + b) / 3;
                int grayRgb = (gray << 16) | (gray << 8) | gray;
                grayImage.setRGB(x, y, grayRgb);
            }
        }
        return grayImage;
    }

    // Apply convolution with Prewitt kernel
    private double[][] applyConvolution(BufferedImage image, int[][] kernel) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] input = imageToMatrix(image);
        double[][] output = new double[height][width];

        int kernelSize = 3;
        int kernelOffset = kernelSize / 2;

        for (int y = kernelOffset; y < height - kernelOffset; y++) {
            for (int x = kernelOffset; x < width - kernelOffset; x++) {
                double sum = 0.0;
                for (int ky = 0; ky < kernelSize; ky++) {
                    for (int kx = 0; kx < kernelSize; kx++) {
                        int pixelX = x + kx - kernelOffset;
                        int pixelY = y + ky - kernelOffset;
                        sum += input[pixelY][pixelX] * kernel[ky][kx];
                    }
                }
                output[y][x] = sum;
            }
        }
        return output;
    }

    // Calculate the gradient magnitude using X and Y gradients
    private double[][] calculateGradientMagnitude(double[][] gradientX, double[][] gradientY) {
        int width = gradientX[0].length;
        int height = gradientX.length;
        double[][] magnitude = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                magnitude[y][x] = Math.sqrt(gradientX[y][x] * gradientX[y][x] + gradientY[y][x] * gradientY[y][x]);
            }
        }

        return magnitude;
    }

    // Create a binary image from the gradient magnitude
    private BufferedImage createBinaryImage(double[][] edges, double threshold) {
        int width = edges[0].length;
        int height = edges.length;
        BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = (edges[y][x] > threshold) ? 0xFFFFFF : 0x000000;
                resultImage.setRGB(x, y, color);
            }
        }

        return resultImage;
    }

    // Convert BufferedImage to 2D array of pixel intensities
    private double[][] imageToMatrix(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] matrix = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                matrix[y][x] = image.getRGB(x, y) & 0xFF; // Extract grayscale value
            }
        }
        return matrix;
    }
}