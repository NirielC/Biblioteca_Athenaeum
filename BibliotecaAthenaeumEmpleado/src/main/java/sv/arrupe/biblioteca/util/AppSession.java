package sv.arrupe.biblioteca.util;

public class AppSession {
    private static String token;
    private static String rol;
    private static int idUsuario;
    private static String nombres;
    private static String apellidos;

    public static void set(String token, String rol, int idUsuario, String nombres, String apellidos) {
        AppSession.token = token;
        AppSession.rol = rol;
        AppSession.idUsuario = idUsuario;
        AppSession.nombres = nombres;
        AppSession.apellidos = apellidos;
    }

    public static String getToken() { return token; }
    public static String getRol() { return rol; }
    public static int getIdUsuario() { return idUsuario; }
    public static String getNombres() { return nombres; }
    public static String getApellidos() { return apellidos; }

    public static boolean isLoggedIn() {
        return token != null && !token.isBlank();
    }

    public static void clear() {
        token = null;
        rol = null;
        idUsuario = 0;
        nombres = null;
        apellidos = null;
    }
}