module com.tank2d.tankverse {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;
    requires com.google.gson;
    requires java.desktop;
    requires javafx.graphics;


    opens com.tank2d.tankverse.ui to javafx.fxml;
    opens com.tank2d.tankverse.utils to com.google.gson;
    exports com.tank2d.tankverse;
    exports com.tank2d.tankverse.utils;
}