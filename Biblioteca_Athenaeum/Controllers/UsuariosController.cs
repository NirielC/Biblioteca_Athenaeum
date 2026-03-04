using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Biblioteca_Athenaeum.DTOs;
using Biblioteca_Athenaeum.Data;
using Biblioteca_Athenaeum.Models;
using Microsoft.AspNetCore.Authorization;

namespace Biblioteca_Athenaeum.Controllers
{
    [Authorize(Roles = "ADMIN")]
    [ApiController]
    [Route("api/[controller]")]
    public class UsuariosController : ControllerBase
    {
        private readonly BibliotecaContext _context;

        public UsuariosController(BibliotecaContext context)
        {
            _context = context;
        }

        // GET: api/usuarios
        [HttpGet]
        public async Task<IActionResult> GetAll()
        {
            var lista = await _context.Usuarios.Select(x => new UsuarioResponseDto
            {
                id_usuario = x.id_usuario,
                nombres = x.nombres,
                apellidos = x.apellidos,
                correo = x.correo,
                rol = x.rol,
                estado = x.estado,
                telefono = x.telefono,
                dui = x.dui,
                fecha_nacimiento = x.fecha_nacimiento
            }).ToListAsync();

            return Ok(lista);
        }

        // GET: api/usuarios/5
        [HttpGet("{id}")]
        public async Task<IActionResult> GetById(int id)
        {
            var u = await _context.Usuarios.FindAsync(id);
            if (u == null)
                return NotFound(new { message = "Usuario no encontrado" });

            var response = new UsuarioResponseDto
            {
                id_usuario = u.id_usuario,
                nombres = u.nombres,
                apellidos = u.apellidos,
                correo = u.correo,
                rol = u.rol,
                estado = u.estado,
                telefono = u.telefono,
                dui = u.dui,
                fecha_nacimiento = u.fecha_nacimiento
            };

            return Ok(response);
        }

        // POST: api/usuarios
        [HttpPost]
        public async Task<IActionResult> Create([FromBody] UsuarioCreateDto dto)
        {
            var correoExiste = await _context.Usuarios.AnyAsync(x => x.correo == dto.correo);
            if (correoExiste) return Conflict(new { message = "Ese correo ya existe" });

            var u = new Usuario
            {
                nombres = dto.nombres,
                apellidos = dto.apellidos,
                correo = dto.correo,
                contraseña = BCrypt.Net.BCrypt.HashPassword(dto.contraseña), // ✅ aquí
                rol = string.IsNullOrWhiteSpace(dto.rol) ? "LECTOR" : dto.rol,
                estado = string.IsNullOrWhiteSpace(dto.estado) ? "ACTIVO" : dto.estado,
                telefono = dto.telefono,
                dui = dto.dui,
                fecha_nacimiento = dto.fecha_nacimiento
            };

            _context.Usuarios.Add(u);
            await _context.SaveChangesAsync();

            return CreatedAtAction(nameof(GetById), new { id = u.id_usuario }, new { u.id_usuario });
        }

        // PUT: api/usuarios/5
        [HttpPut("{id}")]
        public async Task<IActionResult> Update(int id, [FromBody] UsuarioUpdateDto dto)
        {
            var usuarioDb = await _context.Usuarios.FindAsync(id);
            if (usuarioDb == null) return NotFound("Usuario no encontrado");

            usuarioDb.nombres = dto.nombres;
            usuarioDb.apellidos = dto.apellidos;
            usuarioDb.correo = dto.correo;
            usuarioDb.telefono = dto.telefono;
            usuarioDb.dui = dto.dui;
            usuarioDb.fecha_nacimiento = dto.fecha_nacimiento;
            usuarioDb.rol = dto.rol;
            usuarioDb.estado = dto.estado;

            if (!string.IsNullOrWhiteSpace(dto.contraseña))
                usuarioDb.contraseña = BCrypt.Net.BCrypt.HashPassword(dto.contraseña);

            await _context.SaveChangesAsync();

            return Ok();
        }

        // DELETE: api/usuarios/5
        [HttpDelete("{id}")]
        public async Task<IActionResult> Delete(int id)
        {
            var u = await _context.Usuarios.FindAsync(id);
            if (u == null) return NotFound("Usuario no encontrado");

            _context.Usuarios.Remove(u);
            await _context.SaveChangesAsync();

            return Ok(new { message = "Usuario eliminado" });
        }

        // PATCH: api/usuarios/5/estado
        [HttpPatch("{id}/estado")]
        public async Task<IActionResult> CambiarEstado(int id, [FromBody] string estado)
        {
            var u = await _context.Usuarios.FindAsync(id);
            if (u == null) return NotFound("Usuario no encontrado");

            if (string.IsNullOrWhiteSpace(estado)) return BadRequest("Estado inválido");

            u.estado = estado;
            await _context.SaveChangesAsync();

            return Ok(new { message = "Estado actualizado", u.id_usuario, u.estado });
        }

        // PATCH: api/usuarios/5/rol
        [HttpPatch("{id}/rol")]
        public async Task<IActionResult> CambiarRol(int id, [FromBody] string rol)
        {
            var u = await _context.Usuarios.FindAsync(id);
            if (u == null) return NotFound("Usuario no encontrado");

            if (string.IsNullOrWhiteSpace(rol)) return BadRequest("Rol inválido");

            u.rol = rol;
            await _context.SaveChangesAsync();

            return Ok(new { message = "Rol actualizado", u.id_usuario, u.rol });
        }
    }
}