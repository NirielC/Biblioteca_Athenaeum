using Biblioteca_Athenaeum.Data;
using Biblioteca_Athenaeum.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace Biblioteca_Athenaeum.Controllers.Web
{
    public class CatalogoController : Controller
    {
        private readonly BibliotecaContext _context;

        public CatalogoController(BibliotecaContext context)
        {
            _context = context;
        }

        // /Catalogo
        public async Task<IActionResult> Index(string? q, string? autor, string? genero)
        {
            var query = _context.Libros.AsQueryable();

            // ✅ LECTOR: solo disponibles y activos
            query = query.Where(l => l.estado == "ACTIVO" && l.stock > 0);

            if (!string.IsNullOrWhiteSpace(q))
                query = query.Where(l => l.titulo.Contains(q) || l.autor.Contains(q));

            if (!string.IsNullOrWhiteSpace(autor) && autor != "Todos")
                query = query.Where(l => l.autor == autor);

            if (!string.IsNullOrWhiteSpace(genero) && genero != "Todos")
                query = query.Where(l => l.genero == genero);

            var libros = await query.OrderBy(l => l.titulo).ToListAsync();

            // Para llenar dropdowns (solo de lo que es visible)
            var autores = await _context.Libros
                .Where(l => l.estado == "ACTIVO" && l.stock > 0)
                .Select(l => l.autor)
                .Distinct()
                .OrderBy(x => x)
                .ToListAsync();

            var generos = await _context.Libros
                .Where(l => l.estado == "ACTIVO" && l.stock > 0)
                .Select(l => l.genero)
                .Distinct()
                .OrderBy(x => x)
                .ToListAsync();

            ViewBag.Q = q;
            ViewBag.Autor = autor ?? "Todos";
            ViewBag.Genero = genero ?? "Todos";
            ViewBag.Autores = autores;
            ViewBag.Generos = generos;

            return View(libros);
        }

        // /Catalogo/Detalle/5
        public async Task<IActionResult> Detalle(int id)
        {
            var libro = await _context.Libros.FirstOrDefaultAsync(l =>
                l.id_libro == id && l.estado == "ACTIVO" && l.stock > 0);

            if (libro == null)
                return NotFound();

            return View(libro);
        }

        [HttpPost]
        public async Task<IActionResult> Solicitar(int id_libro)
        {
            var userIdStr = HttpContext.Session.GetString("UserId");
            if (string.IsNullOrWhiteSpace(userIdStr))
                return RedirectToAction("Login", "WebAuth");

            int id_lector = int.Parse(userIdStr);

            var libro = await _context.Libros.FirstOrDefaultAsync(l =>
                l.id_libro == id_libro && l.estado == "ACTIVO" && l.stock > 0);

            if (libro == null)
                return NotFound();

            // Evitar duplicados: mismo lector + mismo libro en SOLICITADO o PRESTADO
            bool yaTiene = await _context.Prestamos.AnyAsync(p =>
                p.id_lector == id_lector &&
                p.id_libro == id_libro &&
                (p.estado == "SOLICITADO" || p.estado == "PRESTADO"));

            if (yaTiene)
            {
                TempData["Msg"] = "Ya tienes una solicitud o préstamo activo de este libro.";
                return RedirectToAction("Detalle", new { id = id_libro });
            }

            var p = new Prestamo
            {
                id_lector = id_lector,
                id_libro = id_libro,
                estado = "SOLICITADO",
                fecha_solicitud = DateTime.Now
            };

            _context.Prestamos.Add(p);
            await _context.SaveChangesAsync();

            TempData["Msg"] = "Solicitud enviada. Espera aprobación del empleado.";
            return RedirectToAction("Mis", "PrestamosWeb");
        }
    }
}