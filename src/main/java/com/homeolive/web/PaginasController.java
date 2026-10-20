package com.homeolive.web;

import com.homeolive.acceso.Rol;
import com.homeolive.acceso.Vista;
import com.homeolive.datos.DatosDemo;
import com.homeolive.modelo.Cliente;
import com.homeolive.modelo.EstadoPedido;
import com.homeolive.modelo.Indicador;
import com.homeolive.modelo.Pedido;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Controller
public class PaginasController {

    @GetMapping("/")
    public String raiz() {
        return "redirect:/inicio";
    }

    @GetMapping("/inicio")
    public String inicio(Model model) {
        List<Pedido> pedidos = DatosDemo.pedidos();
        model.addAttribute("vista", Vista.INICIO);
        model.addAttribute("indicadores", List.of(
                indicador("En preparación", EstadoPedido.EN_PREPARACION, "box", "amber"),
                indicador("Listo para envío", EstadoPedido.LISTO_PARA_ENVIO, "truck", "blue"),
                indicador("Por confirmar", EstadoPedido.POR_CONFIRMAR, "eye", "violet"),
                indicador("Entregados hoy", EstadoPedido.ENTREGADO, "check", "green")));
        model.addAttribute("actividad", pedidos.subList(0, Math.min(5, pedidos.size())));
        return "inicio";
    }

    @GetMapping("/registro-pedidos")
    public String registroPedidos(Model model) {
        model.addAttribute("vista", Vista.REGISTRO_PEDIDOS);
        model.addAttribute("numeroPedido", "PED-1049");
        model.addAttribute("numeroVenta", "VEN-5082");
        model.addAttribute("categorias", DatosDemo.CATEGORIAS);
        model.addAttribute("presentaciones", DatosDemo.PRESENTACIONES);
        return "registro-pedidos";
    }

    @GetMapping("/clientes-pedidos")
    public String clientesPedidos(@RequestParam(name = "cliente", required = false) Long idCliente, Model model) {
        Cliente seleccionado = idCliente == null
                ? DatosDemo.clientes().get(0)
                : DatosDemo.cliente(idCliente);
        model.addAttribute("vista", Vista.CLIENTES_PEDIDOS);
        model.addAttribute("clientes", DatosDemo.clientes());
        model.addAttribute("seleccionado", seleccionado);
        model.addAttribute("pedidos", DatosDemo.pedidosDeCliente(seleccionado.id()));
        return "clientes-pedidos";
    }

    @GetMapping("/resumen-ventas")
    public String resumenVentas(Model model) {
        List<Pedido> pedidos = DatosDemo.pedidos();
        BigDecimal total = pedidos.stream().map(Pedido::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("vista", Vista.RESUMEN_VENTAS);
        model.addAttribute("pedidos", pedidos);
        model.addAttribute("totalTexto", String.format(Locale.US, "S/ %.2f", total));
        return "resumen-ventas";
    }

    @GetMapping("/resumen-pedidos")
    public String resumenPedidos(Model model) {
        model.addAttribute("vista", Vista.RESUMEN_PEDIDOS);
        model.addAttribute("pedidos", DatosDemo.pedidos());
        return "resumen-pedidos";
    }

    @GetMapping("/pedidos-pendientes")
    public String pedidosPendientes(Model model) {
        model.addAttribute("vista", Vista.PEDIDOS_PENDIENTES);
        model.addAttribute("pendientes", DatosDemo.pedidosEnEstado(EstadoPedido.EN_PREPARACION));
        return "pedidos-pendientes";
    }

    @GetMapping("/envios-pendientes")
    public String enviosPendientes(Model model) {
        model.addAttribute("vista", Vista.ENVIOS_PENDIENTES);
        model.addAttribute("listos", DatosDemo.pedidosEnEstado(EstadoPedido.LISTO_PARA_ENVIO));
        model.addAttribute("porConfirmar", DatosDemo.pedidosEnEstado(EstadoPedido.POR_CONFIRMAR));
        return "envios-pendientes";
    }

    @GetMapping("/admin-clientes")
    public String adminClientes(Model model) {
        model.addAttribute("vista", Vista.ADMIN_CLIENTES);
        model.addAttribute("clientes", DatosDemo.clientes());
        return "admin-clientes";
    }

    @GetMapping("/admin-pedidos")
    public String adminPedidos(Model model) {
        model.addAttribute("vista", Vista.ADMIN_PEDIDOS);
        model.addAttribute("pedidos", DatosDemo.pedidos());
        model.addAttribute("clientes", DatosDemo.clientes());
        return "admin-pedidos";
    }

    private Indicador indicador(String etiqueta, EstadoPedido estado, String icono, String tono) {
        return new Indicador(etiqueta, DatosDemo.pedidosEnEstado(estado).size(), icono, tono);
    }
}
