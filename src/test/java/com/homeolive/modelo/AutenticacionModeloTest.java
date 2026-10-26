package com.homeolive.modelo;

import com.homeolive.acceso.Rol;
import com.homeolive.acceso.UsuarioSesion;
import com.homeolive.dao.UsuarioDao;
import com.homeolive.modelo.AutenticacionModelo;
import com.homeolive.modelo.ControlIntentos;
import com.homeolive.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutenticacionModeloTest {

    /** Reloj que se puede adelantar para probar el vencimiento del bloqueo. */
    private static final class RelojMutable extends Clock {
        private Instant ahora = Instant.parse("2026-10-22T15:00:00Z");

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    @Mock private UsuarioDao usuarios;

    private final PasswordEncoder codificador = new BCryptPasswordEncoder();
    private RelojMutable reloj;
    private AutenticacionModelo modelo;
    private String hash;

    @BeforeEach
    void preparar() {
        reloj = new RelojMutable();
        hash = codificador.encode("Clave-Segura-1");
        modelo = new AutenticacionModelo(usuarios, codificador, new ControlIntentos(reloj));
        when(usuarios.buscarPorUsernameOCorreo("gestor")).thenReturn(Optional.of(
                new Usuario(4L, "gestor", "gestor@curenci.pe", hash, Rol.GESTOR_ENVIOS, 4L, "Jorge Palacios Nuñez")));
        when(usuarios.buscarPorUsernameOCorreo("gestor@curenci.pe")).thenReturn(Optional.of(
                new Usuario(4L, "gestor", "gestor@curenci.pe", hash, Rol.GESTOR_ENVIOS, 4L, "Jorge Palacios Nuñez")));
        when(usuarios.buscarPorUsernameOCorreo("nadie")).thenReturn(Optional.empty());
    }

    @Test
    void laContrasenaSeGuardaConHashYNoEnTextoVisible() {
        assertThat(hash).startsWith("$2").isNotEqualTo("Clave-Segura-1");
    }

    @Test
    void conCredencialesCorrectasDevuelveLosDatosDeSesionSinElHash() {
        UsuarioSesion sesion = modelo.autenticar("gestor", "Clave-Segura-1");
        assertThat(sesion.rol()).isEqualTo(Rol.GESTOR_ENVIOS);
        assertThat(sesion.idEmpleado()).isEqualTo(4L);
        assertThat(sesion.nombre()).isEqualTo("Jorge Palacios Nuñez");
        assertThat(sesion.toString()).doesNotContain(hash);
    }

    @Test
    void tambienSePuedeEntrarConElCorreoYSinImportarMayusculas() {
        assertThat(modelo.autenticar("  GESTOR@curenci.PE ", "Clave-Segura-1").username()).isEqualTo("gestor");
    }

    @Test
    void elMensajeEsElMismoSiFallaElUsuarioOLaClave() {
        assertThatThrownBy(() -> modelo.autenticar("gestor", "otra"))
                .hasMessage(AutenticacionModelo.MENSAJE_GENERICO);
        assertThatThrownBy(() -> modelo.autenticar("nadie", "Clave-Segura-1"))
                .hasMessage(AutenticacionModelo.MENSAJE_GENERICO);
        assertThatThrownBy(() -> modelo.autenticar("", "x")).hasMessage(AutenticacionModelo.MENSAJE_GENERICO);
        assertThatThrownBy(() -> modelo.autenticar("gestor", "")).hasMessage(AutenticacionModelo.MENSAJE_GENERICO);
    }

    @Test
    void tras5IntentosFallidosSeBloqueaAunqueLaClaveSeaCorrecta() {
        for (int i = 0; i < ControlIntentos.MAXIMO; i++) {
            assertThatThrownBy(() -> modelo.autenticar("gestor", "mala")).hasMessage(AutenticacionModelo.MENSAJE_GENERICO);
        }
        assertThatThrownBy(() -> modelo.autenticar("gestor", "Clave-Segura-1"))
                .hasMessage(AutenticacionModelo.MENSAJE_BLOQUEO);
    }

    @Test
    void elBloqueoVenceDespuesDeLaVentana() {
        for (int i = 0; i < ControlIntentos.MAXIMO; i++) {
            assertThatThrownBy(() -> modelo.autenticar("gestor", "mala"));
        }
        reloj.avanzar(Duration.ofMinutes(16));
        assertThat(modelo.autenticar("gestor", "Clave-Segura-1").rol()).isEqualTo(Rol.GESTOR_ENVIOS);
    }

    @Test
    void unIngresoCorrectoReiniciaElContadorDeFallos() {
        for (int i = 0; i < ControlIntentos.MAXIMO - 1; i++) {
            assertThatThrownBy(() -> modelo.autenticar("gestor", "mala"));
        }
        modelo.autenticar("gestor", "Clave-Segura-1");
        assertThatThrownBy(() -> modelo.autenticar("gestor", "mala")).hasMessage(AutenticacionModelo.MENSAJE_GENERICO);
        modelo.autenticar("gestor", "Clave-Segura-1");
    }
}
