package com.homeolive.dao.jdbc;

import com.homeolive.dao.ClienteDao;
import com.homeolive.modelo.Cliente;
import com.homeolive.modelo.DatosCliente;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ClienteDaoJdbc implements ClienteDao {

    private static final String SELECT = """
            SELECT id_cliente, nombre_cliente, dni_cliente, telefono_cliente, ubicacion
            FROM cliente
            """;

    private static final RowMapper<Cliente> MAPA = (rs, n) -> new Cliente(
            rs.getLong("id_cliente"),
            rs.getString("nombre_cliente"),
            rs.getString("dni_cliente"),
            rs.getString("telefono_cliente"),
            rs.getString("ubicacion"));

    private final NamedParameterJdbcTemplate jdbc;

    public ClienteDaoJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Cliente> listar() {
        return jdbc.query(SELECT + " ORDER BY nombre_cliente, id_cliente", MAPA);
    }

    @Override
    public Optional<Cliente> buscarPorId(long id) {
        return jdbc.query(SELECT + " WHERE id_cliente = :id",
                new MapSqlParameterSource("id", id), MAPA).stream().findFirst();
    }

    @Override
    public Optional<Cliente> buscarPorDni(String dni) {
        return jdbc.query(SELECT + " WHERE dni_cliente = :dni",
                new MapSqlParameterSource("dni", dni), MAPA).stream().findFirst();
    }

    @Override
    public long insertar(DatosCliente d) {
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO cliente (nombre_cliente, telefono_cliente, dni_cliente, ubicacion)
                VALUES (:nombre, :telefono, :dni, :ubicacion)
                """, parametros(d), claves);
        return ClavesGeneradas.id(claves, "id_cliente");
    }

    @Override
    public boolean actualizar(long id, DatosCliente d) {
        return jdbc.update("""
                UPDATE cliente
                SET nombre_cliente = :nombre, telefono_cliente = :telefono,
                    dni_cliente = :dni, ubicacion = :ubicacion
                WHERE id_cliente = :id
                """, parametros(d).addValue("id", id)) > 0;
    }

    @Override
    public boolean eliminar(long id) {
        return jdbc.update("DELETE FROM cliente WHERE id_cliente = :id",
                new MapSqlParameterSource("id", id)) > 0;
    }

    private static MapSqlParameterSource parametros(DatosCliente d) {
        return new MapSqlParameterSource()
                .addValue("nombre", d.nombre())
                .addValue("telefono", d.telefono())
                .addValue("dni", d.dni())
                .addValue("ubicacion", d.ubicacion());
    }
}
