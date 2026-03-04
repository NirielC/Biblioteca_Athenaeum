namespace Biblioteca_Athenaeum.DTOs
{
    public class LibroCreateDto
    {
        public string? isbn { get; set; }
        public string titulo { get; set; }
        public string autor { get; set; }
        public string? editorial { get; set; }
        public string? genero { get; set; }
        public int? año_publicación { get; set; }
        public string? sinopsis { get; set; }
        public int stock { get; set; }
    }
}