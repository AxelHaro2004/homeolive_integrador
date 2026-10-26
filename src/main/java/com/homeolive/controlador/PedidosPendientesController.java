package com.homeolive.controlador;

import com.homeolive.acceso.Vista;
import com.homeolive.modelo.EstadoPedido;
import com.homeolive.modelo.Pedido;
import com.homeolive.modelo.FlujoPedidoModelo;
import com.homeolive.modelo.PedidoModelo;
import com.homeolive.modelo.ReglaNegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Vista pedidos-pendientes (químico farmacéutico): pedidos «en preparación». */
@Controller
public class PedidosPendientesController {

    private final PedidoModelo pedidos;
    private final FlujoPedidoModelo flujo;

    public PedidosPendientesController(PedidoModelo pedidos, FlujoPedidoModelo flujo) {
        this.pedidos = pedidos;
        this.flujo = flujo;
    }

    @GetMapping("/pedidos-pendientes")
    public String listar(Model model) {
        model.addAttribute("vista", Vista.PEDIDOS_PENDIENTES);
        model.addAttribute("pendientes", pedidos.listarPorEstado(EstadoPedido.EN_PREPARACION));
        return "pedidos-pendientes";
    }

    @PostMapping("/pedidos-pendientes/{id}/listo")
    public String marcarListo(@PathVariable long id, RedirectAttributes avisos) {
        try {
            Pedido pedido = flujo.marcarListoParaEnvio(id);
            avisos.addFlashAttribute("mensaje", "El pedido " + pedido.codigo() + " quedó «listo para envío».");
        } catch (ReglaNegocioException e) {
            avisos.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pedidos-pendientes";
    }
}
