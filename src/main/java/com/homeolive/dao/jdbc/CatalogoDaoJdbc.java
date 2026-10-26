package com.homeolive.dao.jdbc;

import com.homeolive.dao.CatalogoDao;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CatalogoDaoJdbc implements CatalogoDao {

    private final NamedParameterJdbcTemplate jdbc;

    public CatalogoDaoJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long obtenerOCrearMedicina(String nombreProducto, long idPresentacion) {
        long idProducto = obtenerOCrearProducto(nombreProducto);
        Optional<Long> existente = buscarMedicina(idProducto, idPresentacion);
        if (existente.isPresent()) {
            return existente.get();
        }
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("producto", idProducto)
                .addValue("presentacion", idPresentacion);
        try {
            KeyHolder claves = new GeneratedKeyHolder();
            jdbc.update("INSERT INTO medicina_talonario (id_producto, id_presentacion) VALUES (:producto, :presentacion)",
                    p, claves);
            return ClavesGeneradas.id(claves, "id_medicina_talonario");
        } catch (DuplicateKeyException e) {
            // Otro registro simultáneo la creó primero.
            return buscarMedicina(idProducto, idPresentacion).orElseThrow(() -> e);
        }
    }

    private long obtenerOCrearProducto(String nombre) {
        Optional<Long> existente = buscarProducto(nombre);
        if (existente.isPresent()) {
            return existente.get();
        }
        try {
            KeyHolder claves = new GeneratedKeyHolder();
            jdbc.update("INSERT INTO producto (nombre_producto) VALUES (:nombre)",
                    new MapSqlParameterSource("nombre", nombre), claves);
            return ClavesGeneradas.id(claves, "id_producto");
        } catch (DuplicateKeyException e) {
            return buscarProducto(nombre).orElseThrow(() -> e);
        }
    }

    private Optional<Long> buscarProducto(String nombre) {
        List<Long> ids = jdbc.queryForList("SELECT id_producto FROM producto WHERE LOWER(nombre_producto) = LOWER(:nombre)",
                new MapSqlParameterSource("nombre", nombre), Long.class);
        return ids.stream().findFirst();
    }

    private Optional<Long> buscarMedicina(long idProducto, long idPresentacion) {
        List<Long> ids = jdbc.queryForList("""
                SELECT id_medicina_talonario FROM medicina_talonario
                WHERE id_producto = :producto AND id_presentacion = :presentacion
                """, new MapSqlParameterSource().addValue("producto", idProducto)
                .addValue("presentacion", idPresentacion), Long.class);
        return ids.stream().findFirst();
    }
}
