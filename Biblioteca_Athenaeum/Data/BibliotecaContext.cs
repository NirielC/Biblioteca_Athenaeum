using Microsoft.EntityFrameworkCore;
using Biblioteca_Athenaeum.Models;

namespace Biblioteca_Athenaeum.Data
{
    public class BibliotecaContext : DbContext
    {
        public BibliotecaContext(DbContextOptions<BibliotecaContext> options) : base(options) { }

        public DbSet<Usuario> Usuarios { get; set; }
        public DbSet<Libro> Libros { get; set; }
        public DbSet<Prestamo> Prestamos { get; set; }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            modelBuilder.Entity<Usuario>().ToTable("Usuario");
            modelBuilder.Entity<Libro>().ToTable("Libro");
            modelBuilder.Entity<Prestamo>().ToTable("Prestamo");
        }
    }
}