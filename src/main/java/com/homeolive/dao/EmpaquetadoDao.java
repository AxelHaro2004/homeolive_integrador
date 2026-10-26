package com.homeolive.dao;

import com.homeolive.modelo.TipoEnvio;

import java.time.LocalDateTime;

/** Acceso a datos de EMPAQUETADO (courier, id_pedido_courier y empleado responsable). */
public interface EmpaquetadoDao {

    void registrar(long idPedido, TipoEnvio courier, String idPedidoCourier, long idEmpleado, LocalDateTime fecha);

    void eliminarPorPedido(long idPedido);
}
