using Biblioteca_Athenaeum.Data;
using Biblioteca_Athenaeum.DTOs;
using Biblioteca_Athenaeum.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace Biblioteca_Athenaeum.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class PrestamosController : ControllerBase
    {
        private readonly BibliotecaContext _context;

        public PrestamosController(BibliotecaContext context)
        {
            _context = context;
        }

        [Authorize(Roles = "EMPLEADO,ADMIN")]
        // GET: api/prestamos
        [HttpGet]
        public async Task<IActionResult> GetAll()
        {
            // Include para ver relación (libro + lector)
            var list = await _context.Prestamos.ToListAsync();
            return Ok(list);
        }

        [Authorize(Roles = "LECTOR")]
        // GET: api/prestamos/mis/3
        [HttpGet("mis/{id_lector}")]
        public async Task<IActionResult> MisPrestamos(int id_lector)
        {
            // Validar id
            if (id_lector <= 0)
                return BadRequest("id_lector inválido");

            // Verificar que el lector exista
            var lector = await _context.Usuarios.FindAsync(id_lector);
            if (lector == null) return NotFound("Lector no existe");

            // Traer solo sus préstamos (historial)
            var list = await _context.Prestamos
                .Where(p => p.id_lector == id_lector)
                .OrderByDescending(p => p.fecha_solicitud)
                .ToListAsync();

            return Ok(list);
        }


        [Authorize(Roles = "LECTOR,EMPLEADO,ADMIN")]
        // GET: api/prestamos/5
        [HttpGet("{id}")]
        public async Task<IActionResult> GetById(int id)
        {
            var p = await _context.Prestamos.FindAsync(id);
            if (p == null) return NotFound("Préstamo no encontrado");
            return Ok(p);
        }

        [Authorize(Roles = "LECTOR")]
        [HttpPost("solicitar")]
        public async Task<IActionResult> Solicitar([FromBody] SolicitarPrestamoDto dto)
        {
            if (dto.id_lector <= 0 || dto.id_libro <= 0)
                return BadRequest("id_lector e id_libro son obligatorios");

            var lector = await _context.Usuarios.FindAsync(dto.id_lector);
            if (lector == null) return NotFound("Lector no existe");
            if (lector.estado != "ACTIVO") return BadRequest("Lector inactivo");

            // No permitir solicitar si el lector tiene préstamos atrasados
            var hoy = DateTime.Now;
            var tieneAtrasados = await _context.Prestamos
                .AnyAsync(p => p.id_lector == dto.id_lector && p.estado == "PRESTADO" && p.fecha_vencimiento != null && p.fecha_vencimiento < hoy);

            if (tieneAtrasados)
                return Conflict("No puedes solicitar nuevos préstamos porque tienes préstamos atrasados");

            var libro = await _context.Libros.FindAsync(dto.id_libro);
            if (libro == null) return NotFound("Libro no existe");
            if (libro.estado != "ACTIVO") return BadRequest("Libro inactivo");
            if (libro.stock <= 0) return Conflict("Sin stock");

            var p = new Prestamo
            {
                id_lector = dto.id_lector,
                id_libro = dto.id_libro,
                estado = "SOLICITADO",
                fecha_solicitud = DateTime.Now
            };

            _context.Prestamos.Add(p);
            await _context.SaveChangesAsync();

            return StatusCode(201, p);
        }

        [Authorize(Roles = "EMPLEADO,ADMIN")]
        [HttpGet("pendientes")]
        public async Task<IActionResult> Pendientes()
        {
            var list = await _context.Prestamos
                .Where(x => x.estado == "SOLICITADO")
                .Join(_context.Usuarios, p => p.id_lector, u => u.id_usuario, (p, u) => new { p, u })
                .Join(_context.Libros, pu => pu.p.id_libro, l => l.id_libro, (pu, l) => new PrestamoResponseDto
                {
                    id_prestamo = pu.p.id_prestamo,
                    id_lector = pu.p.id_lector,
                    lector_nombre = pu.u.nombres + " " + pu.u.apellidos,
                    id_libro = pu.p.id_libro,
                    libro_titulo = l.titulo,
                    id_empleado_aprueba = pu.p.id_empleado_aprueba,
                    fecha_solicitud = pu.p.fecha_solicitud,
                    fecha_aprobacion = pu.p.fecha_aprobacion,
                    fecha_vencimiento = pu.p.fecha_vencimiento,
                    fecha_devolucion = pu.p.fecha_devolucion,
                    estado = pu.p.estado
                })
                .ToListAsync();

            return Ok(list);
        }

        [Authorize(Roles = "EMPLEADO,ADMIN")]
        // PATCH: api/prestamos/5/aprobar
        [HttpPatch("{id}/aprobar")]
        public async Task<IActionResult> Aprobar(int id, [FromBody] int id_empleado)
        {
            var p = await _context.Prestamos.FindAsync(id);
            if (p == null) return NotFound("Préstamo no existe");

            if (p.estado != "SOLICITADO")
                return BadRequest("Solo se puede aprobar si está SOLICITADO");

            var empleado = await _context.Usuarios.FindAsync(id_empleado);
            if (empleado == null) return NotFound("Empleado no existe");
            if (empleado.rol != "EMPLEADO" && empleado.rol != "ADMIN")
                return BadRequest("El usuario no tiene rol de empleado");

            var libro = await _context.Libros.FindAsync(p.id_libro);
            if (libro == null) return NotFound("Libro no existe");
            if (libro.stock <= 0) return Conflict("Sin stock");

            // Verificar si el lector tiene préstamos atrasados: si es así, no aprobar
            var hoy = DateTime.Now;
            var tieneAtrasados = await _context.Prestamos
                .AnyAsync(x => x.id_lector == p.id_lector && x.estado == "PRESTADO" && x.fecha_vencimiento != null && x.fecha_vencimiento < hoy);

            if (tieneAtrasados)
                return BadRequest("No se puede aprobar: el lector tiene préstamos atrasados");

            // Aprobar
            p.estado = "PRESTADO";
            p.id_empleado_aprueba = id_empleado;
            p.fecha_aprobacion = DateTime.Now;
            p.fecha_vencimiento = DateTime.Now.AddDays(7);

            // stock -1
            libro.stock -= 1;

            await _context.SaveChangesAsync();
            return Ok(p);
        }

        [Authorize(Roles = "EMPLEADO,ADMIN")]
        // PATCH: api/prestamos/5/rechazar
        [HttpPatch("{id}/rechazar")]
        public async Task<IActionResult> Rechazar(int id)
        {
            var p = await _context.Prestamos.FindAsync(id);
            if (p == null) return NotFound("Préstamo no existe");

            if (p.estado != "SOLICITADO")
                return BadRequest("Solo se puede rechazar si está SOLICITADO");

            p.estado = "RECHAZADO";
            await _context.SaveChangesAsync();
            return Ok(p);
        }

        [Authorize(Roles = "LECTOR")]
        // PATCH: api/prestamos/5/cancelar
        [HttpPatch("{id}/cancelar")]
        public async Task<IActionResult> Cancelar(int id)
        {
            var p = await _context.Prestamos.FindAsync(id);
            if (p == null) return NotFound("Préstamo no existe");

            if (p.estado != "SOLICITADO")
                return BadRequest("Solo se puede cancelar si está SOLICITADO");

            p.estado = "CANCELADO";
            await _context.SaveChangesAsync();
            return Ok(p);
        }

        [Authorize(Roles = "EMPLEADO,ADMIN")]
        // PATCH: api/prestamos/5/devolver
        [HttpPatch("{id}/devolver")]
        public async Task<IActionResult> Devolver(int id)
        {
            var p = await _context.Prestamos.FindAsync(id);
            if (p == null) return NotFound("Préstamo no existe");

            if (p.estado != "PRESTADO")
                return BadRequest("Solo se puede devolver si está PRESTADO");

            var libro = await _context.Libros.FindAsync(p.id_libro);
            if (libro == null) return NotFound("Libro no existe");

            p.estado = "DEVUELTO";
            p.fecha_devolucion = DateTime.Now;

            // stock +1
            libro.stock += 1;

            await _context.SaveChangesAsync();
            return Ok(p);
        }

        [Authorize(Roles = "EMPLEADO,ADMIN")]
        // DELETE: api/prestamos/5
        [HttpDelete("{id}")]
        public async Task<IActionResult> Delete(int id)
        {
            var p = await _context.Prestamos.FindAsync(id);
            if (p == null) return NotFound("Préstamo no existe");

            _context.Prestamos.Remove(p);
            await _context.SaveChangesAsync();

            return Ok(new { message = "Préstamo eliminado" });
        }

        [Authorize(Roles = "EMPLEADO,ADMIN")]
        [HttpGet("activos")]
        public async Task<IActionResult> Activos()
        {
            var list = await _context.Prestamos
                .Where(x => x.estado == "PRESTADO")
                .Join(_context.Usuarios, p => p.id_lector, u => u.id_usuario, (p, u) => new { p, u })
                .Join(_context.Libros, pu => pu.p.id_libro, l => l.id_libro, (pu, l) => new PrestamoResponseDto
                {
                    id_prestamo = pu.p.id_prestamo,
                    id_lector = pu.p.id_lector,
                    lector_nombre = pu.u.nombres + " " + pu.u.apellidos,
                    id_libro = pu.p.id_libro,
                    libro_titulo = l.titulo,
                    id_empleado_aprueba = pu.p.id_empleado_aprueba,
                    fecha_solicitud = pu.p.fecha_solicitud,
                    fecha_aprobacion = pu.p.fecha_aprobacion,
                    fecha_vencimiento = pu.p.fecha_vencimiento,
                    fecha_devolucion = pu.p.fecha_devolucion,
                    estado = pu.p.estado
                })
                .OrderBy(x => x.fecha_vencimiento)
                .ToListAsync();

            return Ok(list);
        }

        [Authorize(Roles = "EMPLEADO,ADMIN")]
        [HttpGet("atrasados")]
        public async Task<IActionResult> Atrasados()
        {
            var hoy = DateTime.Now;

            var list = await _context.Prestamos
                .Where(x => x.estado == "PRESTADO" &&
                            x.fecha_vencimiento != null &&
                            x.fecha_vencimiento < hoy)
                .Join(_context.Usuarios,
                    p => p.id_lector,
                    u => u.id_usuario,
                    (p, u) => new { p, u })
                .Join(_context.Libros,
                    pu => pu.p.id_libro,
                    l => l.id_libro,
                    (pu, l) => new PrestamoResponseDto
                    {
                        id_prestamo = pu.p.id_prestamo,
                        id_lector = pu.p.id_lector,
                        lector_nombre = pu.u.nombres + " " + pu.u.apellidos,
                        id_libro = pu.p.id_libro,
                        libro_titulo = l.titulo,
                        id_empleado_aprueba = pu.p.id_empleado_aprueba,
                        fecha_solicitud = pu.p.fecha_solicitud,
                        fecha_aprobacion = pu.p.fecha_aprobacion,
                        fecha_vencimiento = pu.p.fecha_vencimiento,
                        fecha_devolucion = pu.p.fecha_devolucion,
                        estado = pu.p.estado
                    })
                .OrderBy(x => x.fecha_vencimiento)
                .ToListAsync();

            return Ok(list);
        }


        [Authorize(Roles = "EMPLEADO,ADMIN")]
        [HttpGet("historial")]
        public async Task<IActionResult> Historial([FromQuery] string? estado)
        {
            var q = _context.Prestamos.AsQueryable();

            if (!string.IsNullOrWhiteSpace(estado) && estado.ToUpper() != "TODOS")
            {
                var est = estado.Trim().ToUpper();
                q = q.Where(x => x.estado == est);
            }

            var list = await q
                .Join(_context.Usuarios, p => p.id_lector, u => u.id_usuario, (p, u) => new { p, u })
                .Join(_context.Libros, pu => pu.p.id_libro, l => l.id_libro, (pu, l) => new PrestamoResponseDto
                {
                    id_prestamo = pu.p.id_prestamo,
                    id_lector = pu.p.id_lector,
                    lector_nombre = pu.u.nombres + " " + pu.u.apellidos,
                    id_libro = pu.p.id_libro,
                    libro_titulo = l.titulo,
                    id_empleado_aprueba = pu.p.id_empleado_aprueba,
                    fecha_solicitud = pu.p.fecha_solicitud,
                    fecha_aprobacion = pu.p.fecha_aprobacion,
                    fecha_vencimiento = pu.p.fecha_vencimiento,
                    fecha_devolucion = pu.p.fecha_devolucion,
                    estado = pu.p.estado
                })
                .OrderByDescending(x => x.fecha_solicitud)
                .ToListAsync();

            return Ok(list);
        }
    }
}