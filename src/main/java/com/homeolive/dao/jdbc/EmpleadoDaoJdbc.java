package com.homeolive.dao.jdbc;

import com.homeolive.dao.EmpleadoDao;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.OptionalLong;

@Repository
public class EmpleadoDaoJdbc implements EmpleadoDao {

    private final NamedParameterJdbcTemplate jdbc;

    public EmpleadoDaoJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public OptionalLong buscarIdPorDni(String dni) {
        List<Long> ids = jdbc.queryForList("SELECT id_empleado FROM empleado WHERE dni_empleado = :dni",
                new MapSqlParameterSource("dni", dni), Long.class);
        return ids.isEmpty() ? OptionalLong.empty() : OptionalLong.of(ids.get(0));
    }
}
