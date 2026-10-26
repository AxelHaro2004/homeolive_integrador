package com.homeolive.dao.jdbc;

import com.homeolive.dao.EmpaquetadoDao;
import com.homeolive.modelo.TipoEnvio;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Repository
public class EmpaquetadoDaoJdbc implements EmpaquetadoDao {

    private final NamedParameterJdbcTemplate jdbc;

    public EmpaquetadoDaoJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void registrar(long idPedido, TipoEnvio courier, String idPedidoCourier, long idEmpleado,
                          LocalDateTime fecha) {
        jdbc.update("""
                INSERT INTO empaquetado (fecha_empaquetado, courier, id_pedido_courier, id_empleado, id_pedido)
                VALUES (:fecha, :courier, :idCourier, :empleado, :pedido)
                """, new MapSqlParameterSource()
                .addValue("fecha", Timestamp.valueOf(fecha))
                .addValue("courier", courier.name())
                .addValue("idCourier", idPedidoCourier)
                .addValue("empleado", idEmpleado)
                .addValue("pedido", idPedido));
    }

    @Override
    public void eliminarPorPedido(long idPedido) {
        jdbc.update("DELETE FROM empaquetado WHERE id_pedido = :pedido",
                new MapSqlParameterSource("pedido", idPedido));
    }
}
