namespace Biblioteca_Athenaeum.DTOs
{
    public class PrestamoResponseDto
    {
        public int id_prestamo { get; set; }
        public int id_lector { get; set; }
        public string lector_nombre { get; set; }
        public int id_libro { get; set; }
        public string libro_titulo { get; set; }

        public int? id_empleado_aprueba { get; set; }
        public DateTime fecha_solicitud { get; set; }
        public DateTime? fecha_aprobacion { get; set; }
        public DateTime? fecha_vencimiento { get; set; }
        public DateTime? fecha_devolucion { get; set; }
        public string estado { get; set; }
    }
}
