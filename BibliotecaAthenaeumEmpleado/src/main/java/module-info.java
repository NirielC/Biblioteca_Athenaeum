module sv.arrupe.biblioteca.bibliotecaathenaeumempleado {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.google.gson;

    opens sv.arrupe.biblioteca.controller to javafx.fxml;
    opens sv.arrupe.biblioteca.model to com.google.gson;

    opens sv.arrupe.biblioteca to javafx.fxml;
    exports sv.arrupe.biblioteca;
}