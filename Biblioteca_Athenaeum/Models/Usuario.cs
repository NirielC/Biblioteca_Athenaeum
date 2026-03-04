using System.ComponentModel.DataAnnotations;

namespace Biblioteca_Athenaeum.Models
{
    public class Usuario
    {
        [Key]
        public int id_usuario { get; set; }
        public string nombres { get; set; }
        public string apellidos { get; set; }
        public string correo { get; set; }
        public string? contraseña { get; set; }
        public string rol { get; set; }
        public string estado { get; set; }
        public string? telefono { get; set; }
        public string? dui { get; set; }
        public DateTime? fecha_nacimiento { get; set; }
    }
}