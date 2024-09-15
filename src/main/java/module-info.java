module ImageWizard {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.graphics;


    opens controller to javafx.fxml;
    exports Main;
}