package com.homeolive.controlador;

import com.homeolive.acceso.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Autorización: cada rol solo entra a sus vistas, y las acciones POST tienen la misma protección. */
class AccesoPorRolTest extends PruebaIntegracion {

    @Test
    void elVendedorSoloVeSusVistas() throws Exception {
        MockHttpSession sesion = entrarComo(Rol.VENDEDOR);
        mvc.perform(get("/inicio").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("registro-pedidos")))
                .andExpect(content().string(containsString("clientes-pedidos")))
                .andExpect(content().string(not(containsString("admin-clientes"))))
                .andExpect(content().string(not(containsString("envios-pendientes"))));
        mvc.perform(get("/registro-pedidos").session(sesion)).andExpect(status().isOk());
        mvc.perform(get("/clientes-pedidos").session(sesion)).andExpect(status().isOk());
        mvc.perform(get("/admin-clientes").session(sesion)).andExpect(status().isForbidden());
        mvc.perform(get("/resumen-ventas").session(sesion)).andExpect(status().isForbidden());
    }

    @Test
    void lasAccionesDeEscrituraNoSePuedenSaltarseConUnPost() throws Exception {
        MockHttpSession vendedor = entrarComo(Rol.VENDEDOR);
        mvc.perform(post("/admin-clientes/1/eliminar").session(vendedor)).andExpect(status().isForbidden());
        mvc.perform(post("/admin-clientes").session(vendedor).param("nombre", "X")).andExpect(status().isForbidden());
        mvc.perform(post("/admin-pedidos/1/eliminar").session(vendedor)).andExpect(status().isForbidden());
        mvc.perform(post("/categorias-presentaciones/categorias/1/editar").session(vendedor).param("nombre", "X")).andExpect(status().isForbidden());
        mvc.perform(post("/categorias-presentaciones/presentaciones/1/eliminar").session(vendedor)).andExpect(status().isForbidden());
        mvc.perform(post("/envios-pendientes/1/entregado").session(vendedor)).andExpect(status().isForbidden());
        mvc.perform(post("/pedidos-pendientes/1/listo").session(vendedor)).andExpect(status().isForbidden());

        MockHttpSession quimico = entrarComo(Rol.QUIMICO_FARMACEUTICO);
        mvc.perform(post("/envios-pendientes/1/entregado").session(quimico)).andExpect(status().isForbidden());
        mvc.perform(post("/registro-pedidos").session(quimico)).andExpect(status().isForbidden());

        MockHttpSession gestor = entrarComo(Rol.GESTOR_ENVIOS);
        mvc.perform(post("/pedidos-pendientes/1/listo").session(gestor)).andExpect(status().isForbidden());
    }

    @Test
    void elAgenteDeFinanzasSoloConsultaLasVentas() throws Exception {
        MockHttpSession sesion = entrarComo(Rol.AGENTE_FINANZAS);
        mvc.perform(get("/resumen-ventas").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Exportar"))));
        mvc.perform(get("/resumen-pedidos").session(sesion)).andExpect(status().isForbidden());
        mvc.perform(get("/admin-pedidos").session(sesion)).andExpect(status().isForbidden());
        mvc.perform(get("/reportes/exportar").session(sesion)).andExpect(status().isForbidden());
    }

    @Test
    void elQuimicoSoloVeLosPedidosPendientes() throws Exception {
        MockHttpSession sesion = entrarComo(Rol.QUIMICO_FARMACEUTICO);
        mvc.perform(get("/pedidos-pendientes").session(sesion)).andExpect(status().isOk());
        mvc.perform(get("/envios-pendientes").session(sesion)).andExpect(status().isForbidden());
    }

    @Test
    void elGestorSoloVeLosEnvios() throws Exception {
        MockHttpSession sesion = entrarComo(Rol.GESTOR_ENVIOS);
        mvc.perform(get("/envios-pendientes").session(sesion)).andExpect(status().isOk());
        mvc.perform(get("/pedidos-pendientes").session(sesion)).andExpect(status().isForbidden());
    }

    @Test
    void elAdministradorAccedeATodasSusVistasYEsElUnicoQueExporta() throws Exception {
        MockHttpSession sesion = entrarComo(Rol.ADMINISTRADOR);
        for (String ruta : new String[]{"/inicio", "/admin-clientes", "/admin-pedidos", "/categorias-presentaciones",
                "/clientes-pedidos", "/resumen-pedidos", "/resumen-ventas"}) {
            mvc.perform(get(ruta).session(sesion)).andExpect(status().isOk());
        }
        mvc.perform(get("/resumen-ventas").session(sesion)).andExpect(content().string(containsString("Exportar")));
        mvc.perform(get("/registro-pedidos").session(sesion)).andExpect(status().isForbidden());
    }

    @Test
    void elCatalogoEnJsonEstaDisponibleParaCualquierUsuarioAutenticado() throws Exception {
        mvc.perform(get("/catalogo").session(entrarComo(Rol.VENDEDOR)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Flores de Bach")))
                .andExpect(content().string(containsString("30 ml")));
    }
}
