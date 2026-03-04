using Biblioteca_Athenaeum.Data;
using Biblioteca_Athenaeum.DTOs;
using Biblioteca_Athenaeum.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.Hosting;

namespace Biblioteca_Athenaeum.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class LibrosController : ControllerBase
    {
        private readonly BibliotecaContext _context;
        private readonly IWebHostEnvironment _env;

        public LibrosController(BibliotecaContext context, IWebHostEnvironment env)
        {
            _context = context;
            _env = env;
        }

        // GET: api/libros?q=algo
        [Authorize(Roles = "LECTOR,EMPLEADO,ADMIN")]
        [HttpGet]
        public async Task<IActionResult> GetAll([FromQuery] string? q)
        {
            var query = _context.Libros.AsQueryable();

            if (!string.IsNullOrWhiteSpace(q))
                query = query.Where(l => l.titulo.Contains(q) || l.autor.Contains(q));

            var libros = await query.ToListAsync();

            var isLector = User.IsInRole("LECTOR");
            if (isLector)
                query = query.Where(l => l.estado == "ACTIVO" && l.stock > 0);

            var resp = libros.Select(l => new LibroResponseDto
            {
                id_libro = l.id_libro,
                isbn = l.isbn,
                titulo = l.titulo,
                autor = l.autor,
                editorial = l.editorial,
                genero = l.genero,
                año_publicación = l.año_publicación,
                sinopsis = l.sinopsis,
                imagen_portada = l.imagen_portada,
                stock = l.stock,
                estado = l.estado
            });

            return Ok(resp);
        }

        // GET: api/libros/5
        [Authorize(Roles = "LECTOR,EMPLEADO,ADMIN")]
        [HttpGet("{id}")]
        public async Task<IActionResult> GetById(int id)
        {
            var l = await _context.Libros.FindAsync(id);
            if (l == null) return NotFound(new { message = "Libro no encontrado" });

            var resp = new LibroResponseDto
            {
                id_libro = l.id_libro,
                isbn = l.isbn,
                titulo = l.titulo,
                autor = l.autor,
                editorial = l.editorial,
                genero = l.genero,
                año_publicación = l.año_publicación,
                sinopsis = l.sinopsis,
                imagen_portada = l.imagen_portada,
                stock = l.stock,
                estado = l.estado
            };

            return Ok(resp);
        }

        // POST: api/libros
        [Authorize(Roles = "EMPLEADO,ADMIN")]
        [HttpPost]
        public async Task<IActionResult> Create([FromBody] LibroCreateDto dto)
        {
            if (string.IsNullOrWhiteSpace(dto.titulo))
                return BadRequest(new { message = "El título es obligatorio" });

            if (string.IsNullOrWhiteSpace(dto.autor))
                return BadRequest(new { message = "El autor es obligatorio" });

            if (dto.stock < 0)
                return BadRequest(new { message = "El stock no puede ser negativo" });

            var libro = new Libro
            {
                isbn = dto.isbn,
                titulo = dto.titulo,
                autor = dto.autor,
                editorial = dto.editorial,
                genero = dto.genero,
                año_publicación = dto.año_publicación,
                sinopsis = dto.sinopsis,
                stock = dto.stock,
                estado = "ACTIVO",
                imagen_portada = null
            };

            _context.Libros.Add(libro);
            await _context.SaveChangesAsync();

            var resp = new LibroResponseDto
            {
                id_libro = libro.id_libro,
                titulo = libro.titulo,
                autor = libro.autor,
                editorial = libro.editorial,
                stock = libro.stock,
                estado = libro.estado,
                imagen_portada = libro.imagen_portada
            };

            return CreatedAtAction(nameof(GetById), new { id = libro.id_libro }, resp);
        }

        // PUT: api/libros/5
        [Authorize(Roles = "EMPLEADO,ADMIN")]
        [HttpPut("{id}")]
        public async Task<IActionResult> Update(int id, [FromBody] LibroUpdateDto dto)
        {
            if (id != dto.id_libro)
                return BadRequest(new { message = "El id de la URL no coincide con el del body" });

            var libro = await _context.Libros.FindAsync(id);
            if (libro == null) return NotFound(new { message = "Libro no encontrado" });

            if (string.IsNullOrWhiteSpace(dto.titulo))
                return BadRequest(new { message = "El título es obligatorio" });

            if (string.IsNullOrWhiteSpace(dto.autor))
                return BadRequest(new { message = "El autor es obligatorio" });

            if (dto.stock < 0)
                return BadRequest(new { message = "El stock no puede ser negativo" });

            libro.isbn = dto.isbn;
            libro.titulo = dto.titulo;
            libro.autor = dto.autor;
            libro.editorial = dto.editorial;
            libro.genero = dto.genero;
            libro.año_publicación = dto.año_publicación;
            libro.sinopsis = dto.sinopsis;
            libro.stock = dto.stock;
            if (!string.IsNullOrWhiteSpace(dto.estado)) libro.estado = dto.estado;

            // imagen_portada NO se actualiza aquí, se actualiza con el endpoint /portada
            await _context.SaveChangesAsync();

            var resp = new LibroResponseDto
            {
                id_libro = libro.id_libro,
                isbn = libro.isbn,
                titulo = libro.titulo,
                autor = libro.autor,
                editorial = libro.editorial,
                genero = libro.genero,
                año_publicación = libro.año_publicación,
                sinopsis = libro.sinopsis,
                imagen_portada = libro.imagen_portada,
                stock = libro.stock,
                estado = libro.estado
            };

            return Ok(resp);
        }

        // DELETE: api/libros/5
        [Authorize(Roles = "EMPLEADO,ADMIN")]
        [HttpDelete("{id}")]
        public async Task<IActionResult> Delete(int id)
        {
            var libro = await _context.Libros.FindAsync(id);
            if (libro == null) return NotFound(new { message = "Libro no encontrado" });

            _context.Libros.Remove(libro);
            await _context.SaveChangesAsync();

            return Ok(new { message = "Libro eliminado" });
        }

        // PATCH: api/libros/5/estado
        [Authorize(Roles = "EMPLEADO,ADMIN")]
        [HttpPatch("{id}/estado")]
        public async Task<IActionResult> CambiarEstado(int id, [FromBody] string estado)
        {
            var libro = await _context.Libros.FindAsync(id);
            if (libro == null) return NotFound(new { message = "Libro no encontrado" });

            if (string.IsNullOrWhiteSpace(estado))
                return BadRequest(new { message = "Estado inválido" });

            libro.estado = estado.Trim().ToUpper();
            await _context.SaveChangesAsync();

            return Ok(new { message = "Estado actualizado", libro.id_libro, libro.estado });
        }

        // ✅ POST: api/libros/{id}/portada  (multipart/form-data)
        [Authorize(Roles = "EMPLEADO,ADMIN")]
        [HttpPost("{id}/portada")]
        public async Task<IActionResult> SubirPortada(int id, IFormFile file)
        {
            var libro = await _context.Libros.FindAsync(id);
            if (libro == null)
                return NotFound(new { message = "Libro no encontrado" });

            if (file == null || file.Length == 0)
                return BadRequest(new { message = "Archivo inválido" });

            var ext = Path.GetExtension(file.FileName).ToLower();
            var permitidas = new[] { ".jpg", ".jpeg", ".png", ".webp" };
            if (!permitidas.Contains(ext))
                return BadRequest(new { message = "Solo se permiten imágenes (.jpg, .png, .webp)" });

            var imgDir = Path.Combine(_env.WebRootPath, "img");
            Directory.CreateDirectory(imgDir);

            // 🔥 Eliminar imagen anterior si existe
            if (!string.IsNullOrWhiteSpace(libro.imagen_portada))
            {
                var rutaVieja = Path.Combine(
                    _env.WebRootPath,
                    libro.imagen_portada.TrimStart('/').Replace("/", Path.DirectorySeparatorChar.ToString())
                );

                if (System.IO.File.Exists(rutaVieja))
                    System.IO.File.Delete(rutaVieja);
            }

            // ✅ Nombre limpio
            var nombre = $"libro_{id}{ext}";
            var rutaFisica = Path.Combine(imgDir, nombre);

            using (var stream = new FileStream(rutaFisica, FileMode.Create))
            {
                await file.CopyToAsync(stream);
            }

            libro.imagen_portada = "/img/" + nombre;
            await _context.SaveChangesAsync();

            return Ok(new { message = "Portada subida", imagen_portada = libro.imagen_portada });
        }
    }
}