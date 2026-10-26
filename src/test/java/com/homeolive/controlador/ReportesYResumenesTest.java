package com.homeolive.controlador;

import com.homeolive.acceso.Rol;
import com.homeolive.modelo.PedidoModelo;
import com.homeolive.modelo.PeriodoReporte;
import com.homeolive.modelo.Zona;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Resúmenes filtrados por mes y año, y exportación de reportes. */
class ReportesYResumenesTest extends PruebaIntegracion {

    @Autowired private PedidoModelo pedidos;

    private final LocalDate hoy = LocalDate.now(Zona.LIMA);

    @Test
    void porOmisionElResumenMuestraElMesActualYNoHayEnlaceDeUltimos30Dias() throws Exception {
        mvc.perform(get("/resumen-pedidos").session(entrarComo(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PED-1048")))
                .andExpect(content().string(containsString("Exportar pedidos del mes")))
                .andExpect(content().string(not(containsString("Últimos 30 días"))));
        mvc.perform(get("/resumen-ventas").session(entrarComo(Rol.ADMINISTRADOR)))
                .andExpect(content().string(containsString("Exportar ventas del mes")))
                .andExpect(content().string(not(containsString("Últimos 30 días"))));
    }

    @Test
    void elMesYAnioElegidosFiltranLaListaDePedidos() throws Exception {
        int anio = hoy.getYear() - 2;
        mvc.perform(get("/resumen-pedidos").param("mes", "1").param("anio", String.valueOf(anio))
                        .session(entrarComo(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("PED-1048"))))
                .andExpect(content().string(containsString("No hay pedidos registrados en " + PeriodoReporte.MESES.get(0) + " " + anio)))
                .andExpect(content().string(containsString("0 pedidos en")));
    }

    @Test
    void elMesYAnioElegidosFiltranLaListaDeVentasYSuTotal() throws Exception {
        int anio = hoy.getYear() - 2;
        mvc.perform(get("/resumen-ventas").param("mes", "1").param("anio", String.valueOf(anio))
                        .session(entrarComo(Rol.AGENTE_FINANZAS)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("0 ventas en")))
                .andExpect(content().string(containsString("Total S/ 0.00")))
                .andExpect(content().string(not(containsString("Exportar"))));
    }

    @Test
    void unMesConPedidosListaSoloLosDeEsePeriodo() throws Exception {
        var delMes = pedidos.listarPorPeriodo(hoy.withDayOfMonth(1), hoy.withDayOfMonth(hoy.lengthOfMonth()));
        var primero = delMes.get(0);
        var otroMes = pedidos.listar().stream()
                .filter(p -> p.fechaDia().getMonthValue() != hoy.getMonthValue()
                        && p.fechaDia().getYear() >= hoy.getYear() - 2).findFirst().orElseThrow();

        mvc.perform(get("/resumen-pedidos").param("mes", String.valueOf(hoy.getMonthValue()))
                        .param("anio", String.valueOf(hoy.getYear())).session(entrarComo(Rol.ADMINISTRADOR)))
                .andExpect(content().string(containsString(primero.codigo())))
                .andExpect(content().string(not(containsString(otroMes.codigo()))));
        mvc.perform(get("/resumen-pedidos").param("mes", String.valueOf(otroMes.fechaDia().getMonthValue()))
                        .param("anio", String.valueOf(otroMes.fechaDia().getYear())).session(entrarComo(Rol.ADMINISTRADOR)))
                .andExpect(content().string(containsString(otroMes.codigo())));
    }

    @Test
    void parametrosInvalidosCaenAlMesActual() throws Exception {
        mvc.perform(get("/resumen-pedidos").param("mes", "99").param("anio", "1900").session(entrarComo(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PED-1048")));
    }

    @Test
    void elAdministradorDescargaElReporteDePedidosConLosDatosDeLaBase() throws Exception {
        MvcResult r = mvc.perform(get("/reportes/exportar").param("tipo", "pedidos").session(entrarComo(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("spreadsheetml")))
                .andReturn();

        long esperados = pedidos.listarPorPeriodo(hoy.minusDays(29), hoy).size();
        assertThat(esperados).isGreaterThan(0);

        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(r.getResponse().getContentAsByteArray()))) {
            long filasDePedido = 0;
            for (Sheet hoja : libro) {
                for (Row fila : hoja) {
                    Cell primera = fila.getCell(0);
                    if (primera != null && primera.getCellType() == CellType.STRING
                            && primera.getStringCellValue().startsWith("PED-")) {
                        filasDePedido++;
                    }
                }
            }
            assertThat(filasDePedido).isEqualTo(esperados);
        }
    }

    @Test
    void elBotonExportarUsaElMesYAnioSeleccionadosYSoloExportaElAdministrador() throws Exception {
        mvc.perform(get("/reportes/exportar").param("tipo", "ventas").param("mes", "3").param("anio", "2026")
                        .session(entrarComo(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")));
        mvc.perform(get("/reportes/exportar").param("tipo", "ventas").session(entrarComo(Rol.AGENTE_FINANZAS)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/reportes/exportar")).andExpect(redirectedUrl("/login"));
    }
}
