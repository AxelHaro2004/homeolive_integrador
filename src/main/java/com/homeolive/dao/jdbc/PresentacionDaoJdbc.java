package com.homeolive.dao.jdbc;

import com.homeolive.dao.PresentacionDao;
import com.homeolive.modelo.Presentacion;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PresentacionDaoJdbc implements PresentacionDao {

    /** {@code activa} es verdadera solo si la presentación y su categoría están activas. */
    private static final String SELECT = """
            SELECT cpr.id_presentacion, cpr.id_categoria, cp.nombre_categoria, cpr.presentacion,
                   cpr.unidad_medida, cpr.precio_unitario, (cpr.activo AND cp.activo) AS activa
            FROM categoria_presentacion cpr
            JOIN categoria_producto cp ON cp.id_categoria = cpr.id_categoria
            """;

    private static final RowMapper<Presentacion> MAPA = (rs, n) -> new Presentacion(
            rs.getLong("id_presentacion"),
            rs.getLong("id_categoria"),
            rs.getString("nombre_categoria"),
            rs.getInt("presentacion"),
            rs.getString("unidad_medida"),
            rs.getBigDecimal("precio_unitario"),
            rs.getBoolean("activa"));

    private final NamedParameterJdbcTemplate jdbc;

    public PresentacionDaoJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Presentacion> listarTodas() {
        return jdbc.query(SELECT + " ORDER BY cp.id_categoria, cpr.unidad_medida, cpr.presentacion, cpr.id_presentacion", MAPA);
    }

    @Override
    public Optional<Presentacion> buscarPorId(long id) {
        return jdbc.query(SELECT + " WHERE cpr.id_presentacion = :id", new MapSqlParameterSource("id", id), MAPA)
                .stream().findFirst();
    }

    @Override
    public Optional<Presentacion> buscarPorClave(long idCategoria, int valor, String unidad) {
        return jdbc.query(SELECT + " WHERE cpr.id_categoria = :categoria AND cpr.presentacion = :valor"
                        + " AND cpr.unidad_medida = :unidad",
                new MapSqlParameterSource().addValue("categoria", idCategoria).addValue("valor", valor)
                        .addValue("unidad", unidad), MAPA).stream().findFirst();
    }

    @Override
    public long insertar(long idCategoria, int valor, String unidad, BigDecimal precio) {
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO categoria_presentacion (id_categoria, presentacion, unidad_medida, precio_unitario)
                VALUES (:categoria, :valor, :unidad, :precio)
                """, new MapSqlParameterSource().addValue("categoria", idCategoria).addValue("valor", valor)
                .addValue("unidad", unidad).addValue("precio", precio), claves);
        return ClavesGeneradas.id(claves, "id_presentacion");
    }

    @Override
    public boolean actualizar(long id, int valor, String unidad, BigDecimal precio) {
        return jdbc.update("""
                UPDATE categoria_presentacion
                SET presentacion = :valor, unidad_medida = :unidad, precio_unitario = :precio
                WHERE id_presentacion = :id
                """, new MapSqlParameterSource().addValue("id", id).addValue("valor", valor)
                .addValue("unidad", unidad).addValue("precio", precio)) > 0;
    }

    @Override
    public boolean reactivar(long id, BigDecimal precio) {
        return jdbc.update("UPDATE categoria_presentacion SET activo = TRUE, precio_unitario = :precio WHERE id_presentacion = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("precio", precio)) > 0;
    }

    @Override
    public boolean desactivar(long id) {
        return jdbc.update("UPDATE categoria_presentacion SET activo = FALSE WHERE id_presentacion = :id",
                new MapSqlParameterSource("id", id)) > 0;
    }

    @Override
    public int desactivarPorCategoria(long idCategoria) {
        return jdbc.update("UPDATE categoria_presentacion SET activo = FALSE WHERE id_categoria = :categoria",
                new MapSqlParameterSource("categoria", idCategoria));
    }

    @Override
    public int contarUsosEnPedidos(long idPresentacion) {
        Integer total = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM detalle_pedido d
                JOIN medicina_talonario mt ON mt.id_medicina_talonario = d.id_medicina_talonario
                WHERE mt.id_presentacion = :id
                """, new MapSqlParameterSource("id", idPresentacion), Integer.class);
        return total == null ? 0 : total;
    }

    @Override
    public Map<Long, Integer> contarActivasPorCategoria() {
        Map<Long, Integer> totales = new LinkedHashMap<>();
        jdbc.query("SELECT id_categoria, COUNT(*) AS total FROM categoria_presentacion WHERE activo = TRUE GROUP BY id_categoria",
                rs -> {
                    totales.put(rs.getLong("id_categoria"), rs.getInt("total"));
                });
        return totales;
    }
}
