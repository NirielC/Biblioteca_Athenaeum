package sv.arrupe.biblioteca.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import sv.arrupe.biblioteca.model.Libro;
import sv.arrupe.biblioteca.services.ApiClient;
import sv.arrupe.biblioteca.services.LibrosService;
import sv.arrupe.biblioteca.util.Navigator;

import java.time.Year;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public class LibrosCardsController {

    private static final String BASE_API = "http://localhost:5145";

    // ====== Límites (ajústalos a tu BD si quieres) ======
    private static final int MAX_TITULO = 200;
    private static final int MAX_AUTOR = 150;
    private static final int MAX_EDITORIAL = 150;
    private static final int MAX_GENERO = 80;
    private static final int MAX_SINOPSIS = 1000;

    private static final int MAX_ISBN = 20;      // si tu DB tiene 13/20/30, ajusta aquí
    private static final int MAX_STOCK = 9999;   // límite sano
    private static final int MIN_ANIO = 1000;
    private static final int MAX_ANIO = 2026;    // o Year.now().getValue()

    @FXML private TextField txtBuscar;
    @FXML private ScrollPane scroll;
    @FXML private TilePane gridLibros;

    private final ObservableList<Libro> data = FXCollections.observableArrayList();
    private final LibrosService service = new LibrosService(new ApiClient(BASE_API + "/api"));

    private Integer selectedId = null;

    // ================== TEXT FORMATTERS ==================
    private static TextFormatter<String> maxLen(int max) {
        return new TextFormatter<>(c -> c.getControlNewText().length() <= max ? c : null);
    }

    private static TextFormatter<String> onlyDigitsMax(int max) {
        UnaryOperator<TextFormatter.Change> filter = c -> {
            String text = c.getControlNewText();
            if (text.length() > max) return null;
            return text.matches("\\d*") ? c : null;
        };
        return new TextFormatter<>(filter);
    }

    private static TextFormatter<String> isbnFormatter(int max) {
        UnaryOperator<TextFormatter.Change> filter = c -> {
            String text = c.getControlNewText();
            if (text.length() > max) return null;
            // permite dígitos, X/x y guiones
            return text.matches("[0-9Xx-]*") ? c : null;
        };
        return new TextFormatter<>(filter);
    }

    @FXML
    private void initialize() {
        if (scroll != null) {
            scroll.viewportBoundsProperty().addListener((obs, oldB, b) -> ajustarColumnas(b.getWidth()));
        }
        cargar(null);
    }

    private void ajustarColumnas(double viewportWidth) {
        double cardWidth = 360;
        double gap = 16;

        int cols = (int) Math.floor((viewportWidth - 24) / (cardWidth + gap));
        if (cols < 1) cols = 1;
        gridLibros.setPrefColumns(cols);
    }

    private void render() {
        gridLibros.getChildren().clear();
        for (Libro l : data) {
            VBox card = crearCard(l);
            gridLibros.getChildren().add(card);
        }
    }

    private VBox crearCard(Libro l) {
        ImageView img = new ImageView();
        img.setFitWidth(140);
        img.setFitHeight(200);
        img.setPreserveRatio(true);
        img.getStyleClass().add("cover");

        String ruta = (l.imagen_portada == null) ? "" : l.imagen_portada.trim();
        if (!ruta.isBlank()) {
            try {
                String url = ruta.startsWith("http") ? ruta : (BASE_API + ruta);
                img.setImage(new Image(url, true));
            } catch (Exception ignored) {}
        }

        StackPane imgWrap = new StackPane(img);
        imgWrap.getStyleClass().add("cover-wrap");

        Label titulo = new Label(safe(l.titulo));
        titulo.getStyleClass().add("book-title");
        titulo.setWrapText(true);

        String est = safe(l.estado);
        Label badge = new Label(est);
        badge.getStyleClass().addAll("badge",
                "ACTIVO".equalsIgnoreCase(est) ? "badge-activo" : "badge-inactivo"
        );

        Label autor = new Label(safe(l.autor));
        autor.getStyleClass().add("meta-strong");

        Label editorial = new Label("Editorial: " + safe(l.editorial));
        editorial.getStyleClass().add("meta");

        Label genero = new Label("Género: " + safe(l.genero));
        genero.getStyleClass().add("meta");

        Label año = new Label("Año: " + (l.año_publicación == null ? "-" : l.año_publicación));
        año.getStyleClass().add("meta");

        Label stock = new Label("Stock: " + l.stock);
        stock.getStyleClass().add("meta");

        String sin = safe(l.sinopsis);
        if (sin.length() > 110) sin = sin.substring(0, 110) + "...";
        Label sinopsis = new Label(sin);
        sinopsis.setWrapText(true);
        sinopsis.getStyleClass().add("synopsis");

        HBox rowYearStock = new HBox(16, año, stock);

        VBox info = new VBox(6, badge, titulo, autor, editorial, genero, rowYearStock, sinopsis);
        info.getStyleClass().add("info");

        Button btnEditar = new Button("Editar");
        btnEditar.getStyleClass().addAll("btn", "btn-soft");
        btnEditar.setOnAction(e -> editarLibro(l));

        Button btnEstado = new Button("Activar/Desactivar");
        btnEstado.getStyleClass().addAll("btn", "btn-soft");
        btnEstado.setOnAction(e -> toggleEstado(l));

        Button btnPortada = new Button("Cambiar portada");
        btnPortada.getStyleClass().addAll("btn", "btn-primary");
        btnPortada.setOnAction(e -> cambiarPortada(l));

        // opcional pro: que ocupe todo el ancho
        btnPortada.setMaxWidth(Double.MAX_VALUE);

        HBox rowBtns = new HBox(10, btnEditar, btnEstado);
        rowBtns.getStyleClass().add("actions-row");

        VBox acciones = new VBox(10, rowBtns, btnPortada);
        acciones.getStyleClass().add("actions");

        VBox card = new VBox(12, imgWrap, info, acciones);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));

        if (selectedId != null && Objects.equals(selectedId, l.id_libro)) {
            if (!card.getStyleClass().contains("card-selected")) card.getStyleClass().add("card-selected");
        }

        card.setOnMouseClicked(e -> {
            selectedId = l.id_libro;
            render();
        });

        return card;
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    // ================== CARGA + ERRORES ==================
    private void cargar(String q) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return service.listar(q);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(list -> Platform.runLater(() -> {
            data.setAll(list);
            render();
        })).exceptionally(ex -> {
            Platform.runLater(() -> alertError(rootMsg(ex)));
            return null;
        });
    }

    private String rootMsg(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) t = t.getCause();
        String msg = t.getMessage();
        if (msg == null || msg.isBlank()) return "Error desconocido";
        return msg;
    }

    @FXML
    private void onBuscar() {
        String q = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim();
        cargar(q.isBlank() ? null : q);
    }

    @FXML
    private void onRefrescar() {
        txtBuscar.clear();
        cargar(null);
    }

    @FXML
    private void onVolverMenu() {
        Stage stage = (Stage) scroll.getScene().getWindow();
        Navigator.go(stage, "/view/menu.fxml", 900, 600);
    }

    @FXML
    private void onNuevo() {
        Libro nuevo = dialogLibro(null);
        if (nuevo == null) return;

        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Seleccionar portada (obligatorio)");
        fc.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        java.io.File file = fc.showOpenDialog(scroll.getScene().getWindow());
        if (file == null) {
            alertWarn("Portada obligatoria", "Debes seleccionar una imagen de portada.");
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                Libro creado = service.crear(nuevo);
                service.subirPortada(creado.id_libro, file);
                return service.getById(creado.id_libro);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(actualizado -> Platform.runLater(() -> {
            data.add(0, actualizado);
            selectedId = actualizado.id_libro;
            render();
            scroll.setVvalue(0);
        })).exceptionally(ex -> {
            Platform.runLater(() -> alertError(rootMsg(ex)));
            return null;
        });
    }

    private void editarLibro(Libro l) {
        Libro edit = dialogLibro(l);
        if (edit == null) return;

        edit.id_libro = l.id_libro;

        CompletableFuture.supplyAsync(() -> {
            try {
                service.actualizar(l.id_libro, edit);
                return service.getById(l.id_libro);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(upd -> Platform.runLater(() -> {
            int idx = data.indexOf(l);
            if (idx >= 0) data.set(idx, upd);
            render();
        })).exceptionally(ex -> {
            Platform.runLater(() -> alertError(rootMsg(ex)));
            return null;
        });
    }

    private void toggleEstado(Libro l) {
        String actual = (l.estado == null) ? "ACTIVO" : l.estado.trim().toUpperCase();
        String nuevo = actual.equals("ACTIVO") ? "INACTIVO" : "ACTIVO";

        CompletableFuture.runAsync(() -> {
            try {
                service.cambiarEstado(l.id_libro, nuevo);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenRun(() -> Platform.runLater(() -> {
            l.estado = nuevo;
            render();
        })).exceptionally(ex -> {
            Platform.runLater(() -> alertError(rootMsg(ex)));
            return null;
        });
    }

    // ================== DIALOG + VALIDACIONES FUERTES ==================
    private Libro dialogLibro(Libro base) {
        Dialog<Libro> dialog = new Dialog<>();
        dialog.setTitle(base == null ? "Nuevo libro" : "Editar libro");
        dialog.setHeaderText(base == null ? "Crear un libro" : "Modificar libro ID " + base.id_libro);

        ButtonType btnOk = new ButtonType(base == null ? "Crear" : "Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);

        TextField tTitulo = new TextField(base == null ? "" : nn(base.titulo));
        TextField tIsbn = new TextField(base == null ? "" : nn(base.isbn));
        TextField tAutor = new TextField(base == null ? "" : nn(base.autor));
        TextField tEditorial = new TextField(base == null ? "" : nn(base.editorial));
        TextField tGenero = new TextField(base == null ? "" : nn(base.genero));
        TextField tAnio = new TextField(base == null || base.año_publicación == null ? "" : base.año_publicación.toString());
        TextArea tSinopsis = new TextArea(base == null ? "" : nn(base.sinopsis));
        tSinopsis.setPrefRowCount(4);

        TextField tStock = new TextField(base == null ? "" : String.valueOf(base.stock));

        // ====== TextFormatters (bloquean entradas inválidas) ======
        tTitulo.setTextFormatter(maxLen(MAX_TITULO));
        tAutor.setTextFormatter(maxLen(MAX_AUTOR));
        tIsbn.setTextFormatter(isbnFormatter(MAX_ISBN));
        tEditorial.setTextFormatter(maxLen(MAX_EDITORIAL));
        tGenero.setTextFormatter(maxLen(MAX_GENERO));
        tSinopsis.setTextFormatter(maxLen(MAX_SINOPSIS));

        tAnio.setTextFormatter(onlyDigitsMax(4));
        tStock.setTextFormatter(onlyDigitsMax(4));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        int r = 0;
        grid.add(new Label("Título*:"), 0, r); grid.add(tTitulo, 1, r++);
        grid.add(new Label("ISBN:"), 0, r); grid.add(tIsbn, 1, r++);
        grid.add(new Label("Autor*:"), 0, r); grid.add(tAutor, 1, r++);
        grid.add(new Label("Editorial:"), 0, r); grid.add(tEditorial, 1, r++);
        grid.add(new Label("Género:"), 0, r); grid.add(tGenero, 1, r++);
        grid.add(new Label("Año publicación:"), 0, r); grid.add(tAnio, 1, r++);
        grid.add(new Label("Stock*:"), 0, r); grid.add(tStock, 1, r++);
        grid.add(new Label("Sinopsis:"), 0, r); grid.add(tSinopsis, 1, r++);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != btnOk) return null;

            String titulo = trim(tTitulo.getText());
            String autor = trim(tAutor.getText());
            String stockTxt = trim(tStock.getText());

            if (titulo.isBlank() || autor.isBlank() || stockTxt.isBlank()) {
                alertWarn("Campos obligatorios", "Título, autor y stock son obligatorios.");
                return null;
            }

            // ===== stock =====
            int stock;
            try { stock = Integer.parseInt(stockTxt); }
            catch (Exception e) { alertWarn("Stock inválido", "Stock debe ser un número entero."); return null; }

            if (stock < 0 || stock > MAX_STOCK) {
                alertWarn("Stock inválido", "Stock debe estar entre 0 y " + MAX_STOCK + ".");
                return null;
            }

            // ===== año =====
            Integer anio = null;
            String anioTxt = trim(tAnio.getText());
            if (!anioTxt.isBlank()) {
                try { anio = Integer.parseInt(anioTxt); }
                catch (Exception e) { alertWarn("Año inválido", "Año debe ser un número."); return null; }

                int max = MAX_ANIO; // o Year.now().getValue()
                if (anio < MIN_ANIO || anio > max) {
                    alertWarn("Año inválido", "Año debe estar entre " + MIN_ANIO + " y " + max + ".");
                    return null;
                }
            }

            // ===== isbn (longitud + formato 10/13 opcional) =====
            String isbn = nullIfBlank(tIsbn.getText());
            if (isbn != null) {
                String clean = isbn.replace("-", "").trim();
                // si quieres permitir cualquier longitud <= MAX_ISBN, comenta este if
                if (!(clean.length() == 10 || clean.length() == 13)) {
                    alertWarn("ISBN inválido", "ISBN debe tener 10 o 13 dígitos (puede llevar guiones).");
                    return null;
                }
            }

            Libro x = new Libro();
            x.titulo = titulo;
            x.isbn = isbn;
            x.autor = autor;
            x.editorial = nullIfBlank(tEditorial.getText());
            x.genero = nullIfBlank(tGenero.getText());
            x.año_publicación = anio;
            x.sinopsis = nullIfBlank(tSinopsis.getText());
            x.stock = stock;
            x.estado = (base == null) ? "ACTIVO" : base.estado;
            return x;
        });

        return dialog.showAndWait().orElse(null);
    }

    private void cambiarPortada(Libro l) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Seleccionar nueva portada");
        fc.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        java.io.File file = fc.showOpenDialog(scroll.getScene().getWindow());
        if (file == null) return;

        CompletableFuture.runAsync(() -> {
            try {
                service.subirPortada(l.id_libro, file);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenRun(() -> Platform.runLater(() -> {
            CompletableFuture.supplyAsync(() -> {
                try { return service.getById(l.id_libro); }
                catch (Exception e) { throw new RuntimeException(e); }
            }).thenAccept(upd -> Platform.runLater(() -> {
                int idx = data.indexOf(l);
                if (idx >= 0) data.set(idx, upd);
                selectedId = upd.id_libro;
                render();
            })).exceptionally(ex -> {
                Platform.runLater(() -> alertError(rootMsg(ex)));
                return null;
            });
        })).exceptionally(ex -> {
            Platform.runLater(() -> alertError(rootMsg(ex)));
            return null;
        });
    }

    private String nn(String s) { return s == null ? "" : s; }

    private String trim(String s) { return s == null ? "" : s.trim(); }

    private String nullIfBlank(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isBlank() ? null : s;
    }

    private void alertError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText("Ocurrió un problema");
        a.setContentText(msg);
        a.showAndWait();
    }

    private void alertWarn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}