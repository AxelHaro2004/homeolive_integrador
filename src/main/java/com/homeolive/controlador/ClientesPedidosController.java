package com.homeolive.controlador;

import com.homeolive.acceso.Vista;
import com.homeolive.modelo.Cliente;
import com.homeolive.modelo.ClienteModelo;
import com.homeolive.modelo.PedidoModelo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** Vista clientes-pedidos (vendedor y administrador): historial de pedidos de un cliente. */
@Controller
public class ClientesPedidosController {

    private final ClienteModelo clientes;
    private final PedidoModelo pedidos;

    public ClientesPedidosController(ClienteModelo clientes, PedidoModelo pedidos) {
        this.clientes = clientes;
        this.pedidos = pedidos;
    }

    @GetMapping("/clientes-pedidos")
    public String historial(@RequestParam(name = "cliente", required = false) Long idCliente, Model model) {
        List<Cliente> todos = clientes.listar();
        Cliente seleccionado = todos.stream()
                .filter(c -> idCliente != null && c.id() == idCliente)
                .findFirst()
                .orElse(todos.isEmpty() ? null : todos.get(0));

        model.addAttribute("vista", Vista.CLIENTES_PEDIDOS);
        model.addAttribute("clientes", todos);
        model.addAttribute("seleccionado", seleccionado);
        model.addAttribute("pedidos", seleccionado == null ? List.of() : pedidos.listarPorCliente(seleccionado.id()));
        return "clientes-pedidos";
    }
}
