package sv.arrupe.biblioteca.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import sv.arrupe.biblioteca.model.Libro;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LibrosService {

    private final ApiClient api;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<Libro>>() {}.getType();

    public LibrosService(ApiClient api) {
        this.api = api;
    }

    // GET /Libros?q=...
    public List<Libro> listar(String q) throws Exception {
        String path = "/Libros";
        if (q != null && !q.isBlank()) {
            String enc = URLEncoder.encode(q.trim(), StandardCharsets.UTF_8);
            path += "?q=" + enc;
        }

        HttpResponse<String> res = api.get(path);
        if (res.statusCode() == 200) {
            return gson.fromJson(res.body(), listType);
        }
        throw new RuntimeException("Error listar libros (" + res.statusCode() + "): " + res.body());
    }

    public Libro getById(int id) throws Exception {
        var res = api.get("/Libros/" + id);
        if (res.statusCode() == 200) {
            return gson.fromJson(res.body(), Libro.class);
        }
        throw new RuntimeException("Error getById (" + res.statusCode() + "): " + res.body());
    }

    // PUT /Libros/{id} (tu API exige id_libro en body y en URL)
    public Libro actualizar(int id, Libro libro) throws Exception {
        String body = gson.toJson(libro);
        HttpResponse<String> res = api.put("/Libros/" + id, body);

        if (res.statusCode() == 200) {
            return gson.fromJson(res.body(), Libro.class);
        }
        throw new RuntimeException("Error actualizar (" + res.statusCode() + "): " + res.body());
    }

    // PATCH /Libros/{id}/estado  body: "INACTIVO" (string JSON)
    public void cambiarEstado(int id, String nuevoEstado) throws Exception {
        String body = gson.toJson(nuevoEstado); // queda con comillas -> "ACTIVO"
        HttpResponse<String> res = api.patch("/Libros/" + id + "/estado", body);

        if (res.statusCode() == 200 || res.statusCode() == 204) return;
        throw new RuntimeException("Error cambiar estado (" + res.statusCode() + "): " + res.body());
    }

    public Libro crear(Libro libro) throws Exception {
        String body = gson.toJson(libro);
        var res = api.post("/Libros", body);

        if (res.statusCode() == 201 || res.statusCode() == 200) {
            return gson.fromJson(res.body(), Libro.class);
        }
        throw new RuntimeException("Error crear (" + res.statusCode() + "): " + res.body());
    }

    public void subirPortada(int idLibro, java.io.File file) throws Exception {
        var res = api.postMultipart("/Libros/" + idLibro + "/portada", file);
        if (res.statusCode() == 200) return;
        throw new RuntimeException("Error subiendo portada (" + res.statusCode() + "): " + res.body());
    }
}