package be.libis.teneo.tool;

import be.libis.teneo.tool.model.I18N;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.ResourceBundle;

public class Main extends Application {

    private Parent root;

    @Override
    public void start(Stage primaryStage) throws Exception{
        primaryStage.getIcons().add(new Image("images/logo_64x64.png"));
        ResourceBundle resources = ResourceBundle.getBundle("TeneoTool", I18N.getDefaultLocale());
        root = FXMLLoader.load(getClass().getResource("MainGui.fxml"), resources);
        primaryStage.setTitle("LIBIS Teneo Tool");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
