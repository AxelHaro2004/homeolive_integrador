package com.homeolive.controlador;

import com.homeolive.acceso.UsuarioSesion;
import com.homeolive.modelo.AutenticacionModelo;
import com.homeolive.modelo.ReglaNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final AutenticacionModelo autenticacion;

    public AuthController(AutenticacionModelo autenticacion) {
        this.autenticacion = autenticacion;
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        if (UsuarioSesion.de(request.getSession(false)) != null) {
            return "redirect:/inicio";
        }
        return "login";
    }

    @PostMapping("/login")
    public String entrar(@RequestParam(name = "usuario", defaultValue = "") String usuario,
                         @RequestParam(name = "clave", defaultValue = "") String clave,
                         HttpServletRequest request, Model model) {
        try {
            UsuarioSesion autenticado = autenticacion.autenticar(usuario, clave);
            // Se descarta la sesión anterior y se crea una nueva al autenticar (evita fijación de sesión).
            HttpSession anterior = request.getSession(false);
            if (anterior != null) {
                anterior.invalidate();
            }
            request.getSession(true).setAttribute(UsuarioSesion.SESION, autenticado);
            return "redirect:/inicio";
        } catch (ReglaNegocioException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("identificador", usuario);
            return "login";
        }
    }

    @GetMapping("/logout")
    public String salir(HttpSession sesion) {
        sesion.invalidate();
        return "redirect:/login";
    }
}
