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

import com.aegisvault.service.VaultService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApplication extends Application {

    private static final String APP_TITLE = "AegisVault-J";
    private static final int DEFAULT_WIDTH = 1024;
    private static final int DEFAULT_HEIGHT = 768;

    private final VaultService vaultService = new VaultService();

    @Override
    public void start(Stage primaryStage) {
        MainController controller = new MainController(vaultService, primaryStage);
        Scene scene = new Scene(controller.getRoot(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.setOnCloseRequest(e -> {
            vaultService.close();
        });
        primaryStage.show();
    }

    @Override
    public void stop() {
        vaultService.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
