package com.homeolive.controlador;

import com.homeolive.acceso.Rol;
import com.homeolive.modelo.CategoriaProductoModelo;
import com.homeolive.modelo.Pedido;
import com.homeolive.modelo.PedidoModelo;
import com.homeolive.modelo.Presentacion;
import com.homeolive.modelo.PresentacionModelo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Vista categorias-presentaciones: crear, editar y eliminar (lógicamente) categorías y presentaciones. */
class CategoriasPresentacionesTest extends PruebaIntegracion {

    private static final String RUTA = "/categorias-presentaciones";

    @Autowired private CategoriaProductoModelo categorias;
    @Autowired private PresentacionModelo presentaciones;
    @Autowired private PedidoModelo pedidos;

    private long idCategoria(String nombre) {
        return categorias.listar("").stream().filter(c -> c.nombre().equals(nombre)).findFirst().orElseThrow().id();
    }

    @Test
    void laPaginaListaCategoriasYPresentacionesConSuPrecio() throws Exception {
        mvc.perform(get(RUTA).session(entrarComo(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Flores de Bach")))
                .andExpect(content().string(containsString("Nueva presentación")))
                .andExpect(content().string(containsString("30 ml")))
                .andExpect(content().string(containsString("S/ 42.00")));
        assertThat(categorias.listar("zzz")).isEmpty();
    }

    @Test
    void creaEditaYEliminaUnaCategoria() throws Exception {
        MockHttpSession admin = entrarComo(Rol.ADMINISTRADOR);
        mvc.perform(post(RUTA + "/categorias").session(admin).param("nombre", "Aceites Esenciales"))
                .andExpect(redirectedUrl(RUTA))
                .andExpect(flash().attribute("mensaje", containsString("creada")));
        long id = idCategoria("Aceites Esenciales");

        mvc.perform(post(RUTA + "/categorias/" + id + "/editar").session(admin).param("nombre", "Aceites"))
                .andExpect(flash().attribute("mensaje", containsString("actualizada")));
        assertThat(categorias.buscar(id).orElseThrow().nombre()).isEqualTo("Aceites");

        mvc.perform(post(RUTA + "/categorias/" + id + "/eliminar").session(admin))
                .andExpect(flash().attribute("mensaje", containsString("eliminada")));
        assertThat(categorias.buscar(id)).isEmpty();
    }

    @Test
    void noPermiteNombresVaciosNiRepetidosSinImportarTildes() throws Exception {
        MockHttpSession admin = entrarComo(Rol.ADMINISTRADOR);
        mvc.perform(post(RUTA + "/categorias").session(admin).param("nombre", "   "))
                .andExpect(flash().attribute("error", containsString("obligatorio")));
        mvc.perform(post(RUTA + "/categorias").session(admin).param("nombre", "homeopatico"))
                .andExpect(flash().attribute("error", containsString("Ya existe una categoría")));
    }

    @Test
    void eliminarUnaCategoriaEsLogicaNoSeOfreceMasPeroLosPedidosAntiguosLaConservan() throws Exception {
        MockHttpSession admin = entrarComo(Rol.ADMINISTRADOR);
        long id = idCategoria("Spirulina");
        assertThat(pedidos.listar().stream().flatMap(p -> p.items().stream()).anyMatch(i -> i.categoria().equals("Spirulina"))).isTrue();

        mvc.perform(post(RUTA + "/categorias/" + id + "/eliminar").session(admin))
                .andExpect(flash().attribute("mensaje", containsString("eliminada")));

        mvc.perform(get("/catalogo").session(entrarComo(Rol.VENDEDOR)))
                .andExpect(content().string(not(containsString("Spirulina"))));
        mvc.perform(get(RUTA).session(admin))
                .andExpect(content().string(not(containsString("CAT-" + String.format("%03d", id)))));
        // el historial sigue mostrando la categoría
        assertThat(pedidos.listar().stream().flatMap(p -> p.items().stream()).anyMatch(i -> i.categoria().equals("Spirulina"))).isTrue();
    }

    @Test
    void crearUnaCategoriaConElNombreDeUnaEliminadaLaReactiva() throws Exception {
        MockHttpSession admin = entrarComo(Rol.ADMINISTRADOR);
        long id = idCategoria("Omega 3");
        mvc.perform(post(RUTA + "/categorias/" + id + "/eliminar").session(admin));
        assertThat(categorias.buscar(id)).isEmpty();

        mvc.perform(post(RUTA + "/categorias").session(admin).param("nombre", "Omega 3"))
                .andExpect(flash().attribute("mensaje", containsString("creada")));
        assertThat(categorias.buscar(id)).isPresent();
    }

    @Test
    void creaUnaPresentacionYApareceAlVender() throws Exception {
        MockHttpSession admin = entrarComo(Rol.ADMINISTRADOR);
        mvc.perform(post(RUTA + "/presentaciones").session(admin).param("categoria", String.valueOf(idCategoria("Flores de Bach")))
                        .param("valor", "60").param("unidad", "ml").param("precio", "65.50"))
                .andExpect(flash().attribute("mensaje", containsString("60 ml")));

        mvc.perform(get("/catalogo").session(entrarComo(Rol.VENDEDOR)))
                .andExpect(content().string(containsString("60 ml")))
                .andExpect(content().string(containsString("65.5")));
        assertThat(presentaciones.listar()).anyMatch(p -> p.categoria().equals("Flores de Bach") && p.etiqueta().equals("60 ml"));
    }

    @Test
    void rechazaPresentacionesRepetidasOInvalidas() throws Exception {
        MockHttpSession admin = entrarComo(Rol.ADMINISTRADOR);
        String flores = String.valueOf(idCategoria("Flores de Bach"));
        mvc.perform(post(RUTA + "/presentaciones").session(admin).param("categoria", flores)
                        .param("valor", "30").param("unidad", "ml").param("precio", "40"))
                .andExpect(flash().attribute("error", containsString("Ya existe la presentación 30 ml")));
        mvc.perform(post(RUTA + "/presentaciones").session(admin).param("categoria", flores)
                        .param("valor", "30").param("unidad", "kg").param("precio", "40"))
                .andExpect(flash().attribute("error", containsString("unidad")));
        mvc.perform(post(RUTA + "/presentaciones").session(admin).param("categoria", flores)
                        .param("valor", "70").param("unidad", "ml").param("precio", "-1"))
                .andExpect(flash().attribute("error", containsString("mayor que 0")));
    }

    @Test
    void editaElPrecioPeroNoLaMedidaDeUnaPresentacionYaVendida() throws Exception {
        MockHttpSession admin = entrarComo(Rol.ADMINISTRADOR);
        Presentacion p = presentaciones.listar().stream()
                .filter(x -> x.categoria().equals("Flores de Bach") && x.etiqueta().equals("30 ml")).findFirst().orElseThrow();
        Pedido antiguo = pedidos.listar().stream()
                .filter(x -> x.items().stream().anyMatch(i -> i.categoria().equals("Flores de Bach"))).findFirst().orElseThrow();
        var precioAntes = antiguo.items().stream().filter(i -> i.categoria().equals("Flores de Bach")).findFirst().orElseThrow().precioUnitario();

        mvc.perform(post(RUTA + "/presentaciones/" + p.id() + "/editar").session(admin)
                        .param("valor", "30").param("unidad", "ml").param("precio", "99.00"))
                .andExpect(flash().attribute("mensaje", containsString("actualizada")));
        assertThat(presentaciones.buscar(p.id()).orElseThrow().precio()).isEqualByComparingTo("99.00");

        mvc.perform(post(RUTA + "/presentaciones/" + p.id() + "/editar").session(admin)
                        .param("valor", "50").param("unidad", "ml").param("precio", "99.00"))
                .andExpect(flash().attribute("error", containsString("solo puedes cambiar su precio")));

        // el precio congelado de los pedidos ya registrados no cambia
        Pedido despues = pedidos.obtener(antiguo.id());
        var precioDespues = despues.items().stream().filter(i -> i.categoria().equals("Flores de Bach")).findFirst().orElseThrow().precioUnitario();
        assertThat(precioDespues).isEqualByComparingTo(precioAntes);
    }

    @Test
    void unaPresentacionEliminadaNoSePuedeVenderAunqueSePostee() throws Exception {
        MockHttpSession admin = entrarComo(Rol.ADMINISTRADOR);
        long id = idPresentacion("Homeopático", "120 ml");
        mvc.perform(post(RUTA + "/presentaciones/" + id + "/eliminar").session(admin))
                .andExpect(flash().attribute("mensaje", containsString("eliminada")));

        mvc.perform(post("/registro-pedidos").session(entrarComo(Rol.VENDEDOR))
                        .param("nombre", "Rosa Martínez").param("dni", "60000099").param("telefono", "955123456")
                        .param("ubicacion", "Lima").param("envio", "Olva").param("pago", "true")
                        .param("presentacion", String.valueOf(id)).param("producto", "Hipertensión").param("cantidad", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("no existe en el catálogo")));
    }
}
