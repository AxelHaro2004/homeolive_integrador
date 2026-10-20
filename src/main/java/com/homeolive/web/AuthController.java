package com.homeolive.web;

import com.homeolive.acceso.Rol;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(HttpSession sesion, Model model) {
        if (sesion.getAttribute(Rol.SESION) != null) {
            return "redirect:/inicio";
        }
        model.addAttribute("roles", Rol.values());
        return "login";
    }

    @PostMapping("/login")
    public String entrar(@RequestParam("rol") String clave, HttpSession sesion) {
        Rol rol = Rol.deClave(clave);
        if (rol == null) {
            return "redirect:/login";
        }
        sesion.setAttribute(Rol.SESION, rol);
        return "redirect:/inicio";
    }

    @GetMapping("/logout")
    public String salir(HttpSession sesion) {
        sesion.invalidate();
        return "redirect:/login";
    }
}
