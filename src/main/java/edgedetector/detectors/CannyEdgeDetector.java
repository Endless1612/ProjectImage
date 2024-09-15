package edgedetector.detectors;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Stack;

import javax.imageio.ImageIO;

import edgedetector.util.KMeans;
import edgedetector.util.CSVwriter;
import edgedetector.util.Hypotenuse;
import edgedetector.util.Threshold;
import edgedetector.imagederivatives.ConvolutionKernel;
import edgedetector.imagederivatives.ImageConvolution;
import edgedetector.util.NonMaximumSuppression;
import edgedetector.util.Grayscale;

public class CannyEdgeDetector implements EdgeDetector {

    private static final double[][] X_KERNEL = {{-1, 0 ,  1},
            {-2, 0 ,  2},
            {-1, 0 ,  1}};
    private static final double[][] Y_KERNEL = {{1 , 2 ,  1},
            {0 , 0 ,  0},
            {-1, -2, -1}};

    private boolean L1norm;
    private boolean calcThreshold;
    private int highThreshold;
    private int lowThreshold;
    private int minEdgeSize;

    private boolean[][] edges;
    private boolean[][] strongEdges;
    private boolean[][] weakEdges;

    private int rows;
    private int columns;

    // เปลี่ยนจาก private เป็น public
    public CannyEdgeDetector() {}

    // ใช้ Builder pattern ในการสร้าง CannyEdgeDetector
    private CannyEdgeDetector(Builder builder) {
        this.L1norm = builder.L1norm;
        this.minEdgeSize = builder.minEdgeSize;
        if (!(this.calcThreshold = builder.calcThreshold)) {
            this.lowThreshold = builder.lowThreshold;
            this.highThreshold = builder.highThreshold;
        }
        findEdges(builder.image);
    }

    public static class Builder {
        private int[][] image;
        private boolean calcThreshold = true;
        private int lowThreshold;
        private int highThreshold;
        private boolean L1norm = false;
        private int minEdgeSize = 0;

        public Builder(int[][] image) {
            this.image = image;
        }

        public Builder thresholds(int lowThreshold, int highThreshold) {
            if (lowThreshold > highThreshold || lowThreshold < 0 || highThreshold > 255)
                throw new IllegalArgumentException("Invalid threshold values");
            this.calcThreshold = false;
            this.lowThreshold = lowThreshold;
            this.highThreshold = highThreshold;
            return this;
        }

        public Builder L1norm(boolean L1norm) {
            this.L1norm = L1norm;
            return this;
        }

        public Builder minEdgeSize(int minEdgeSize) {
            this.minEdgeSize = minEdgeSize;
            return this;
        }

        public CannyEdgeDetector build() {
            return new CannyEdgeDetector(this);
        }
    }

    private void findEdges(int[][] image) {
        ImageConvolution gaussianConvolution = new ImageConvolution(image, ConvolutionKernel.GAUSSIAN_KERNEL);
        int[][] smoothedImage = gaussianConvolution.getConvolvedImage();

        ImageConvolution x_ic = new ImageConvolution(smoothedImage, X_KERNEL);
        ImageConvolution y_ic = new ImageConvolution(smoothedImage, Y_KERNEL);

        int[][] x_imageConvolution = x_ic.getConvolvedImage();
        int[][] y_imageConvolution = y_ic.getConvolvedImage();

        rows = x_imageConvolution.length;
        columns = x_imageConvolution[0].length;

        int[][] mag = new int[rows][columns];
        NonMaximumSuppression.EdgeDirection[][] angle = new NonMaximumSuppression.EdgeDirection[rows][columns];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < columns; j++) {
                mag[i][j] = hypotenuse(x_imageConvolution[i][j], y_imageConvolution[i][j]);
                angle[i][j] = direction(x_imageConvolution[i][j], y_imageConvolution[i][j]);
            }

        edges = new boolean[rows][columns];
        weakEdges = new boolean[rows][columns];
        strongEdges = new boolean[rows][columns];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (NonMaximumSuppression.nonMaximumSuppression(mag, angle[i][j], i, j, lowThreshold)) {
                    mag[i][j] = 0;
                }
            }
        }

        if (calcThreshold) {
            int k = 3;
            double[][] points = new double[rows * columns][1];
            int counter = 0;
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < columns; j++)
                    points[counter++][0] = mag[i][j];

            KMeans clustering = new KMeans.Builder(k, points)
                    .iterations(10)
                    .epsilon(.01)
                    .useEpsilon(true)
                    .build();
            double[][] centroids = clustering.getCentroids();

            boolean b = centroids[0][0] < centroids[1][0];
            lowThreshold = (int) (b ? centroids[0][0] : centroids[1][0]);
            highThreshold = (int) (b ? centroids[1][0] : centroids[0][0]);
        }

        HashSet<Integer> strongSet = new HashSet<>();
        HashSet<Integer> weakSet = new HashSet<>();

        int index = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if (mag[r][c] >= highThreshold) {
                    strongSet.add(index);
                    strongEdges[r][c] = true;
                } else if (mag[r][c] >= lowThreshold) {
                    weakSet.add(index);
                    weakEdges[r][c] = true;
                }
                index++;
            }
        }

        boolean[][] marked = new boolean[rows][columns];
        Stack<Integer> toAdd = new Stack<>();

        for (int strongIndex : strongSet) {
            dfs(ind2sub(strongIndex, columns)[0], ind2sub(strongIndex, columns)[1], weakSet, strongSet, marked, toAdd);
            if (toAdd.size() >= minEdgeSize)
                for (int edgeIndex : toAdd)
                    edges[ind2sub(edgeIndex, columns)[0]][ind2sub(edgeIndex, columns)[1]] = true;
            toAdd.clear();
        }
    }

    private void dfs(int r, int c, HashSet<Integer> weakSet, HashSet<Integer> strongSet, boolean[][] marked, Stack<Integer> toAdd) {
        if (r < 0 || r >= rows || c < 0 || c >= columns || marked[r][c])
            return;

        marked[r][c] = true;
        int index = sub2ind(r, c, columns);
        if (weakSet.contains(index) || strongSet.contains(index)) {
            toAdd.push(sub2ind(r, c, columns));

            dfs(r - 1, c - 1, weakSet, strongSet, marked, toAdd);
            dfs(r - 1, c, weakSet, strongSet, marked, toAdd);
            dfs(r - 1, c + 1, weakSet, strongSet, marked, toAdd);
            dfs(r, c - 1, weakSet, strongSet, marked, toAdd);
            dfs(r, c + 1, weakSet, strongSet, marked, toAdd);
            dfs(r + 1, c - 1, weakSet, strongSet, marked, toAdd);
            dfs(r + 1, c, weakSet, strongSet, marked, toAdd);
            dfs(r + 1, c + 1, weakSet, strongSet, marked, toAdd);
        }
    }

    private static int[] ind2sub(int index, int columns) {
        return new int[] {index / columns, index - columns * (index / columns)};
    }

    private static int sub2ind(int r, int c, int columns) {
        return columns * r + c;
    }

    private int hypotenuse(int x, int y) {
        return (int) (L1norm ? Hypotenuse.L1(x, y) : Hypotenuse.L2(x, y));
    }

    private NonMaximumSuppression.EdgeDirection direction(int G_x, int G_y) {
        return NonMaximumSuppression.EdgeDirection.getDirection(G_x, G_y);
    }

    public boolean[][] getEdges() {
        return edges;
    }

    @Override
    public File detectEdges(File imageFile) throws IOException {
        BufferedImage originalImage = ImageIO.read(imageFile);
        int[][] pixels = Grayscale.imgToGrayPixels(originalImage);

        CannyEdgeDetector detector = new CannyEdgeDetector.Builder(pixels)
                .minEdgeSize(10)
                .thresholds(15, 35)
                .L1norm(false)
                .build();

        boolean[][] edges = detector.getEdges();
        BufferedImage edgeImage = Threshold.applyThresholdReversed(edges);
        File result = new File("canny_result.png");
        ImageIO.write(edgeImage, "png", result);
        return result;
    }
}