using System.ComponentModel.DataAnnotations;

namespace Biblioteca_Athenaeum.Models.ViewModels
{
    public class PerfilVm
    {
        [Required] public string nombres { get; set; }
        [Required] public string apellidos { get; set; }

        public string correo { get; set; } // solo lectura
        [Phone(ErrorMessage = "Teléfono inválido")]
        public string? telefono { get; set; }
        public DateTime? fecha_nacimiento { get; set; }

        // 🔐 contraseña (usar nombres ASCII para evitar problemas de bind)
        public string? contrasena_actual { get; set; }

        [MinLength(6, ErrorMessage = "Mínimo 6 caracteres")]
        public string? contrasena_nueva { get; set; }

        [Compare("contrasena_nueva", ErrorMessage = "No coincide con la contraseña nueva")]
        public string? confirmar_contrasena { get; set; }
    }
}