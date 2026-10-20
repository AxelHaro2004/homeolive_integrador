package com.homeolive.acceso;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

public class AccesoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession sesion = request.getSession(false);
        Rol rol = sesion == null ? null : (Rol) sesion.getAttribute(Rol.SESION);

        if (rol == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        Optional<Vista> vista = Vista.porRuta(ruta);
        if (vista.isPresent() && !rol.puede(vista.get())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }
}
