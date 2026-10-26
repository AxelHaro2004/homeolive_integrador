package com.homeolive.controlador;

import com.homeolive.acceso.Rol;
import com.homeolive.dao.PresentacionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de integración: arranca la aplicación completa (controladores, Model, DAO y la
 * base H2 en memoria con sus datos) e inicia sesión de verdad con usuario y contraseña. Cada prueba se
 * ejecuta en una transacción que se revierte al terminar, así no se contaminan entre sí.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class PruebaIntegracion {

    @Autowired protected MockMvc mvc;
    @Autowired protected PresentacionDao presentacionesDao;
    @Value("${homeolive.demo.password}") private String claveDemo;

    protected MockHttpSession entrarComo(Rol rol) throws Exception {
        String usuario = switch (rol) {
            case VENDEDOR -> "vendedor";
            case AGENTE_FINANZAS -> "finanzas";
            case QUIMICO_FARMACEUTICO -> "quimico";
            case GESTOR_ENVIOS -> "gestor";
            case ADMINISTRADOR -> "admin";
        };
        MvcResult resultado = mvc.perform(post("/login").param("usuario", usuario).param("clave", claveDemo))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    /** Id de una presentación del catálogo (por ejemplo, «Flores de Bach» · «30 ml»). */
    protected long idPresentacion(String categoria, String etiqueta) {
        return presentacionesDao.listarTodas().stream()
                .filter(p -> p.categoria().equals(categoria) && p.etiqueta().equals(etiqueta))
                .findFirst().orElseThrow().id();
    }
}
