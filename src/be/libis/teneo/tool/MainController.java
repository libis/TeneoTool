package be.libis.teneo.tool;

import be.libis.teneo.tool.model.I18N;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
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
    }

    private void setupI18N() {
        I18N.setText(mnuFile,"menu.File");
        I18N.setText(miClose, "menu.Close");
        I18N.setText(mnuTools, "menu.Tools");
        I18N.setText(mnuHelp, "menu.Help");
        I18N.setText(mnuLanguage, "menu.Language");
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
            alert.setHeaderText(MessageFormat.format("Teneo Tool version {0}", getClass().getPackage().getImplementationVersion()));
            Label label = new Label(I18N.get("msg.About"));
            alert.getDialogPane().setContent(label);
            alert.showAndWait();
        });
        miMd5.setOnAction(event -> {
            ResourceBundle resources = ResourceBundle.getBundle("TeneoTool", I18N.getDefaultLocale());
            try {
                VBox scene = FXMLLoader.load(getClass().getResource("Md5Checker.fxml"), resources);
                paneApp.centerProperty().set(scene);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        miUpload.setOnAction(event -> {
            ResourceBundle resources = ResourceBundle.getBundle("TeneoTool", I18N.getDefaultLocale());
            try {
                VBox scene = FXMLLoader.load(getClass().getResource("Uploader.fxml"), resources);
                paneApp.centerProperty().set(scene);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}
