namespace Biblioteca_Athenaeum.DTOs
{
    public class UsuarioResponseDto
    {
        public int id_usuario { get; set; }
        public string nombres { get; set; }
        public string apellidos { get; set; }
        public string correo { get; set; }
        public string rol { get; set; }
        public string estado { get; set; }
        public string? telefono { get; set; }
        public string? dui { get; set; }
        public DateTime? fecha_nacimiento { get; set; }
    }
}