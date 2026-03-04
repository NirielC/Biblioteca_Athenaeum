namespace Biblioteca_Athenaeum.DTOs
{
    public class LoginResponseDto
    {
        public string token { get; set; }

        public int id_usuario { get; set; }
        public string nombres { get; set; }
        public string apellidos { get; set; }
        public string rol { get; set; }
    }
}