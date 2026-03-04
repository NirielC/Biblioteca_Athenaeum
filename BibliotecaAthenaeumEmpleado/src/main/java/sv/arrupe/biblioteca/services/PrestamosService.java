package sv.arrupe.biblioteca.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import sv.arrupe.biblioteca.model.Prestamo;

import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.List;

public class PrestamosService {

    private final ApiClient api;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<Prestamo>>() {}.getType();

    public PrestamosService(ApiClient api) {
        this.api = api;
    }

    // GET /Prestamos/pendientes
    public List<Prestamo> pendientes() throws Exception {
        HttpResponse<String> res = api.get("/Prestamos/pendientes");

        if (res.statusCode() == 200) {
            return gson.fromJson(res.body(), listType);
        }
        throw new RuntimeException("Error pendientes (" + res.statusCode() + "): " + res.body());
    }

    // PATCH /Prestamos/{id}/aprobar  body: int id_empleado  -> JSON: 5
    public void aprobar(int idPrestamo, int idEmpleado) throws Exception {
        String body = gson.toJson(idEmpleado); // "5" (sin comillas)
        HttpResponse<String> res = api.patch("/Prestamos/" + idPrestamo + "/aprobar", body);

        if (res.statusCode() == 200) return;
        throw new RuntimeException("Error aprobar (" + res.statusCode() + "): " + res.body());
    }

    // PATCH /Préstamos/{id}/rechazar body: vacío (pero mandamos {} por compatibilidad)
    public void rechazar(int idPrestamo) throws Exception {
        String body = "{}";
        HttpResponse<String> res = api.patch("/Prestamos/" + idPrestamo + "/rechazar", body);

        if (res.statusCode() == 200) return;
        throw new RuntimeException("Error rechazar (" + res.statusCode() + "): " + res.body());
    }

    // GET /Préstamos/activos
    public List<Prestamo> activos() throws Exception {
        HttpResponse<String> res = api.get("/Prestamos/activos");

        if (res.statusCode() == 200) {
            return gson.fromJson(res.body(), listType);
        }

        throw new RuntimeException("Error activos (" + res.statusCode() + "): " + res.body());
    }

    // PATCH /Prestamos/{id}/devolver
    public void devolver(int idPrestamo) throws Exception {
        String body = "{}";

        HttpResponse<String> res =
                api.patch("/Prestamos/" + idPrestamo + "/devolver", body);

        if (res.statusCode() == 200) return;

        throw new RuntimeException("Error devolver (" + res.statusCode() + "): " + res.body());
    }

    public List<Prestamo> atrasados() throws Exception {
        HttpResponse<String> res = api.get("/Prestamos/atrasados");

        if (res.statusCode() == 200) {
            return gson.fromJson(res.body(), listType);
        }

        throw new RuntimeException("Error atrasados (" + res.statusCode() + "): " + res.body());
    }

    public List<Prestamo> historial(String estado) throws Exception {
        String path = "/Prestamos/historial";
        if (estado != null && !estado.isBlank() && !estado.equalsIgnoreCase("TODOS")) {
            path += "?estado=" + estado.trim().toUpperCase();
        }

        HttpResponse<String> res = api.get(path);

        if (res.statusCode() == 200) {
            return gson.fromJson(res.body(), listType);
        }
        throw new RuntimeException("Error historial (" + res.statusCode() + "): " + res.body());
    }
}