package com.homeolive.dao.inicializacion;

import com.homeolive.acceso.Rol;
import com.homeolive.dao.EmpleadoDao;
import com.homeolive.dao.UsuarioDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea al iniciar las cuentas de acceso de los empleados cargados en data.sql, guardando la contraseña
 * con hash BCrypt. La contraseña inicial se toma de la configuración ({@code homeolive.demo.password}),
 * no está escrita en el código.
 */
@Component
@Order(1)
public class InicializadorUsuarios implements ApplicationRunner {

    private record Cuenta(String username, String correo, Rol rol, String dniEmpleado) {
    }

    private static final Cuenta[] CUENTAS = {
            new Cuenta("vendedor", "vendedor@curenci.pe", Rol.VENDEDOR, "40125836"),
            new Cuenta("finanzas", "finanzas@curenci.pe", Rol.AGENTE_FINANZAS, "41236947"),
            new Cuenta("quimico", "quimico@curenci.pe", Rol.QUIMICO_FARMACEUTICO, "42347058"),
            new Cuenta("gestor", "gestor@curenci.pe", Rol.GESTOR_ENVIOS, "43458169"),
            new Cuenta("admin", "admin@curenci.pe", Rol.ADMINISTRADOR, "44569270"),
    };

    private final UsuarioDao usuarios;
    private final EmpleadoDao empleados;
    private final PasswordEncoder codificador;
    private final String claveInicial;

    public InicializadorUsuarios(UsuarioDao usuarios, EmpleadoDao empleados, PasswordEncoder codificador,
                                 @Value("${homeolive.demo.password}") String claveInicial) {
        this.usuarios = usuarios;
        this.empleados = empleados;
        this.codificador = codificador;
        this.claveInicial = claveInicial;
    }

    @Override
    public void run(ApplicationArguments args) {
        String hash = codificador.encode(claveInicial);
        for (Cuenta c : CUENTAS) {
            if (usuarios.existeUsername(c.username())) {
                continue;
            }
            empleados.buscarIdPorDni(c.dniEmpleado())
                    .ifPresent(idEmpleado -> usuarios.insertar(c.username(), c.correo(), hash, c.rol(), idEmpleado));
        }
    }
}
