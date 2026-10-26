package com.homeolive.dao;

import com.homeolive.dao.jdbc.CatalogoDaoJdbc;
import com.homeolive.dao.jdbc.CategoriaProductoDaoJdbc;
import com.homeolive.dao.jdbc.PresentacionDaoJdbc;
import com.homeolive.modelo.CategoriaProducto;
import com.homeolive.modelo.Presentacion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@Import({CatalogoDaoJdbc.class, CategoriaProductoDaoJdbc.class, PresentacionDaoJdbc.class})
class CatalogoYPresentacionDaoJdbcTest {

    @Autowired private CatalogoDao catalogo;
    @Autowired private CategoriaProductoDao categorias;
    @Autowired private PresentacionDao presentaciones;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void elProductoEscritoSeGuardaUnaSolaVezSinImportarMayusculas() {
        Presentacion p = presentaciones.listarTodas().get(0);
        long a = catalogo.obtenerOCrearMedicina("Calma Kids", p.id());
        long b = catalogo.obtenerOCrearMedicina("calma kids", p.id());
        assertThat(b).isEqualTo(a);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM producto", Integer.class)).isEqualTo(1);

        Presentacion otra = presentaciones.listarTodas().get(1);
        long c = catalogo.obtenerOCrearMedicina("Calma Kids", otra.id());
        assertThat(c).isNotEqualTo(a);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM producto", Integer.class)).isEqualTo(1);
    }

    @Test
    void lasPresentacionesInicialesVienenConSuCategoriaYPrecio() {
        assertThat(presentaciones.listarTodas()).anyMatch(p -> p.categoria().equals("Flores de Bach")
                && p.etiqueta().equals("30 ml") && p.precio().compareTo(new BigDecimal("42.00")) == 0 && p.activa());
    }

    @Test
    void crudDePresentaciones() {
        long categoria = categorias.insertar("Aceites");
        long id = presentaciones.insertar(categoria, 15, "ml", new BigDecimal("30.00"));
        Presentacion p = presentaciones.buscarPorId(id).orElseThrow();
        assertThat(p.etiqueta()).isEqualTo("15 ml");
        assertThat(p.activa()).isTrue();
        assertThat(presentaciones.buscarPorClave(categoria, 15, "ml")).isPresent();

        assertThat(presentaciones.actualizar(id, 20, "ml", new BigDecimal("35.50"))).isTrue();
        assertThat(presentaciones.buscarPorId(id).orElseThrow().precio()).isEqualByComparingTo("35.50");
        assertThat(presentaciones.contarUsosEnPedidos(id)).isZero();
    }

    @Test
    void laEliminacionEsLogicaYSePuedeReactivar() {
        long categoria = categorias.insertar("Aceites");
        long id = presentaciones.insertar(categoria, 15, "ml", new BigDecimal("30.00"));

        assertThat(presentaciones.desactivar(id)).isTrue();
        Presentacion eliminada = presentaciones.buscarPorId(id).orElseThrow();
        assertThat(eliminada.activa()).isFalse();

        assertThat(presentaciones.reactivar(id, new BigDecimal("33.00"))).isTrue();
        Presentacion reactivada = presentaciones.buscarPorId(id).orElseThrow();
        assertThat(reactivada.activa()).isTrue();
        assertThat(reactivada.precio()).isEqualByComparingTo("33.00");
    }

    @Test
    void desactivarUnaCategoriaOcultaSusPresentacionesPeroNoBorraNada() {
        long categoria = categorias.insertar("Aceites");
        long id = presentaciones.insertar(categoria, 15, "ml", new BigDecimal("30.00"));

        assertThat(presentaciones.desactivarPorCategoria(categoria)).isEqualTo(1);
        assertThat(categorias.desactivar(categoria)).isTrue();

        CategoriaProducto c = categorias.buscarPorId(categoria).orElseThrow();
        assertThat(c.activa()).isFalse();
        assertThat(presentaciones.buscarPorId(id).orElseThrow().activa()).isFalse();
        assertThat(presentaciones.contarActivasPorCategoria()).doesNotContainKey(categoria);

        assertThat(categorias.reactivar(categoria, "Aceites Esenciales")).isTrue();
        assertThat(categorias.buscarPorId(categoria).orElseThrow().nombre()).isEqualTo("Aceites Esenciales");
    }

    @Test
    void laBaseRechazaPresentacionesRepetidasYUnidadesInvalidas() {
        long categoria = categorias.insertar("Aceites");
        presentaciones.insertar(categoria, 15, "ml", new BigDecimal("30.00"));
        assertThatThrownBy(() -> presentaciones.insertar(categoria, 15, "ml", new BigDecimal("31.00")))
                .isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> presentaciones.insertar(categoria, 15, "litros", new BigDecimal("31.00")))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
