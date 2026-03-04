package sv.arrupe.biblioteca.model;

public class LoginRequest {
    public String correo;
    public String contraseña;

    public LoginRequest(String correo, String contraseña) {
        this.correo = correo;
        this.contraseña = contraseña;
    }
}