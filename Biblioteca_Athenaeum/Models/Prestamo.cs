using System.ComponentModel.DataAnnotations;

namespace Biblioteca_Athenaeum.Models
{
    public class Prestamo
    {
        [Key]
        public int id_prestamo { get; set; }
        public int id_lector { get; set; }
        public int? id_empleado_aprueba { get; set; }
        public int id_libro { get; set; }
        public DateTime fecha_solicitud { get; set; }
        public DateTime? fecha_aprobacion { get; set; }
        public DateTime? fecha_vencimiento { get; set; }
        public DateTime? fecha_devolucion { get; set; }
        public string estado { get; set; }
    }
}