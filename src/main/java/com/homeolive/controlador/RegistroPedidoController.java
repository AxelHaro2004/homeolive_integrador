package com.homeolive.controlador;

import com.homeolive.acceso.Vista;
import com.homeolive.modelo.Pedido;
import com.homeolive.modelo.PedidoModelo;
import com.homeolive.modelo.ReglaNegocioException;
import com.homeolive.modelo.SolicitudPedido;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Vista registro-pedidos (vendedor): captura la venta y genera el pedido «en preparación». */
@Controller
public class RegistroPedidoController {

    private final PedidoModelo pedidos;

    public RegistroPedidoController(PedidoModelo pedidos) {
        this.pedidos = pedidos;
    }

    @GetMapping("/registro-pedidos")
    public String formulario(Model model) {
        prepararFormulario(model);
        return "registro-pedidos";
    }

    @PostMapping("/registro-pedidos")
    public String registrar(@RequestParam(name = "nombre", defaultValue = "") String nombre,
                            @RequestParam(name = "dni", defaultValue = "") String dni,
                            @RequestParam(name = "telefono", defaultValue = "") String telefono,
                            @RequestParam(name = "ubicacion", defaultValue = "") String ubicacion,
                            @RequestParam(name = "envio", defaultValue = "") String envio,
                            @RequestParam(name = "pago", defaultValue = "false") boolean pago,
                            HttpServletRequest request, Model model, RedirectAttributes avisos) {
        List<SolicitudPedido.Linea> lineas = FormularioPedido.lineas(request);
        try {
            Pedido pedido = pedidos.registrar(new SolicitudPedido(nombre, dni, telefono, ubicacion, envio, pago, lineas));
            avisos.addFlashAttribute("mensaje", "Pedido " + pedido.codigo() + " registrado correctamente y enviado a preparación.");
            return "redirect:/registro-pedidos";
        } catch (ReglaNegocioException e) {
            // Se vuelve a mostrar el formulario con lo que el vendedor ya había escrito.
            prepararFormulario(model);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("nombre", nombre);
            model.addAttribute("dni", dni);
            model.addAttribute("telefono", telefono);
            model.addAttribute("ubicacionElegida", ubicacion);
            model.addAttribute("envioElegido", envio);
            model.addAttribute("pagoMarcado", pago);
            model.addAttribute("lineasJson", FormularioPedido.json(lineas));
            return "registro-pedidos";
        }
    }

    private void prepararFormulario(Model model) {
        model.addAttribute("vista", Vista.REGISTRO_PEDIDOS);
        model.addAttribute("numeroPedido", pedidos.siguienteCodigoPedido());
        model.addAttribute("numeroVenta", pedidos.siguienteCodigoVenta());
    }
}
