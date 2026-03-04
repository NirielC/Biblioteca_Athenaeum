using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Biblioteca_Athenaeum.Data;
using Biblioteca_Athenaeum.DTOs;

namespace Biblioteca_Athenaeum.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly BibliotecaContext _context;
        private readonly IConfiguration _config;

        public AuthController(BibliotecaContext context, IConfiguration config)
        {
            _context = context;
            _config = config;
        }

        [HttpPost("login")]
        public async Task<IActionResult> Login([FromBody] LoginRequestDto dto)
        {
            if (!ModelState.IsValid)
                return BadRequest(ModelState);

            var user = await _context.Usuarios
                .FirstOrDefaultAsync(u => u.correo == dto.correo);

            if (user == null)
                return Unauthorized(new { message = "Credenciales inválidas" });

            bool ok = BCrypt.Net.BCrypt.Verify(dto.contraseña, user.contraseña);
            if (!ok)
                return Unauthorized(new { message = "Credenciales inválidas" });

            if (user.estado != "ACTIVO")
                return Forbid();

            string token = GenerateJwtToken(user);

            var response = new LoginResponseDto
            {
                token = token,
                id_usuario = user.id_usuario,
                nombres = user.nombres,
                apellidos = user.apellidos,
                rol = user.rol
            };

            return Ok(response);
        }

        private string GenerateJwtToken(Models.Usuario user)
        {
            var claims = new[]
            {
                new Claim(ClaimTypes.NameIdentifier, user.id_usuario.ToString()),
                new Claim(ClaimTypes.Role, user.rol),
                new Claim(ClaimTypes.Email, user.correo)
            };

            var keyStr = _config["Jwt:Key"];
            if (string.IsNullOrWhiteSpace(keyStr))
                throw new Exception("Falta Jwt:Key en appsettings.json");

            var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(keyStr));
            var creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

            var token = new JwtSecurityToken(
                issuer: _config["Jwt:Issuer"],
                audience: _config["Jwt:Audience"],
                claims: claims,
                expires: DateTime.Now.AddHours(3),
                signingCredentials: creds
            );

            return new JwtSecurityTokenHandler().WriteToken(token);
        }
    }
}