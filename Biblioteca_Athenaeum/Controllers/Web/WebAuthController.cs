using Biblioteca_Athenaeum.Data;
using Biblioteca_Athenaeum.DTOs;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace Biblioteca_Athenaeum.Controllers.Web
{
    public class WebAuthController : Controller
    {
        private readonly BibliotecaContext _context;

        public WebAuthController(BibliotecaContext context)
        {
            _context = context;
        }

        [HttpGet]
        public IActionResult Login()
        {
            // si ya hay sesión, luego lo mandamos al catálogo
            if (!string.IsNullOrWhiteSpace(HttpContext.Session.GetString("UserId")))
                return RedirectToAction("Index", "Catalogo");

            return View(new LoginRequestDto());
        }

        [HttpPost]
        public async Task<IActionResult> Login(LoginRequestDto dto)
        {
            if (!ModelState.IsValid)
                return View(dto);

            var user = await _context.Usuarios.FirstOrDefaultAsync(u => u.correo == dto.correo);
            if (user == null || !BCrypt.Net.BCrypt.Verify(dto.contraseña, user.contraseña))
            {
                ViewBag.Error = "Credenciales inválidas.";
                return View(dto);
            }

            if (user.estado != "ACTIVO")
            {
                ViewBag.Error = "Usuario inactivo.";
                return View(dto);
            }

            if (user.rol != "LECTOR")
            {
                ViewBag.Error = "Este portal es solo para LECTORES.";
                return View(dto);
            }

            // ✅ Guardamos sesión (Rol/Nombres)
            HttpContext.Session.SetString("Rol", user.rol);
            HttpContext.Session.SetString("Nombre", $"{user.nombres} {user.apellidos}");
            HttpContext.Session.SetString("UserId", user.id_usuario.ToString());

            // ✅ Opcional: guardar token también (si quieres usar API desde MVC en algún punto)
            // Si quieres, aquí puedo llamarte al AuthController por código o generar token igual.
            // Por ahora no hace falta para vistas.

            return RedirectToAction("Index", "Catalogo");
        }

        public IActionResult Logout()
        {
            HttpContext.Session.Clear();
            return RedirectToAction("Login");
        }
    }
}