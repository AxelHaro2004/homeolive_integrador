package com.homeolive.modelo;

import com.homeolive.dao.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD del requisito «El sistema debe permitir registrar un pedido»: pedido con datos completos se registra;
 * pedido sin cliente, producto inexistente o cantidad 0 muestran error. La lógica del Model se prueba aislada,
 * con DAO simulados (principio de inversión de dependencias).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PedidoModeloTest {

    private static final Presentacion FLORES_30 =
            new Presentacion(2L, 1L, "Flores de Bach", 30, "ml", new BigDecimal("42.00"), true);

    @Mock private PedidoDao pedidos;
    @Mock private PresentacionDao presentaciones;
    @Mock private CatalogoDao catalogo;
    @Mock private EmpaquetadoDao empaquetado;
    @Mock private ClienteDao clienteDao;

    private PedidoModelo modelo;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-22T15:00:00Z"), Zona.LIMA);
        modelo = new PedidoModelo(pedidos, presentaciones, catalogo, empaquetado, new ClienteModelo(clienteDao, pedidos), reloj);

        when(presentaciones.buscarPorId(2L)).thenReturn(Optional.of(FLORES_30));
        when(presentaciones.buscarPorId(99L)).thenReturn(Optional.empty());
        when(presentaciones.buscarPorId(3L)).thenReturn(Optional.of(
                new Presentacion(3L, 1L, "Flores de Bach", 60, "ml", new BigDecimal("58.00"), false)));
        when(catalogo.obtenerOCrearMedicina(anyString(), eq(2L))).thenReturn(20L);
        when(clienteDao.buscarPorDni("72541863")).thenReturn(Optional.empty());
        when(clienteDao.insertar(any())).thenReturn(10L);
        when(pedidos.siguienteCodigoPedido()).thenReturn("PED-1049");
        when(pedidos.siguienteCodigoVenta()).thenReturn("VEN-5082");
        when(pedidos.insertar(any())).thenReturn(77L);
        when(pedidos.buscarPorId(77L)).thenReturn(Optional.of(pedidoGuardado()));
    }

    private Pedido pedidoGuardado() {
        Cliente cliente = new Cliente(10L, "Rosa Martínez López", "72541863", "987 654 321", "Lima");
        return new Pedido(77L, "PED-1049", "VEN-5082", cliente, "Lima", TipoEnvio.MOTORIZADO,
                EstadoPedido.EN_PREPARACION, new BigDecimal("84.00"), LocalDateTime.now(), LocalDateTime.now(),
                List.of(), null);
    }

    private SolicitudPedido solicitud(String nombre, String ubicacion, String envio, boolean pago,
                                      SolicitudPedido.Linea... lineas) {
        return new SolicitudPedido(nombre, "72541863", "987654321", ubicacion, envio, pago, List.of(lineas));
    }

    private SolicitudPedido.Linea linea(Long id, int cantidad) {
        return new SolicitudPedido.Linea(id, "Calma Kids", cantidad);
    }

    @Test
    void pedidoConDatosCompletosSeRegistraEnPreparacionConMontoCalculado() {
        Pedido registrado = modelo.registrar(solicitud("Rosa Martínez López", "Lima", "Motorizado", true, linea(2L, 2)));

        assertThat(registrado.codigo()).isEqualTo("PED-1049");
        ArgumentCaptor<NuevoPedido> captura = ArgumentCaptor.forClass(NuevoPedido.class);
        verify(pedidos).insertar(captura.capture());
        NuevoPedido guardado = captura.getValue();
        assertThat(guardado.estado()).isEqualTo(EstadoPedido.EN_PREPARACION);
        assertThat(guardado.monto()).isEqualByComparingTo("84.00");
        assertThat(guardado.idCliente()).isEqualTo(10L);
        assertThat(guardado.codigoPedido()).isEqualTo("PED-1049");
        assertThat(guardado.codigoVenta()).isEqualTo("VEN-5082");
        assertThat(guardado.items()).hasSize(1);
        assertThat(guardado.items().get(0).precioUnitario()).isEqualByComparingTo("42.00");
        assertThat(guardado.items().get(0).producto()).isEqualTo("Calma Kids");
        assertThat(guardado.items().get(0).presentacion()).isEqualTo("30 ml");
        assertThat(guardado.items().get(0).idMedicina()).isEqualTo(20L);
    }

    @Test
    void pedidoSinClienteMuestraError() {
        assertThatThrownBy(() -> modelo.registrar(solicitud("  ", "Lima", "Motorizado", true, linea(2L, 1))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("obligatorios");
        verify(pedidos, never()).insertar(any());
    }

    @Test
    void presentacionInexistenteMuestraError() {
        assertThatThrownBy(() -> modelo.registrar(solicitud("Rosa Martínez", "Lima", "Motorizado", true, linea(99L, 1))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no existe en el catálogo");
        verify(pedidos, never()).insertar(any());
    }

    @Test
    void cantidadCeroMuestraError() {
        assertThatThrownBy(() -> modelo.registrar(solicitud("Rosa Martínez", "Lima", "Motorizado", true, linea(2L, 0))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("al menos 1");
        verify(pedidos, never()).insertar(any());
    }

    @Test
    void sinItemsMuestraError() {
        assertThatThrownBy(() -> modelo.registrar(solicitud("Rosa Martínez", "Lima", "Motorizado", true)))
                .hasMessageContaining("al menos un ítem");
    }

    @Test
    void sinPagoNoSeGeneraElPedido() {
        assertThatThrownBy(() -> modelo.registrar(solicitud("Rosa Martínez", "Lima", "Motorizado", false, linea(2L, 1))))
                .hasMessageContaining("ya realizó el pago");
        verify(pedidos, never()).insertar(any());
        verify(clienteDao, never()).insertar(any());
    }

    @Test
    void enProvinciaNoSePermiteMotorizado() {
        assertThatThrownBy(() -> modelo.registrar(solicitud("Rosa Martínez", "Provincia", "Motorizado", true, linea(2L, 1))))
                .hasMessageContaining("provincia");
    }

    @Test
    void enProvinciaSePermiteCourier() {
        modelo.registrar(solicitud("Rosa Martínez", "Provincia", "Olva", true, linea(2L, 1)));
        ArgumentCaptor<NuevoPedido> captura = ArgumentCaptor.forClass(NuevoPedido.class);
        verify(pedidos).insertar(captura.capture());
        assertThat(captura.getValue().envio()).isEqualTo(TipoEnvio.OLVA);
        assertThat(captura.getValue().ubicacion()).isEqualTo("Provincia");
    }

    @Test
    void unDniQueYaExisteReutilizaAlClienteSinDuplicarlo() {
        Cliente existente = new Cliente(5L, "Rosa Martínez López", "72541863", "911 111 111", "Lima");
        when(clienteDao.buscarPorDni("72541863")).thenReturn(Optional.of(existente));

        modelo.registrar(solicitud("Rosa M. López", "Lima", "Motorizado", true, linea(2L, 1)));

        verify(clienteDao, never()).insertar(any());
        ArgumentCaptor<NuevoPedido> captura = ArgumentCaptor.forClass(NuevoPedido.class);
        verify(pedidos).insertar(captura.capture());
        assertThat(captura.getValue().idCliente()).isEqualTo(5L);
    }

    @Test
    void lasFilasRepetidasDeUnaMismaPresentacionSeSuman() {
        modelo.registrar(solicitud("Rosa Martínez", "Lima", "Motorizado", true, linea(2L, 1), linea(2L, 2)));
        ArgumentCaptor<NuevoPedido> captura = ArgumentCaptor.forClass(NuevoPedido.class);
        verify(pedidos).insertar(captura.capture());
        List<ItemPedido> items = captura.getValue().items();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).cantidad()).isEqualTo(3);
        assertThat(captura.getValue().monto()).isEqualByComparingTo("126.00");
    }

    @Test
    void elDniInvalidoSeRechazaAntesDeTocarLaBaseDeDatos() {
        SolicitudPedido s = new SolicitudPedido("Rosa Martínez", "123", "987654321", "Lima", "Motorizado", true,
                List.of(linea(2L, 1)));
        assertThatThrownBy(() -> modelo.registrar(s)).hasMessageContaining("8 dígitos");
        verify(clienteDao, never()).insertar(any());
        verify(pedidos, never()).insertar(any());
    }

    @Test
    void siElProductoSeDejaVacioSeUsaLaCategoria() {
        servicioConLinea(new SolicitudPedido.Linea(2L, "   ", 1));
        ArgumentCaptor<String> nombre = ArgumentCaptor.forClass(String.class);
        verify(catalogo).obtenerOCrearMedicina(nombre.capture(), eq(2L));
        assertThat(nombre.getValue()).isEqualTo("Flores de Bach");
    }

    @Test
    void unaPresentacionEliminadaLogicamenteYaNoSePuedeVender() {
        assertThatThrownBy(() -> modelo.registrar(solicitud("Rosa Martínez", "Lima", "Motorizado", true, linea(3L, 1))))
                .hasMessageContaining("no existe en el catálogo");
        verify(pedidos, never()).insertar(any());
    }

    @Test
    void elNombreDelProductoNoAdmiteCaracteresRaros() {
        assertThatThrownBy(() -> servicioConLinea(new SolicitudPedido.Linea(2L, "x'; DROP TABLE producto;--", 1)))
                .hasMessageContaining("caracteres no permitidos");
        assertThatThrownBy(() -> servicioConLinea(new SolicitudPedido.Linea(2L, "A".repeat(101), 1)))
                .hasMessageContaining("100");
        verify(pedidos, never()).insertar(any());
    }

    @Test
    void elMismoProductoEscritoConOtraMayusculaSeSuma() {
        servicioConLinea(new SolicitudPedido.Linea(2L, "Calma Kids", 1), new SolicitudPedido.Linea(2L, "  calma   kids ", 2));
        ArgumentCaptor<NuevoPedido> captura = ArgumentCaptor.forClass(NuevoPedido.class);
        verify(pedidos).insertar(captura.capture());
        assertThat(captura.getValue().items()).hasSize(1);
        assertThat(captura.getValue().items().get(0).cantidad()).isEqualTo(3);
    }

    @Test
    void dosProductosDistintosConLaMismaPresentacionSonItemsDistintos() {
        when(catalogo.obtenerOCrearMedicina("Ansiedad", 2L)).thenReturn(21L);
        servicioConLinea(new SolicitudPedido.Linea(2L, "Calma Kids", 1), new SolicitudPedido.Linea(2L, "Ansiedad", 1));
        ArgumentCaptor<NuevoPedido> captura = ArgumentCaptor.forClass(NuevoPedido.class);
        verify(pedidos).insertar(captura.capture());
        assertThat(captura.getValue().items()).hasSize(2);
        assertThat(captura.getValue().monto()).isEqualByComparingTo("84.00");
    }

    private void servicioConLinea(SolicitudPedido.Linea... lineas) {
        modelo.registrar(solicitud("Rosa Martínez", "Lima", "Motorizado", true, lineas));
    }

    @Test
    void eliminarUnPedidoInexistenteMuestraError() {
        when(pedidos.buscarPorId(anyLong())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> modelo.eliminar(123L)).hasMessageContaining("no existe");
        verify(pedidos, never()).eliminar(anyLong());
    }
}
