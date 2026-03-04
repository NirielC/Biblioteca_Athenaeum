package sv.arrupe.biblioteca.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import sv.arrupe.biblioteca.util.AppSession;

import java.util.Objects;

public class MenuController {

    @FXML private Label lblBienvenida;
    @FXML private Label lblRol;

    @FXML
    private void initialize() {
        lblBienvenida.setText("Bienvenido, " +
                AppSession.getNombres() + " " +
                AppSession.getApellidos());

        lblRol.setText("Rol: " + AppSession.getRol());
    }

    @FXML
    private void onLogout() {
        AppSession.clear();
        navegar("/view/login.fxml", 900, 600);
    }

    @FXML
    private void irLibros() {
        navegar("/view/libros_cards.fxml", 1200, 650);
    }

    @FXML
    private void irPendientes() {
        navegar("/view/pendientes.fxml", 1000, 650);
    }

    @FXML
    private void irActivos() {
        navegar("/view/activos.fxml", 1000, 650);
    }

    @FXML
    private void irAtrasados() {
        navegar("/view/atrasados.fxml", 1000, 650);
    }

    @FXML
    private void irHistorial() {
        navegar("/view/historial.fxml", 1000, 650);
    }

    private void navegar(String ruta, int w, int h) {
        try {
            Stage stage = (Stage) lblRol.getScene().getWindow();
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource(ruta))
            );
            stage.setScene(new Scene(root, w, h));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}