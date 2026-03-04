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
import sv.arrupe.biblioteca.util.AppSession;
import sv.arrupe.biblioteca.util.Navigator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class PendientesController {

    @FXML private ListView<Prestamo> listPendientes;

    private final ObservableList<Prestamo> data = FXCollections.observableArrayList();
    private final PrestamosService service =
            new PrestamosService(new ApiClient("http://localhost:5145/api"));

    private static final DateTimeFormatter OUT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        listPendientes.setItems(data);

        // Evita que se marque raro por foco
        listPendientes.setFocusTraversable(false);

        listPendientes.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Prestamo p, boolean empty) {
                super.updateItem(p, empty);

                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                // NO mostrar IDs
                Label lector = new Label("Lector: " + safe(p.lector_nombre));
                lector.getStyleClass().add("row-title");

                Label libro = new Label("Libro: " + safe(p.libro_titulo));
                libro.getStyleClass().add("row-title");

                Label fecha = new Label("Solicitud: " + formatFecha(p.fecha_solicitud));
                fecha.getStyleClass().add("row-meta");

                Button btnAprobar = new Button("Aprobar");
                btnAprobar.getStyleClass().addAll("btn", "btn-approve");
                btnAprobar.setOnAction(e -> aprobar(p));

                Button btnRechazar = new Button("Rechazar");
                btnRechazar.getStyleClass().addAll("btn", "btn-reject");
                btnRechazar.setOnAction(e -> rechazar(p));

                HBox botones = new HBox(10, btnAprobar, btnRechazar);

                VBox box = new VBox(8, lector, libro, fecha, botones);
                box.getStyleClass().add("card");

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

        // intenta ISO: 2026-03-03T20:44:28.913
        try {
            // cortar nanos si vienen largos o con Z
            if (s.endsWith("Z")) s = s.substring(0, s.length() - 1);
            // Si trae más de 3 decimales, recorta (JavaFX/Java a veces se pone picky)
            if (s.contains(".")) {
                String[] parts = s.split("\\.");
                String ms = parts[1];
                if (ms.length() > 3) ms = ms.substring(0, 3);
                s = parts[0] + "." + ms;
            }
            LocalDateTime dt = LocalDateTime.parse(s);
            return dt.format(OUT_FMT);
        } catch (Exception ignored) {}

        // fallback si no es ISO
        return raw;
    }

    private void cargar() {
        CompletableFuture.supplyAsync(() -> {
                    try { return service.pendientes(); }
                    catch (Exception e) { throw new RuntimeException(e); }
                }).thenAccept(list -> Platform.runLater(() -> data.setAll(list)))
                .exceptionally(ex -> { Platform.runLater(() -> alertError(rootMsg(ex))); return null; });
    }

    private void aprobar(Prestamo p) {
        int idEmpleado = AppSession.getIdUsuario();

        CompletableFuture.runAsync(() -> {
                    try { service.aprobar(p.id_prestamo, idEmpleado); }
                    catch (Exception e) { throw new RuntimeException(e); }
                }).thenRun(() -> Platform.runLater(() -> data.remove(p)))
                .exceptionally(ex -> { Platform.runLater(() -> alertError(rootMsg(ex))); return null; });
    }

    private void rechazar(Prestamo p) {
        CompletableFuture.runAsync(() -> {
                    try { service.rechazar(p.id_prestamo); }
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
        Stage stage = (Stage) listPendientes.getScene().getWindow();
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