package com.homeolive.modelo;

import com.homeolive.dao.EmpaquetadoDao;
import com.homeolive.dao.PedidoDao;
import com.homeolive.modelo.ConfirmacionEntrega;
import com.homeolive.modelo.Zona;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** Ciclo de vida del pedido según el contexto: cada rol avanza solo su tramo y con sus confirmaciones. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FlujoPedidoModeloTest {

    @Mock private PedidoDao pedidos;
    @Mock private EmpaquetadoDao empaquetado;

    private FlujoPedidoModelo flujo;

    @BeforeEach
    void preparar() {
        flujo = new FlujoPedidoModelo(pedidos, empaquetado,
                Clock.fixed(Instant.parse("2026-10-22T15:00:00Z"), Zona.LIMA));
        when(pedidos.cambiarEstado(anyLong(), any(), any(), any())).thenReturn(true);
    }

    private void existe(EstadoPedido estado, TipoEnvio envio) {
        Cliente cliente = new Cliente(1L, "Jorge Salazar", "43812675", "946 120 883", "Provincia");
        when(pedidos.buscarPorId(1L)).thenReturn(Optional.of(new Pedido(1L, "PED-1047", "VEN-5080", cliente,
                envio.esMotorizado() ? "Lima" : "Provincia", envio, estado, new BigDecimal("58.00"),
                LocalDateTime.now(), LocalDateTime.now(), List.of(), null)));
    }

    private static final ConfirmacionEntrega NADA = new ConfirmacionEntrega(false, false, false);

    @Test
    void elQuimicoPasaDeEnPreparacionAListoParaEnvio() {
        existe(EstadoPedido.EN_PREPARACION, TipoEnvio.OLVA);
        flujo.marcarListoParaEnvio(1L);
        verify(pedidos).cambiarEstado(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(EstadoPedido.EN_PREPARACION),
                org.mockito.ArgumentMatchers.eq(EstadoPedido.LISTO_PARA_ENVIO), any(LocalDateTime.class));
    }

    @Test
    void unPedidoQueNoEstaEnPreparacionNoPuedeMarcarseListo() {
        existe(EstadoPedido.ENTREGADO, TipoEnvio.OLVA);
        assertThatThrownBy(() -> flujo.marcarListoParaEnvio(1L)).hasMessageContaining("no puede");
        verify(pedidos, never()).cambiarEstado(anyLong(), any(), any(), any());
    }

    @Test
    void elCourierExigeElIdDelPedidoCourier() {
        existe(EstadoPedido.LISTO_PARA_ENVIO, TipoEnvio.OLVA);
        assertThatThrownBy(() -> flujo.marcarPorConfirmar(1L, "Olva", "  ", 9L))
                .hasMessageContaining("id_pedido_courier es obligatorio");
        verify(empaquetado, never()).registrar(anyLong(), any(), any(), anyLong(), any());
    }

    @Test
    void elFormularioSoloAceptaOlvaOShalom() {
        existe(EstadoPedido.LISTO_PARA_ENVIO, TipoEnvio.OLVA);
        assertThatThrownBy(() -> flujo.marcarPorConfirmar(1L, "Motorizado", "ABC-1", 9L))
                .hasMessageContaining("Olva o Shalom");
    }

    @Test
    void unPedidoPorCourierPasaAPorConfirmarYSeRegistraElEmpaquetado() {
        existe(EstadoPedido.LISTO_PARA_ENVIO, TipoEnvio.OLVA);
        flujo.marcarPorConfirmar(1L, "Olva", "OLV-2026-00129", 9L);
        verify(empaquetado).registrar(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(TipoEnvio.OLVA),
                org.mockito.ArgumentMatchers.eq("OLV-2026-00129"),
                org.mockito.ArgumentMatchers.eq(9L), any(LocalDateTime.class));
    }

    @Test
    void elMotorizadoNoPasaPorPorConfirmar() {
        existe(EstadoPedido.LISTO_PARA_ENVIO, TipoEnvio.MOTORIZADO);
        assertThatThrownBy(() -> flujo.marcarPorConfirmar(1L, "Olva", "OLV-1", 9L))
                .hasMessageContaining("motorizado");
    }

    @Test
    void elIdDelCourierRechazaCaracteresRaros() {
        existe(EstadoPedido.LISTO_PARA_ENVIO, TipoEnvio.OLVA);
        assertThatThrownBy(() -> flujo.marcarPorConfirmar(1L, "Olva", "x'; DROP TABLE pedido_verificado;--", 9L))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void elMotorizadoSoloSeEntregaCuandoElClienteConfirmaLaRecepcion() {
        existe(EstadoPedido.LISTO_PARA_ENVIO, TipoEnvio.MOTORIZADO);
        assertThatThrownBy(() -> flujo.marcarEntregado(1L, NADA, 9L)).hasMessageContaining("recibió");
        verify(pedidos, never()).cambiarEstado(anyLong(), any(), any(), any());

        flujo.marcarEntregado(1L, new ConfirmacionEntrega(true, false, false), 9L);
        verify(pedidos).cambiarEstado(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(EstadoPedido.LISTO_PARA_ENVIO),
                org.mockito.ArgumentMatchers.eq(EstadoPedido.ENTREGADO), any(LocalDateTime.class));
        verify(empaquetado).registrar(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(TipoEnvio.MOTORIZADO), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(9L), any(LocalDateTime.class));
    }

    @Test
    void elCourierSoloSeEntregaConPlataformaListaYClienteAvisadoPorLlamada() {
        existe(EstadoPedido.POR_CONFIRMAR, TipoEnvio.SHALOM);
        assertThatThrownBy(() -> flujo.marcarEntregado(1L, new ConfirmacionEntrega(false, false, true), 9L))
                .hasMessageContaining("listo para recoger");
        assertThatThrownBy(() -> flujo.marcarEntregado(1L, new ConfirmacionEntrega(false, true, false), 9L))
                .hasMessageContaining("llamar al cliente");
        verify(pedidos, never()).cambiarEstado(anyLong(), any(), any(), any());

        flujo.marcarEntregado(1L, new ConfirmacionEntrega(false, true, true), 9L);
        verify(pedidos).cambiarEstado(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(EstadoPedido.POR_CONFIRMAR),
                org.mockito.ArgumentMatchers.eq(EstadoPedido.ENTREGADO), any(LocalDateTime.class));
    }

    @Test
    void unPedidoPorCourierNoSeEntregaSinPasarPorPorConfirmar() {
        existe(EstadoPedido.LISTO_PARA_ENVIO, TipoEnvio.OLVA);
        assertThatThrownBy(() -> flujo.marcarEntregado(1L, new ConfirmacionEntrega(true, true, true), 9L))
                .hasMessageContaining("por confirmar");
    }

    @Test
    void unPedidoEnPreparacionNoSePuedeEntregar() {
        existe(EstadoPedido.EN_PREPARACION, TipoEnvio.MOTORIZADO);
        assertThatThrownBy(() -> flujo.marcarEntregado(1L, new ConfirmacionEntrega(true, true, true), 9L))
                .hasMessageContaining("no puede marcarse como entregado");
    }

    @Test
    void siOtroUsuarioYaCambioElEstadoSeAvisa() {
        existe(EstadoPedido.EN_PREPARACION, TipoEnvio.OLVA);
        when(pedidos.cambiarEstado(anyLong(), any(), any(), any())).thenReturn(false);
        assertThatThrownBy(() -> flujo.marcarListoParaEnvio(1L)).hasMessageContaining("ya cambió de estado");
    }
}
