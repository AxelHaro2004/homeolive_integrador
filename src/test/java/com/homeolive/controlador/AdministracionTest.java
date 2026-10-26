package com.homeolive.controlador;

import com.homeolive.acceso.Rol;
import com.homeolive.modelo.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** CRUD del administrador sobre clientes y pedidos. */
class AdministracionTest extends PruebaIntegracion {

    @Autowired private ClienteModelo clientes;
    @Autowired private PedidoModelo pedidos;

    private Cliente porDni(String dni) {
        return clientes.listar().stream().filter(c -> c.dni().equals(dni)).findFirst().orElseThrow();
    }

    @Test
    void creaEditaYEliminaUnCliente() throws Exception {
        MockHttpSession admin = entrarComo(Rol.ADMINISTRADOR);
        mvc.perform(post("/admin-clientes").session(admin).param("nombre", "Pedro Castillo Vega")
                        .param("dni", "61000001").param("telefono", "955-111-222").param("ubicacion", "Provincia"))
                .andExpect(redirectedUrl("/admin-clientes"))
                .andExpect(flash().attribute("mensaje", containsString("creado")));
        Cliente c = porDni("61000001");
        assertThat(c.telefono()).isEqualTo("955 111 222");

        mvc.perform(get("/admin-clientes").session(admin)).andExpect(content().string(containsString("Pedro Castillo Vega")));

        mvc.perform(post("/admin-clientes/" + c.id() + "/editar").session(admin).param("nombre", "Pedro Castillo")
                        .param("dni", "61000001").param("telefono", "955111222").param("ubicacion", "Lima"))
                .andExpect(flash().attribute("mensaje", containsString("actualizado")));
        assertThat(clientes.obtener(c.id()).ubicacion()).isEqualTo("Lima");

        mvc.perform(post("/admin-clientes/" + c.id() + "/eliminar").session(admin))
                .andExpect(flash().attribute("mensaje", containsString("eliminado")));
        assertThat(clientes.listar().stream().anyMatch(x -> x.dni().equals("61000001"))).isFalse();
    }

    @Test
    void rechazaDniRepetidoYDatosInvalidos() throws Exception {
        MockHttpSession admin = entrarComo(Rol.ADMINISTRADOR);
        mvc.perform(post("/admin-clientes").session(admin).param("nombre", "Otra Persona")
                        .param("dni", "72541863").param("telefono", "955111222").param("ubicacion", "Lima"))
                .andExpect(flash().attribute("error", containsString("Ya existe un cliente con el DNI 72541863")));
        mvc.perform(post("/admin-clientes").session(admin).param("nombre", "Otra Persona")
                        .param("dni", "ABC").param("telefono", "955111222").param("ubicacion", "Lima"))
                .andExpect(flash().attribute("error", containsString("8 dígitos")));
    }

    @Test
    void noEliminaUnClienteQueTienePedidos() throws Exception {
        Cliente maria = porDni("72541863");
        mvc.perform(post("/admin-clientes/" + maria.id() + "/eliminar").session(entrarComo(Rol.ADMINISTRADOR)))
                .andExpect(flash().attribute("error", containsString("No se puede eliminar")));
        assertThat(clientes.obtener(maria.id())).isNotNull();
    }

    @Test
    void creaUnPedidoParaUnClienteConUnItemDelCatalogo() throws Exception {
        MockHttpSession admin = entrarComo(Rol.ADMINISTRADOR);
        Cliente maria = porDni("72541863");
        int antes = pedidos.listarPorCliente(maria.id()).size();

        mvc.perform(post("/admin-pedidos").session(admin).param("cliente", String.valueOf(maria.id()))
                        .param("ubicacion", "Lima").param("envio", "Motorizado")
                        .param("presentacion", String.valueOf(idPresentacion("Flores de Bach", "30 ml")))
                        .param("producto", "Ansiedad").param("cantidad", "3"))
                .andExpect(flash().attribute("mensaje", containsString("creado")));

        Pedido nuevo = pedidos.listarPorCliente(maria.id()).get(0);
        assertThat(pedidos.listarPorCliente(maria.id())).hasSize(antes + 1);
        assertThat(nuevo.estado()).isEqualTo(EstadoPedido.EN_PREPARACION);
        assertThat(nuevo.monto()).isEqualByComparingTo("126.00");
        assertThat(nuevo.items().get(0).producto()).isEqualTo("Ansiedad");
    }

    @Test
    void editaElEstadoDeUnPedidoRespetandoLasReglasDelCourier() throws Exception {
        MockHttpSession admin = entrarComo(Rol.ADMINISTRADOR);
        Pedido p = pedidos.listarPorEstado(EstadoPedido.LISTO_PARA_ENVIO).stream()
                .filter(x -> x.envio().esCourier()).findFirst().orElseThrow();

        mvc.perform(post("/admin-pedidos/" + p.id() + "/editar").session(admin)
                        .param("cliente", String.valueOf(p.cliente().id())).param("ubicacion", p.ubicacion())
                        .param("envio", p.envio().etiqueta()).param("estado", "POR_CONFIRMAR"))
                .andExpect(flash().attribute("error", containsString("id_pedido_courier")));
        assertThat(pedidos.obtener(p.id()).estado()).isEqualTo(EstadoPedido.LISTO_PARA_ENVIO);

        mvc.perform(post("/admin-pedidos/" + p.id() + "/editar").session(admin)
                        .param("cliente", String.valueOf(p.cliente().id())).param("ubicacion", p.ubicacion())
                        .param("envio", p.envio().etiqueta()).param("estado", "POR_CONFIRMAR").param("idCourier", "ADM-1"))
                .andExpect(flash().attribute("mensaje", containsString("actualizado")));
        Pedido editado = pedidos.obtener(p.id());
        assertThat(editado.estado()).isEqualTo(EstadoPedido.POR_CONFIRMAR);
        assertThat(editado.idCourier()).isEqualTo("ADM-1");

        // volver a «listo para envío» limpia el id del courier
        mvc.perform(post("/admin-pedidos/" + p.id() + "/editar").session(admin)
                .param("cliente", String.valueOf(p.cliente().id())).param("ubicacion", p.ubicacion())
                .param("envio", p.envio().etiqueta()).param("estado", "LISTO_PARA_ENVIO"));
        assertThat(pedidos.obtener(p.id()).idCourier()).isNull();
    }

    @Test
    void noPermiteMotorizadoEnProvinciaAlEditar() throws Exception {
        Pedido p = pedidos.listar().get(0);
        mvc.perform(post("/admin-pedidos/" + p.id() + "/editar").session(entrarComo(Rol.ADMINISTRADOR))
                        .param("cliente", String.valueOf(p.cliente().id())).param("ubicacion", "Provincia")
                        .param("envio", "Motorizado").param("estado", p.estado().name()))
                .andExpect(flash().attribute("error", containsString("provincia")));
        assertThat(pedidos.obtener(p.id()).envio()).isEqualTo(p.envio());
    }

    @Test
    void eliminaUnPedidoConSusItems() throws Exception {
        Pedido p = pedidos.listar().get(0);
        mvc.perform(post("/admin-pedidos/" + p.id() + "/eliminar").session(entrarComo(Rol.ADMINISTRADOR)))
                .andExpect(flash().attribute("mensaje", containsString("eliminado")));
        assertThat(pedidos.buscar(p.id())).isEmpty();
    }

    @Test
    void laListaDePedidosDelAdministradorSeMuestra() throws Exception {
        mvc.perform(get("/admin-pedidos").session(entrarComo(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PED-1048")))
                .andExpect(content().string(containsString("Crear pedido")));
        assertThat(TipoEnvio.valueOf("OLVA").esCourier()).isTrue();
    }
}
