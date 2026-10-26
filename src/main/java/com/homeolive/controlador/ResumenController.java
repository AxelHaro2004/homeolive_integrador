package com.homeolive.controlador;

import com.homeolive.acceso.Vista;
import com.homeolive.modelo.OpcionMes;
import com.homeolive.modelo.Pedido;
import com.homeolive.modelo.PedidoModelo;
import com.homeolive.modelo.PeriodoReporte;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Vistas resumen-ventas (finanzas y administrador) y resumen-pedidos (administrador): solo consulta.
 * El mes y el año elegidos filtran la lista y son los mismos que usa el botón de exportar.
 */
@Controller
public class ResumenController {

    private final PedidoModelo pedidos;
    private final Clock reloj;

    public ResumenController(PedidoModelo pedidos, Clock reloj) {
        this.pedidos = pedidos;
        this.reloj = reloj;
    }

    @GetMapping("/resumen-ventas")
    public String resumenVentas(@RequestParam(name = "mes", required = false) Integer mes,
                                @RequestParam(name = "anio", required = false) Integer anio, Model model) {
        YearMonth periodo = periodoElegido(mes, anio);
        List<Pedido> delPeriodo = pedidos.listarPorPeriodo(periodo.atDay(1), periodo.atEndOfMonth());
        BigDecimal total = delPeriodo.stream().map(Pedido::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("vista", Vista.RESUMEN_VENTAS);
        model.addAttribute("pedidos", delPeriodo);
        model.addAttribute("totalTexto", String.format(Locale.US, "S/ %.2f", total));
        agregarSelectorPeriodo(model, periodo);
        return "resumen-ventas";
    }

    @GetMapping("/resumen-pedidos")
    public String resumenPedidos(@RequestParam(name = "mes", required = false) Integer mes,
                                 @RequestParam(name = "anio", required = false) Integer anio, Model model) {
        YearMonth periodo = periodoElegido(mes, anio);
        model.addAttribute("vista", Vista.RESUMEN_PEDIDOS);
        model.addAttribute("pedidos", pedidos.listarPorPeriodo(periodo.atDay(1), periodo.atEndOfMonth()));
        agregarSelectorPeriodo(model, periodo);
        return "resumen-pedidos";
    }

    /** Mes y año pedidos; si faltan o no son válidos, el mes actual. Los años válidos son los del desplegable. */
    private YearMonth periodoElegido(Integer mes, Integer anio) {
        YearMonth actual = YearMonth.now(reloj);
        int m = mes != null && mes >= 1 && mes <= 12 ? mes : actual.getMonthValue();
        int a = anio != null && anio >= actual.getYear() - 2 && anio <= actual.getYear() ? anio : actual.getYear();
        return YearMonth.of(a, m);
    }

    /** Alimenta los desplegables de mes y año (filtro de la lista y exportación). */
    private void agregarSelectorPeriodo(Model model, YearMonth elegido) {
        int anioActual = YearMonth.now(reloj).getYear();
        List<Integer> anios = new ArrayList<>();
        for (int a = anioActual - 2; a <= anioActual; a++) {
            anios.add(a);
        }
        model.addAttribute("meses", OpcionMes.todos());
        model.addAttribute("anios", anios);
        model.addAttribute("mesElegido", elegido.getMonthValue());
        model.addAttribute("anioElegido", elegido.getYear());
        model.addAttribute("periodoTexto", PeriodoReporte.MESES.get(elegido.getMonthValue() - 1) + " " + elegido.getYear());
    }
}
