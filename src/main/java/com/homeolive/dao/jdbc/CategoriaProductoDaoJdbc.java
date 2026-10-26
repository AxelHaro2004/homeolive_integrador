package com.homeolive.dao.jdbc;

import com.homeolive.dao.CategoriaProductoDao;
import com.homeolive.modelo.CategoriaProducto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CategoriaProductoDaoJdbc implements CategoriaProductoDao {

    private static final String SELECT = "SELECT id_categoria, nombre_categoria, activo FROM categoria_producto";

    private static final RowMapper<CategoriaProducto> MAPA = (rs, n) -> new CategoriaProducto(
            rs.getLong("id_categoria"), rs.getString("nombre_categoria"), rs.getBoolean("activo"));

    private final NamedParameterJdbcTemplate jdbc;

    public CategoriaProductoDaoJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CategoriaProducto> listarTodas() {
        return jdbc.query(SELECT + " ORDER BY id_categoria", MAPA);
    }

    @Override
    public Optional<CategoriaProducto> buscarPorId(long id) {
        return jdbc.query(SELECT + " WHERE id_categoria = :id", new MapSqlParameterSource("id", id), MAPA)
                .stream().findFirst();
    }

    @Override
    public long insertar(String nombre) {
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO categoria_producto (nombre_categoria) VALUES (:nombre)",
                new MapSqlParameterSource("nombre", nombre), claves);
        return ClavesGeneradas.id(claves, "id_categoria");
    }

    @Override
    public boolean actualizar(long id, String nombre) {
        return jdbc.update("UPDATE categoria_producto SET nombre_categoria = :nombre WHERE id_categoria = :id",
                new MapSqlParameterSource("id", id).addValue("nombre", nombre)) > 0;
    }

    @Override
    public boolean reactivar(long id, String nombre) {
        return jdbc.update("UPDATE categoria_producto SET nombre_categoria = :nombre, activo = TRUE WHERE id_categoria = :id",
                new MapSqlParameterSource("id", id).addValue("nombre", nombre)) > 0;
    }

    @Override
    public boolean desactivar(long id) {
        return jdbc.update("UPDATE categoria_producto SET activo = FALSE WHERE id_categoria = :id",
                new MapSqlParameterSource("id", id)) > 0;
    }

    @Override
    public Map<Long, Integer> productosPorCategoria() {
        Map<Long, Integer> totales = new LinkedHashMap<>();
        jdbc.query("""
                SELECT cpr.id_categoria, COUNT(DISTINCT mt.id_producto) AS productos
                FROM categoria_presentacion cpr
                JOIN medicina_talonario mt ON mt.id_presentacion = cpr.id_presentacion
                GROUP BY cpr.id_categoria
                """, rs -> {
            totales.put(rs.getLong("id_categoria"), rs.getInt("productos"));
        });
        return totales;
    }

    @Override
    public int totalProductosAsociados() {
        Integer total = jdbc.getJdbcTemplate()
                .queryForObject("SELECT COUNT(DISTINCT id_producto) FROM medicina_talonario", Integer.class);
        return total == null ? 0 : total;
    }
}
