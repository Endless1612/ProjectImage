package controller;

import edgedetector.detectors.CannyEdgeDetector;
import edgedetector.detectors.LaplacianEdgeDetector;
import edgedetector.detectors.PrewittEdgeDetector;
import edgedetector.detectors.RobertsCrossEdgeDetector;
import edgedetector.detectors.SobelEdgeDetector;
import edgedetector.detectors.GaussianEdgeDetector;
import edgedetector.detectors.EdgeDetector;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ImageController {

    @FXML
    private Button cropButton;  // ปุ่มสำหรับตรวจจับขอบภาพ
    @FXML
    private Button revertButton;  // ปุ่ม revert กลับไปยังภาพต้นฉบับ
    @FXML
    private ImageView imageView;  // สำหรับแสดงภาพใน UI
    @FXML
    private ComboBox<String> algorithmChoice;  // กล่องเลือกอัลกอริทึม
    @FXML
    private Label dropArea;  // พื้นที่สำหรับลากและวางไฟล์ภาพ
    @FXML
    private Label statusLabel;  // แสดงข้อความสถานะการทำงาน

    private File selectedFile;  // ไฟล์ภาพที่ถูกเลือก
    private Image originalImage;  // ภาพต้นฉบับ
    private Image processedImage;  // สำหรับภาพที่ผ่านการตรวจจับขอบแล้ว
    private File resultFile;  // ไฟล์ผลลัพธ์หลังจากการตรวจจับขอบ

    // แผนที่สำหรับเชื่อมต่ออัลกอริทึมตรวจจับขอบแต่ละชนิด
    private final Map<String, Class<? extends EdgeDetector>> edgeAlgorithms = new HashMap<>();

    @FXML
    public void initialize() {
        // การตั้งค่าอัลกอริทึมการตรวจจับขอบ (Edge Detection)
        edgeAlgorithms.put("Canny", CannyEdgeDetector.class);
        edgeAlgorithms.put("Sobel", SobelEdgeDetector.class);
        edgeAlgorithms.put("Laplacian", LaplacianEdgeDetector.class);
        edgeAlgorithms.put("Prewitt", PrewittEdgeDetector.class);
        edgeAlgorithms.put("Roberts Cross", RobertsCrossEdgeDetector.class);
        edgeAlgorithms.put("Gaussian", GaussianEdgeDetector.class);

        algorithmChoice.getItems().addAll(edgeAlgorithms.keySet());
        algorithmChoice.setPromptText("Select Method");

        // Listener สำหรับเลือกอัลกอริทึม และเปิดปุ่มตรวจจับขอบเมื่อมีการเลือกไฟล์และอัลกอริทึม
        algorithmChoice.valueProperty().addListener((observable, oldValue, newValue) -> {
            cropButton.setDisable(selectedFile == null || newValue == null);
        });

        setUpDragAndDrop();
    }

    private void setUpDragAndDrop() {
        dropArea.setOnDragOver(event -> {
            if (event.getGestureSource() != dropArea && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        dropArea.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                selectedFile = db.getFiles().get(0);
                originalImage = new Image(selectedFile.toURI().toString());
                imageView.setImage(originalImage);
                statusLabel.setText("File loaded: " + selectedFile.getName());
            }
            event.setDropCompleted(db.hasFiles());
            event.consume();
        });
    }

    @FXML
    public void onDetectEdges() {
        if (selectedFile == null || algorithmChoice.getValue() == null) {
            statusLabel.setText("Please select a file and an algorithm.");
            return;
        }

        try {
            Class<? extends EdgeDetector> detectorClass = edgeAlgorithms.get(algorithmChoice.getValue());
            EdgeDetector detector = detectorClass.getDeclaredConstructor().newInstance();

            resultFile = detector.detectEdges(selectedFile);
            processedImage = new Image(resultFile.toURI().toString());

            imageView.setImage(processedImage);
            statusLabel.setText("Edge detection completed.");
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Edge detection failed.");
        }
    }

    @FXML
    public void onSaveFile() {
        if (processedImage == null) {
            statusLabel.setText("No image to save.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Files", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Files", "*.jpg")
        );
        File saveFile = fileChooser.showSaveDialog(null);

        if (saveFile != null) {
            try {
                // บันทึกภาพผลลัพธ์เป็นไฟล์
                if (saveFile.getName().endsWith(".png")) {
                    ImageIO.write(ImageIO.read(resultFile), "png", saveFile);
                } else if (saveFile.getName().endsWith(".jpg") || saveFile.getName().endsWith(".jpeg")) {
                    ImageIO.write(ImageIO.read(resultFile), "jpg", saveFile);
                }
                statusLabel.setText("File saved successfully.");
            } catch (IOException e) {
                e.printStackTrace();
                statusLabel.setText("Failed to save file.");
            }
        }
    }

    @FXML
    public void onChooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose an image file");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            originalImage = new Image(selectedFile.toURI().toString());
            imageView.setImage(originalImage);
            statusLabel.setText("File selected: " + selectedFile.getName());
        }
    }

    @FXML
    public void onRevertToOriginal() {
        if (originalImage != null) {
            imageView.setImage(originalImage);
            statusLabel.setText("Reverted to original image.");
        }
    }
}