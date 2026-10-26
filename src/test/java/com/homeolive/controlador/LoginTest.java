package com.homeolive.controlador;

import com.homeolive.acceso.Rol;
import com.homeolive.acceso.UsuarioSesion;
import com.homeolive.controlador.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LoginTest extends PruebaIntegracion {

    @Autowired private JdbcTemplate jdbc;
    @Value("${homeolive.demo.password}") private String claveDemo;

    @Test
    void laPantallaDeLoginPideUsuarioYContrasenaSinElegirRol() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"usuario\"")))
                .andExpect(content().string(containsString("name=\"clave\"")))
                .andExpect(content().string(not(containsString("name=\"rol\""))));
    }

    @Test
    void conCredencialesCorrectasEntraConElRolDeSuCuenta() throws Exception {
        MvcResult r = mvc.perform(post("/login").param("usuario", "gestor").param("clave", claveDemo))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicio"))
                .andReturn();
        UsuarioSesion usuario = UsuarioSesion.de(r.getRequest().getSession(false));
        assertThat(usuario.rol()).isEqualTo(Rol.GESTOR_ENVIOS);
        assertThat(usuario.nombre()).isEqualTo("Jorge Palacios Nuñez");
    }

    @Test
    void tambienSePuedeEntrarConElCorreo() throws Exception {
        mvc.perform(post("/login").param("usuario", "ADMIN@curenci.pe").param("clave", claveDemo))
                .andExpect(redirectedUrl("/inicio"));
    }

    @Test
    void conClaveIncorrectaNoEntraYElMensajeNoRevelaQueFallo() throws Exception {
        MvcResult r = mvc.perform(post("/login").param("usuario", "admin").param("clave", "incorrecta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Usuario o contraseña incorrectos")))
                .andReturn();
        assertThat(UsuarioSesion.de(r.getRequest().getSession(false))).isNull();

        mvc.perform(post("/login").param("usuario", "no.existe").param("clave", claveDemo))
                .andExpect(content().string(containsString("Usuario o contraseña incorrectos")));
    }

    @Test
    void tras5FallosSeBloqueaElUsuario() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/login").param("usuario", "intruso").param("clave", "x" + i));
        }
        mvc.perform(post("/login").param("usuario", "intruso").param("clave", "otra"))
                .andExpect(content().string(containsString("Demasiados intentos")));
    }

    @Test
    void lasContrasenasSeGuardanConHashBcrypt() {
        java.util.List<String> claves = jdbc.queryForList("SELECT password FROM usuario", String.class);
        assertThat(claves).hasSize(5).allSatisfy(c -> assertThat(c).startsWith("$2").isNotEqualTo(claveDemo));
    }

    @Test
    void alCerrarSesionYaNoSePuedeEntrarALasVistas() throws Exception {
        MockHttpSession sesion = entrarComo(Rol.ADMINISTRADOR);
        mvc.perform(get("/admin-clientes").session(sesion)).andExpect(status().isOk());
        mvc.perform(get("/logout").session(sesion)).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/admin-clientes").session(sesion)).andExpect(redirectedUrl("/login"));
    }

    @Test
    void sinSesionTodoRedirigeAlLogin() throws Exception {
        mvc.perform(get("/inicio")).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/admin-clientes").param("nombre", "X")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/catalogo")).andExpect(redirectedUrl("/login"));
    }
}
