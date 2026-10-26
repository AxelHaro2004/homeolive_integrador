package com.homeolive.modelo;

import com.homeolive.dao.ClienteDao;
import com.homeolive.dao.PedidoDao;
import com.homeolive.modelo.DatosCliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClienteModeloTest {

    @Mock private ClienteDao clientes;
    @Mock private PedidoDao pedidos;

    private ClienteModelo modelo;

    @BeforeEach
    void preparar() {
        modelo = new ClienteModelo(clientes, pedidos);
        when(clientes.insertar(any())).thenReturn(1L);
    }

    @Test
    void registrarClienteValido() {
        when(clientes.buscarPorDni("72541863")).thenReturn(Optional.empty());

        Cliente c = modelo.crear(new DatosCliente(" María  Rojas ", "72541863", "987-654-321", "lima"));

        assertThat(c.id()).isEqualTo(1L);
        ArgumentCaptor<DatosCliente> captura = ArgumentCaptor.forClass(DatosCliente.class);
        verify(clientes).insertar(captura.capture());
        assertThat(captura.getValue()).isEqualTo(new DatosCliente("María Rojas", "72541863", "987 654 321", "Lima"));
    }

    @Test
    void noPermiteDosClientesConElMismoDni() {
        when(clientes.buscarPorDni("72541863")).thenReturn(Optional.of(new Cliente(9L, "Otro", "72541863", "911 111 111", "Lima")));
        assertThatThrownBy(() -> modelo.crear(new DatosCliente("María Rojas", "72541863", "987654321", "Lima")))
                .hasMessageContaining("Ya existe un cliente con el DNI 72541863");
        verify(clientes, never()).insertar(any());
    }

    @Test
    void alEditarPuedeConservarSuPropioDniPeroNoTomarElDeOtro() {
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(new Cliente(1L, "María", "72541863", "987 654 321", "Lima")));
        when(clientes.buscarPorDni("72541863")).thenReturn(Optional.of(new Cliente(1L, "María", "72541863", "987 654 321", "Lima")));
        modelo.actualizar(1L, new DatosCliente("María Rojas", "72541863", "987654321", "Lima"));
        verify(clientes).actualizar(anyLong(), any());

        when(clientes.buscarPorDni("43812675")).thenReturn(Optional.of(new Cliente(2L, "Jorge", "43812675", "946 120 883", "Provincia")));
        assertThatThrownBy(() -> modelo.actualizar(1L, new DatosCliente("María Rojas", "43812675", "987654321", "Lima")))
                .hasMessageContaining("Ya existe un cliente");
    }

    @Test
    void noSeEliminaUnClienteConPedidos() {
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(new Cliente(1L, "María Rojas", "72541863", "987 654 321", "Lima")));
        when(pedidos.contarPorCliente(1L)).thenReturn(3);
        assertThatThrownBy(() -> modelo.eliminar(1L)).hasMessageContaining("tiene 3 pedidos registrados");
        verify(clientes, never()).eliminar(anyLong());
    }

    @Test
    void seEliminaUnClienteSinPedidos() {
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(new Cliente(1L, "María Rojas", "72541863", "987 654 321", "Lima")));
        when(pedidos.contarPorCliente(1L)).thenReturn(0);
        modelo.eliminar(1L);
        verify(clientes).eliminar(1L);
    }

    @Test
    void laUbicacionDebeSerLimaOProvincia() {
        assertThatThrownBy(() -> modelo.crear(new DatosCliente("María Rojas", "72541863", "987654321", "Cusco")))
                .hasMessageContaining("Lima o Provincia");
    }
}
