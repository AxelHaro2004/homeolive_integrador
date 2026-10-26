package com.homeolive.dao.jdbc;

import com.homeolive.dao.PedidoDao;
import com.homeolive.modelo.Cliente;
import com.homeolive.modelo.EstadoPedido;
import com.homeolive.modelo.ItemPedido;
import com.homeolive.modelo.NuevoPedido;
import com.homeolive.modelo.Pedido;
import com.homeolive.modelo.TipoEnvio;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DAO de pedidos con JDBC. Todos los valores viajan como parámetros enlazados (consultas
 * parametrizadas): nunca se concatena texto del usuario dentro del SQL.
 */
@Repository
public class PedidoDaoJdbc implements PedidoDao {

    /** Máximo de ids por cláusula IN al cargar los ítems. */
    private static final int LOTE_IN = 500;

    private static final String SELECT_PEDIDO = """
            SELECT p.id_pedido, p.codigo_pedido, p.codigo_venta, p.ubicacion_entrega, p.tipo_envio, p.estado,
                   p.monto_pago, p.fecha_verificacion, p.fecha_actualizacion,
                   c.id_cliente, c.nombre_cliente, c.dni_cliente, c.telefono_cliente, c.ubicacion,
                   (SELECT e.id_pedido_courier FROM empaquetado e
                     WHERE e.id_pedido = p.id_pedido
                     ORDER BY e.id_empaquetado DESC LIMIT 1) AS id_pedido_courier
            FROM pedido_verificado p
            JOIN cliente c ON c.id_cliente = p.id_cliente
            """;

    private static final String SELECT_ITEMS = """
            SELECT d.id_pedido, d.id_medicina_talonario, cp.nombre_categoria, pr.nombre_producto,
                   CONCAT(cpr.presentacion, ' ', cpr.unidad_medida) AS presentacion,
                   d.cantidad, d.precio_unitario_venta
            FROM detalle_pedido d
            JOIN medicina_talonario mt ON mt.id_medicina_talonario = d.id_medicina_talonario
            JOIN producto pr ON pr.id_producto = mt.id_producto
            JOIN categoria_presentacion cpr ON cpr.id_presentacion = mt.id_presentacion
            JOIN categoria_producto cp ON cp.id_categoria = cpr.id_categoria
            WHERE d.id_pedido IN (:ids)
            ORDER BY d.id_detalle_pedido
            """;

    private static final String ORDEN_RECIENTES = "p.fecha_verificacion DESC, p.id_pedido DESC";

    private final NamedParameterJdbcTemplate jdbc;

    public PedidoDaoJdbc(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Fila del pedido sin sus ítems; se completa en {@link #consultar}. */
    private record Fila(long id, String codigo, String venta, Cliente cliente, String ubicacion, TipoEnvio envio,
                        EstadoPedido estado, BigDecimal monto, LocalDateTime registro,
                        LocalDateTime actualizacion, String idCourier) {

        Pedido aPedido(List<ItemPedido> items) {
            return new Pedido(id, codigo, venta, cliente, ubicacion, envio, estado, monto,
                    registro, actualizacion, items, idCourier);
        }
    }

    @Override
    public long insertar(NuevoPedido p) {
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO pedido_verificado
                    (codigo_pedido, codigo_venta, id_cliente, fecha_verificacion, fecha_actualizacion,
                     monto_pago, ubicacion_entrega, tipo_envio, estado)
                VALUES (:codigoPedido, :codigoVenta, :cliente, :fecha, :fecha, :monto, :ubicacion, :envio, :estado)
                """, new MapSqlParameterSource()
                .addValue("codigoPedido", p.codigoPedido())
                .addValue("codigoVenta", p.codigoVenta())
                .addValue("cliente", p.idCliente())
                .addValue("fecha", Timestamp.valueOf(p.fecha()))
                .addValue("monto", p.monto())
                .addValue("ubicacion", p.ubicacion())
                .addValue("envio", p.envio().name())
                .addValue("estado", p.estado().name()), claves);
        long idPedido = ClavesGeneradas.id(claves, "id_pedido");

        SqlParameterSource[] lotes = p.items().stream()
                .map(it -> new MapSqlParameterSource()
                        .addValue("pedido", idPedido)
                        .addValue("medicina", it.idMedicina())
                        .addValue("cantidad", it.cantidad())
                        .addValue("precio", it.precioUnitario()))
                .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate("""
                INSERT INTO detalle_pedido (id_pedido, id_medicina_talonario, cantidad, precio_unitario_venta)
                VALUES (:pedido, :medicina, :cantidad, :precio)
                """, lotes);
        return idPedido;
    }

    @Override
    public Optional<Pedido> buscarPorId(long id) {
        return consultar("p.id_pedido = :id", new MapSqlParameterSource("id", id), ORDEN_RECIENTES, 0)
                .stream().findFirst();
    }

    @Override
    public List<Pedido> listar() {
        return consultar("", new MapSqlParameterSource(), ORDEN_RECIENTES, 0);
    }

    @Override
    public List<Pedido> listarPorEstado(EstadoPedido estado) {
        // Los pendientes se atienden en orden de llegada: primero el más antiguo.
        return consultar("p.estado = :estado", new MapSqlParameterSource("estado", estado.name()),
                "p.fecha_verificacion ASC, p.id_pedido ASC", 0);
    }

    @Override
    public List<Pedido> listarPorCliente(long idCliente) {
        return consultar("p.id_cliente = :cliente", new MapSqlParameterSource("cliente", idCliente),
                ORDEN_RECIENTES, 0);
    }

    @Override
    public List<Pedido> listarPorPeriodo(LocalDate desde, LocalDate hasta) {
        return consultar("p.fecha_verificacion >= :desde AND p.fecha_verificacion < :hasta",
                new MapSqlParameterSource()
                        .addValue("desde", Timestamp.valueOf(desde.atStartOfDay()))
                        .addValue("hasta", Timestamp.valueOf(hasta.plusDays(1).atStartOfDay())),
                ORDEN_RECIENTES, 0);
    }

    @Override
    public List<Pedido> listarRecientes(int limite) {
        return consultar("", new MapSqlParameterSource(),
                "p.fecha_actualizacion DESC, p.id_pedido DESC", limite);
    }

    @Override
    public boolean cambiarEstado(long id, EstadoPedido desde, EstadoPedido hacia, LocalDateTime cuando) {
        return jdbc.update("""
                UPDATE pedido_verificado
                SET estado = :hacia, fecha_actualizacion = :cuando
                WHERE id_pedido = :id AND estado = :desde
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("desde", desde.name())
                .addValue("hacia", hacia.name())
                .addValue("cuando", Timestamp.valueOf(cuando))) > 0;
    }

    @Override
    public boolean actualizarAdmin(long id, long idCliente, String ubicacion, TipoEnvio envio, EstadoPedido estado,
                                   LocalDateTime fechaActualizacion) {
        return jdbc.update("""
                UPDATE pedido_verificado
                SET id_cliente = :cliente, ubicacion_entrega = :ubicacion, tipo_envio = :envio,
                    estado = :estado, fecha_actualizacion = :fecha
                WHERE id_pedido = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("cliente", idCliente)
                .addValue("ubicacion", ubicacion)
                .addValue("envio", envio.name())
                .addValue("estado", estado.name())
                .addValue("fecha", Timestamp.valueOf(fechaActualizacion))) > 0;
    }

    @Override
    public boolean eliminar(long id) {
        // Los ítems y el empaquetado se eliminan en cascada.
        return jdbc.update("DELETE FROM pedido_verificado WHERE id_pedido = :id",
                new MapSqlParameterSource("id", id)) > 0;
    }

    @Override
    public int contarPorCliente(long idCliente) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM pedido_verificado WHERE id_cliente = :cliente",
                new MapSqlParameterSource("cliente", idCliente), Integer.class);
        return total == null ? 0 : total;
    }

    @Override
    public Map<EstadoPedido, Integer> contarPorEstado() {
        Map<EstadoPedido, Integer> totales = new EnumMap<>(EstadoPedido.class);
        for (EstadoPedido estado : EstadoPedido.values()) {
            totales.put(estado, 0);
        }
        jdbc.query("SELECT estado, COUNT(*) AS total FROM pedido_verificado GROUP BY estado", rs -> {
            totales.put(EstadoPedido.valueOf(rs.getString("estado")), rs.getInt("total"));
        });
        return totales;
    }

    @Override
    public int contarEntregadosEntre(LocalDateTime desde, LocalDateTime hasta) {
        Integer total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM pedido_verificado
                WHERE estado = 'ENTREGADO' AND fecha_actualizacion >= :desde AND fecha_actualizacion < :hasta
                """, new MapSqlParameterSource()
                .addValue("desde", Timestamp.valueOf(desde))
                .addValue("hasta", Timestamp.valueOf(hasta)), Integer.class);
        return total == null ? 0 : total;
    }

    @Override
    public String siguienteCodigoPedido() {
        return "PED-" + (maximoNumerico("codigo_pedido", 1000) + 1);
    }

    @Override
    public String siguienteCodigoVenta() {
        return "VEN-" + (maximoNumerico("codigo_venta", 5000) + 1);
    }

    /** {@code columna} es siempre una constante de esta clase, nunca texto del usuario. */
    private int maximoNumerico(String columna, int base) {
        Integer max = jdbc.getJdbcTemplate().queryForObject(
                "SELECT COALESCE(MAX(CAST(SUBSTRING(" + columna + ", 5) AS INT)), " + base
                        + ") FROM pedido_verificado", Integer.class);
        return max == null ? base : max;
    }

    /**
     * Consulta de pedidos. {@code filtro} y {@code orden} son fragmentos fijos escritos en esta clase;
     * los valores variables siempre van en {@code parametros}. {@code limite = 0} significa sin límite.
     */
    private List<Pedido> consultar(String filtro, MapSqlParameterSource parametros, String orden, int limite) {
        String sql = SELECT_PEDIDO
                + (filtro.isEmpty() ? "" : " WHERE " + filtro)
                + " ORDER BY " + orden
                + (limite > 0 ? " LIMIT " + limite : "");
        List<Fila> filas = jdbc.query(sql, parametros, (rs, n) -> new Fila(
                rs.getLong("id_pedido"),
                rs.getString("codigo_pedido"),
                rs.getString("codigo_venta"),
                new Cliente(rs.getLong("id_cliente"), rs.getString("nombre_cliente"),
                        rs.getString("dni_cliente"), rs.getString("telefono_cliente"), rs.getString("ubicacion")),
                rs.getString("ubicacion_entrega"),
                TipoEnvio.valueOf(rs.getString("tipo_envio")),
                EstadoPedido.valueOf(rs.getString("estado")),
                rs.getBigDecimal("monto_pago"),
                rs.getTimestamp("fecha_verificacion").toLocalDateTime(),
                rs.getTimestamp("fecha_actualizacion").toLocalDateTime(),
                rs.getString("id_pedido_courier")));

        Map<Long, List<ItemPedido>> items = cargarItems(filas.stream().map(Fila::id).toList());
        return filas.stream().map(f -> f.aPedido(items.getOrDefault(f.id(), List.of()))).toList();
    }

    private Map<Long, List<ItemPedido>> cargarItems(List<Long> ids) {
        Map<Long, List<ItemPedido>> porPedido = new HashMap<>();
        for (int desde = 0; desde < ids.size(); desde += LOTE_IN) {
            List<Long> lote = ids.subList(desde, Math.min(desde + LOTE_IN, ids.size()));
            jdbc.query(SELECT_ITEMS, new MapSqlParameterSource("ids", lote), rs -> {
                porPedido.computeIfAbsent(rs.getLong("id_pedido"), k -> new ArrayList<>())
                        .add(new ItemPedido(
                                rs.getLong("id_medicina_talonario"),
                                rs.getString("nombre_categoria"),
                                rs.getString("nombre_producto"),
                                rs.getString("presentacion"),
                                rs.getInt("cantidad"),
                                rs.getBigDecimal("precio_unitario_venta")));
            });
        }
        return porPedido;
    }
}
