package com.homeolive.acceso;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Autenticación y autorización de cada petición: sin sesión se redirige al login; con sesión, el rol
 * debe tener permiso sobre la vista (o sobre las acciones que cuelgan de ella).
 */
public class AccesoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        UsuarioSesion usuario = UsuarioSesion.de(request.getSession(false));

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        Optional<Vista> vista = Vista.porRuta(ruta);
        if (vista.isPresent() && !usuario.rol().puede(vista.get())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }
}
