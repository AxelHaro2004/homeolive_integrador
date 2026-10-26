package com.homeolive.modelo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Generación de los reportes .xlsx con Apache POI.
 *
 * <p>Dos libros: resumen de pedidos (una fila por pedido) y resumen de ventas
 * (una fila por venta, más hoja de ventas por categoría). Ambos aceptan un
 * periodo: por omisión los últimos 30 días, o el mes y año elegidos. Los datos
 * se consultan a través de {@link PedidoModelo} (capa DAO sobre H2).</p>
 */
@Component
public class ReporteModelo {

    private static final String EMPRESA = "Homeolive E.I.R.L.";
    private static final String SISTEMA = "Sistema web de gestión de pedidos y envíos";

    /** Reportes disponibles en el desplegable de exportación. */
    public enum Tipo {
        PEDIDOS("pedidos", "Resumen de pedidos", "reporte-pedidos"),
        VENTAS("ventas", "Resumen de ventas", "reporte-ventas");

        private final String clave;
        private final String titulo;
        private final String archivo;

        Tipo(String clave, String titulo, String archivo) {
            this.clave = clave;
            this.titulo = titulo;
            this.archivo = archivo;
        }

        public String clave() {
            return clave;
        }

        public String titulo() {
            return titulo;
        }

        public String archivo() {
            return archivo;
        }

        public static Tipo deClave(String clave) {
            if (clave != null) {
                for (Tipo tipo : values()) {
                    if (tipo.clave.equalsIgnoreCase(clave.trim())) {
                        return tipo;
                    }
                }
            }
            return PEDIDOS;
        }
    }

    private record Fila(String etiqueta, String extra, int cantidad, BigDecimal monto) {
    }

    private final PedidoModelo pedidos;

    public ReporteModelo(PedidoModelo pedidos) {
        this.pedidos = pedidos;
    }

    public byte[] generar(Tipo tipo, PeriodoReporte periodo) {
        List<Pedido> pedidos = filtrar(periodo, null);
        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            if (tipo == Tipo.VENTAS) {
                hojaVentas(libro.createSheet("Resumen de ventas"), libro, periodo, pedidos);
                hojaVentasPorCategoria(libro.createSheet("Ventas por categoría"), libro, pedidos);
            } else {
                hojaPedidos(libro.createSheet("Resumen de pedidos"), libro, periodo, pedidos);
                hojaPedidosPorEstado(libro.createSheet("Pedidos por estado"), libro, pedidos);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el reporte " + tipo.titulo(), e);
        }
    }

    public String nombreArchivo(Tipo tipo, PeriodoReporte periodo) {
        return tipo.archivo() + "-" + periodo.sufijoArchivo() + ".xlsx";
    }

    /* ------------------------------------------------------------------ */
    /* Hojas                                                              */
    /* ------------------------------------------------------------------ */

    private void hojaPedidos(Sheet hoja, Workbook libro, PeriodoReporte periodo, List<Pedido> pedidos) {
        Estilos est = new Estilos(libro);

        int fila = cabecera(hoja, est, "REPORTE DE PEDIDOS", periodo,
                "Detalle de los pedidos registrados en el periodo, con su estado actual.");
        fila = filtros(hoja, est, fila, pedidos);

        int inicio = fila;
        String[] titulos = {"N° pedido", "N° venta", "Fecha", "Hora", "Cliente", "DNI",
                "Ubicación", "Tipo de envío", "Ítems", "Total (S/)", "Estado", "ID courier"};
        for (int c = 0; c < titulos.length; c++) {
            hoja.createRow(inicio).createCell(c).setCellValue(titulos[c]);
        }
        estiloFila(hoja.getRow(inicio), est.encabezado, titulos.length);

        int r = inicio + 1;
        for (Pedido p : pedidos) {
            Row row = hoja.createRow(r++);
            row.createCell(0).setCellValue(p.codigo());
            row.createCell(1).setCellValue(p.venta());
            row.createCell(2).setCellValue(p.fechaDia().toString());
            row.createCell(3).setCellValue(p.fechaRegistro().toLocalTime().withNano(0).toString());
            row.createCell(4).setCellValue(p.cliente().nombre());
            row.createCell(5).setCellValue(p.cliente().dni());
            row.createCell(6).setCellValue(p.ubicacion());
            row.createCell(7).setCellValue(p.envio().etiqueta());
            row.createCell(8).setCellValue(p.cantidadItems());
            row.createCell(9).setCellValue(p.monto().doubleValue());
            row.createCell(10).setCellValue(p.estado().etiqueta());
            row.createCell(11).setCellValue(p.idCourier() == null ? "—" : p.idCourier());
            estiloFila(row, est.detalle, titulos.length);
            row.getCell(2).setCellStyle(est.fecha);
            row.getCell(8).setCellStyle(est.entero);
            row.getCell(9).setCellStyle(est.moneda);
        }

        int fin = Math.max(r - 1, inicio);
        hoja.setAutoFilter(new CellRangeAddress(inicio, fin, 0, titulos.length - 1));
        anchos(hoja, 12, 12, 12, 9, 26, 12, 11, 15, 8, 13, 15, 15);
        notas(hoja, est, fin + 2, "El estado corresponde a la etapa actual del pedido al momento de generar el reporte.");
    }

    private void hojaVentas(Sheet hoja, Workbook libro, PeriodoReporte periodo, List<Pedido> pedidos) {
        Estilos est = new Estilos(libro);

        int fila = cabecera(hoja, est, "REPORTE DE VENTAS", periodo,
                "Ventas registradas en el periodo. Cada pedido verificado constituye una venta.");
        fila = filtros(hoja, est, fila, pedidos);

        int inicio = fila;
        String[] titulos = {"N° venta", "N° pedido", "Fecha", "DNI", "Cliente",
                "Tipo de envío", "Estado", "Monto pagado (S/)"};
        for (int c = 0; c < titulos.length; c++) {
            hoja.createRow(inicio).createCell(c).setCellValue(titulos[c]);
        }
        estiloFila(hoja.getRow(inicio), est.encabezado, titulos.length);

        int r = inicio + 1;
        BigDecimal total = BigDecimal.ZERO;
        for (Pedido p : pedidos) {
            Row row = hoja.createRow(r++);
            row.createCell(0).setCellValue(p.venta());
            row.createCell(1).setCellValue(p.codigo());
            row.createCell(2).setCellValue(p.fechaDia().toString());
            row.createCell(3).setCellValue(p.cliente().dni());
            row.createCell(4).setCellValue(p.cliente().nombre());
            row.createCell(5).setCellValue(p.envio().etiqueta());
            row.createCell(6).setCellValue(p.estado().etiqueta());
            row.createCell(7).setCellValue(p.monto().doubleValue());
            estiloFila(row, est.detalle, titulos.length);
            row.getCell(2).setCellStyle(est.fecha);
            row.getCell(7).setCellStyle(est.moneda);
            total = total.add(p.monto());
        }

        Row totalRow = hoja.createRow(r);
        totalRow.createCell(0).setCellValue("TOTAL DEL PERIODO");
        totalRow.createCell(1).setCellValue(pedidos.size() + " ventas");
        totalRow.createCell(7).setCellValue(total.doubleValue());
        estiloFila(totalRow, est.total, titulos.length);
        totalRow.getCell(1).setCellStyle(est.total);
        totalRow.getCell(7).setCellStyle(est.totalMoneda);

        int fin = Math.max(r - 1, inicio);
        hoja.setAutoFilter(new CellRangeAddress(inicio, fin, 0, titulos.length - 1));
        anchos(hoja, 13, 13, 12, 12, 26, 15, 15, 18);
        notas(hoja, est, r + 2, "Monto pagado = monto_pago del pedido verificado, congelado al momento de la venta.");
    }

    private void hojaVentasPorCategoria(Sheet hoja, Workbook libro, List<Pedido> pedidos) {
        Estilos est = new Estilos(libro);
        hoja.createRow(0).createCell(0).setCellValue("VENTAS POR CATEGORÍA DE PRODUCTO");
        hoja.getRow(0).getCell(0).setCellStyle(est.titulo);
        hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        String[] titulos = {"Categoría", "Ítems vendidos", "Unidades", "Monto vendido (S/)", "Participación"};
        for (int c = 0; c < titulos.length; c++) {
            hoja.createRow(2).createCell(c).setCellValue(titulos[c]);
        }
        estiloFila(hoja.getRow(2), est.encabezado, titulos.length);

        Map<String, BigDecimal> montos = new LinkedHashMap<>();
        Map<String, Integer> items = new LinkedHashMap<>();
        Map<String, Integer> unidades = new LinkedHashMap<>();
        for (Pedido p : pedidos) {
            for (ItemPedido it : p.items()) {
                montos.merge(it.categoria(), it.subtotal(), BigDecimal::add);
                items.merge(it.categoria(), 1, Integer::sum);
                unidades.merge(it.categoria(), it.cantidad(), Integer::sum);
            }
        }
        BigDecimal total = montos.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Fila> filas = new ArrayList<>();
        montos.forEach((categoria, monto) -> filas.add(new Fila(categoria, null, unidades.get(categoria), monto)));
        filas.sort(Comparator.comparing(Fila::monto).reversed());

        int r = 3;
        for (Fila f : filas) {
            Row row = hoja.createRow(r++);
            row.createCell(0).setCellValue(f.etiqueta());
            row.createCell(1).setCellValue(items.get(f.etiqueta()));
            row.createCell(2).setCellValue(f.cantidad());
            row.createCell(3).setCellValue(f.monto().doubleValue());
            double parte = total.signum() == 0 ? 0
                    : f.monto().multiply(BigDecimal.valueOf(100))
                            .divide(total, 2, RoundingMode.HALF_UP).doubleValue() / 100;
            row.createCell(4).setCellValue(parte);
            estiloFila(row, est.detalle, titulos.length);
            row.getCell(1).setCellStyle(est.entero);
            row.getCell(2).setCellStyle(est.entero);
            row.getCell(3).setCellStyle(est.moneda);
            row.getCell(4).setCellStyle(est.porcentaje);
        }

        Row totalRow = hoja.createRow(r);
        totalRow.createCell(0).setCellValue("TOTAL");
        totalRow.createCell(1).setCellValue(items.values().stream().mapToInt(Integer::intValue).sum());
        totalRow.createCell(2).setCellValue(unidades.values().stream().mapToInt(Integer::intValue).sum());
        totalRow.createCell(3).setCellValue(total.doubleValue());
        totalRow.createCell(4).setCellValue(total.signum() == 0 ? 0 : 1);
        estiloFila(totalRow, est.total, titulos.length);
        totalRow.getCell(1).setCellStyle(est.total);
        totalRow.getCell(2).setCellStyle(est.total);
        totalRow.getCell(3).setCellStyle(est.totalMoneda);
        totalRow.getCell(4).setCellStyle(est.totalPorcentaje);

        anchos(hoja, 26, 15, 12, 20, 15);
        notas(hoja, est, r + 2,
                "El monto por categoría se calcula sumando cantidad × precio_unitario_venta de cada detalle del pedido.",
                "Participación = monto de la categoría / monto total vendido en el periodo.");
    }

    private void hojaPedidosPorEstado(Sheet hoja, Workbook libro, List<Pedido> pedidos) {
        Estilos est = new Estilos(libro);
        hoja.createRow(0).createCell(0).setCellValue("PEDIDOS POR ESTADO Y TIPO DE ENVÍO");
        hoja.getRow(0).getCell(0).setCellStyle(est.titulo);
        hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        String[] titulos = {"Estado", "Pedidos", "Ítems", "Monto (S/)", "Participación"};
        for (int c = 0; c < titulos.length; c++) {
            hoja.createRow(2).createCell(c).setCellValue(titulos[c]);
        }
        estiloFila(hoja.getRow(2), est.encabezado, titulos.length);

        int r = 3;
        BigDecimal total = pedidos.stream().map(Pedido::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        for (EstadoPedido estado : EstadoPedido.values()) {
            List<Pedido> delEstado = pedidos.stream().filter(p -> p.estado() == estado).toList();
            BigDecimal monto = delEstado.stream().map(Pedido::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
            Row row = hoja.createRow(r++);
            row.createCell(0).setCellValue(estado.etiqueta());
            row.createCell(1).setCellValue(delEstado.size());
            row.createCell(2).setCellValue(delEstado.stream().mapToInt(Pedido::cantidadItems).sum());
            row.createCell(3).setCellValue(monto.doubleValue());
            double parte = total.signum() == 0 ? 0
                    : monto.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP).doubleValue() / 100;
            row.createCell(4).setCellValue(parte);
            estiloFila(row, est.detalle, titulos.length);
            row.getCell(1).setCellStyle(est.entero);
            row.getCell(2).setCellStyle(est.entero);
            row.getCell(3).setCellStyle(est.moneda);
            row.getCell(4).setCellStyle(est.porcentaje);
        }

        Row totalRow = hoja.createRow(r++);
        totalRow.createCell(0).setCellValue("TOTAL");
        totalRow.createCell(1).setCellValue(pedidos.size());
        totalRow.createCell(2).setCellValue(pedidos.stream().mapToInt(Pedido::cantidadItems).sum());
        totalRow.createCell(3).setCellValue(total.doubleValue());
        totalRow.createCell(4).setCellValue(total.signum() == 0 ? 0 : 1);
        estiloFila(totalRow, est.total, titulos.length);
        totalRow.getCell(1).setCellStyle(est.total);
        totalRow.getCell(2).setCellStyle(est.total);
        totalRow.getCell(3).setCellStyle(est.totalMoneda);
        totalRow.getCell(4).setCellStyle(est.totalPorcentaje);

        r += 1;
        Row sub = hoja.createRow(r++);
        sub.createCell(0).setCellValue("Tipo de envío");
        sub.createCell(1).setCellValue("Pedidos");
        sub.createCell(3).setCellValue("Monto (S/)");
        estiloFila(sub, est.encabezado, 4);
        for (TipoEnvio envio : TipoEnvio.values()) {
            List<Pedido> delTipo = pedidos.stream().filter(p -> p.envio() == envio).toList();
            Row row = hoja.createRow(r++);
            row.createCell(0).setCellValue(envio.etiqueta());
            row.createCell(1).setCellValue(delTipo.size());
            row.createCell(3).setCellValue(delTipo.stream().map(Pedido::monto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue());
            estiloFila(row, est.detalle, 4);
            row.getCell(1).setCellStyle(est.entero);
            row.getCell(3).setCellStyle(est.moneda);
        }

        anchos(hoja, 26, 12, 12, 18, 15);
        notas(hoja, est, r + 1, "Distribución de los pedidos registrados en el periodo según su estado y tipo de envío.");
    }

    /* ------------------------------------------------------------------ */
    /* Utilidades de maquetado                                            */
    /* ------------------------------------------------------------------ */

    private List<Pedido> filtrar(PeriodoReporte periodo, EstadoPedido estado) {
        return pedidos.listarPorPeriodo(periodo.desde(), periodo.hasta()).stream()
                .filter(p -> estado == null || p.estado() == estado)
                .toList();
    }

    private int cabecera(Sheet hoja, Estilos est, String titulo, PeriodoReporte periodo, String descripcion) {
        hoja.createRow(0).createCell(0).setCellValue(EMPRESA);
        hoja.getRow(0).getCell(0).setCellStyle(est.empresa);
        hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

        hoja.createRow(1).createCell(0).setCellValue(titulo);
        hoja.getRow(1).getCell(0).setCellStyle(est.titulo);
        hoja.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));

        hoja.createRow(2).createCell(0).setCellValue(periodo.etiqueta());
        hoja.getRow(2).getCell(0).setCellStyle(est.subtitulo);
        hoja.addMergedRegion(new CellRangeAddress(2, 2, 0, 7));

        hoja.createRow(3).createCell(0).setCellValue(descripcion);
        hoja.getRow(3).getCell(0).setCellStyle(est.nota);
        hoja.addMergedRegion(new CellRangeAddress(3, 3, 0, 7));

        hoja.createRow(4).createCell(0).setCellValue("Periodo del reporte: "
                + periodo.desde() + " a " + periodo.hasta()
                + "   ·   Generado: " + LocalDate.now() + "   ·   " + SISTEMA);
        hoja.getRow(4).getCell(0).setCellStyle(est.nota);
        hoja.addMergedRegion(new CellRangeAddress(4, 4, 0, 7));

        return 6;
    }

    private int filtros(Sheet hoja, Estilos est, int fila, List<Pedido> pedidos) {
        BigDecimal total = pedidos.stream().map(Pedido::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        Row row = hoja.createRow(fila);
        row.createCell(0).setCellValue("Registros del periodo");
        row.createCell(2).setCellValue(pedidos.size());
        row.createCell(4).setCellValue("Monto total (S/)");
        row.createCell(6).setCellValue(total.doubleValue());
        estiloFila(row, est.resumen, 7);
        row.getCell(4).setCellStyle(est.resumen);
        row.getCell(6).setCellStyle(est.resumenMoneda);
        hoja.addMergedRegion(new CellRangeAddress(fila, fila, 0, 1));
        hoja.addMergedRegion(new CellRangeAddress(fila, fila, 2, 3));
        hoja.addMergedRegion(new CellRangeAddress(fila, fila, 4, 5));
        hoja.addMergedRegion(new CellRangeAddress(fila, fila, 6, 7));
        return fila + 2;
    }

    private void notas(Sheet hoja, Estilos est, int fila, String... lineas) {
        for (String linea : lineas) {
            Row row = hoja.createRow(fila++);
            Cell celda = row.createCell(0);
            celda.setCellValue(linea);
            celda.setCellStyle(est.nota);
            hoja.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, 7));
        }
    }

    private void estiloFila(Row row, CellStyle estilo, int columnas) {
        for (int c = 0; c < columnas; c++) {
            Cell celda = row.getCell(c);
            if (celda == null) {
                celda = row.createCell(c);
            }
            celda.setCellStyle(estilo);
        }
    }

    private void anchos(Sheet hoja, int... anchos) {
        for (int i = 0; i < anchos.length; i++) {
            hoja.setColumnWidth(i, anchos[i] * 256);
        }
    }

    private static String formato(BigDecimal valor) {
        return String.format(Locale.US, "S/ %.2f", valor);
    }

    /** Estilos del libro, creados una sola vez por hoja. */
    private static final class Estilos {

        private final CellStyle empresa;
        private final CellStyle titulo;
        private final CellStyle subtitulo;
        private final CellStyle nota;
        private final CellStyle encabezado;
        private final CellStyle detalle;
        private final CellStyle total;
        private final CellStyle totalMoneda;
        private final CellStyle totalPorcentaje;
        private final CellStyle resumen;
        private final CellStyle resumenMoneda;
        private final CellStyle moneda;
        private final CellStyle entero;
        private final CellStyle fecha;
        private final CellStyle porcentaje;

        private Estilos(Workbook libro) {
            empresa = texto(libro, 11, true, "1F3B4D", HorizontalAlignment.LEFT, null, false);
            titulo = texto(libro, 16, true, "005A87", HorizontalAlignment.LEFT, null, false);
            subtitulo = texto(libro, 11, true, "1E8BC3", HorizontalAlignment.LEFT, null, false);
            nota = texto(libro, 9, false, "7A8F9C", HorizontalAlignment.LEFT, null, false);

            encabezado = texto(libro, 10, true, "FFFFFF", HorizontalAlignment.LEFT, IndexedColors.DARK_BLUE, true);
            detalle = texto(libro, 10, false, "2F4C5E", HorizontalAlignment.LEFT, null, true);
            total = texto(libro, 10, true, "005A87", HorizontalAlignment.LEFT, IndexedColors.PALE_BLUE, true);
            resumen = texto(libro, 10, true, "0B4C6E", HorizontalAlignment.LEFT, IndexedColors.PALE_BLUE, true);

            moneda = libro.createCellStyle();
            moneda.cloneStyleFrom(detalle);
            moneda.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));

            entero = libro.createCellStyle();
            entero.cloneStyleFrom(detalle);
            entero.setAlignment(HorizontalAlignment.CENTER);

            fecha = libro.createCellStyle();
            fecha.cloneStyleFrom(detalle);
            fecha.setAlignment(HorizontalAlignment.CENTER);

            porcentaje = libro.createCellStyle();
            porcentaje.cloneStyleFrom(detalle);
            porcentaje.setDataFormat(libro.createDataFormat().getFormat("0.0%"));

            totalMoneda = libro.createCellStyle();
            totalMoneda.cloneStyleFrom(total);
            totalMoneda.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));

            totalPorcentaje = libro.createCellStyle();
            totalPorcentaje.cloneStyleFrom(total);
            totalPorcentaje.setDataFormat(libro.createDataFormat().getFormat("0.0%"));

            resumenMoneda = libro.createCellStyle();
            resumenMoneda.cloneStyleFrom(resumen);
            resumenMoneda.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));
        }

        private static short colorIndexado(String hex) {
            IndexedColors color = switch (hex.toUpperCase(Locale.ROOT)) {
                case "FFFFFF" -> IndexedColors.WHITE;
                case "005A87" -> IndexedColors.DARK_BLUE;
                case "1E8BC3" -> IndexedColors.BLUE;
                case "0B4C6E" -> IndexedColors.DARK_TEAL;
                case "2F4C5E" -> IndexedColors.DARK_TEAL;
                default -> IndexedColors.GREY_80_PERCENT;
            };
            return color.getIndex();
        }

        private static CellStyle texto(Workbook libro, int tam, boolean negrita, String color,
                                       HorizontalAlignment alineacion, IndexedColors fondo, boolean borde) {
            CellStyle estilo = libro.createCellStyle();
            Font fuente = libro.createFont();
            fuente.setFontName("Calibri");
            fuente.setFontHeightInPoints((short) tam);
            fuente.setBold(negrita);
            fuente.setColor(colorIndexado(color));
            estilo.setFont(fuente);
            estilo.setAlignment(alineacion);
            estilo.setVerticalAlignment(VerticalAlignment.CENTER);
            if (fondo != null) {
                estilo.setFillForegroundColor(fondo.getIndex());
                estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            if (borde) {
                estilo.setBorderBottom(BorderStyle.THIN);
                estilo.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            }
            return estilo;
        }
    }
}
