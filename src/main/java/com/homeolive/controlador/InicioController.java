package com.homeolive.controlador;

import com.homeolive.acceso.Vista;
import com.homeolive.modelo.Indicador;
import com.homeolive.modelo.PedidoModelo;
import com.homeolive.modelo.ResumenPanel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class InicioController {

    private static final int ACTIVIDAD_RECIENTE = 5;

    private final PedidoModelo pedidos;

    public InicioController(PedidoModelo pedidos) {
        this.pedidos = pedidos;
    }

    @GetMapping("/")
    public String raiz() {
        return "redirect:/inicio";
    }

    @GetMapping("/inicio")
    public String inicio(Model model) {
        ResumenPanel r = pedidos.resumenPanel();
        model.addAttribute("vista", Vista.INICIO);
        model.addAttribute("indicadores", List.of(
                new Indicador("En preparación", r.enPreparacion(), "box", "amber"),
                new Indicador("Listo para envío", r.listoParaEnvio(), "truck", "blue"),
                new Indicador("Por confirmar", r.porConfirmar(), "eye", "violet"),
                new Indicador("Entregados hoy", r.entregadosHoy(), "check", "green")));
        model.addAttribute("actividad", pedidos.listarRecientes(ACTIVIDAD_RECIENTE));
        return "inicio";
    }
}
