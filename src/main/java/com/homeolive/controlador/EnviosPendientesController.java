package com.homeolive.controlador;

import com.homeolive.acceso.UsuarioSesion;
import com.homeolive.acceso.Vista;
import com.homeolive.modelo.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Vista envios-pendientes (gestor de envíos): «listo para envío» y «por confirmar». */
@Controller
public class EnviosPendientesController {

    private final PedidoModelo pedidos;
    private final FlujoPedidoModelo flujo;

    public EnviosPendientesController(PedidoModelo pedidos, FlujoPedidoModelo flujo) {
        this.pedidos = pedidos;
        this.flujo = flujo;
    }

    @GetMapping("/envios-pendientes")
    public String listar(Model model) {
        model.addAttribute("vista", Vista.ENVIOS_PENDIENTES);
        model.addAttribute("listos", pedidos.listarPorEstado(EstadoPedido.LISTO_PARA_ENVIO));
        model.addAttribute("porConfirmar", pedidos.listarPorEstado(EstadoPedido.POR_CONFIRMAR));
        return "envios-pendientes";
    }

    /** Pequeño formulario del courier: solo courier e id_pedido_courier. */
    @PostMapping("/envios-pendientes/{id}/por-confirmar")
    public String marcarPorConfirmar(@PathVariable long id,
                                     @RequestParam(name = "courier", defaultValue = "") String courier,
                                     @RequestParam(name = "idPedidoCourier", defaultValue = "") String idPedidoCourier,
                                     HttpSession sesion, RedirectAttributes avisos) {
        try {
            Pedido pedido = flujo.marcarPorConfirmar(id, courier, idPedidoCourier,
                    UsuarioSesion.de(sesion).idEmpleado());
            avisos.addFlashAttribute("mensaje", "El pedido " + pedido.codigo() + " quedó «por confirmar» con "
                    + pedido.envio().etiqueta() + " (" + pedido.idCourier() + ").");
        } catch (ReglaNegocioException e) {
            avisos.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/envios-pendientes";
    }

    @PostMapping("/envios-pendientes/{id}/entregado")
    public String marcarEntregado(@PathVariable long id,
                                  @RequestParam(name = "clienteConfirmo", defaultValue = "false") boolean clienteConfirmo,
                                  @RequestParam(name = "plataformaListo", defaultValue = "false") boolean plataformaListo,
                                  @RequestParam(name = "clienteAvisado", defaultValue = "false") boolean clienteAvisado,
                                  HttpSession sesion, RedirectAttributes avisos) {
        try {
            Pedido pedido = flujo.marcarEntregado(id,
                    new ConfirmacionEntrega(clienteConfirmo, plataformaListo, clienteAvisado),
                    UsuarioSesion.de(sesion).idEmpleado());
            avisos.addFlashAttribute("mensaje", "El pedido " + pedido.codigo() + " fue marcado como entregado.");
        } catch (ReglaNegocioException e) {
            avisos.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/envios-pendientes";
    }
}
