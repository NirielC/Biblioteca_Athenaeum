using System.ComponentModel.DataAnnotations;

namespace Biblioteca_Athenaeum.DTOs
{
    public class UsuarioCreateDto
    {
        [Required] public string nombres { get; set; }
        [Required] public string apellidos { get; set; }
        [Required, EmailAddress] public string correo { get; set; }
        [Required] public string contraseña { get; set; }   // texto plano SOLO aquí

        public string rol { get; set; } = "LECTOR";
        public string estado { get; set; } = "ACTIVO";

        public string? telefono { get; set; }
        public string? dui { get; set; }
        public DateTime? fecha_nacimiento { get; set; }
    }
}