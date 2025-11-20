package com.tank2d.tankverse.ui;

import com.tank2d.tankverse.core.GameClient;
import com.tank2d.tankverse.core.PacketListener;
import com.tank2d.tankverse.utils.AnimationHelper;
import com.tank2d.tankverse.utils.Constant;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginController implements PacketListener {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblStatus;
    @FXML private Button btnLogin;
    @FXML private Button btnRegister;
    @FXML private VBox loginContainer;

    private GameClient client;
    private boolean isConnected = false;

    @FXML
    public void initialize() {
        // Fade in animation on load
        if (loginContainer != null) {
            loginContainer.setOpacity(0);
            AnimationHelper.fadeIn(loginContainer, 500);
        }
        
        // Auto-connect to default Playit server
        connectToServerAsync(Constant.DEFAULT_SERVER_HOST, Constant.DEFAULT_SERVER_PORT);
    }
    
    private void connectToServerAsync(String host, int port) {
        lblStatus.setText("Connecting to server...");
        isConnected = false;
        
        // Connect in background thread
        new Thread(() -> {
            try {
                client = new GameClient();
                client.setPacketListener(this);
                client.connect(host, port);
                
                isConnected = true;
                
                Platform.runLater(() -> {
                    lblStatus.setText("Connected to server");
                });
                
            } catch (Exception e) {
                isConnected = false;
                Platform.runLater(() -> {
                    lblStatus.setText("Connection failed: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    public void onLoginClick() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        
        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Please enter username and password!");
            AnimationHelper.shake(txtUsername);
            AnimationHelper.shake(txtPassword);
            return;
        }
        
        if (!isConnected) {
            lblStatus.setText("Not connected to server!");
            return;
        }
        
        // Show loading state
        AnimationHelper.showButtonLoading(btnLogin);
        lblStatus.setText("Logging in...");
        
        client.login(username, password);
    }

    @FXML
    public void onRegisterClick() {
        Platform.runLater(() -> {
            RegisterController controller = UiNavigator.loadSceneWithController("register.fxml");
            // Pass the same client connection to RegisterController
            if (controller != null && client != null) {
                controller.setClient(client);
            }
        });
    }

    public void setClient(GameClient client) {
        if (client != null) {
            this.client = client;
            client.setPacketListener(this);
        }
    }

    // ========== PacketListener Implementation ==========

    @Override
    public void onLoginSuccess(String message) {
        Platform.runLater(() -> {
            AnimationHelper.hideButtonLoading(btnLogin, "Login");
            lblStatus.setText(message);
            client.setUserName(this.txtUsername.getText());
            
            // Fade out then navigate
            AnimationHelper.fadeOut(loginContainer, 300, () -> {
                MainMenuController controller = UiNavigator.loadSceneWithController("main_menu.fxml");
                controller.setClient(client);
                client.setPacketListener(controller);
            });
        });
    }

    @Override
    public void onLoginFail(String message) {
        Platform.runLater(() -> {
            AnimationHelper.hideButtonLoading(btnLogin, "Login");
            lblStatus.setText(message);
            
            // Shake animation on fail
            AnimationHelper.shake(txtUsername);
            AnimationHelper.shake(txtPassword);
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> lblStatus.setText("Disconnected from server"));
    }
}