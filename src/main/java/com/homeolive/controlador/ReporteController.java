package com.homeolive.controlador;

import com.homeolive.acceso.UsuarioSesion;
import com.homeolive.modelo.PeriodoReporte;
import com.homeolive.modelo.ReporteModelo;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Descarga de los reportes .xlsx (Apache POI).
 *
 * <p>Sin parámetros entrega los últimos 30 días; con {@code mes} y {@code anio} entrega el mes elegido
 * en el desplegable. Solo el administrador puede exportar.</p>
 */
@RestController
public class ReporteController {

    private final ReporteModelo reportes;
    private final Clock reloj;

    public ReporteController(ReporteModelo reportes, Clock reloj) {
        this.reportes = reportes;
        this.reloj = reloj;
    }

    @GetMapping("/reportes/exportar")
    public void exportar(@RequestParam(name = "tipo", defaultValue = "pedidos") String tipo,
                         @RequestParam(name = "mes", required = false) Integer mes,
                         @RequestParam(name = "anio", required = false) Integer anio,
                         HttpSession sesion,
                         HttpServletResponse respuesta) throws IOException {
        UsuarioSesion usuario = UsuarioSesion.de(sesion);
        if (usuario == null) {
            respuesta.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (!usuario.rol().puedeExportar()) {
            respuesta.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        ReporteModelo.Tipo tipoReporte = ReporteModelo.Tipo.deClave(tipo);
        PeriodoReporte periodo = PeriodoReporte.desdeParametros(mes, anio, LocalDate.now(reloj));
        byte[] archivo = reportes.generar(tipoReporte, periodo);
        String nombre = reportes.nombreArchivo(tipoReporte, periodo);

        respuesta.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        respuesta.setContentLength(archivo.length);
        respuesta.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(nombre, StandardCharsets.UTF_8)
                .build()
                .toString());
        respuesta.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        respuesta.getOutputStream().write(archivo);
        respuesta.flushBuffer();
    }
}
