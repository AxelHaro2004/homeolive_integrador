package com.homeolive.acceso;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class RolAdvice {

    @ModelAttribute("rol")
    public Rol rol(HttpSession sesion) {
        return (Rol) sesion.getAttribute(Rol.SESION);
    }
}
