package ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageViewer extends Application {

    private static BufferedImage[] imagesToShow;
    private static String title;

    public static void showImages(BufferedImage[] images, String titleToShow, int rows, int cols) throws Exception {
        imagesToShow = images;
        title = titleToShow;
        launch();  // Launch the JavaFX application
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        GridPane grid = new GridPane();

        int cols = 2;  // Define the number of columns (e.g., 2 columns)

        for (int i = 0; i < imagesToShow.length; i++) {
            File tmpFile = new File("tmp" + i + ".png");
            ImageIO.write(imagesToShow[i], "png", tmpFile);

            Image image = new Image(tmpFile.toURI().toString());
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(300);  // Set fit size (can be adjusted)
            imageView.setPreserveRatio(true);

            grid.add(imageView, i % cols, i / cols);  // Add images in a grid layout
        }

        primaryStage.setTitle(title);
        primaryStage.setScene(new Scene(grid, 800, 600));  // Set the stage size
        primaryStage.show();
    }
}