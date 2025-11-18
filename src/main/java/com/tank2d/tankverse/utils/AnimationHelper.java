package com.tank2d.tankverse.utils;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.util.Duration;

/**
 * Utility class for common UI animations
 */
public class AnimationHelper {
    
    // ========== Fade Animations ==========
    
    public static void fadeIn(Node node, double durationMs) {
        fadeIn(node, durationMs, null);
    }
    
    public static void fadeIn(Node node, double durationMs, Runnable onFinish) {
        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        if (onFinish != null) {
            fade.setOnFinished(e -> onFinish.run());
        }
        fade.play();
    }
    
    public static void fadeOut(Node node, double durationMs, Runnable onFinish) {
        FadeTransition fade = new FadeTransition(Duration.millis(durationMs), node);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        if (onFinish != null) {
            fade.setOnFinished(e -> onFinish.run());
        }
        fade.play();
    }
    
    // ========== Shake Animation ==========
    
    public static void shake(Node node) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(50), node);
        transition.setFromX(0);
        transition.setByX(10);
        transition.setCycleCount(6);
        transition.setAutoReverse(true);
        transition.setOnFinished(e -> node.setTranslateX(0));
        transition.play();
    }
    
    // ========== Scale Animations ==========
    
    public static void scaleUp(Node node, double scale, double durationMs) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(durationMs), node);
        scaleTransition.setToX(scale);
        scaleTransition.setToY(scale);
        scaleTransition.play();
    }
    
    public static void scaleDown(Node node, double durationMs) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(durationMs), node);
        scaleTransition.setToX(1.0);
        scaleTransition.setToY(1.0);
        scaleTransition.play();
    }
    
    // ========== Slide Animations ==========
    
    public static void slideInFromRight(Node node, double durationMs) {
        TranslateTransition slide = new TranslateTransition(Duration.millis(durationMs), node);
        slide.setFromX(300);
        slide.setToX(0);
        slide.play();
    }
    
    public static void slideInFromLeft(Node node, double durationMs) {
        TranslateTransition slide = new TranslateTransition(Duration.millis(durationMs), node);
        slide.setFromX(-300);
        slide.setToX(0);
        slide.play();
    }
    
    public static void slideOutToRight(Node node, double durationMs, Runnable onFinish) {
        TranslateTransition slide = new TranslateTransition(Duration.millis(durationMs), node);
        slide.setFromX(0);
        slide.setToX(300);
        if (onFinish != null) {
            slide.setOnFinished(e -> onFinish.run());
        }
        slide.play();
    }
    
    // ========== Button Loading State ==========
    
    public static void showButtonLoading(Button button) {
        button.setDisable(true);
        button.setText("Loading...");
        
        // Add pulsing effect
        FadeTransition fade = new FadeTransition(Duration.millis(800), button);
        fade.setFromValue(1.0);
        fade.setToValue(0.6);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);
        button.setUserData(fade); // Store to stop later
        fade.play();
    }
    
    public static void hideButtonLoading(Button button, String originalText) {
        button.setDisable(false);
        button.setText(originalText);
        
        // Stop pulsing
        Object userData = button.getUserData();
        if (userData instanceof FadeTransition fade) {
            fade.stop();
            button.setOpacity(1.0);
        }
    }
    
    // ========== Bounce Animation ==========
    
    public static void bounce(Node node) {
        ScaleTransition scale1 = new ScaleTransition(Duration.millis(100), node);
        scale1.setToX(1.2);
        scale1.setToY(1.2);
        
        ScaleTransition scale2 = new ScaleTransition(Duration.millis(100), node);
        scale2.setToX(1.0);
        scale2.setToY(1.0);
        
        SequentialTransition seq = new SequentialTransition(scale1, scale2);
        seq.play();
    }
    
    // ========== Pulse Animation (for notifications) ==========
    
    public static void pulse(Node node, int cycles) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), node);
        scale.setToX(1.1);
        scale.setToY(1.1);
        scale.setCycleCount(cycles * 2);
        scale.setAutoReverse(true);
        scale.setOnFinished(e -> {
            node.setScaleX(1.0);
            node.setScaleY(1.0);
        });
        scale.play();
    }
    
    // ========== Rotate Animation (for loading spinner) ==========
    
    public static RotateTransition createSpinAnimation(Node node) {
        RotateTransition rotate = new RotateTransition(Duration.millis(1000), node);
        rotate.setByAngle(360);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.setInterpolator(Interpolator.LINEAR);
        return rotate;
    }
    
    // ========== Scene Transition ==========
    
    public static void crossFadeTransition(Node oldNode, Node newNode, double durationMs, Runnable onFinish) {
        ParallelTransition parallel = new ParallelTransition();
        
        FadeTransition fadeOut = new FadeTransition(Duration.millis(durationMs), oldNode);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(durationMs), newNode);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        parallel.getChildren().addAll(fadeOut, fadeIn);
        if (onFinish != null) {
            parallel.setOnFinished(e -> onFinish.run());
        }
        parallel.play();
    }
}
