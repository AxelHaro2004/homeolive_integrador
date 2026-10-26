package com.homeolive.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Datos ya validados que la capa DAO persiste como un pedido con sus ítems. */
public record NuevoPedido(String codigoPedido, String codigoVenta, long idCliente, String ubicacion,
                          TipoEnvio envio, EstadoPedido estado, BigDecimal monto, LocalDateTime fecha,
                          List<ItemPedido> items) {
}
