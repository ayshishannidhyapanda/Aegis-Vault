/*
 * Copyright (c) 2026 Aegis Vault
 * All rights reserved.
 */
package com.aegisvault.crypto.experimental;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CryptoOptionsDialog extends Dialog<CryptoSettings> {

    private final ComboBox<String> cipherCombo;
    private final ComboBox<String> hashCombo;
    private final CheckBox mouseEntropyCheck;
    private final Label cipherInfoLabel;
    private final VBox warningSection;
    private final Label warningLabel;

    public CryptoOptionsDialog(Stage owner) {
        setTitle("Encryption Options");
        setHeaderText(null);
        initOwner(owner);

        cipherCombo = new ComboBox<>();
        hashCombo = new ComboBox<>();
        mouseEntropyCheck = new CheckBox("Collect mouse entropy during vault creation");
        cipherInfoLabel = new Label();
        warningLabel = new Label();
        warningSection = new VBox(warningLabel);

        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(25));
        mainContent.setStyle("-fx-background-color: #f8f9fa;");

        Label titleLabel = new Label("Volume Encryption Options");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.web("#2c3e50"));

        VBox cipherSection = createCipherSection();
        VBox hashSection = createHashSection();
        VBox entropySection = createEntropySection();
        setupWarningSection();

        mainContent.getChildren().addAll(titleLabel, cipherSection, hashSection, entropySection, warningSection);

        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(420);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #f8f9fa;");

        getDialogPane().setContent(scrollPane);
        getDialogPane().setPrefWidth(480);
        getDialogPane().setStyle("-fx-background-color: #f8f9fa;");

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okButton, cancelButton);

        setResultConverter(buttonType -> {
            if (buttonType == okButton) {
                CryptoSettings settings = CryptoSettings.getInstance();
                settings.setSelectedCipher(cipherCombo.getValue());
                settings.setSelectedHash(hashCombo.getValue());
                settings.setUseMouseEntropy(mouseEntropyCheck.isSelected());
                return settings;
            }
            return null;
        });

        updateCipherInfo();
    }

    private VBox createCipherSection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(18));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        Label header = new Label("Encryption Algorithm");
        header.setFont(Font.font("System", FontWeight.BOLD, 14));
        header.setTextFill(Color.web("#34495e"));

        List<String> allCiphers = new ArrayList<>();
        allCiphers.addAll(CipherRegistry.getAllSingleCiphers());
        allCiphers.addAll(CipherRegistry.getAllCascadeCiphers());
        cipherCombo.setItems(FXCollections.observableArrayList(allCiphers));
        cipherCombo.setValue(CryptoSettings.getInstance().getSelectedCipher());
        cipherCombo.setMaxWidth(Double.MAX_VALUE);
        cipherCombo.setStyle("-fx-font-size: 14; -fx-background-radius: 5;");
        HBox.setHgrow(cipherCombo, Priority.ALWAYS);

        cipherCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setPadding(new Insets(10, 15, 10, 15));
                    setStyle("-fx-font-size: 13;");
                    if (CipherRegistry.isExperimental(item)) {
                        setTextFill(Color.web("#d35400"));
                    } else {
                        setTextFill(Color.web("#2c3e50"));
                    }
                }
            }
        });

        cipherCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-font-size: 14;");
            }
        });

        cipherCombo.setOnAction(e -> updateCipherInfo());

        cipherInfoLabel.setWrapText(true);
        cipherInfoLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #7f8c8d;");
        cipherInfoLabel.setPadding(new Insets(8, 0, 0, 0));

        section.getChildren().addAll(header, cipherCombo, cipherInfoLabel);
        return section;
    }

    private VBox createHashSection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(18));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        Label header = new Label("Hash Algorithm");
        header.setFont(Font.font("System", FontWeight.BOLD, 14));
        header.setTextFill(Color.web("#34495e"));

        hashCombo.setItems(FXCollections.observableArrayList(HashProviders.getAllHashAlgorithms()));
        hashCombo.setValue(CryptoSettings.getInstance().getSelectedHash());
        hashCombo.setMaxWidth(Double.MAX_VALUE);
        hashCombo.setStyle("-fx-font-size: 14; -fx-background-radius: 5;");

        hashCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setPadding(new Insets(10, 15, 10, 15));
                    setStyle("-fx-font-size: 13;");
                    try {
                        HashProvider provider = HashProviders.get(item);
                        setTextFill(provider.isExperimental() ? Color.web("#d35400") : Color.web("#2c3e50"));
                    } catch (Exception ex) {
                        setTextFill(Color.web("#2c3e50"));
                    }
                }
            }
        });

        hashCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-font-size: 14;");
            }
        });

        Label infoLabel = new Label("Used for key derivation with Argon2id");
        infoLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #7f8c8d;");
        infoLabel.setPadding(new Insets(8, 0, 0, 0));

        section.getChildren().addAll(header, hashCombo, infoLabel);
        return section;
    }

    private VBox createEntropySection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(18));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        Label header = new Label("Random Number Generator");
        header.setFont(Font.font("System", FontWeight.BOLD, 14));
        header.setTextFill(Color.web("#34495e"));

        mouseEntropyCheck.setSelected(CryptoSettings.getInstance().isUseMouseEntropy());
        mouseEntropyCheck.setStyle("-fx-font-size: 13;");

        Label infoLabel = new Label(
                "When enabled, you will be asked to move your mouse randomly " +
                "to collect additional entropy. This supplements system entropy.");
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #7f8c8d;");
        infoLabel.setPadding(new Insets(8, 0, 0, 0));

        section.getChildren().addAll(header, mouseEntropyCheck, infoLabel);
        return section;
    }

    private void setupWarningSection() {
        warningSection.setPadding(new Insets(15));
        warningSection.setStyle("-fx-background-color: #fdebd0; -fx-background-radius: 10; " +
                "-fx-border-color: #f0b27a; -fx-border-radius: 10; -fx-border-width: 1;");

        warningLabel.setWrapText(true);
        warningLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #d35400;");

        warningSection.setVisible(false);
        warningSection.setManaged(false);
    }

    private void updateCipherInfo() {
        String selected = cipherCombo.getValue();
        if (selected == null) {
            cipherInfoLabel.setText("");
            warningSection.setVisible(false);
            warningSection.setManaged(false);
            return;
        }

        try {
            CipherProvider provider = CipherRegistry.get(selected);
            StringBuilder info = new StringBuilder();
            info.append("Key: ").append(provider.getKeyLengthBytes() * 8).append("-bit");
            info.append("  •  Block: 128-bit  •  Mode: GCM");

            if (CipherRegistry.isCascade(selected)) {
                CascadeCipherProvider cascade = (CascadeCipherProvider) provider;
                info.append("\nLayers: ").append(String.join(" → ", cascade.getLayerAlgorithms()));
            }

            cipherInfoLabel.setText(info.toString());

            if (provider.isExperimental()) {
                warningLabel.setText(
                        "⚠️ EXPERIMENTAL: This algorithm has not been security audited. " +
                        "The security guarantees in SECURITY_AUDIT.md do not apply.");
                warningSection.setVisible(true);
                warningSection.setManaged(true);
            } else {
                warningSection.setVisible(false);
                warningSection.setManaged(false);
            }
        } catch (Exception e) {
            cipherInfoLabel.setText("Error loading cipher info");
            warningSection.setVisible(false);
            warningSection.setManaged(false);
        }
    }

    public static Optional<CryptoSettings> show(Stage owner) {
        CryptoOptionsDialog dialog = new CryptoOptionsDialog(owner);
        return dialog.showAndWait();
    }
}
