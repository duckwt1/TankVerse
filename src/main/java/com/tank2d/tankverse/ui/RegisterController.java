package com.tank2d.tankverse.ui;

import com.tank2d.tankverse.core.GameClient;
import com.tank2d.tankverse.core.PacketListener;
import com.tank2d.tankverse.utils.AnimationHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class RegisterController implements PacketListener {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private TextField txtEmail;
    @FXML private Label lblStatus;
    @FXML private Button btnRegister;
    @FXML private VBox registerContainer;

    private GameClient client;

    @FXML
    public void initialize() {
        // Slide in from right animation
        if (registerContainer != null) {
            registerContainer.setTranslateX(300);
            AnimationHelper.slideInFromRight(registerContainer, 400);
        }
    }

    @FXML
    public void onRegisterClick() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String confirmPassword = txtConfirmPassword.getText().trim();
        String email = txtEmail.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Please fill in all information!");
            AnimationHelper.shake(txtUsername);
            AnimationHelper.shake(txtPassword);
            return;
        }

        if (!password.equals(confirmPassword)) {
            lblStatus.setText("Password doesn't match!");
            AnimationHelper.shake(txtPassword);
            AnimationHelper.shake(txtConfirmPassword);
            return;
        }

        // Show loading
        AnimationHelper.showButtonLoading(btnRegister);
        lblStatus.setText("Creating account...");
        
        client.register(username, password, email);
    }

    @FXML
    public void onBackToLoginClick() {
        // Navigate back and pass the client
        Platform.runLater(() -> {
            LoginController controller = UiNavigator.loadSceneWithController("login.fxml");
            if (controller != null) {
                controller.setClient(client);
            }
        });
    }
    
    public void setClient(GameClient client) {
        this.client = client;
        client.setPacketListener(this);
    }
    
    // ========== PacketListener Implementation ==========
    
    @Override
    public void onRegisterSuccess(String message) {
        Platform.runLater(() -> {
            AnimationHelper.hideButtonLoading(btnRegister, "Register");
            lblStatus.setText(message);
            AnimationHelper.bounce(lblStatus);
            
            // Wait a bit then navigate to login with fade
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    Platform.runLater(() -> {
                        AnimationHelper.fadeOut(registerContainer, 300, () -> {
                            LoginController controller = UiNavigator.loadSceneWithController("login.fxml");
                            if (controller != null) {
                                controller.setClient(client);
                            }
                        });
                    });
                } catch (InterruptedException e) {
                    System.err.println("Sleep interrupted: " + e.getMessage());
                }
            }).start();
        });
    }
    
    @Override
    public void onRegisterFail(String message) {
        Platform.runLater(() -> {
            AnimationHelper.hideButtonLoading(btnRegister, "Register");
            lblStatus.setText(message);
            AnimationHelper.shake(txtUsername);
        });
    }
    
    @Override
    public void onDisconnected() {
        Platform.runLater(() -> lblStatus.setText("Disconnected from server"));
    }
}