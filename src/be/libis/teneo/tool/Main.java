package be.libis.teneo.tool;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        primaryStage.getIcons().add(new Image("images/logo_64x64.png"));
        Parent root = FXMLLoader.load(getClass().getResource("MainGui.fxml"), ResourceBundle.getBundle("TeneoTool", Locale.ENGLISH));
        primaryStage.setTitle("LIBIS Teneo Tool");
        primaryStage.setScene(new Scene(root, 960, 700));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
