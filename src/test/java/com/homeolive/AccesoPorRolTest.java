package com.homeolive;

import com.homeolive.acceso.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccesoPorRolTest {

    @Autowired
    private MockMvc mvc;

    private MockHttpSession sesion(Rol rol) {
        MockHttpSession sesion = new MockHttpSession();
        sesion.setAttribute(Rol.SESION, rol);
        return sesion;
    }

    @Test
    void sinSesionRedirigeAlLogin() throws Exception {
        mvc.perform(get("/inicio"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void elVendedorSoloVeSuMenu() throws Exception {
        mvc.perform(get("/inicio").session(sesion(Rol.VENDEDOR)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("registro-pedidos")))
                .andExpect(content().string(containsString("clientes-pedidos")))
                .andExpect(content().string(not(containsString("admin-clientes"))))
                .andExpect(content().string(not(containsString("envios-pendientes"))));
    }

    @Test
    void elVendedorNoPuedeAbrirUnaPantallaDeAdministracion() throws Exception {
        mvc.perform(get("/admin-clientes").session(sesion(Rol.VENDEDOR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void elAdministradorSiPuedeAbrirAdminClientes() throws Exception {
        mvc.perform(get("/admin-clientes").session(sesion(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk());
    }

    @Test
    void soloElAdministradorVeElBotonExportar() throws Exception {
        mvc.perform(get("/resumen-ventas").session(sesion(Rol.AGENTE_FINANZAS)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Exportar reporte"))));
        mvc.perform(get("/resumen-ventas").session(sesion(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Exportar reporte")));
    }
}
