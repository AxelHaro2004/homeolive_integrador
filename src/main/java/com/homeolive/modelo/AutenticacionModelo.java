package com.homeolive.modelo;

import com.homeolive.acceso.UsuarioSesion;
import com.homeolive.dao.UsuarioDao;
import com.homeolive.modelo.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;


/**
 * Autenticación por usuario (o correo) y contraseña. Las contraseñas se comparan contra su hash BCrypt;
 * el mensaje de error es el mismo si falla el usuario o la clave, para no revelar qué cuentas existen.
 */
@Component
public class AutenticacionModelo {

    static final String MENSAJE_GENERICO = "Usuario o contraseña incorrectos.";
    static final String MENSAJE_BLOQUEO = "Demasiados intentos fallidos. Espera unos minutos e inténtalo de nuevo.";

    private final UsuarioDao usuarios;
    private final PasswordEncoder codificador;
    private final ControlIntentos intentos;
    /** Hash de relleno: permite gastar el mismo tiempo de cálculo aunque el usuario no exista. */
    private final String hashFicticio;

    public AutenticacionModelo(UsuarioDao usuarios, PasswordEncoder codificador, ControlIntentos intentos) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.intentos = intentos;
        this.hashFicticio = codificador.encode("clave-inexistente");
    }

    public UsuarioSesion autenticar(String identificador, String clave) {
        String id = identificador == null ? "" : identificador.trim().toLowerCase(Locale.ROOT);
        if (id.isEmpty() || clave == null || clave.isEmpty()) {
            throw new ReglaNegocioException(MENSAJE_GENERICO);
        }
        if (intentos.estaBloqueado(id)) {
            throw new ReglaNegocioException(MENSAJE_BLOQUEO);
        }

        Optional<Usuario> encontrado = usuarios.buscarPorUsernameOCorreo(id);
        boolean correcto;
        if (encontrado.isPresent()) {
            correcto = codificador.matches(clave, encontrado.get().passwordHash());
        } else {
            codificador.matches(clave, hashFicticio);
            correcto = false;
        }
        if (!correcto) {
            intentos.registrarFallo(id);
            throw new ReglaNegocioException(MENSAJE_GENERICO);
        }

        intentos.limpiar(id);
        Usuario u = encontrado.get();
        return new UsuarioSesion(u.id(), u.idEmpleado(), u.nombreEmpleado(), u.username(), u.rol());
    }
}
