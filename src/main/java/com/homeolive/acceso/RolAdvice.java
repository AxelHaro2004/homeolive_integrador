package com.homeolive.acceso;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Expone el usuario y su rol a todas las vistas Thymeleaf (menú, saludo, permisos de exportación). */
@ControllerAdvice
public class RolAdvice {

    @ModelAttribute("usuario")
    public UsuarioSesion usuario(HttpServletRequest request) {
        return UsuarioSesion.de(request.getSession(false));
    }

    @ModelAttribute("rol")
    public Rol rol(HttpServletRequest request) {
        UsuarioSesion usuario = UsuarioSesion.de(request.getSession(false));
        return usuario == null ? null : usuario.rol();
    }
}
