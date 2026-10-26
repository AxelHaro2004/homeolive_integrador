package com.homeolive.controlador;

import com.homeolive.acceso.Rol;
import com.homeolive.modelo.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Recorrido completo del negocio: vendedor → químico → gestor de envíos, por Motorizado y por courier. */
class FlujoDePedidoIntegracionTest extends PruebaIntegracion {

    @Autowired private PedidoModelo pedidos;
    @Autowired private ClienteModelo clientes;

    private MockHttpServletRequestBuilder registro(MockHttpSession sesion, String dni, String ubicacion, String envio) {
        return post("/registro-pedidos").session(sesion)
                .param("nombre", "Rosa Martínez López").param("dni", dni).param("telefono", "955 123 456")
                .param("ubicacion", ubicacion).param("envio", envio).param("pago", "true")
                .param("presentacion", String.valueOf(idPresentacion("Flores de Bach", "30 ml")))
                .param("producto", "Calma Kids")
                .param("cantidad", "2");
    }

    private Pedido ultimoDe(String dni) {
        long idCliente = clientes.listar().stream().filter(c -> c.dni().equals(dni)).findFirst().orElseThrow().id();
        return pedidos.listarPorCliente(idCliente).get(0);
    }

    @Test
    void pedidoPorMotorizadoRecorreEnPreparacionListoYEntregado() throws Exception {
        MockHttpSession vendedor = entrarComo(Rol.VENDEDOR);
        mvc.perform(registro(vendedor, "60000001", "Lima", "Motorizado"))
                .andExpect(redirectedUrl("/registro-pedidos"))
                .andExpect(flash().attribute("mensaje", containsString("registrado correctamente")));

        Pedido pedido = ultimoDe("60000001");
        assertThat(pedido.estado()).isEqualTo(EstadoPedido.EN_PREPARACION);
        assertThat(pedido.monto()).isEqualByComparingTo("84.00");
        assertThat(pedido.items()).hasSize(1);
        assertThat(pedido.cliente().telefono()).isEqualTo("955 123 456");

        MockHttpSession quimico = entrarComo(Rol.QUIMICO_FARMACEUTICO);
        mvc.perform(get("/pedidos-pendientes").session(quimico))
                .andExpect(content().string(containsString(pedido.codigo())))
                .andExpect(content().string(containsString("Calma Kids")));
        mvc.perform(post("/pedidos-pendientes/" + pedido.id() + "/listo").session(quimico))
                .andExpect(flash().attribute("mensaje", containsString("listo para envío")));
        assertThat(pedidos.obtener(pedido.id()).estado()).isEqualTo(EstadoPedido.LISTO_PARA_ENVIO);

        MockHttpSession gestor = entrarComo(Rol.GESTOR_ENVIOS);
        mvc.perform(get("/envios-pendientes").session(gestor)).andExpect(content().string(containsString(pedido.codigo())));
        // sin la confirmación del cliente no se entrega
        mvc.perform(post("/envios-pendientes/" + pedido.id() + "/entregado").session(gestor))
                .andExpect(flash().attribute("error", containsString("recibió")));
        assertThat(pedidos.obtener(pedido.id()).estado()).isEqualTo(EstadoPedido.LISTO_PARA_ENVIO);
        mvc.perform(post("/envios-pendientes/" + pedido.id() + "/entregado").session(gestor).param("clienteConfirmo", "on"))
                .andExpect(flash().attribute("mensaje", containsString("entregado")));
        assertThat(pedidos.obtener(pedido.id()).estado()).isEqualTo(EstadoPedido.ENTREGADO);
    }

    @Test
    void pedidoPorCourierPasaPorPorConfirmarYSoloSeEntregaConLasConfirmaciones() throws Exception {
        mvc.perform(registro(entrarComo(Rol.VENDEDOR), "60000002", "Provincia", "Olva"))
                .andExpect(flash().attribute("mensaje", containsString("registrado correctamente")));
        Pedido pedido = ultimoDe("60000002");

        mvc.perform(post("/pedidos-pendientes/" + pedido.id() + "/listo").session(entrarComo(Rol.QUIMICO_FARMACEUTICO)));
        MockHttpSession gestor = entrarComo(Rol.GESTOR_ENVIOS);

        // el courier no se puede entregar directamente
        mvc.perform(post("/envios-pendientes/" + pedido.id() + "/entregado").session(gestor).param("clienteConfirmo", "on"))
                .andExpect(flash().attribute("error", containsString("por confirmar")));

        // el formulario exige el id_pedido_courier
        mvc.perform(post("/envios-pendientes/" + pedido.id() + "/por-confirmar").session(gestor).param("courier", "Olva"))
                .andExpect(flash().attribute("error", containsString("id_pedido_courier")));

        mvc.perform(post("/envios-pendientes/" + pedido.id() + "/por-confirmar").session(gestor)
                        .param("courier", "Olva").param("idPedidoCourier", "OLV-2026-00129"))
                .andExpect(flash().attribute("mensaje", containsString("por confirmar")));
        Pedido porConfirmar = pedidos.obtener(pedido.id());
        assertThat(porConfirmar.estado()).isEqualTo(EstadoPedido.POR_CONFIRMAR);
        assertThat(porConfirmar.idCourier()).isEqualTo("OLV-2026-00129");

        mvc.perform(get("/envios-pendientes").session(gestor))
                .andExpect(content().string(containsString("OLV-2026-00129")));

        mvc.perform(post("/envios-pendientes/" + pedido.id() + "/entregado").session(gestor).param("plataformaListo", "on"))
                .andExpect(flash().attribute("error", containsString("llamar al cliente")));
        assertThat(pedidos.obtener(pedido.id()).estado()).isEqualTo(EstadoPedido.POR_CONFIRMAR);

        mvc.perform(post("/envios-pendientes/" + pedido.id() + "/entregado").session(gestor)
                        .param("plataformaListo", "on").param("clienteAvisado", "on"))
                .andExpect(flash().attribute("mensaje", containsString("entregado")));
        assertThat(pedidos.obtener(pedido.id()).estado()).isEqualTo(EstadoPedido.ENTREGADO);
    }

    @Test
    void elCourierEfectivoDelGestorQuedaRegistradoEnElPedido() throws Exception {
        mvc.perform(registro(entrarComo(Rol.VENDEDOR), "60000003", "Provincia", "Olva"));
        Pedido pedido = ultimoDe("60000003");
        mvc.perform(post("/pedidos-pendientes/" + pedido.id() + "/listo").session(entrarComo(Rol.QUIMICO_FARMACEUTICO)));
        mvc.perform(post("/envios-pendientes/" + pedido.id() + "/por-confirmar").session(entrarComo(Rol.GESTOR_ENVIOS))
                .param("courier", "Shalom").param("idPedidoCourier", "SHA-77"));
        assertThat(pedidos.obtener(pedido.id()).envio()).isEqualTo(TipoEnvio.SHALOM);
    }

    @Test
    void elMismoClienteNoSeDuplicaAlComprarOtraVez() throws Exception {
        MockHttpSession vendedor = entrarComo(Rol.VENDEDOR);
        mvc.perform(registro(vendedor, "60000004", "Lima", "Motorizado"));
        mvc.perform(registro(vendedor, "60000004", "Lima", "Olva"));
        assertThat(clientes.listar().stream().filter(c -> c.dni().equals("60000004"))).hasSize(1);
        assertThat(pedidos.listarPorCliente(ultimoDe("60000004").cliente().id())).hasSize(2);

        mvc.perform(get("/clientes-pedidos").param("cliente", String.valueOf(ultimoDe("60000004").cliente().id()))
                        .session(vendedor))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rosa Martínez López")))
                .andExpect(content().string(containsString("Calma Kids")));
    }

    @Test
    void siElFormularioEsInvalidoSeMuestraElErrorYSeConservanLosDatos() throws Exception {
        MockHttpSession vendedor = entrarComo(Rol.VENDEDOR);

        mvc.perform(registro(vendedor, "123", "Lima", "Motorizado"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("8 dígitos")))
                .andExpect(content().string(containsString("Rosa Martínez López")));

        mvc.perform(registro(vendedor, "60000005", "Provincia", "Motorizado"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("provincia")));

        mvc.perform(post("/registro-pedidos").session(vendedor)
                        .param("nombre", "Rosa Martínez").param("dni", "60000005").param("telefono", "955123456")
                        .param("ubicacion", "Lima").param("envio", "Olva")
                        .param("presentacion", String.valueOf(idPresentacion("Flores de Bach", "30 ml")))
                        .param("producto", "Calma Kids").param("cantidad", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ya realizó el pago")));

        mvc.perform(post("/registro-pedidos").session(vendedor)
                        .param("nombre", "Rosa Martínez").param("dni", "60000005").param("telefono", "955123456")
                        .param("ubicacion", "Lima").param("envio", "Olva").param("pago", "true")
                        .param("presentacion", String.valueOf(idPresentacion("Flores de Bach", "30 ml")))
                        .param("producto", "Calma Kids").param("cantidad", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("al menos 1")));

        assertThat(clientes.listar().stream().filter(c -> c.dni().equals("60000005"))).isEmpty();
    }

    @Test
    void unPostManipuladoConUnaPresentacionInexistenteSeRechaza() throws Exception {
        mvc.perform(post("/registro-pedidos").session(entrarComo(Rol.VENDEDOR))
                        .param("nombre", "Rosa Martínez").param("dni", "60000006").param("telefono", "955123456")
                        .param("ubicacion", "Lima").param("envio", "Olva").param("pago", "true")
                        .param("presentacion", "99999").param("producto", "Calma Kids").param("cantidad", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("no existe en el catálogo")));
    }

    @Test
    void variasFilasConProductoEscritoOVacioQuedanAlineadasEnElPedido() throws Exception {
        MockHttpSession vendedor = entrarComo(Rol.VENDEDOR);
        mvc.perform(post("/registro-pedidos").session(vendedor)
                .param("nombre", "Rosa Martínez").param("dni", "60000008").param("telefono", "955123456")
                .param("ubicacion", "Lima").param("envio", "Motorizado").param("pago", "true")
                .param("presentacion", String.valueOf(idPresentacion("Spirulina", "45 gr"))).param("producto", "").param("cantidad", "1")
                .param("presentacion", String.valueOf(idPresentacion("Flores de Bach", "30 ml"))).param("producto", "Calma Kids").param("cantidad", "2"))
                .andExpect(flash().attribute("mensaje", containsString("registrado correctamente")));

        Pedido pedido = ultimoDe("60000008");
        assertThat(pedido.items()).extracting(i -> i.producto()).containsExactly("Spirulina", "Calma Kids");
        assertThat(pedido.items()).extracting(i -> i.presentacion()).containsExactly("45 gr", "30 ml");
        assertThat(pedido.monto()).isEqualByComparingTo("120.00");
    }

    @Test
    void elFormularioMuestraElSiguienteNumeroDePedidoYDeVenta() throws Exception {
        mvc.perform(get("/registro-pedidos").session(entrarComo(Rol.VENDEDOR)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PED-1049")))
                .andExpect(content().string(containsString("VEN-5082")));
    }

    @Test
    void elPanelDeInicioCuentaLosPedidosPorEstado() throws Exception {
        mvc.perform(registro(entrarComo(Rol.VENDEDOR), "60000007", "Lima", "Motorizado"));
        mvc.perform(get("/inicio").session(entrarComo(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("En preparación")))
                .andExpect(content().string(containsString("Entregados hoy")));
    }
}
