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

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class ChangePasswordDialog extends Dialog<char[][]> {

    private final PasswordField currentPassword;
    private final PasswordField newPassword;
    private final PasswordField confirmPassword;

    public ChangePasswordDialog() {
        setTitle("Change Password");
        setHeaderText("Enter current and new password");

        ButtonType changeButtonType = new ButtonType("Change", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(changeButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        currentPassword = new PasswordField();
        currentPassword.setPromptText("Current password");
        GridPane.setHgrow(currentPassword, Priority.ALWAYS);

        newPassword = new PasswordField();
        newPassword.setPromptText("New password");
        GridPane.setHgrow(newPassword, Priority.ALWAYS);

        confirmPassword = new PasswordField();
        confirmPassword.setPromptText("Confirm new password");
        GridPane.setHgrow(confirmPassword, Priority.ALWAYS);

        grid.add(new Label("Current Password:"), 0, 0);
        grid.add(currentPassword, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPassword, 1, 1);
        grid.add(new Label("Confirm New:"), 0, 2);
        grid.add(confirmPassword, 1, 2);

        getDialogPane().setContent(grid);

        Platform.runLater(currentPassword::requestFocus);

        setResultConverter(dialogButton -> {
            if (dialogButton == changeButtonType) {
                String newPwd = newPassword.getText();
                String confirm = confirmPassword.getText();

                if (currentPassword.getText().isEmpty()) {
                    showError("Current password is required.");
                    return null;
                }

                if (newPwd.isEmpty()) {
                    showError("New password cannot be empty.");
                    return null;
                }

                if (!newPwd.equals(confirm)) {
                    showError("New passwords do not match.");
                    return null;
                }

                return new char[][]{
                        currentPassword.getText().toCharArray(),
                        newPassword.getText().toCharArray()
                };
            }
            return null;
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
