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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CategoriaProductoModeloTest {

    @Mock private CategoriaProductoDao categorias;
    @Mock private PresentacionDao presentaciones;

    private CategoriaProductoModelo modelo;

    @BeforeEach
    void preparar() {
        modelo = new CategoriaProductoModelo(categorias, presentaciones);
        when(categorias.listarTodas()).thenReturn(List.of(
                new CategoriaProducto(1L, "Flores de Bach", true),
                new CategoriaProducto(2L, "Homeopático", true),
                new CategoriaProducto(3L, "Aceites", false)));
        when(categorias.buscarPorId(1L)).thenReturn(Optional.of(new CategoriaProducto(1L, "Flores de Bach", true)));
        when(categorias.buscarPorId(3L)).thenReturn(Optional.of(new CategoriaProducto(3L, "Aceites", false)));
        when(categorias.productosPorCategoria()).thenReturn(Map.of(1L, 5));
        when(presentaciones.contarActivasPorCategoria()).thenReturn(Map.of(1L, 2));
        when(categorias.insertar(anyString())).thenReturn(9L);
    }

    @Test
    void laListaSoloMuestraCategoriasVigentesConSusConteos() {
        List<CategoriaProductoVista> lista = modelo.listar("");
        assertThat(lista).extracting(CategoriaProductoVista::nombre).containsExactly("Flores de Bach", "Homeopático");
        assertThat(lista.get(0).productos()).isEqualTo(5);
        assertThat(lista.get(0).presentaciones()).isEqualTo(2);
        assertThat(lista.get(1).presentaciones()).isZero();
        assertThat(modelo.total()).isEqualTo(2);
        assertThat(modelo.listar("homeo")).hasSize(1);
    }

    @Test
    void creaUnaCategoriaNueva() {
        CategoriaProducto c = modelo.crear("  Aceites   Esenciales ");
        assertThat(c.nombre()).isEqualTo("Aceites Esenciales");
        verify(categorias).insertar("Aceites Esenciales");
    }

    @Test
    void noPermiteNombresVaciosNiRepetidosSinImportarTildesNiMayusculas() {
        assertThatThrownBy(() -> modelo.crear("  ")).hasMessageContaining("obligatorio");
        assertThatThrownBy(() -> modelo.crear("homeopatico")).hasMessageContaining("Ya existe una categoría");
        assertThatThrownBy(() -> modelo.crear("A".repeat(101))).hasMessageContaining("100");
        verify(categorias, never()).insertar(anyString());
    }

    @Test
    void crearUnaCategoriaConElNombreDeUnaEliminadaLaReactiva() {
        CategoriaProducto c = modelo.crear("aceites");
        assertThat(c.id()).isEqualTo(3L);
        assertThat(c.activa()).isTrue();
        verify(categorias).reactivar(3L, "aceites");
        verify(categorias, never()).insertar(anyString());
    }

    @Test
    void alEditarPuedeConservarSuNombrePeroNoTomarElDeOtra() {
        modelo.actualizar(1L, "Flores de Bach Premium");
        verify(categorias).actualizar(1L, "Flores de Bach Premium");
        assertThatThrownBy(() -> modelo.actualizar(1L, "Homeopático")).hasMessageContaining("Ya existe");
        assertThatThrownBy(() -> modelo.actualizar(3L, "Otro")).hasMessageContaining("ya no existe");
    }

    @Test
    void eliminarEsLogicoYDesactivaTambienSusPresentaciones() {
        modelo.eliminar(1L);
        verify(presentaciones).desactivarPorCategoria(1L);
        verify(categorias).desactivar(1L);
    }

    @Test
    void noSeEliminaUnaCategoriaYaEliminada() {
        assertThatThrownBy(() -> modelo.eliminar(3L)).hasMessageContaining("ya no existe");
        verify(categorias, never()).desactivar(anyLong());
    }
}
