package com.homeolive.modelo;

import com.homeolive.dao.CategoriaProductoDao;
import com.homeolive.dao.PresentacionDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresentacionModeloTest {

    @Mock private PresentacionDao presentaciones;
    @Mock private CategoriaProductoDao categorias;

    private PresentacionModelo modelo;

    private static Presentacion pres(long id, int valor, String unidad, String precio, boolean activa) {
        return new Presentacion(id, 1L, "Flores de Bach", valor, unidad, new BigDecimal(precio), activa);
    }

    @BeforeEach
    void preparar() {
        modelo = new PresentacionModelo(presentaciones, categorias);
        when(categorias.buscarPorId(1L)).thenReturn(Optional.of(new CategoriaProducto(1L, "Flores de Bach", true)));
        when(categorias.buscarPorId(2L)).thenReturn(Optional.of(new CategoriaProducto(2L, "Eliminada", false)));
        when(presentaciones.buscarPorClave(anyLong(), anyInt(), anyString())).thenReturn(Optional.empty());
        when(presentaciones.insertar(anyLong(), anyInt(), anyString(), any())).thenReturn(7L);
        when(presentaciones.buscarPorId(5L)).thenReturn(Optional.of(pres(5L, 30, "ml", "42.00", true)));
        when(presentaciones.buscarPorId(6L)).thenReturn(Optional.of(pres(6L, 60, "ml", "50.00", false)));
    }

    @Test
    void creaUnaPresentacionConPrecioDeDosDecimales() {
        Presentacion p = modelo.crear(1L, "50", "ml", "45,5");
        assertThat(p.etiqueta()).isEqualTo("50 ml");
        assertThat(p.precio()).isEqualByComparingTo("45.50");
        verify(presentaciones).insertar(1L, 50, "ml", new BigDecimal("45.50"));
    }

    @Test
    void validaCategoriaMedidaUnidadYPrecio() {
        assertThatThrownBy(() -> modelo.crear(2L, "50", "ml", "10")).hasMessageContaining("categoría válida");
        assertThatThrownBy(() -> modelo.crear(99L, "50", "ml", "10")).hasMessageContaining("categoría válida");
        assertThatThrownBy(() -> modelo.crear(1L, "0", "ml", "10")).hasMessageContaining("medida");
        assertThatThrownBy(() -> modelo.crear(1L, "abc", "ml", "10")).hasMessageContaining("medida");
        assertThatThrownBy(() -> modelo.crear(1L, "50", "litros", "10")).hasMessageContaining("unidad");
        assertThatThrownBy(() -> modelo.crear(1L, "50", "ml", "0")).hasMessageContaining("mayor que 0");
        assertThatThrownBy(() -> modelo.crear(1L, "50", "ml", "-5")).hasMessageContaining("mayor que 0");
        assertThatThrownBy(() -> modelo.crear(1L, "50", "ml", "10.123")).hasMessageContaining("2 decimales");
        assertThatThrownBy(() -> modelo.crear(1L, "50", "ml", "x")).hasMessageContaining("no es válido");
        verify(presentaciones, never()).insertar(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    void noPermiteDuplicarUnaPresentacionVigente() {
        when(presentaciones.buscarPorClave(1L, 30, "ml")).thenReturn(Optional.of(pres(5L, 30, "ml", "42.00", true)));
        assertThatThrownBy(() -> modelo.crear(1L, "30", "ml", "40")).hasMessageContaining("Ya existe la presentación 30 ml");
    }

    @Test
    void crearUnaIgualAUnaEliminadaLaReactivaConElNuevoPrecio() {
        when(presentaciones.buscarPorClave(1L, 60, "ml")).thenReturn(Optional.of(pres(6L, 60, "ml", "50.00", false)));
        Presentacion p = modelo.crear(1L, "60", "ml", "55");
        assertThat(p.id()).isEqualTo(6L);
        verify(presentaciones).reactivar(6L, new BigDecimal("55.00"));
        verify(presentaciones, never()).insertar(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    void sinPedidosSePuedeCambiarTodo() {
        when(presentaciones.contarUsosEnPedidos(5L)).thenReturn(0);
        modelo.actualizar(5L, "40", "ml", "44");
        verify(presentaciones).actualizar(5L, 40, "ml", new BigDecimal("44.00"));
    }

    @Test
    void conPedidosSoloSePuedeCambiarElPrecio() {
        when(presentaciones.contarUsosEnPedidos(5L)).thenReturn(3);
        modelo.actualizar(5L, "30", "ml", "48");
        verify(presentaciones).actualizar(5L, 30, "ml", new BigDecimal("48.00"));

        assertThatThrownBy(() -> modelo.actualizar(5L, "40", "ml", "48")).hasMessageContaining("solo puedes cambiar su precio");
        assertThatThrownBy(() -> modelo.actualizar(5L, "30", "gr", "48")).hasMessageContaining("solo puedes cambiar su precio");
    }

    @Test
    void noSeEditaNiSeEliminaUnaPresentacionYaEliminada() {
        assertThatThrownBy(() -> modelo.actualizar(6L, "60", "ml", "50")).hasMessageContaining("ya no existe");
        assertThatThrownBy(() -> modelo.eliminar(6L)).hasMessageContaining("ya no existe");
        verify(presentaciones, never()).desactivar(anyLong());
    }

    @Test
    void eliminarEsLogico() {
        modelo.eliminar(5L);
        verify(presentaciones).desactivar(5L);
    }
}
