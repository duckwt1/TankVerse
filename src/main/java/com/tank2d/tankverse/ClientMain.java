package com.tank2d.tankverse;

import com.tank2d.tankverse.ui.UiNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class ClientMain extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        UiNavigator.setStage(stage);
        UiNavigator.loadScene("login.fxml");
        stage.setTitle("Pixel Tank Online");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}