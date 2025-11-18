package com.tank2d.tankverse.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class UiNavigator {
    private static Stage mainStage;

    public static void setStage(Stage stage) {
        mainStage = stage;
    }

    public static void loadScene(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(UiNavigator.class.getResource(fxml));
            Scene scene = new Scene(loader.load());
            mainStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <T> T loadSceneWithController(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(UiNavigator.class.getResource(fxml));
            Parent root = loader.load();
            mainStage.setScene(new Scene(root));
            return loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}