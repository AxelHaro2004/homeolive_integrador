package com.homeolive.controlador;

import com.homeolive.acceso.UsuarioSesion;
import com.homeolive.acceso.Vista;
import com.homeolive.modelo.EstadoPedido;
import com.homeolive.modelo.Pedido;
import com.homeolive.modelo.ClienteModelo;
import com.homeolive.modelo.PedidoModelo;
import com.homeolive.modelo.ReglaNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Vista admin-pedidos (administrador): CRUD de pedidos. */
@Controller
public class AdminPedidoController {

    private final PedidoModelo pedidos;
    private final ClienteModelo clientes;

    public AdminPedidoController(PedidoModelo pedidos, ClienteModelo clientes) {
        this.pedidos = pedidos;
        this.clientes = clientes;
    }

    @GetMapping("/admin-pedidos")
    public String listar(Model model) {
        model.addAttribute("vista", Vista.ADMIN_PEDIDOS);
        model.addAttribute("pedidos", pedidos.listar());
        model.addAttribute("clientes", clientes.listar());
        model.addAttribute("estados", EstadoPedido.values());
        return "admin-pedidos";
    }

    @PostMapping("/admin-pedidos")
    public String crear(@RequestParam(name = "cliente", defaultValue = "0") long idCliente,
                        @RequestParam(name = "ubicacion", defaultValue = "") String ubicacion,
                        @RequestParam(name = "envio", defaultValue = "") String envio,
                        HttpServletRequest request, RedirectAttributes avisos) {
        try {
            Pedido pedido = pedidos.registrarParaCliente(idCliente, ubicacion, envio,
                    FormularioPedido.lineas(request));
            avisos.addFlashAttribute("mensaje", "Pedido " + pedido.codigo() + " creado correctamente.");
        } catch (ReglaNegocioException e) {
            avisos.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin-pedidos";
    }

    @PostMapping("/admin-pedidos/{id}/editar")
    public String editar(@PathVariable long id,
                         @RequestParam(name = "cliente", defaultValue = "0") long idCliente,
                         @RequestParam(name = "ubicacion", defaultValue = "") String ubicacion,
                         @RequestParam(name = "envio", defaultValue = "") String envio,
                         @RequestParam(name = "estado", defaultValue = "") String estado,
                         @RequestParam(name = "idCourier", defaultValue = "") String idCourier,
                         HttpSession sesion, RedirectAttributes avisos) {
        try {
            Pedido pedido = pedidos.actualizarAdmin(id, idCliente, ubicacion, envio, estado, idCourier,
                    UsuarioSesion.de(sesion).idEmpleado());
            avisos.addFlashAttribute("mensaje", "Pedido " + pedido.codigo() + " actualizado correctamente.");
        } catch (ReglaNegocioException e) {
            avisos.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin-pedidos";
    }

    @PostMapping("/admin-pedidos/{id}/eliminar")
    public String eliminar(@PathVariable long id, RedirectAttributes avisos) {
        try {
            pedidos.eliminar(id);
            avisos.addFlashAttribute("mensaje", "Pedido eliminado correctamente.");
        } catch (ReglaNegocioException e) {
            avisos.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin-pedidos";
    }
}
