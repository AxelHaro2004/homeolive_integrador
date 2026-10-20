package com.homeolive.modelo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public record Pedido(
        String codigo,
        String venta,
        Cliente cliente,
        TipoEnvio envio,
        EstadoPedido estado,
        BigDecimal monto,
        String fecha,
        List<ItemPedido> items,
        String idCourier) {

    public String montoTexto() {
        return String.format(Locale.US, "S/ %.2f", monto);
    }
}
