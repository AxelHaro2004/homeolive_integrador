package com.homeolive.modelo;

import com.homeolive.acceso.Rol;
import com.homeolive.acceso.Vista;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReglasDeEnvioTest {

    @Test
    void enLimaSePuedeElegirMotorizadoOlvaOShalom() {
        assertThat(Ubicacion.LIMA.enviosDisponibles())
                .containsExactly(TipoEnvio.MOTORIZADO, TipoEnvio.OLVA, TipoEnvio.SHALOM);
    }

    @Test
    void enProvinciaSoloOlvaOShalom() {
        assertThat(Ubicacion.PROVINCIA.enviosDisponibles()).containsExactly(TipoEnvio.OLVA, TipoEnvio.SHALOM);
        assertThat(Ubicacion.PROVINCIA.permite(TipoEnvio.MOTORIZADO)).isFalse();
    }

    @Test
    void lasAccionesQueCuelganDeUnaVistaPertenecenAEsaVista() {
        assertThat(Vista.porRuta("/categorias-presentaciones/categorias/3/editar")).contains(Vista.CATEGORIAS_PRESENTACIONES);
        assertThat(Vista.porRuta("/categorias-presentaciones/presentaciones/2/eliminar")).contains(Vista.CATEGORIAS_PRESENTACIONES);
        assertThat(Vista.porRuta("/envios-pendientes/7/entregado")).contains(Vista.ENVIOS_PENDIENTES);
        assertThat(Vista.porRuta("/admin-clientes")).contains(Vista.ADMIN_CLIENTES);
        assertThat(Vista.porRuta("/admin-clientesX")).isEmpty();
        assertThat(Vista.porRuta("/catalogo")).isEmpty();
    }

    @Test
    void soloElAdministradorExporta() {
        assertThat(Rol.ADMINISTRADOR.puedeExportar()).isTrue();
        assertThat(Rol.AGENTE_FINANZAS.puedeExportar()).isFalse();
    }
}
