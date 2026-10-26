package com.homeolive.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Pedido verificado (PEDIDO_VERIFICADO). Cada pedido constituye una venta, por eso conserva también
 * el número de venta y el monto pagado.
 */
public record Pedido(
        long id,
        String codigo,
        String venta,
        Cliente cliente,
        String ubicacion,
        TipoEnvio envio,
        EstadoPedido estado,
        BigDecimal monto,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaActualizacion,
        List<ItemPedido> items,
        String idCourier) {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd MMM, HH:mm", new Locale("es", "PE"));
    private static final DateTimeFormatter CORTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String montoTexto() {
        return String.format(Locale.US, "S/ %.2f", monto);
    }

    /** Fecha de registro relativa: «Hoy, 09:42», «Ayer, 17:35», «05 jun, 16:20». */
    public String fecha() {
        return etiquetaRelativa(fechaRegistro);
    }

    /** Momento del último cambio de estado, con el mismo formato relativo. */
    public String fechaActualizacionTexto() {
        return etiquetaRelativa(fechaActualizacion);
    }

    public LocalDate fechaDia() {
        return fechaRegistro.toLocalDate();
    }

    public String fechaCorta() {
        return fechaRegistro.format(CORTA);
    }

    public int cantidadItems() {
        return items.stream().mapToInt(ItemPedido::cantidad).sum();
    }

    private static String etiquetaRelativa(LocalDateTime momento) {
        LocalDate dia = momento.toLocalDate();
        LocalDate hoy = LocalDate.now(Zona.LIMA);
        if (dia.equals(hoy)) {
            return "Hoy, " + momento.format(HORA);
        }
        if (dia.equals(hoy.minusDays(1))) {
            return "Ayer, " + momento.format(HORA);
        }
        return momento.format(DIA_MES);
    }
}
