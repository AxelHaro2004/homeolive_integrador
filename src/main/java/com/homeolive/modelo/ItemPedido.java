package com.homeolive.modelo;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Ítem de un pedido (DETALLE_PEDIDO): categoría, producto, presentación, cantidad y el precio
 * unitario de venta congelado al momento de la operación.
 */
public record ItemPedido(long idMedicina, String categoria, String producto, String presentacion,
                         int cantidad, BigDecimal precioUnitario) {

    public BigDecimal subtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public String precioTexto() {
        return String.format(Locale.US, "S/ %.2f", precioUnitario);
    }

    public String subtotalTexto() {
        return String.format(Locale.US, "S/ %.2f", subtotal());
    }
}
