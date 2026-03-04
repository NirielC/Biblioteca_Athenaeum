package sv.arrupe.biblioteca.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import sv.arrupe.biblioteca.services.*;
import sv.arrupe.biblioteca.util.AppSession;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class LoginController {

    @FXML private TextField txtCorreo;
    @FXML private PasswordField txtPass;
    @FXML private Label lblStatus;

    private final AuthService authService =
            new AuthService(new ApiClient("http://localhost:5145/api"));

    @FXML
    private void onLogin() {

        String correo = txtCorreo.getText() == null ? "" : txtCorreo.getText().trim();
        String pass = txtPass.getText() == null ? "" : txtPass.getText();

        if (correo.isBlank() || pass.isBlank()) {
            alert("Campos requeridos", "Escribe correo y contraseña.", Alert.AlertType.WARNING);
            return;
        }

        lblStatus.setText("Iniciando sesión...");

        CompletableFuture.supplyAsync(() -> {
            try {
                return authService.login(correo, pass);
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage());
            }
        }).thenAccept(data -> Platform.runLater(() -> {
            AppSession.set(data.token, data.rol, data.id_usuario, data.nombres, data.apellidos);
            lblStatus.setText("");

            try {
                cambiarVista();
            } catch (Exception ex) {
                alert("Error", "No se pudo abrir el menú: " + ex.getMessage(), Alert.AlertType.ERROR);
            }

        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                lblStatus.setText("");
                alert("Login falló", ex.getMessage(), Alert.AlertType.ERROR);
            });
            return null;
        });
    }

    private void cambiarVista() throws Exception {
        Stage stage = (Stage) txtCorreo.getScene().getWindow();
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/view/menu.fxml")));
        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    private void alert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}