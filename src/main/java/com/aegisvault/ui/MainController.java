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

import com.aegisvault.crypto.experimental.CryptoOptionsDialog;
import com.aegisvault.crypto.experimental.CryptoSettings;
import com.aegisvault.crypto.experimental.EntropyCollectionDialog;
import com.aegisvault.exception.AuthenticationException;
import com.aegisvault.service.VaultService;
import com.aegisvault.vfs.VfsEntry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

public class MainController {

    private static final String PREFS_RECENT_VAULTS = "recentVaults";
    private static final String PREFS_AUTO_LOCK_MINUTES = "autoLockMinutes";
    private static final int MAX_RECENT_VAULTS = 5;

    private final VaultService vaultService;
    private final Stage stage;
    private final BorderPane root;
    private final ListView<VfsEntry> fileListView;
    private final Label statusLabel;
    private final Label pathLabel;
    private final Preferences prefs;
    private String currentPath = "/";

    private TextField searchField;
    private ObservableList<VfsEntry> allEntries;
    private FilteredList<VfsEntry> filteredEntries;

    private Menu recentVaultsMenu;
    private MenuItem closeVaultMenuItem;
    private MenuItem importFileMenuItem;
    private MenuItem importFolderMenuItem;
    private MenuItem exportSelectedMenuItem;
    private MenuItem newFolderMenuItem;
    private MenuItem changePasswordMenuItem;
    private MenuItem backupMenuItem;
    private Menu settingsMenu;

    public MainController(VaultService vaultService, Stage stage) {
        this.vaultService = vaultService;
        this.stage = stage;
        this.root = new BorderPane();
        this.fileListView = new ListView<>();
        this.statusLabel = new Label("No vault open");
        this.pathLabel = new Label("/");
        this.prefs = Preferences.userNodeForPackage(MainController.class);
        this.allEntries = FXCollections.observableArrayList();
        this.filteredEntries = new FilteredList<>(allEntries, p -> true);

        fileListView.setItems(filteredEntries);
        fileListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        int savedTimeout = prefs.getInt(PREFS_AUTO_LOCK_MINUTES, 15);
        vaultService.setAutoLockTimeout(savedTimeout * 60 * 1000L);

        vaultService.setOnAutoLockCallback(() -> {
            javafx.application.Platform.runLater(() -> {
                showWelcomeView();
                updateMenuState(false);
                updateStatus("Vault auto-locked due to inactivity — keys wiped");
                showInfo("Auto-Lock", "Your vault has been automatically locked due to inactivity.\n\nAll encryption keys have been wiped from memory.");
            });
        });

        initializeUI();
        setupKeyboardShortcuts();
    }

    private void initializeUI() {
        root.setTop(createMenuBar());
        root.setCenter(createMainContent());
        root.setBottom(createStatusBar());

        showWelcomeView();
        updateMenuState(false);
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("_File");
        fileMenu.setMnemonicParsing(true);

        MenuItem newVault = new MenuItem("_New Vault...");
        newVault.setMnemonicParsing(true);
        newVault.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));

        MenuItem openVault = new MenuItem("_Open Vault...");
        openVault.setMnemonicParsing(true);
        openVault.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));

        recentVaultsMenu = new Menu("Recent Vaults");
        updateRecentVaultsMenu();

        closeVaultMenuItem = new MenuItem("_Close Vault");
        closeVaultMenuItem.setMnemonicParsing(true);
        closeVaultMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN));

        MenuItem exit = new MenuItem("E_xit");
        exit.setMnemonicParsing(true);
        exit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));

        newVault.setOnAction(e -> handleNewVault());
        openVault.setOnAction(e -> handleOpenVault());
        closeVaultMenuItem.setOnAction(e -> handleCloseVault());
        exit.setOnAction(e -> Platform.exit());

        fileMenu.getItems().addAll(newVault, openVault, recentVaultsMenu, new SeparatorMenuItem(),
                closeVaultMenuItem, new SeparatorMenuItem(), exit);

        Menu vaultMenu = new Menu("_Vault");
        vaultMenu.setMnemonicParsing(true);

        importFileMenuItem = new MenuItem("_Import File...");
        importFileMenuItem.setMnemonicParsing(true);
        importFileMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN));

        importFolderMenuItem = new MenuItem("Import _Folder...");
        importFolderMenuItem.setMnemonicParsing(true);
        importFolderMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));

        exportSelectedMenuItem = new MenuItem("_Export Selected...");
        exportSelectedMenuItem.setMnemonicParsing(true);
        exportSelectedMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN));

        newFolderMenuItem = new MenuItem("New Fol_der...");
        newFolderMenuItem.setMnemonicParsing(true);
        newFolderMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN));

        changePasswordMenuItem = new MenuItem("Change _Password...");
        changePasswordMenuItem.setMnemonicParsing(true);

        backupMenuItem = new MenuItem("_Backup Vault...");
        backupMenuItem.setMnemonicParsing(true);
        backupMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN));

        importFileMenuItem.setOnAction(e -> handleImportFile());
        importFolderMenuItem.setOnAction(e -> handleImportFolder());
        exportSelectedMenuItem.setOnAction(e -> handleExportSelected());
        newFolderMenuItem.setOnAction(e -> handleNewFolder());
        changePasswordMenuItem.setOnAction(e -> handleChangePassword());
        backupMenuItem.setOnAction(e -> handleBackupVault());

        vaultMenu.getItems().addAll(importFileMenuItem, importFolderMenuItem, exportSelectedMenuItem,
                new SeparatorMenuItem(), newFolderMenuItem, new SeparatorMenuItem(),
                changePasswordMenuItem, backupMenuItem);

        settingsMenu = new Menu("_Settings");
        settingsMenu.setMnemonicParsing(true);

        MenuItem encryptionOptions = new MenuItem("Encryption Options...");
        encryptionOptions.setOnAction(e -> showEncryptionOptions());

        MenuItem autoLockSettings = new MenuItem("Auto-Lock Timeout...");
        autoLockSettings.setOnAction(e -> showAutoLockSettings());

        settingsMenu.getItems().addAll(encryptionOptions, new SeparatorMenuItem(), autoLockSettings);

        Menu helpMenu = new Menu("_Help");
        helpMenu.setMnemonicParsing(true);

        MenuItem about = new MenuItem("_About AegisVault-J");
        about.setMnemonicParsing(true);
        about.setAccelerator(new KeyCodeCombination(KeyCode.F1));
        about.setOnAction(e -> showAboutDialog());

        MenuItem shortcuts = new MenuItem("Keyboard _Shortcuts");
        shortcuts.setMnemonicParsing(true);
        shortcuts.setOnAction(e -> showShortcutsDialog());

        helpMenu.getItems().addAll(shortcuts, new SeparatorMenuItem(), about);

        menuBar.getMenus().addAll(fileMenu, vaultMenu, settingsMenu, helpMenu);
        return menuBar;
    }

    private VBox createMainContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        HBox toolbar = createToolbar();

        HBox pathBar = new HBox(10);
        pathBar.setAlignment(Pos.CENTER_LEFT);
        Button upButton = new Button("↑ Up");
        upButton.setOnAction(e -> navigateUp());
        upButton.setTooltip(new Tooltip("Go to parent folder"));
        pathLabel.getStyleClass().add("path-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("🔍 Search files...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.toLowerCase().trim();
            filteredEntries.setPredicate(entry -> {
                if (filter.isEmpty()) return true;
                return entry.getName().toLowerCase().contains(filter);
            });
        });

        pathBar.getChildren().addAll(upButton, pathLabel, spacer, searchField);

        fileListView.setCellFactory(lv -> new FileListCell());
        fileListView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                handleItemDoubleClick();
            }
        });
        fileListView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleItemDoubleClick();
            } else if (e.getCode() == KeyCode.DELETE) {
                handleDeleteSelected();
            } else if (e.getCode() == KeyCode.BACK_SPACE) {
                navigateUp();
            } else if (e.getCode() == KeyCode.F && e.isControlDown()) {
                searchField.requestFocus();
                searchField.selectAll();
            }
        });

        fileListView.setContextMenu(createContextMenu());

        VBox.setVgrow(fileListView, Priority.ALWAYS);
        content.getChildren().addAll(toolbar, pathBar, fileListView);
        return content;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(5));
        toolbar.getStyleClass().add("toolbar");

        Button importBtn = new Button("Import");
        importBtn.setTooltip(new Tooltip("Import files (Ctrl+I)"));

        Button exportBtn = new Button("Export");
        exportBtn.setTooltip(new Tooltip("Export selected (Ctrl+E)"));

        Button newFolderBtn = new Button("New Folder");
        newFolderBtn.setTooltip(new Tooltip("Create folder (Ctrl+D)"));

        Button deleteBtn = new Button("Delete");
        deleteBtn.setTooltip(new Tooltip("Delete selected (Delete)"));

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setTooltip(new Tooltip("Refresh file list (F5)"));

        importBtn.setOnAction(e -> handleImportFile());
        exportBtn.setOnAction(e -> handleExportSelected());
        newFolderBtn.setOnAction(e -> handleNewFolder());
        deleteBtn.setOnAction(e -> handleDeleteSelected());
        refreshBtn.setOnAction(e -> refreshFileList());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label selectionLabel = new Label("");
        fileListView.getSelectionModel().getSelectedItems().addListener(
            (javafx.collections.ListChangeListener<VfsEntry>) c -> {
                int count = fileListView.getSelectionModel().getSelectedItems().size();
                if (count == 0) {
                    selectionLabel.setText("");
                } else if (count == 1) {
                    selectionLabel.setText("1 item selected");
                } else {
                    selectionLabel.setText(count + " items selected");
                }
            });

        toolbar.getChildren().addAll(importBtn, exportBtn, new Separator(),
                newFolderBtn, deleteBtn, new Separator(), refreshBtn,
                spacer, selectionLabel);
        return toolbar;
    }

    private ContextMenu createContextMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem export = new MenuItem("Export...");
        MenuItem rename = new MenuItem("Rename...");
        MenuItem delete = new MenuItem("Delete");

        export.setOnAction(e -> handleExportSelected());
        rename.setOnAction(e -> handleRenameSelected());
        delete.setOnAction(e -> handleDeleteSelected());

        menu.getItems().addAll(export, new SeparatorMenuItem(), rename, delete);
        return menu;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.getStyleClass().add("status-bar");
        statusBar.getChildren().add(statusLabel);
        return statusBar;
    }

    private void showWelcomeView() {
        VBox welcome = new VBox(20);
        welcome.setAlignment(Pos.CENTER);
        welcome.setPadding(new Insets(50));

        Label title = new Label("AegisVault-J");
        title.getStyleClass().add("welcome-title");

        Label subtitle = new Label("Your Digital Safe for Sensitive Files");
        subtitle.getStyleClass().add("welcome-subtitle");

        Label description = new Label(
                "Import files into an encrypted vault • Export when needed • Your files stay protected");
        description.setStyle("-fx-text-fill: #555555; -fx-font-size: 13px;");

        Button newBtn = new Button("Create New Vault");
        Button openBtn = new Button("Open Existing Vault");

        newBtn.getStyleClass().add("welcome-button");
        openBtn.getStyleClass().add("welcome-button");

        newBtn.setOnAction(e -> handleNewVault());
        openBtn.setOnAction(e -> handleOpenVault());

        welcome.getChildren().addAll(title, subtitle, description, newBtn, openBtn);
        root.setCenter(welcome);
    }

    private void handleNewVault() {
        Optional<CryptoSettings> cryptoResult = CryptoOptionsDialog.show(stage);
        if (cryptoResult.isEmpty()) {
            return;
        }

        CryptoSettings settings = cryptoResult.get();

        if (settings.isUseMouseEntropy()) {
            byte[] entropy = EntropyCollectionDialog.collectEntropy(stage);
            if (entropy != null) {
                settings.setCollectedEntropy(entropy);
                EntropyCollectionDialog.contributeToSecureRandom(entropy, new SecureRandom());
            }
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Create New Vault");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("AegisVault Files", "*.avj"));
        chooser.setInitialFileName("vault.avj");
        File file = chooser.showSaveDialog(stage);

        if (file != null) {
            PasswordDialog dialog = new PasswordDialog("Create Vault", "Enter password for new vault:", true);
            Optional<char[]> result = dialog.showAndWait();

            result.ifPresent(password -> {
                try {
                    vaultService.createVault(file.toPath(), password);
                    currentPath = "/";
                    showVaultView();
                    refreshFileList();
                    updateMenuState(true);
                    addToRecentVaults(file.getAbsolutePath());
                    stage.setTitle("AegisVault-J — " + file.getName());
                    String statusMsg = "Vault created: " + file.getName() +
                                       " | Cipher: " + settings.getSelectedCipher() +
                                       " | Hash: " + settings.getSelectedHash();
                    updateStatus(statusMsg);
                } catch (Exception ex) {
                    showError("Failed to create vault", ex.getMessage());
                }
            });
        }
    }

    private void handleOpenVault() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Vault");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("AegisVault Files", "*.avj"));
        File file = chooser.showOpenDialog(stage);

        if (file != null) {
            openVaultFile(file);
        }
    }

    private void openVaultFile(File file) {
        PasswordDialog dialog = new PasswordDialog("Open Vault", "Enter password:", false);
        Optional<char[]> result = dialog.showAndWait();

        result.ifPresent(password -> {
            try {
                vaultService.openVault(file.toPath(), password);
                currentPath = "/";
                showVaultView();
                refreshFileList();
                updateMenuState(true);
                addToRecentVaults(file.getAbsolutePath());
                stage.setTitle("AegisVault-J — " + file.getName());
                updateStatus("Vault opened: " + file.getName());
            } catch (AuthenticationException ex) {
                showError("Authentication Failed", "Invalid password");
            } catch (Exception ex) {
                showError("Failed to open vault", ex.getMessage());
            }
        });
    }

    private void handleCloseVault() {
        if (vaultService.isVaultOpen()) {
            vaultService.close();
            allEntries.clear();
            showWelcomeView();
            updateMenuState(false);
            stage.setTitle("AegisVault-J");
            updateStatus("Vault locked — encryption keys wiped from memory");
        }
    }

    private void showVaultView() {
        VBox content = createMainContent();
        root.setCenter(content);
    }

    private void refreshFileList() {
        if (!vaultService.isVaultOpen()) return;

        try {
            List<VfsEntry> entries = vaultService.listDirectory(currentPath);
            allEntries.clear();
            allEntries.addAll(entries);
            pathLabel.setText(currentPath);
            if (searchField != null) {
                searchField.clear();
            }
            int fileCount = (int) entries.stream().filter(VfsEntry::isFile).count();
            int folderCount = entries.size() - fileCount;
            updateStatus(String.format("%d folder(s), %d file(s)", folderCount, fileCount));
        } catch (Exception ex) {
            showError("Error", ex.getMessage());
        }
    }

    private void handleItemDoubleClick() {
        VfsEntry selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (selected.isDirectory()) {
            currentPath = currentPath.equals("/") ? "/" + selected.getName() : currentPath + "/" + selected.getName();
            refreshFileList();
        }
    }

    private void navigateUp() {
        if (currentPath.equals("/")) return;

        int lastSlash = currentPath.lastIndexOf('/');
        currentPath = lastSlash <= 0 ? "/" : currentPath.substring(0, lastSlash);
        refreshFileList();
    }

    private void handleImportFile() {
        if (!vaultService.isVaultOpen()) {
            showError("No Vault Open", "Please open or create a vault first.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import File");
        List<File> files = chooser.showOpenMultipleDialog(stage);

        if (files != null && !files.isEmpty()) {
            if (files.size() == 1 && files.get(0).length() < 1024 * 1024) {
                try {
                    File file = files.get(0);
                    byte[] content = Files.readAllBytes(file.toPath());
                    String targetPath = currentPath.equals("/") ? "/" + file.getName() : currentPath + "/" + file.getName();
                    vaultService.createFile(targetPath, content);
                    refreshFileList();
                    updateStatus("File imported: " + file.getName());
                } catch (Exception ex) {
                    showError("Import Failed", ex.getMessage());
                }
            } else {
                importFilesWithProgress(files);
            }
        }
    }

    private void importFilesWithProgress(List<File> files) {
        ProgressDialog progressDialog = new ProgressDialog("Importing Files", "Preparing to import...");
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Task<Void> importTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int total = files.size();
                for (int i = 0; i < files.size(); i++) {
                    if (progressDialog.isCancelled()) {
                        cancelled.set(true);
                        break;
                    }

                    File file = files.get(i);
                    updateTitle("Importing Files");
                    updateMessage("Importing: " + file.getName() + " (" + formatFileSize(file.length()) + ")");
                    updateProgress(i, total);

                    try {
                        byte[] content = Files.readAllBytes(file.toPath());
                        String targetPath = currentPath.equals("/") ? "/" + file.getName() : currentPath + "/" + file.getName();
                        vaultService.createFile(targetPath, content);
                        successCount.incrementAndGet();
                    } catch (Exception ex) {
                        failCount.incrementAndGet();
                    }
                }
                updateProgress(total, total);
                return null;
            }
        };

        progressDialog.bindTask(importTask);

        Thread importThread = new Thread(importTask);
        importThread.setDaemon(true);
        importThread.start();

        progressDialog.showAndWait();

        Platform.runLater(() -> {
            refreshFileList();
            if (cancelled.get()) {
                updateStatus(String.format("Import cancelled: %d imported, %d failed", successCount.get(), failCount.get()));
            } else if (failCount.get() == 0) {
                updateStatus("✓ " + successCount.get() + " file(s) imported successfully");
            } else {
                updateStatus(String.format("%d imported, %d failed", successCount.get(), failCount.get()));
            }
        });
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private void handleImportFolder() {
        if (!vaultService.isVaultOpen()) {
            showError("No Vault Open", "Please open or create a vault first.");
            return;
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Import Folder");
        File folder = chooser.showDialog(stage);

        if (folder != null) {
            importFolderWithProgress(folder);
        }
    }

    private void importFolderWithProgress(File folder) {
        ProgressDialog progressDialog = new ProgressDialog("Importing Folder", "Scanning folder...");
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Task<Void> importTask = new Task<>() {
            @Override
            protected Void call() {
                List<Path> allFiles;
                try (Stream<Path> walk = Files.walk(folder.toPath())) {
                    allFiles = walk.filter(Files::isRegularFile).toList();
                } catch (Exception ex) {
                    allFiles = List.of();
                }

                int total = allFiles.size();
                updateTitle("Importing Folder");
                updateMessage("Importing: " + folder.getName());

                importFolderRecursiveWithProgress(folder.toPath(), currentPath, successCount, failCount, progressDialog, new AtomicInteger(0), total);
                return null;
            }
        };

        progressDialog.bindTask(importTask);

        Thread importThread = new Thread(importTask);
        importThread.setDaemon(true);
        importThread.start();

        progressDialog.showAndWait();

        Platform.runLater(() -> {
            refreshFileList();
            if (progressDialog.isCancelled()) {
                updateStatus(String.format("Import cancelled: %d files imported, %d failed", successCount.get(), failCount.get()));
            } else if (failCount.get() == 0) {
                updateStatus("✓ Folder imported: " + successCount.get() + " file(s)");
            } else {
                updateStatus(String.format("Folder imported: %d files, %d failed", successCount.get(), failCount.get()));
            }
        });
    }

    private void importFolderRecursiveWithProgress(Path source, String targetBase,
            AtomicInteger successCount, AtomicInteger failCount, ProgressDialog progressDialog,
            AtomicInteger processedCount, int totalFiles) {

        if (progressDialog.isCancelled()) return;

        String folderName = source.getFileName().toString();
        String targetPath = targetBase.equals("/") ? "/" + folderName : targetBase + "/" + folderName;

        try {
            vaultService.createDirectory(targetPath);
        } catch (Exception ex) {
            return;
        }

        File[] children = source.toFile().listFiles();
        if (children != null) {
            for (File child : children) {
                if (progressDialog.isCancelled()) break;

                if (child.isDirectory()) {
                    importFolderRecursiveWithProgress(child.toPath(), targetPath, successCount, failCount, progressDialog, processedCount, totalFiles);
                } else {
                    try {
                        byte[] content = Files.readAllBytes(child.toPath());
                        String filePath = targetPath + "/" + child.getName();
                        vaultService.createFile(filePath, content);
                        successCount.incrementAndGet();
                    } catch (Exception ex) {
                        failCount.incrementAndGet();
                    }
                    int processed = processedCount.incrementAndGet();
                    progressDialog.updateProgress((double) processed / totalFiles, "Importing: " + child.getName());
                }
            }
        }
    }


    private void handleExportSelected() {
        if (!vaultService.isVaultOpen()) return;

        List<VfsEntry> selectedItems = new ArrayList<>(fileListView.getSelectionModel().getSelectedItems());
        if (selectedItems.isEmpty()) {
            showError("No Selection", "Please select one or more files or folders to export.");
            return;
        }

        Alert warning = new Alert(Alert.AlertType.WARNING);
        warning.setTitle("⚠️ Security Warning");
        warning.setHeaderText("Exported files are NOT protected!");
        warning.getDialogPane().setStyle("-fx-background-color: #fff3cd;");
        int count = selectedItems.size();
        warning.setContentText(
                "You are about to export " + count + " item(s).\n\n" +
                "⚠️ IMPORTANT: When you export files from the vault, they become regular unencrypted files.\n\n" +
                "• Anyone with access to your computer can read them\n" +
                "• They are YOUR responsibility to protect\n" +
                "• DELETE exported files when you're done using them\n\n" +
                "Do you want to continue?");

        ButtonType yesButton = new ButtonType("Yes, Export", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("Cancel", ButtonBar.ButtonData.NO);
        warning.getButtonTypes().setAll(yesButton, noButton);

        if (warning.showAndWait().orElse(noButton) != yesButton) {
            return;
        }

        if (selectedItems.size() == 1 && selectedItems.get(0).isFile()) {
            VfsEntry selected = selectedItems.get(0);
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export File");
            chooser.setInitialFileName(selected.getName());
            File file = chooser.showSaveDialog(stage);

            if (file != null) {
                try {
                    String sourcePath = currentPath.equals("/") ? "/" + selected.getName() : currentPath + "/" + selected.getName();
                    byte[] content = vaultService.readFile(sourcePath);
                    Files.write(file.toPath(), content);
                    updateStatus("✓ File exported: " + file.getName());
                    showExportReminder();
                } catch (Exception ex) {
                    showError("Export Failed", ex.getMessage());
                }
            }
        } else {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Export To Folder");
            File targetDir = chooser.showDialog(stage);

            if (targetDir != null) {
                exportItemsWithProgress(selectedItems, targetDir.toPath());
            }
        }
    }

    private void exportItemsWithProgress(List<VfsEntry> items, Path targetDir) {
        ProgressDialog progressDialog = new ProgressDialog("Exporting Files", "Preparing export...");
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Task<Void> exportTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int total = items.size();
                for (int i = 0; i < items.size(); i++) {
                    if (progressDialog.isCancelled()) break;

                    VfsEntry entry = items.get(i);
                    String sourcePath = currentPath.equals("/") ? "/" + entry.getName() : currentPath + "/" + entry.getName();

                    updateTitle("Exporting Files");
                    updateMessage("Exporting: " + entry.getName());
                    updateProgress(i, total);

                    try {
                        if (entry.isDirectory()) {
                            exportFolderRecursive(sourcePath, targetDir);
                        } else {
                            byte[] content = vaultService.readFile(sourcePath);
                            Files.write(targetDir.resolve(entry.getName()), content);
                        }
                        successCount.incrementAndGet();
                    } catch (Exception ex) {
                        failCount.incrementAndGet();
                    }
                }
                updateProgress(total, total);
                return null;
            }
        };

        progressDialog.bindTask(exportTask);

        Thread exportThread = new Thread(exportTask);
        exportThread.setDaemon(true);
        exportThread.start();

        progressDialog.showAndWait();

        Platform.runLater(() -> {
            if (progressDialog.isCancelled()) {
                updateStatus(String.format("Export cancelled: %d exported, %d failed", successCount.get(), failCount.get()));
            } else if (failCount.get() == 0) {
                updateStatus("✓ " + successCount.get() + " item(s) exported successfully");
                showExportReminder();
            } else {
                updateStatus(String.format("%d exported, %d failed", successCount.get(), failCount.get()));
            }
        });
    }

    private void showExportReminder() {
        Alert reminder = new Alert(Alert.AlertType.INFORMATION);
        reminder.setTitle("Export Complete");
        reminder.setHeaderText("Remember to delete exported files");
        reminder.setContentText(
                "Your files have been exported successfully.\n\n" +
                "🔓 Remember: Exported files are NOT encrypted.\n" +
                "Delete them when you're done using them to maintain security.");
        reminder.showAndWait();
    }

    private void exportFolderRecursive(String sourcePath, Path targetDir) throws Exception {
        VfsEntry entry = vaultService.getEntry(sourcePath);
        Path targetPath = targetDir.resolve(entry.getName());

        if (entry.isDirectory()) {
            Files.createDirectories(targetPath);
            List<VfsEntry> children = vaultService.listDirectory(sourcePath);
            for (VfsEntry child : children) {
                String childPath = sourcePath.equals("/") ? "/" + child.getName() : sourcePath + "/" + child.getName();
                exportFolderRecursive(childPath, targetPath);
            }
        } else {
            byte[] content = vaultService.readFile(sourcePath);
            Files.write(targetPath, content);
        }
    }

    private void handleNewFolder() {
        if (!vaultService.isVaultOpen()) {
            showError("No Vault Open", "Please open or create a vault first.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Create New Folder");
        dialog.setContentText("Folder name:");

        dialog.showAndWait().ifPresent(name -> {
            try {
                String targetPath = currentPath.equals("/") ? "/" + name : currentPath + "/" + name;
                vaultService.createDirectory(targetPath);
                refreshFileList();
                updateStatus("Folder created: " + name);
            } catch (Exception ex) {
                showError("Error", ex.getMessage());
            }
        });
    }

    private void handleDeleteSelected() {
        if (!vaultService.isVaultOpen()) return;

        List<VfsEntry> selectedItems = new ArrayList<>(fileListView.getSelectionModel().getSelectedItems());
        if (selectedItems.isEmpty()) return;

        String message = selectedItems.size() == 1
                ? "Delete \"" + selectedItems.get(0).getName() + "\"?"
                : "Delete " + selectedItems.size() + " selected items?";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(message);
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int deleted = 0;
                int failed = 0;
                for (VfsEntry entry : selectedItems) {
                    try {
                        String targetPath = currentPath.equals("/") ? "/" + entry.getName() : currentPath + "/" + entry.getName();
                        vaultService.delete(targetPath);
                        deleted++;
                    } catch (Exception ex) {
                        failed++;
                    }
                }
                refreshFileList();
                if (failed == 0) {
                    updateStatus(deleted + " item(s) deleted");
                } else {
                    updateStatus(deleted + " deleted, " + failed + " failed");
                }
            }
        });
    }

    private void handleRenameSelected() {
        if (!vaultService.isVaultOpen()) return;

        VfsEntry selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("Rename");
        dialog.setHeaderText("Rename " + selected.getName());
        dialog.setContentText("New name:");

        dialog.showAndWait().ifPresent(newName -> {
            try {
                String sourcePath = currentPath.equals("/") ? "/" + selected.getName() : currentPath + "/" + selected.getName();
                String targetPath = currentPath.equals("/") ? "/" + newName : currentPath + "/" + newName;
                vaultService.move(sourcePath, targetPath);
                refreshFileList();
                updateStatus("Renamed to: " + newName);
            } catch (Exception ex) {
                showError("Error", ex.getMessage());
            }
        });
    }

    private void handleChangePassword() {
        if (!vaultService.isVaultOpen()) {
            showError("No Vault Open", "Please open a vault first.");
            return;
        }

        ChangePasswordDialog dialog = new ChangePasswordDialog();
        Optional<char[][]> result = dialog.showAndWait();

        result.ifPresent(passwords -> {
            try {
                vaultService.changePassword(passwords[0], passwords[1]);
                updateStatus("Password changed successfully");
                showInfo("Success", "Password has been changed.");
            } catch (Exception ex) {
                showError("Error", ex.getMessage());
            }
        });
    }

    private void handleBackupVault() {
        if (!vaultService.isVaultOpen()) {
            showError("No Vault Open", "Please open a vault first.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Backup Vault");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("AegisVault Backup", "*.avj.bak"));
        Path currentVault = vaultService.getCurrentVaultPath();
        chooser.setInitialFileName(currentVault.getFileName().toString() + ".bak");
        File backupFile = chooser.showSaveDialog(stage);

        if (backupFile != null) {
            ProgressDialog progressDialog = new ProgressDialog("Creating Backup", "Backing up vault...");
            progressDialog.hideCancelButton();

            Task<Void> backupTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    updateTitle("Creating Backup");
                    updateMessage("Copying vault file...");
                    Files.copy(currentVault, backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    return null;
                }
            };

            progressDialog.bindTask(backupTask);

            backupTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    updateStatus("✓ Backup created: " + backupFile.getName());
                    showInfo("Backup Complete", "Vault backed up to:\n" + backupFile.getAbsolutePath());
                });
            });

            backupTask.setOnFailed(e -> {
                Platform.runLater(() -> {
                    Throwable ex = backupTask.getException();
                    showError("Backup Failed", ex != null ? ex.getMessage() : "Unknown error");
                });
            });

            Thread backupThread = new Thread(backupTask);
            backupThread.setDaemon(true);
            backupThread.start();

            progressDialog.show();
        }
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About AegisVault-J");
        alert.setHeaderText("AegisVault-J v0.1.0");
        alert.setContentText("A Java-only encrypted container system.\n\n" +
                "Security Features:\n" +
                "• AES-256-GCM encryption\n" +
                "• Argon2id key derivation\n" +
                "• Cross-platform support\n\n" +
                "© 2026 Aegis Vault. All rights reserved.");
        alert.showAndWait();
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public BorderPane getRoot() {
        return root;
    }

    private void setupKeyboardShortcuts() {
        root.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F5) {
                refreshFileList();
                e.consume();
            }
        });
    }

    private void updateMenuState(boolean vaultOpen) {
        closeVaultMenuItem.setDisable(!vaultOpen);
        importFileMenuItem.setDisable(!vaultOpen);
        importFolderMenuItem.setDisable(!vaultOpen);
        exportSelectedMenuItem.setDisable(!vaultOpen);
        newFolderMenuItem.setDisable(!vaultOpen);
        changePasswordMenuItem.setDisable(!vaultOpen);
        backupMenuItem.setDisable(!vaultOpen);
    }

    private void updateRecentVaultsMenu() {
        recentVaultsMenu.getItems().clear();
        List<String> recentVaults = getRecentVaults();

        if (recentVaults.isEmpty()) {
            MenuItem empty = new MenuItem("(No recent vaults)");
            empty.setDisable(true);
            recentVaultsMenu.getItems().add(empty);
        } else {
            for (String path : recentVaults) {
                File file = new File(path);
                MenuItem item = new MenuItem(file.getName() + " — " + file.getParent());
                item.setOnAction(e -> {
                    if (file.exists()) {
                        if (vaultService.isVaultOpen()) {
                            handleCloseVault();
                        }
                        openVaultFile(file);
                    } else {
                        showError("File Not Found", "The vault file no longer exists:\n" + path);
                        removeFromRecentVaults(path);
                    }
                });
                recentVaultsMenu.getItems().add(item);
            }
            recentVaultsMenu.getItems().add(new SeparatorMenuItem());
            MenuItem clearRecent = new MenuItem("Clear Recent");
            clearRecent.setOnAction(e -> {
                prefs.put(PREFS_RECENT_VAULTS, "");
                updateRecentVaultsMenu();
            });
            recentVaultsMenu.getItems().add(clearRecent);
        }
    }

    private List<String> getRecentVaults() {
        String recent = prefs.get(PREFS_RECENT_VAULTS, "");
        if (recent.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(List.of(recent.split("\\|")));
    }

    private void addToRecentVaults(String path) {
        List<String> recent = getRecentVaults();
        recent.remove(path);
        recent.add(0, path);
        while (recent.size() > MAX_RECENT_VAULTS) {
            recent.remove(recent.size() - 1);
        }
        prefs.put(PREFS_RECENT_VAULTS, String.join("|", recent));
        updateRecentVaultsMenu();
    }

    private void removeFromRecentVaults(String path) {
        List<String> recent = getRecentVaults();
        recent.remove(path);
        prefs.put(PREFS_RECENT_VAULTS, String.join("|", recent));
        updateRecentVaultsMenu();
    }

    private void showEncryptionOptions() {
        Optional<CryptoSettings> result = CryptoOptionsDialog.show(stage);
        result.ifPresent(settings -> {
            StringBuilder msg = new StringBuilder();
            msg.append("Encryption settings updated:\n\n");
            msg.append("Cipher: ").append(settings.getSelectedCipher()).append("\n");
            msg.append("Hash: ").append(settings.getSelectedHash()).append("\n");
            msg.append("Mouse Entropy: ").append(settings.isUseMouseEntropy() ? "Enabled" : "Disabled");

            if (settings.isExperimentalCipher()) {
                msg.append("\n\n⚠️ WARNING: Experimental cipher selected.\n");
                msg.append("Security audit findings do NOT apply.");
            }

            showInfo("Encryption Options", msg.toString());
            updateStatus("Encryption: " + settings.getSelectedCipher() + " | Hash: " + settings.getSelectedHash());
        });
    }

    private void showAutoLockSettings() {
        int currentMinutes = (int) (vaultService.getAutoLockTimeout() / 60000);

        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(currentMinutes,
                1, 5, 10, 15, 30, 60);
        dialog.setTitle("Auto-Lock Settings");
        dialog.setHeaderText("Auto-Lock Timeout");
        dialog.setContentText("Lock vault after inactivity (minutes):");

        dialog.showAndWait().ifPresent(minutes -> {
            vaultService.setAutoLockTimeout(minutes * 60 * 1000L);
            prefs.putInt(PREFS_AUTO_LOCK_MINUTES, minutes);
            updateStatus("Auto-lock set to " + minutes + " minutes");
        });
    }

    private void showShortcutsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("AegisVault-J Keyboard Shortcuts");
        alert.setContentText(
                "File Operations:\n" +
                "  Ctrl+N     Create new vault\n" +
                "  Ctrl+O     Open vault\n" +
                "  Ctrl+L     Close/Lock vault\n" +
                "  Ctrl+Q     Exit application\n\n" +
                "Vault Operations:\n" +
                "  Ctrl+I     Import file\n" +
                "  Ctrl+Shift+I  Import folder\n" +
                "  Ctrl+E     Export selected\n" +
                "  Ctrl+D     New folder\n" +
                "  Ctrl+B     Backup vault\n\n" +
                "Navigation:\n" +
                "  Enter      Navigate into folder\n" +
                "  Backspace  Go to parent folder\n" +
                "  Delete     Delete selected\n" +
                "  Ctrl+F     Search files\n" +
                "  F5         Refresh\n" +
                "  F1         About"
        );
        alert.showAndWait();
    }

    private static class FileListCell extends ListCell<VfsEntry> {
        @Override
        protected void updateItem(VfsEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                String icon = item.isDirectory() ? "📁 " : "📄 ";
                String size = item.isFile() ? formatSize(item.getSize()) : "";
                setText(icon + item.getName() + (size.isEmpty() ? "" : "  (" + size + ")"));
            }
        }

        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
