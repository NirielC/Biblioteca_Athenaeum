using System.Text;
using Biblioteca_Athenaeum.Data;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllersWithViews();

// ✅ Session (para Rol, Nombre, UserId en Layout)
builder.Services.AddSession(opt =>
{
    opt.IdleTimeout = TimeSpan.FromHours(3);
    opt.Cookie.HttpOnly = true;
    opt.Cookie.IsEssential = true;
});

// ✅ DB Context
builder.Services.AddDbContext<BibliotecaContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("BibliotecaConnection"))
);

// ✅ JWT Authentication
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        var key = Encoding.UTF8.GetBytes(builder.Configuration["Jwt:Key"]!);

        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidateAudience = true,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            ValidIssuer = builder.Configuration["Jwt:Issuer"],
            ValidAudience = builder.Configuration["Jwt:Audience"],
            IssuerSigningKey = new SymmetricSecurityKey(key)
        };
    });

builder.Services.AddAuthorization();

var app = builder.Build();

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Home/Error");
    app.UseHsts();
}

app.UseHttpsRedirection();
app.UseStaticFiles();

app.UseRouting();

app.UseSession();        // ✅ antes de auth, para leer session en Layout si quieres

app.UseAuthentication();
app.UseAuthorization();

// ✅ API endpoints (/api/...)
app.MapControllers();

// ✅ MVC views
app.MapControllerRoute(
    name: "default",
    pattern: "{controller=WebAuth}/{action=Login}/{id?}"
);

app.Run();