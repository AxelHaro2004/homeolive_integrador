package com.homeolive.modelo;

import com.homeolive.modelo.ReglaNegocioException;
import com.homeolive.modelo.Validaciones;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidacionesTest {

    @Test
    void elDniDebeTenerOchoDigitos() {
        assertThat(Validaciones.dni(" 72541863 ")).isEqualTo("72541863");
        assertThatThrownBy(() -> Validaciones.dni("1234567")).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> Validaciones.dni("1234567A")).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> Validaciones.dni(null)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void elTelefonoSeNormalizaANueveDigitosAgrupados() {
        assertThat(Validaciones.telefono("987654321")).isEqualTo("987 654 321");
        assertThat(Validaciones.telefono("987-654-321")).isEqualTo("987 654 321");
        assertThat(Validaciones.telefono("+51 987 654 321")).isEqualTo("987 654 321");
    }

    @Test
    void rechazaTelefonosInvalidos() {
        assertThatThrownBy(() -> Validaciones.telefono("98765432")).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> Validaciones.telefono("887654321")).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> Validaciones.telefono("")).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void elProductoVacioUsaElNombreDeLaCategoria() {
        assertThat(Validaciones.producto("  ", "Spirulina")).isEqualTo("Spirulina");
        assertThat(Validaciones.producto(null, "Omega 3")).isEqualTo("Omega 3");
        assertThat(Validaciones.producto("  Calma   Kids ", "Flores de Bach")).isEqualTo("Calma Kids");
        assertThat(Validaciones.producto("Hipertensión (adultos)", "Homeopático")).isEqualTo("Hipertensión (adultos)");
        assertThatThrownBy(() -> Validaciones.producto("<script>", "X")).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void elNombreSeLimpiaYSeValida() {
        assertThat(Validaciones.nombrePersona("  Rosa   Martínez  López ")).isEqualTo("Rosa Martínez López");
        assertThatThrownBy(() -> Validaciones.nombrePersona("  ")).hasMessageContaining("obligatorios");
        assertThatThrownBy(() -> Validaciones.nombrePersona("Robert'); DROP TABLE cliente;--"))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> Validaciones.nombrePersona("A".repeat(101))).hasMessageContaining("100");
    }
}
