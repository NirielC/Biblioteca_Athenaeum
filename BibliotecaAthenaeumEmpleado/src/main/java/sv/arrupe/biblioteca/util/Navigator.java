package sv.arrupe.biblioteca.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Navigator {

    public static void go(Stage stage, String fxml, int w, int h) {
        try {
            Parent root = FXMLLoader.load(Navigator.class.getResource(fxml));
            stage.setScene(new Scene(root, w, h));
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo abrir: " + fxml + " -> " + e.getMessage());
        }
    }
}