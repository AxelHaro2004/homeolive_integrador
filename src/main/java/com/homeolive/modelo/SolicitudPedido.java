package com.homeolive.modelo;

import java.util.List;

/**
 * Datos que el vendedor captura en registro-pedidos. {@code pagoConfirmado} refleja la regla del
 * negocio: el pedido solo se genera si el cliente ya realizó el pago.
 */
public record SolicitudPedido(String nombre, String dni, String telefono, String ubicacion, String envio,
                              boolean pagoConfirmado, List<Linea> lineas) {

    /** Una fila de ítem: presentación elegida (de una categoría), producto escrito por el vendedor y cantidad. */
    public record Linea(Long idPresentacion, String producto, int cantidad) {
    }
}
