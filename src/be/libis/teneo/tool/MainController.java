package be.libis.teneo.tool;

import be.libis.teneo.tool.model.I18N;
import be.libis.teneo.tool.model.UserSettings;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class MainController {
    public MenuBar menuBar;
    public Menu mnuFile;
    public MenuItem miClose;
    public Menu mnuEdit;
    public MenuItem miDesktopShortcut;
    public MenuItem miMenuShortcut;
    public Menu mnuTools;
    public MenuItem miMd5;
    public MenuItem miUpload;
    public Menu mnuHelp;
    public Menu mnuLanguage;
    public MenuItem miAbout;
    public BorderPane paneApp;

    public MainController() {
    }

    public void initialize() {
        setupI18N();
        setupMenu();
        switch (UserSettings.getTool()) {
            case "MD5Checker":
                loadMD5Checker();
                break;
            case "Uploader":
                loadUploader();
                break;
        }
    }

    private void setupI18N() {
        I18N.setText(mnuFile,"menu.File");
        I18N.setText(miClose, "menu.Close");
        I18N.setText(mnuTools, "menu.Tools");
        I18N.setText(mnuHelp, "menu.Help");
        I18N.setText(mnuLanguage, "menu.Language");
        I18N.setText(miAbout, "menu.About");
    }

    private void setupMenu() {
        miClose.setOnAction(event -> Platform.exit());
        I18N.getSupportedLocales().forEach(locale -> {
            MenuItem menuItem = new MenuItem();
            menuItem.setText(locale.getDisplayLanguage(locale));
            menuItem.setOnAction(event -> {
                MenuItem item = (MenuItem) event.getSource();
                I18N.setLocale(item.getText());
            });
            mnuLanguage.getItems().add(menuItem);
        });
        miAbout.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(MessageFormat.format("Teneo Tool - {0} (C) LIBIS, 2018", getClass().getPackage().getImplementationVersion()));
            Label label = new Label(I18N.get("msg.About"));
            switch (UserSettings.getTool()) {
                case "MD5Checker":
                    label = new Label(I18N.get("msg.Md5Help"));
                    break;
                case "Uploader":
                    label = new Label(I18N.get("msg.UploadHelp"));
            }
            alert.getDialogPane().setContent(label);
            alert.showAndWait();
        });
        miMd5.setOnAction(event -> {
            loadMD5Checker();
        });
        miUpload.setOnAction(event -> {
            loadUploader();
        });
    }

    private void loadUploader() {
        ResourceBundle resources = ResourceBundle.getBundle("TeneoTool", I18N.getDefaultLocale());
        try {
            VBox scene = FXMLLoader.load(getClass().getResource("Uploader.fxml"), resources);
            paneApp.centerProperty().set(scene);
            UserSettings.setTool("Uploader");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadMD5Checker() {
        ResourceBundle resources = ResourceBundle.getBundle("TeneoTool", I18N.getDefaultLocale());
        try {
            VBox scene = FXMLLoader.load(getClass().getResource("Md5Checker.fxml"), resources);
            paneApp.centerProperty().set(scene);
            UserSettings.setTool("MD5Checker");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
