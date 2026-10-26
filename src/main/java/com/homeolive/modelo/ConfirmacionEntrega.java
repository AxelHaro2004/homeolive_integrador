package com.homeolive.modelo;

/**
 * Confirmaciones que exige el negocio para pasar un pedido a «entregado».
 * Motorizado: el cliente confirmó que recibió su pedido.
 * Courier: la plataforma muestra «listo para recoger» y se avisó al cliente por llamada.
 */
public record ConfirmacionEntrega(boolean clienteConfirmoRecepcion, boolean plataformaListoParaRecoger,
                                  boolean clienteAvisadoPorLlamada) {
}
