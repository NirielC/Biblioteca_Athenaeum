using System.ComponentModel.DataAnnotations;

namespace Biblioteca_Athenaeum.DTOs
{
    public class LoginRequestDto
    {
        [Required]
        [EmailAddress]
        public string correo { get; set; }

        [Required]
        public string contraseña { get; set; }
    }
}