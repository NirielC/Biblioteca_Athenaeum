package sv.arrupe.biblioteca.model;

public class Prestamo {
    public int id_prestamo;
    public int id_lector;
    public String lector_nombre;
    public int id_libro;
    public String libro_titulo;

    public Integer id_empleado_aprueba;
    public String estado;

    public String fecha_solicitud;
    public String fecha_aprobacion;
    public String fecha_vencimiento;
    public String fecha_devolucion;
}