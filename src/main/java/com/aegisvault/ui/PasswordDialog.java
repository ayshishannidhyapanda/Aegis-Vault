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

import com.aegisvault.util.PasswordStrength;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.shape.Rectangle;

public class PasswordDialog extends Dialog<char[]> {

    private final PasswordField passwordField;
    private final PasswordField confirmField;
    private final boolean requireConfirmation;
    private Label strengthLabel;
    private HBox strengthBar;

    public PasswordDialog(String title, String prompt, boolean requireConfirmation) {
        this.requireConfirmation = requireConfirmation;

        setTitle(title);
        setHeaderText(prompt);

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        GridPane.setHgrow(passwordField, Priority.ALWAYS);

        grid.add(new Label("Password:"), 0, 0);
        grid.add(passwordField, 1, 0);

        int rowIndex = 1;

        if (requireConfirmation) {
            strengthBar = new HBox(2);
            strengthLabel = new Label("");
            updateStrengthIndicator("");

            passwordField.textProperty().addListener((obs, oldVal, newVal) -> updateStrengthIndicator(newVal));

            grid.add(new Label("Strength:"), 0, rowIndex);
            HBox strengthContainer = new HBox(10, strengthBar, strengthLabel);
            grid.add(strengthContainer, 1, rowIndex);
            rowIndex++;

            confirmField = new PasswordField();
            confirmField.setPromptText("Confirm password");
            GridPane.setHgrow(confirmField, Priority.ALWAYS);

            grid.add(new Label("Confirm:"), 0, rowIndex);
            grid.add(confirmField, 1, rowIndex);
        } else {
            confirmField = null;
        }

        getDialogPane().setContent(grid);

        Platform.runLater(passwordField::requestFocus);

        setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                if (requireConfirmation) {
                    String pwd = passwordField.getText();
                    String confirm = confirmField.getText();
                    if (!pwd.equals(confirm)) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Password Mismatch");
                        alert.setHeaderText(null);
                        alert.setContentText("Passwords do not match.");
                        alert.showAndWait();
                        return null;
                    }
                    if (pwd.isEmpty()) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Invalid Password");
                        alert.setHeaderText(null);
                        alert.setContentText("Password cannot be empty.");
                        alert.showAndWait();
                        return null;
                    }
                    PasswordStrength.Strength strength = PasswordStrength.evaluate(pwd);
                    if (strength.getScore() < 2) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Weak Password");
                        alert.setHeaderText(null);
                        alert.setContentText("Your password is weak. Consider using a stronger password with:\n" +
                                "• At least 12 characters\n" +
                                "• Upper and lowercase letters\n" +
                                "• Numbers and special characters");
                        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
                        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                            return null;
                        }
                    }
                }
                return passwordField.getText().toCharArray();
            }
            return null;
        });
    }

    private void updateStrengthIndicator(String password) {
        PasswordStrength.Strength strength = PasswordStrength.evaluate(password);

        strengthBar.getChildren().clear();
        for (int i = 0; i <= 4; i++) {
            Rectangle rect = new Rectangle(20, 8);
            if (i <= strength.getScore()) {
                rect.setStyle("-fx-fill: " + strength.getColor() + ";");
            } else {
                rect.setStyle("-fx-fill: #ddd;");
            }
            strengthBar.getChildren().add(rect);
        }

        strengthLabel.setText(strength.getLabel());
        strengthLabel.setStyle("-fx-text-fill: " + strength.getColor() + "; -fx-font-weight: bold;");
    }
}
