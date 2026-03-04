using System.ComponentModel.DataAnnotations;

namespace Biblioteca_Athenaeum.DTOs
{
    public class UsuarioUpdateDto
    {
        [Required] public string nombres { get; set; }
        [Required] public string apellidos { get; set; }
        [Required, EmailAddress] public string correo { get; set; }

        public string? contraseña { get; set; } // opcional (si viene, se re-hashea)

        [Required] public string rol { get; set; }
        [Required] public string estado { get; set; }

        public string? telefono { get; set; }
        public string? dui { get; set; }
        public DateTime? fecha_nacimiento { get; set; }
    }
}