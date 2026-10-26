package com.homeolive.dao;

import com.homeolive.dao.jdbc.*;
import com.homeolive.modelo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de la capa DAO contra la base H2 real (schema.sql + data.sql), cada una con rollback. */
@JdbcTest
@Import({PedidoDaoJdbc.class, ClienteDaoJdbc.class, CatalogoDaoJdbc.class, CategoriaProductoDaoJdbc.class,
        PresentacionDaoJdbc.class, EmpaquetadoDaoJdbc.class, EmpleadoDaoJdbc.class})
class PedidoDaoJdbcTest {

    @Autowired private PedidoDao pedidos;
    @Autowired private ClienteDao clientes;
    @Autowired private CatalogoDao catalogo;
    @Autowired private PresentacionDao presentaciones;
    @Autowired private CategoriaProductoDao categorias;
    @Autowired private EmpaquetadoDao empaquetado;
    @Autowired private EmpleadoDao empleados;

    private long idCliente;
    private ItemPedido item;

    @BeforeEach
    void preparar() {
        idCliente = clientes.insertar(new DatosCliente("María Rojas", "72541863", "987 654 321", "Lima"));
        Presentacion p = presentaciones.listarTodas().get(0);
        long idMedicina = catalogo.obtenerOCrearMedicina("Calma Kids", p.id());
        item = new ItemPedido(idMedicina, p.categoria(), "Calma Kids", p.etiqueta(), 2, p.precio());
    }

    private long guardar(String codigo, String venta, TipoEnvio envio, String ubicacion, EstadoPedido estado,
                         LocalDateTime fecha) {
        return pedidos.insertar(new NuevoPedido(codigo, venta, idCliente, ubicacion, envio, estado,
                item.subtotal(), fecha, List.of(item)));
    }

    @Test
    void guardaElPedidoConSusItemsYLoRecuperaCompleto() {
        long id = guardar("PED-2001", "VEN-6001", TipoEnvio.MOTORIZADO, "Lima", EstadoPedido.EN_PREPARACION,
                LocalDateTime.of(2026, 10, 22, 9, 42));

        Pedido p = pedidos.buscarPorId(id).orElseThrow();
        assertThat(p.codigo()).isEqualTo("PED-2001");
        assertThat(p.venta()).isEqualTo("VEN-6001");
        assertThat(p.cliente().nombre()).isEqualTo("María Rojas");
        assertThat(p.estado()).isEqualTo(EstadoPedido.EN_PREPARACION);
        assertThat(p.monto()).isEqualByComparingTo(item.subtotal());
        assertThat(p.items()).hasSize(1);
        assertThat(p.items().get(0).cantidad()).isEqualTo(2);
        assertThat(p.items().get(0).producto()).isEqualTo("Calma Kids");
        assertThat(p.items().get(0).presentacion()).isEqualTo(item.presentacion());
        assertThat(p.idCourier()).isNull();
    }

    @Test
    void lasConsultasSeEnlazanPorParametrosYNoEjecutanTextoMalicioso() {
        clientes.insertar(new DatosCliente("Robert'); DROP TABLE cliente;--", "11111111", "987 654 322", "Lima"));
        assertThat(clientes.buscarPorDni("11111111' OR '1'='1")).isEmpty();
        assertThat(clientes.buscarPorDni("11111111")).isPresent();
        assertThat(clientes.listar()).hasSize(2);
    }

    @Test
    void cambiarEstadoSoloFuncionaSiElEstadoSigueSiendoElEsperado() {
        long id = guardar("PED-2002", "VEN-6002", TipoEnvio.OLVA, "Provincia", EstadoPedido.EN_PREPARACION,
                LocalDateTime.of(2026, 10, 22, 9, 0));
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 22, 10, 0);

        assertThat(pedidos.cambiarEstado(id, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO_PARA_ENVIO, ahora)).isTrue();
        assertThat(pedidos.cambiarEstado(id, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO_PARA_ENVIO, ahora)).isFalse();
        Pedido p = pedidos.buscarPorId(id).orElseThrow();
        assertThat(p.estado()).isEqualTo(EstadoPedido.LISTO_PARA_ENVIO);
        assertThat(p.fechaActualizacion()).isEqualTo(ahora);
    }

    @Test
    void listaPorEstadoEnOrdenDeLlegadaYCuentaPorEstado() {
        guardar("PED-2003", "VEN-6003", TipoEnvio.MOTORIZADO, "Lima", EstadoPedido.EN_PREPARACION, LocalDateTime.of(2026, 10, 22, 11, 0));
        guardar("PED-2004", "VEN-6004", TipoEnvio.MOTORIZADO, "Lima", EstadoPedido.EN_PREPARACION, LocalDateTime.of(2026, 10, 22, 8, 0));
        guardar("PED-2005", "VEN-6005", TipoEnvio.OLVA, "Lima", EstadoPedido.LISTO_PARA_ENVIO, LocalDateTime.of(2026, 10, 22, 7, 0));

        List<Pedido> enPreparacion = pedidos.listarPorEstado(EstadoPedido.EN_PREPARACION);
        List<String> codigos = enPreparacion.stream().map(Pedido::codigo).toList();
        assertThat(codigos.indexOf("PED-2004")).isLessThan(codigos.indexOf("PED-2003"));

        Map<EstadoPedido, Integer> conteo = pedidos.contarPorEstado();
        assertThat(conteo.get(EstadoPedido.LISTO_PARA_ENVIO)).isGreaterThanOrEqualTo(1);
        assertThat(conteo).containsKeys(EstadoPedido.values());
    }

    @Test
    void listaPorPeriodoIncluyendoAmbosExtremos() {
        guardar("PED-2006", "VEN-6006", TipoEnvio.MOTORIZADO, "Lima", EstadoPedido.ENTREGADO, LocalDateTime.of(2026, 3, 1, 0, 0));
        guardar("PED-2007", "VEN-6007", TipoEnvio.MOTORIZADO, "Lima", EstadoPedido.ENTREGADO, LocalDateTime.of(2026, 3, 31, 23, 59));
        guardar("PED-2008", "VEN-6008", TipoEnvio.MOTORIZADO, "Lima", EstadoPedido.ENTREGADO, LocalDateTime.of(2026, 4, 1, 0, 0));

        List<String> marzo = pedidos.listarPorPeriodo(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))
                .stream().map(Pedido::codigo).toList();
        assertThat(marzo).contains("PED-2006", "PED-2007").doesNotContain("PED-2008");
    }

    @Test
    void elCourierRegistradoSeVeEnElPedidoYSeBorraConEl() {
        long id = guardar("PED-2009", "VEN-6009", TipoEnvio.SHALOM, "Provincia", EstadoPedido.POR_CONFIRMAR,
                LocalDateTime.of(2026, 10, 21, 15, 0));
        long gestor = empleados.buscarIdPorDni("43458169").orElseThrow();
        empaquetado.registrar(id, TipoEnvio.SHALOM, "SHA-9384512", gestor, LocalDateTime.of(2026, 10, 21, 18, 0));

        assertThat(pedidos.buscarPorId(id).orElseThrow().idCourier()).isEqualTo("SHA-9384512");

        assertThat(pedidos.eliminar(id)).isTrue();
        assertThat(pedidos.buscarPorId(id)).isEmpty();
    }

    @Test
    void laBaseDeDatosRechazaMotorizadoEnProvincia() {
        assertThatThrownBy(() -> guardar("PED-2010", "VEN-6010", TipoEnvio.MOTORIZADO, "Provincia",
                EstadoPedido.EN_PREPARACION, LocalDateTime.of(2026, 10, 22, 9, 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void losCodigosSiguientesContinuanDespuesDelMayorExistente() {
        guardar("PED-3001", "VEN-7001", TipoEnvio.MOTORIZADO, "Lima", EstadoPedido.EN_PREPARACION, LocalDateTime.of(2026, 10, 22, 9, 0));
        assertThat(pedidos.siguienteCodigoPedido()).isEqualTo("PED-3002");
        assertThat(pedidos.siguienteCodigoVenta()).isEqualTo("VEN-7002");
    }

    @Test
    void elConteoDeProductosPorCategoriaSaleDeLosProductosRegistrados() {
        assertThat(categorias.totalProductosAsociados()).isZero();
        Presentacion p = presentaciones.listarTodas().get(0);
        catalogo.obtenerOCrearMedicina("Calma Kids", p.id());
        catalogo.obtenerOCrearMedicina("Ansiedad", p.id());

        assertThat(categorias.productosPorCategoria().get(p.idCategoria())).isEqualTo(2);
        assertThat(categorias.totalProductosAsociados()).isEqualTo(2);
    }
}
