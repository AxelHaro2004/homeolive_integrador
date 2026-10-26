package com.homeolive.dao;

import com.homeolive.acceso.Rol;
import com.homeolive.dao.jdbc.ClienteDaoJdbc;
import com.homeolive.dao.jdbc.EmpleadoDaoJdbc;
import com.homeolive.dao.jdbc.UsuarioDaoJdbc;
import com.homeolive.modelo.Cliente;
import com.homeolive.modelo.DatosCliente;
import com.homeolive.modelo.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@Import({ClienteDaoJdbc.class, UsuarioDaoJdbc.class, EmpleadoDaoJdbc.class})
class ClienteYUsuarioDaoJdbcTest {

    @Autowired private ClienteDao clientes;
    @Autowired private UsuarioDao usuarios;
    @Autowired private EmpleadoDao empleados;

    @Test
    void crudDeClientes() {
        long id = clientes.insertar(new DatosCliente("Ana Paredes", "70186324", "912 446 720", "Lima"));
        assertThat(clientes.buscarPorId(id)).contains(new Cliente(id, "Ana Paredes", "70186324", "912 446 720", "Lima"));

        assertThat(clientes.actualizar(id, new DatosCliente("Ana Lucía Paredes", "70186324", "912 446 721", "Provincia"))).isTrue();
        assertThat(clientes.buscarPorId(id).orElseThrow().ubicacion()).isEqualTo("Provincia");

        assertThat(clientes.eliminar(id)).isTrue();
        assertThat(clientes.buscarPorId(id)).isEmpty();
        assertThat(clientes.eliminar(id)).isFalse();
    }

    @Test
    void elDniEsUnicoEnLaBaseDeDatos() {
        clientes.insertar(new DatosCliente("Ana Paredes", "70186324", "912 446 720", "Lima"));
        assertThatThrownBy(() -> clientes.insertar(new DatosCliente("Otra Ana", "70186324", "912 446 799", "Lima")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void busquedaDeUsuarioPorNombreOCorreoSinDistinguirMayusculas() {
        long empleado = empleados.buscarIdPorDni("40125836").orElseThrow();
        usuarios.insertar("andrea", "Andrea@Curenci.pe", "$2a$10$hashdeprueba", Rol.VENDEDOR, empleado);

        Usuario porNombre = usuarios.buscarPorUsernameOCorreo("ANDREA").orElseThrow();
        assertThat(porNombre.rol()).isEqualTo(Rol.VENDEDOR);
        assertThat(porNombre.nombreEmpleado()).isEqualTo("Andrea Ramírez Soto");
        assertThat(usuarios.buscarPorUsernameOCorreo(" andrea@curenci.pe ")).isPresent();
        assertThat(usuarios.buscarPorUsernameOCorreo("nadie")).isEmpty();
        assertThat(usuarios.existeUsername("Andrea")).isTrue();
    }
}
