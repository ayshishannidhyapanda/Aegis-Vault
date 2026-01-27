/*
 * Copyright (c) 2026 Aegis Vault
 * All rights reserved.
 *
 * This software, known as "AegisVault-J", including its source code, documentation,
 * design, and associated materials, is the intellectual property of the author.
 *
 * No part of this software may be copied, modified, distributed, or used in
 * derivative works without explicit written permission from the copyright holder,
 * except for academic evaluation purposes.
 *
 * This software is provided "as is", without warranty of any kind, express or
 * implied, including but not limited to the warranties of merchantability,
 * fitness for a particular purpose, and noninfringement.
 */
package com.aegisvault.ui;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

import java.util.concurrent.atomic.AtomicBoolean;

public class ProgressDialog extends Dialog<Void> {

    private final ProgressBar progressBar;
    private final ProgressIndicator spinner;
    private final Label messageLabel;
    private final Label detailLabel;
    private final Button cancelButton;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public ProgressDialog(String title, String initialMessage) {
        setTitle(title);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);
        setResizable(false);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20, 40, 20, 40));
        content.setAlignment(Pos.CENTER);
        content.setMinWidth(350);

        messageLabel = new Label(initialMessage);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(280);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        spinner = new ProgressIndicator();
        spinner.setPrefSize(24, 24);
        spinner.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        detailLabel = new Label("");
        detailLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");

        cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> {
            cancelled.set(true);
            cancelButton.setDisable(true);
            cancelButton.setText("Cancelling...");
        });

        content.getChildren().addAll(messageLabel, progressBar, detailLabel, cancelButton);
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().clear();

        setOnCloseRequest(e -> {
            if (!cancelled.get()) {
                e.consume();
            }
        });
    }

    public void updateProgress(double progress, String detail) {
        javafx.application.Platform.runLater(() -> {
            if (progress >= 0) {
                progressBar.setProgress(progress);
            }
            if (detail != null) {
                detailLabel.setText(detail);
            }
        });
    }

    public void updateMessage(String message) {
        javafx.application.Platform.runLater(() -> messageLabel.setText(message));
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void allowClose() {
        javafx.application.Platform.runLater(() -> {
            cancelled.set(true);
            close();
        });
    }

    public void bindTask(Task<?> task) {
        progressBar.progressProperty().bind(task.progressProperty());
        messageLabel.textProperty().bind(task.titleProperty());
        detailLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> allowClose());
        task.setOnFailed(e -> allowClose());
        task.setOnCancelled(e -> allowClose());
    }

    public void hideCancelButton() {
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
    }

    public void showIndeterminate(String message) {
        javafx.application.Platform.runLater(() -> {
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            messageLabel.setText(message);
            detailLabel.setText("");
        });
    }
}
