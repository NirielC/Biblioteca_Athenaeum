package sv.arrupe.biblioteca.controller;

import javafx.application.Platform;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import sv.arrupe.biblioteca.model.Prestamo;
import sv.arrupe.biblioteca.services.ApiClient;
import sv.arrupe.biblioteca.services.PrestamosService;
import sv.arrupe.biblioteca.util.Navigator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class ActivosController {

    @FXML private ListView<Prestamo> listActivos;

    private final ObservableList<Prestamo> data = FXCollections.observableArrayList();
    private final PrestamosService service =
            new PrestamosService(new ApiClient("http://localhost:5145/api"));

    private static final DateTimeFormatter OUT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        listActivos.setItems(data);
        listActivos.setFocusTraversable(false);

        listActivos.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Prestamo p, boolean empty) {
                super.updateItem(p, empty);

                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label badge = new Label(isAtrasado(p.fecha_vencimiento) ? "ATRASADO" : "EN TIEMPO");
                badge.getStyleClass().addAll("badge", isAtrasado(p.fecha_vencimiento) ? "badge-late" : "badge-ok");

                Label lector = new Label("Lector: " + safe(p.lector_nombre));
                lector.getStyleClass().add("row-title");

                Label libro = new Label("Libro: " + safe(p.libro_titulo));
                libro.getStyleClass().add("row-title");

                Label venc = new Label("Vence: " + formatFecha(p.fecha_vencimiento));
                venc.getStyleClass().add("row-meta");

                Button btnDevolver = new Button("Registrar devolución");
                btnDevolver.getStyleClass().addAll("btn", "btn-primary");
                btnDevolver.setOnAction(e -> devolver(p));

                VBox box = new VBox(8,
                        new HBox(10, badge),
                        lector, libro, venc,
                        btnDevolver
                );

                box.getStyleClass().add("card");
                if (isAtrasado(p.fecha_vencimiento)) box.getStyleClass().add("card-late");

                setText(null);
                setGraphic(box);
            }
        });

        cargar();
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private String formatFecha(String raw) {
        if (raw == null || raw.isBlank()) return "-";
        String s = raw.trim();

        try {
            if (s.endsWith("Z")) s = s.substring(0, s.length() - 1);

            // recorta milisegundos largos si vienen
            if (s.contains(".")) {
                String[] parts = s.split("\\.");
                String ms = parts[1];
                if (ms.length() > 3) ms = ms.substring(0, 3);
                s = parts[0] + "." + ms;
            }

            LocalDateTime dt = LocalDateTime.parse(s);
            return dt.format(OUT_FMT);
        } catch (Exception ignored) {
            return raw;
        }
    }

    private boolean isAtrasado(String fechaVenc) {
        try {
            if (fechaVenc == null || fechaVenc.isBlank()) return false;

            String s = fechaVenc.trim();
            if (s.endsWith("Z")) s = s.substring(0, s.length() - 1);

            if (s.contains(".")) {
                String[] parts = s.split("\\.");
                String ms = parts[1];
                if (ms.length() > 3) ms = ms.substring(0, 3);
                s = parts[0] + "." + ms;
            }

            LocalDateTime venc = LocalDateTime.parse(s);
            return LocalDateTime.now().isAfter(venc);
        } catch (Exception e) {
            return false;
        }
    }

    private void cargar() {
        CompletableFuture.supplyAsync(() -> {
                    try { return service.activos(); }
                    catch (Exception e) { throw new RuntimeException(e); }
                }).thenAccept(list -> Platform.runLater(() -> data.setAll(list)))
                .exceptionally(ex -> { Platform.runLater(() -> alertError(rootMsg(ex))); return null; });
    }

    private void devolver(Prestamo p) {
        CompletableFuture.runAsync(() -> {
                    try { service.devolver(p.id_prestamo); }
                    catch (Exception e) { throw new RuntimeException(e); }
                }).thenRun(() -> Platform.runLater(() -> data.remove(p)))
                .exceptionally(ex -> { Platform.runLater(() -> alertError(rootMsg(ex))); return null; });
    }

    @FXML
    private void onRefrescar() {
        cargar();
    }

    @FXML
    private void onVolverMenu() {
        Stage stage = (Stage) listActivos.getScene().getWindow();
        Navigator.go(stage, "/view/menu.fxml", 900, 600);
    }

    private String rootMsg(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) t = t.getCause();
        String msg = t.getMessage();
        return (msg == null || msg.isBlank()) ? "Error desconocido" : msg;
    }

    private void alertError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText("Ocurrió un problema");
        a.setContentText(msg);
        a.showAndWait();
    }
}