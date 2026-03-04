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

public class HistorialController {

    @FXML private ComboBox<String> cbEstado;
    @FXML private ListView<Prestamo> listHistorial;

    private final ObservableList<Prestamo> data = FXCollections.observableArrayList();
    private final PrestamosService service =
            new PrestamosService(new ApiClient("http://localhost:5145/api"));

    private static final DateTimeFormatter OUT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {

        cbEstado.getItems().addAll("TODOS", "SOLICITADO", "PRESTADO", "DEVUELTO", "RECHAZADO", "CANCELADO");
        cbEstado.setValue("TODOS");

        listHistorial.setItems(data);
        listHistorial.setFocusTraversable(false);

        listHistorial.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Prestamo p, boolean empty) {
                super.updateItem(p, empty);

                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                String est = (p.estado == null) ? "-" : p.estado.trim().toUpperCase();

                Label badge = new Label(est);
                badge.getStyleClass().addAll("badge", badgeClass(est));

                // filas principales
                Label lector = new Label("Lector: " + safe(p.lector_nombre));
                lector.getStyleClass().add("value-strong");

                Label libro = new Label("Libro: " + safe(p.libro_titulo));
                libro.getStyleClass().add("value-strong");

                // fechas formateadas
                Label f1 = new Label("Solicitud: " + formatFecha(p.fecha_solicitud));
                f1.getStyleClass().add("value-meta");

                Label f2 = new Label("Aprobación: " + formatFecha(p.fecha_aprobacion));
                f2.getStyleClass().add("value-meta");

                Label f3 = new Label("Vencimiento: " + formatFecha(p.fecha_vencimiento));
                f3.getStyleClass().add("value-meta");

                Label f4 = new Label("Devolución: " + formatFecha(p.fecha_devolucion));
                f4.getStyleClass().add("value-meta");

                HBox rowEstado = new HBox(10, labelKV("Estado:"), badge);
                HBox row1 = new HBox(14, f1, f2);
                HBox row2 = new HBox(14, f3, f4);

                VBox box = new VBox(8,
                        rowEstado,
                        lector,
                        libro,
                        row1,
                        row2
                );
                box.getStyleClass().add("card");

                setText(null);
                setGraphic(box);
            }
        });

        cargar("TODOS");
    }

    private Label labelKV(String txt){
        Label l = new Label(txt);
        l.getStyleClass().add("kv");
        return l;
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private String badgeClass(String est) {
        if (est == null) return "badge-otro";
        return switch (est) {
            case "SOLICITADO" -> "badge-solicitado";
            case "PRESTADO"   -> "badge-prestado";
            case "DEVUELTO"   -> "badge-devuelto";
            case "RECHAZADO"  -> "badge-rechazado";
            case "CANCELADO"  -> "badge-cancelado";
            default -> "badge-otro";
        };
    }

    private String normalizeIso(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isBlank()) return null;

        if (s.endsWith("Z")) s = s.substring(0, s.length() - 1);

        if (s.contains(".")) {
            String[] parts = s.split("\\.");
            String ms = parts[1];
            if (ms.length() > 3) ms = ms.substring(0, 3);
            s = parts[0] + "." + ms;
        }
        return s;
    }

    private String formatFecha(String raw) {
        if (raw == null || raw.isBlank()) return "-";
        try {
            String s = normalizeIso(raw);
            if (s == null) return "-";
            LocalDateTime dt = LocalDateTime.parse(s);
            return dt.format(OUT_FMT);
        } catch (Exception e) {
            return raw; // si viene otro formato, al menos no truena
        }
    }

    private void cargar(String estado) {
        CompletableFuture.supplyAsync(() -> {
                    try { return service.historial(estado); }
                    catch (Exception e) { throw new RuntimeException(e); }
                }).thenAccept(list -> Platform.runLater(() -> data.setAll(list)))
                .exceptionally(ex -> { Platform.runLater(() -> alertError(rootMsg(ex))); return null; });
    }

    @FXML
    private void onFiltrar() {
        String estado = cbEstado.getValue();
        cargar(estado == null ? "TODOS" : estado);
    }

    @FXML
    private void onRefrescar() {
        cbEstado.setValue("TODOS");
        cargar("TODOS");
    }

    @FXML
    private void onVolverMenu() {
        Stage stage = (Stage) listHistorial.getScene().getWindow();
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