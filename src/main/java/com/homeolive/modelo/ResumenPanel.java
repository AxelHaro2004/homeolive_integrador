package com.homeolive.modelo;

/** Cantidad de pedidos por estado para el panel de inicio. */
public record ResumenPanel(int enPreparacion, int listoParaEnvio, int porConfirmar, int entregadosHoy) {
}
