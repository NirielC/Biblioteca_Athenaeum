package sv.arrupe.biblioteca.services;

import com.google.gson.Gson;
import sv.arrupe.biblioteca.model.LoginRequest;
import sv.arrupe.biblioteca.model.LoginResponse;

public class AuthService {

    private final ApiClient api;
    private final Gson gson = new Gson();

    public AuthService(ApiClient api) {
        this.api = api;
    }

    public LoginResponse login(String correo, String contraseña) throws Exception {
        String body = gson.toJson(new LoginRequest(correo, contraseña));

        var res = api.post("/Auth/login", body);
        int code = res.statusCode();

        if (code == 200) {
            LoginResponse data = gson.fromJson(res.body(), LoginResponse.class);

            // Solo EMPLEADO o ADMIN
            if (data.rol == null || !(data.rol.equals("EMPLEADO") || data.rol.equals("ADMIN"))) {
                throw new RuntimeException("No tienes permisos para entrar (rol: " + data.rol + ")");
            }
            if (data.token == null || data.token.isBlank()) {
                throw new RuntimeException("El servidor no devolvió token.");
            }

            return data;
        }

        if (code == 401) throw new RuntimeException("Credenciales inválidas.");
        if (code == 403) throw new RuntimeException("Usuario no activo o acceso denegado.");
        throw new RuntimeException("Error del servidor (" + code + "): " + res.body());
    }
}