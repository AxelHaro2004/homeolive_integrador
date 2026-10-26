package com.homeolive.dao.jdbc;

import com.homeolive.acceso.Rol;
import com.homeolive.dao.UsuarioDao;
import com.homeolive.modelo.Usuario;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.Optional;

@Repository
public class UsuarioDaoJdbc implements UsuarioDao {

    private final NamedParameterJdbcTemplate jdbc;

    public UsuarioDaoJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Usuario> buscarPorUsernameOCorreo(String identificador) {
        String clave = identificador == null ? "" : identificador.trim().toLowerCase(Locale.ROOT);
        return jdbc.query("""
                SELECT u.id_usuario, u.username, u.correo_user, u.password, u.rol_sistema,
                       u.id_empleado, e.nombre_empleado
                FROM usuario u
                JOIN empleado e ON e.id_empleado = u.id_empleado
                WHERE LOWER(u.username) = :clave OR LOWER(u.correo_user) = :clave
                """, new MapSqlParameterSource("clave", clave), (rs, n) -> {
            String claveRol = rs.getString("rol_sistema");
            Rol rol = Rol.deClaveBd(claveRol)
                    .orElseThrow(() -> new IllegalStateException("Rol de sistema desconocido: " + claveRol));
            return new Usuario(
                    rs.getLong("id_usuario"),
                    rs.getString("username"),
                    rs.getString("correo_user"),
                    rs.getString("password"),
                    rol,
                    rs.getLong("id_empleado"),
                    rs.getString("nombre_empleado"));
        }).stream().findFirst();
    }

    @Override
    public boolean existeUsername(String username) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE LOWER(username) = :u",
                new MapSqlParameterSource("u", username.toLowerCase(Locale.ROOT)), Integer.class);
        return total != null && total > 0;
    }

    @Override
    public long insertar(String username, String correo, String passwordHash, Rol rol, long idEmpleado) {
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO usuario (username, password, rol_sistema, correo_user, id_empleado)
                VALUES (:username, :password, :rol, :correo, :empleado)
                """, new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("password", passwordHash)
                .addValue("rol", rol.claveBd())
                .addValue("correo", correo)
                .addValue("empleado", idEmpleado), claves);
        return ClavesGeneradas.id(claves, "id_usuario");
    }
}
