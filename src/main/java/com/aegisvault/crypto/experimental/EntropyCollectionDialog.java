/*
 * Copyright (c) 2026 Aegis Vault
 * All rights reserved.
 */
package com.aegisvault.crypto.experimental;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class EntropyCollectionDialog {

    private static final int REQUIRED_SAMPLES = 256;
    private static final int POOL_SIZE = 64;

    private final Stage dialogStage;
    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final Label poolDisplay;
    private final Label percentLabel;
    private final Button continueButton;

    private final byte[] entropyPool = new byte[POOL_SIZE];
    private final AtomicInteger sampleCount = new AtomicInteger(0);
    private final AtomicInteger poolIndex = new AtomicInteger(0);
    private final AtomicLong lastX = new AtomicLong(0);
    private final AtomicLong lastY = new AtomicLong(0);
    private final AtomicLong lastTime = new AtomicLong(System.nanoTime());

    private volatile boolean completed = false;

    public EntropyCollectionDialog(Stage owner) {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.initStyle(StageStyle.DECORATED);
        dialogStage.setTitle("Collecting Random Data");
        dialogStage.setResizable(false);

        VBox root = new VBox(18);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");

        Label titleLabel = new Label("Random Data Collection");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.WHITE);

        Label instructionLabel = new Label(
                "Move your mouse randomly within the area below.\n" +
                "This randomness helps generate secure cryptographic keys.");
        instructionLabel.setWrapText(true);
        instructionLabel.setTextFill(Color.web("#b0b0b0"));
        instructionLabel.setStyle("-fx-font-size: 13;");
        instructionLabel.setAlignment(Pos.CENTER);

        Pane mouseArea = new Pane();
        mouseArea.setPrefSize(420, 180);
        mouseArea.setStyle("-fx-background-color: #0f0f23; -fx-border-color: #3a3a5c; " +
                "-fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label mouseAreaLabel = new Label("Move mouse here");
        mouseAreaLabel.setTextFill(Color.web("#4a4a6a"));
        mouseAreaLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        mouseAreaLabel.setLayoutX(160);
        mouseAreaLabel.setLayoutY(80);
        mouseArea.getChildren().add(mouseAreaLabel);

        mouseArea.setOnMouseMoved(this::handleMouseMove);
        mouseArea.setOnMouseDragged(this::handleMouseMove);

        HBox progressBox = new HBox(15);
        progressBox.setAlignment(Pos.CENTER);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(340);
        progressBar.setPrefHeight(22);
        progressBar.setStyle("-fx-accent: #27ae60;");
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        percentLabel = new Label("0%");
        percentLabel.setTextFill(Color.WHITE);
        percentLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        percentLabel.setMinWidth(45);

        progressBox.getChildren().addAll(progressBar, percentLabel);

        statusLabel = new Label("Collected: 0 / " + REQUIRED_SAMPLES + " samples");
        statusLabel.setTextFill(Color.web("#8e8e8e"));
        statusLabel.setStyle("-fx-font-size: 12;");

        poolDisplay = new Label(formatPoolDisplay());
        poolDisplay.setFont(Font.font("Consolas", 11));
        poolDisplay.setTextFill(Color.web("#00ff88"));
        poolDisplay.setStyle("-fx-background-color: #0a0a15; -fx-padding: 12; " +
                "-fx-background-radius: 6; -fx-border-color: #2a2a4a; -fx-border-radius: 6;");
        poolDisplay.setMinWidth(420);
        poolDisplay.setMaxWidth(420);

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button skipButton = new Button("Skip");
        skipButton.setStyle("-fx-background-color: #4a4a6a; -fx-text-fill: white; " +
                "-fx-font-size: 13; -fx-padding: 10 25; -fx-background-radius: 5; -fx-cursor: hand;");
        skipButton.setOnAction(e -> {
            completed = false;
            dialogStage.close();
        });

        continueButton = new Button("Continue");
        continueButton.setDisable(true);
        continueButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                "-fx-font-size: 13; -fx-font-weight: bold; -fx-padding: 10 25; " +
                "-fx-background-radius: 5; -fx-cursor: hand; -fx-opacity: 0.5;");
        continueButton.setOnAction(e -> {
            completed = true;
            dialogStage.close();
        });

        buttonBox.getChildren().addAll(skipButton, continueButton);

        Label warningLabel = new Label(
                "This supplements system entropy. Security benefit not guaranteed.");
        warningLabel.setTextFill(Color.web("#f39c12"));
        warningLabel.setStyle("-fx-font-size: 11;");

        root.getChildren().addAll(
                titleLabel,
                instructionLabel,
                mouseArea,
                progressBox,
                statusLabel,
                poolDisplay,
                buttonBox,
                warningLabel
        );

        Scene scene = new Scene(root, 470, 530);
        dialogStage.setScene(scene);
    }

    private void handleMouseMove(MouseEvent event) {
        if (completed) return;

        long currentTime = System.nanoTime();
        long x = Double.doubleToLongBits(event.getX());
        long y = Double.doubleToLongBits(event.getY());

        long prevX = lastX.getAndSet(x);
        long prevY = lastY.getAndSet(y);
        long prevTime = lastTime.getAndSet(currentTime);

        if (x == prevX && y == prevY) {
            return;
        }

        long deltaTime = currentTime - prevTime;
        long deltaX = x ^ prevX;
        long deltaY = y ^ prevY;

        long entropy = deltaX ^ Long.rotateLeft(deltaY, 17) ^
                       Long.rotateLeft(deltaTime, 31) ^
                       Long.rotateLeft(currentTime, 47) ^
                       Long.rotateLeft(System.identityHashCode(event), 11);

        int idx = poolIndex.getAndUpdate(i -> (i + 1) % POOL_SIZE);
        byte[] entropyBytes = ByteBuffer.allocate(8).putLong(entropy).array();
        for (int i = 0; i < 8 && (idx + i) < POOL_SIZE; i++) {
            entropyPool[(idx + i) % POOL_SIZE] ^= entropyBytes[i];
        }

        int count = sampleCount.incrementAndGet();

        Platform.runLater(() -> {
            double progress = Math.min(1.0, (double) count / REQUIRED_SAMPLES);
            progressBar.setProgress(progress);
            int percent = (int) (progress * 100);
            percentLabel.setText(percent + "%");
            statusLabel.setText("Collected: " + Math.min(count, REQUIRED_SAMPLES) + " / " + REQUIRED_SAMPLES + " samples");
            poolDisplay.setText(formatPoolDisplay());

            if (count >= REQUIRED_SAMPLES && continueButton.isDisable()) {
                continueButton.setDisable(false);
                continueButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                        "-fx-font-size: 13; -fx-font-weight: bold; -fx-padding: 10 25; " +
                        "-fx-background-radius: 5; -fx-cursor: hand;");
                statusLabel.setText("Sufficient randomness collected!");
                statusLabel.setTextFill(Color.web("#27ae60"));
            }
        });
    }

    private String formatPoolDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < POOL_SIZE; i++) {
            sb.append(String.format("%02X", entropyPool[i] & 0xFF));
            if ((i + 1) % 16 == 0) sb.append("\n");
            else if ((i + 1) % 4 == 0) sb.append(" ");
        }
        return sb.toString().trim();
    }

    public byte[] getEntropyBytes() {
        return entropyPool.clone();
    }

    public void show() {
        dialogStage.showAndWait();
    }

    public boolean isCompleted() {
        return completed;
    }

    public static void contributeToSecureRandom(byte[] entropy, SecureRandom random) {
        if (entropy != null && entropy.length > 0 && random != null) {
            random.nextBytes(new byte[1]);
            random.setSeed(entropy);
            System.out.println("[ENTROPY] Mouse entropy contributed (" + entropy.length + " bytes)");
        }
    }

    public static byte[] collectEntropy(Stage owner) {
        EntropyCollectionDialog dialog = new EntropyCollectionDialog(owner);
        dialog.show();
        return dialog.isCompleted() ? dialog.getEntropyBytes() : null;
    }
}
