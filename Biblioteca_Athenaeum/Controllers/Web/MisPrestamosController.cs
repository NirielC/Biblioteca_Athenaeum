using Biblioteca_Athenaeum.Data;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace Biblioteca_Athenaeum.Controllers.Web
{
    public class PrestamosWebController : Controller
    {
        private readonly BibliotecaContext _context;

        public PrestamosWebController(BibliotecaContext context)
        {
            _context = context;
        }

        private bool TryGetUserId(out int idLector)
        {
            idLector = 0;
            var userIdStr = HttpContext.Session.GetString("UserId");
            return !string.IsNullOrWhiteSpace(userIdStr) && int.TryParse(userIdStr, out idLector);
        }

        // Vista completa
        [HttpGet]
        public async Task<IActionResult> Mis()
        {
            if (!TryGetUserId(out int id_lector))
                return RedirectToAction("Login", "WebAuth");

            var list = await GetMisPrestamosList(id_lector);
            return View(list);
        }

        // ✅ SOLO el HTML del grid (para “auto refrescar” sin recargar toda la página)
        [HttpGet]
        public async Task<IActionResult> MisPartial()
        {
            if (!TryGetUserId(out int id_lector))
                return Unauthorized(); // si se perdió la sesión

            var list = await GetMisPrestamosList(id_lector);
            return PartialView("_MisGrid", list);
        }

        private async Task<List<dynamic>> GetMisPrestamosList(int id_lector)
        {
            var list = await _context.Prestamos
                .Where(p => p.id_lector == id_lector)
                .Join(_context.Libros,
                      p => p.id_libro,
                      l => l.id_libro,
                      (p, l) => new
                      {
                          id_prestamo = p.id_prestamo,
                          titulo = l.titulo,
                          autor = l.autor,
                          imagen_portada = l.imagen_portada,
                          estado = p.estado,
                          fecha_solicitud = p.fecha_solicitud,
                          fecha_aprobacion = p.fecha_aprobacion,
                          fecha_vencimiento = p.fecha_vencimiento,
                          fecha_devolucion = p.fecha_devolucion
                      })
                .OrderByDescending(x => x.fecha_solicitud)
                .ToListAsync();

            return list.Cast<dynamic>().ToList();
        }

        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Cancelar(int id_prestamo)
        {
            if (!TryGetUserId(out int id_lector))
                return RedirectToAction("Login", "WebAuth");

            if (id_prestamo <= 0)
                return RedirectToAction(nameof(Mis));

            var p = await _context.Prestamos.FirstOrDefaultAsync(x => x.id_prestamo == id_prestamo);
            if (p == null)
            {
                TempData["Msg"] = "Préstamo no encontrado.";
                return RedirectToAction(nameof(Mis));
            }

            if (p.id_lector != id_lector)
                return Forbid();

            if (p.estado != "SOLICITADO")
            {
                TempData["Msg"] = "Solo puedes cancelar si está en SOLICITADO.";
                return RedirectToAction(nameof(Mis));
            }

            p.estado = "CANCELADO";
            await _context.SaveChangesAsync();

            TempData["Msg"] = "Solicitud cancelada ✅";
            return RedirectToAction(nameof(Mis));
        }
    }
}