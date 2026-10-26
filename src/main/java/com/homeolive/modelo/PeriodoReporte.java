package com.homeolive.modelo;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Periodo cubierto por un reporte exportado.
 *
 * <p>Sin parámetros el reporte cubre los últimos 30 días; si el usuario elige un
 * mes y un año, el periodo va del primer al último día de ese mes.</p>
 */
public record PeriodoReporte(LocalDate desde, LocalDate hasta, String titulo, String etiqueta, boolean ultimos30Dias) {

    public static final List<String> MESES = List.of(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Setiembre", "Octubre", "Noviembre", "Diciembre");

    private static final DateTimeFormatter CORTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_MES = DateTimeFormatter.ofPattern("yyyy-MM");

    public static PeriodoReporte ultimos30Dias(LocalDate hoy) {
        LocalDate hasta = hoy;
        LocalDate desde = hasta.minusDays(29);
        return new PeriodoReporte(desde, hasta, "Últimos 30 días",
                "Del " + desde.format(CORTA) + " al " + hasta.format(CORTA) + " (últimos 30 días)",
                true);
    }

    public static PeriodoReporte delMes(int mes, int anio) {
        YearMonth periodo = YearMonth.of(anio, mes);
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();
        return new PeriodoReporte(desde, hasta, MESES.get(mes - 1) + " " + anio,
                "Del " + desde.format(CORTA) + " al " + hasta.format(CORTA),
                false);
    }

    /**
     * Arma el periodo a partir de los parámetros del formulario. Si falta el mes,
     * el año o no son válidos, se usa la ventana de los últimos 30 días.
     */
    public static PeriodoReporte desdeParametros(Integer mes, Integer anio, LocalDate hoy) {
        if (mes == null || anio == null || mes < 1 || mes > 12 || anio < 2000 || anio > 2100) {
            return ultimos30Dias(hoy);
        }
        return delMes(mes, anio);
    }

    public boolean contiene(Pedido pedido) {
        LocalDate dia = pedido.fechaDia();
        return !dia.isBefore(desde) && !dia.isAfter(hasta);
    }

    public String sufijoArchivo() {
        return ultimos30Dias ? "ultimos-30-dias" : desde.format(ISO_MES);
    }
}
