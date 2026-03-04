using Biblioteca_Athenaeum.Data;
using Biblioteca_Athenaeum.Models.ViewModels;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace Biblioteca_Athenaeum.Controllers.Web
{
    public class PerfilController : Controller
    {
        private readonly BibliotecaContext _context;

        public PerfilController(BibliotecaContext context)
        {
            _context = context;
        }

        [HttpGet]
        public async Task<IActionResult> Index()
        {
            // Obtener userId/rol desde session o claims (si se usa JWT/u otra autenticación)
            var userIdStr = HttpContext.Session.GetString("UserId") ?? User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrWhiteSpace(userIdStr))
                return RedirectToAction("Login", "WebAuth");

            int id = int.Parse(userIdStr);

            var rol = HttpContext.Session.GetString("Rol") ?? User.FindFirst(ClaimTypes.Role)?.Value;
            if (string.IsNullOrWhiteSpace(rol) || rol != "LECTOR")
                return Forbid();

            var u = await _context.Usuarios.AsNoTracking().FirstOrDefaultAsync(x => x.id_usuario == id);
            if (u == null) return NotFound();

            var vm = new PerfilVm
            {
                nombres = u.nombres,
                apellidos = u.apellidos,
                correo = u.correo,
                telefono = u.telefono,
                fecha_nacimiento = u.fecha_nacimiento
            };

            return View(vm);
        }

        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Index(PerfilVm vm)
        {
            // Obtener userId/rol desde session o claims (si se usa JWT/u otra autenticación)
            var userIdStr = HttpContext.Session.GetString("UserId") ?? User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            if (string.IsNullOrWhiteSpace(userIdStr))
                return RedirectToAction("Login", "WebAuth");

            int id = int.Parse(userIdStr);

            var rol = HttpContext.Session.GetString("Rol") ?? User.FindFirst(ClaimTypes.Role)?.Value;
            if (string.IsNullOrWhiteSpace(rol) || rol != "LECTOR")
                return Forbid();

            var u = await _context.Usuarios.FirstOrDefaultAsync(x => x.id_usuario == id);
            if (u == null) return NotFound();

            // Protección adicional: asegurar que el usuario en sesión coincide con el recurso
            if (u.id_usuario.ToString() != userIdStr)
            {
                // Esto no debería pasar porque ya buscamos por session id, pero por seguridad
                return Forbid();
            }

            // ⚠️ correo viene disabled => NO se manda => lo rellenamos para la vista si hay error
            vm.correo = u.correo;

            // validación mínima
            if (string.IsNullOrWhiteSpace(vm.nombres))
                ModelState.AddModelError("nombres", "Nombres es requerido");
            if (string.IsNullOrWhiteSpace(vm.apellidos))
                ModelState.AddModelError("apellidos", "Apellidos es requerido");

            // validar fecha de nacimiento (no puede ser en el futuro)
            if (vm.fecha_nacimiento.HasValue && vm.fecha_nacimiento.Value.Date > DateTime.UtcNow.Date)
                ModelState.AddModelError("fecha_nacimiento", "La fecha de nacimiento no puede ser en el futuro.");

            // ---- cambio de contraseña (opcional) ----
            bool quiereCambiarPass = !string.IsNullOrWhiteSpace(vm.contrasena_nueva) ||
                         !string.IsNullOrWhiteSpace(vm.confirmar_contrasena);

            if (quiereCambiarPass)
            {
                if (string.IsNullOrWhiteSpace(vm.contrasena_actual))
                    ModelState.AddModelError("contrasena_actual", "Escribe tu contraseña actual.");

                if (string.IsNullOrWhiteSpace(vm.contrasena_nueva))
                    ModelState.AddModelError("contrasena_nueva", "Escribe una nueva contraseña.");

                if (vm.contrasena_nueva != vm.confirmar_contrasena)
                    ModelState.AddModelError("confirmar_contrasena", "No coincide con la contraseña nueva.");

                if (ModelState.IsValid)
                {
                    bool ok = BCrypt.Net.BCrypt.Verify(vm.contrasena_actual, u.contraseña);
                    if (!ok)
                        ModelState.AddModelError("contrasena_actual", "La contraseña actual es incorrecta.");
                    else
                        u.contraseña = BCrypt.Net.BCrypt.HashPassword(vm.contrasena_nueva);
                }
            }

            if (!ModelState.IsValid)
                return View(vm);

            // guardar cambios
            u.nombres = vm.nombres;
            u.apellidos = vm.apellidos;
            u.telefono = vm.telefono;
            u.fecha_nacimiento = vm.fecha_nacimiento;

            try
            {
                await _context.SaveChangesAsync();
            }
            catch (DbUpdateException)
            {
                ModelState.AddModelError(string.Empty, "Ocurrió un error al guardar. Intenta más tarde.");
                return View(vm);
            }

            HttpContext.Session.SetString("Nombre", $"{u.nombres} {u.apellidos}");
            TempData["Msg"] = "Perfil actualizado";

            return RedirectToAction("Index");
        }
    }
}