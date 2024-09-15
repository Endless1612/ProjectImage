package edgedetector.detectors;


import java.awt.image.BufferedImage;
import java.io.File;

public interface EdgeDetector {
    File detectEdges(File inputFile) throws Exception;
}