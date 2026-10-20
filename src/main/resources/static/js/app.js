(function () {
  'use strict';

  var $ = function (selector, root) { return (root || document).querySelector(selector); };
  var $$ = function (selector, root) { return Array.prototype.slice.call((root || document).querySelectorAll(selector)); };

  /* ---------- Menú lateral (móvil) ---------- */
  var sidebar = $('#sidebar');
  var overlay = $('#sidebar-overlay');
  if (sidebar && overlay) {
    var abrirMenu = function () { sidebar.classList.add('sidebar-open'); overlay.hidden = false; };
    var cerrarMenu = function () { sidebar.classList.remove('sidebar-open'); overlay.hidden = true; };
    $$('[data-menu-abrir]').forEach(function (b) { b.addEventListener('click', abrirMenu); });
    overlay.addEventListener('click', cerrarMenu);
    $$('#sidebar a').forEach(function (a) { a.addEventListener('click', cerrarMenu); });
  }

  /* ---------- Modales ---------- */
  function abrirModal(boton) {
    var modal = document.getElementById(boton.getAttribute('data-modal-abrir'));
    if (!modal) { return; }
    var form = $('form', modal);
    if (form) { form.reset(); }
    Object.keys(boton.dataset).forEach(function (clave) {
      if (clave === 'modalAbrir') { return; }
      var valor = boton.dataset[clave];
      var campo = $('[name="' + clave + '"]', modal);
      if (campo) { campo.value = valor; }
      $$('[data-slot="' + clave + '"]', modal).forEach(function (el) { el.textContent = valor; });
    });
    modal.hidden = false;
    var primero = $('input:not([readonly]), select', modal);
    if (primero) { primero.focus(); }
  }
  function cerrarModal(modal) { modal.hidden = true; }

  $$('[data-modal-abrir]').forEach(function (boton) {
    boton.addEventListener('click', function () { abrirModal(boton); });
  });
  $$('.modal-backdrop').forEach(function (modal) {
    modal.addEventListener('mousedown', function (e) { if (e.target === modal) { cerrarModal(modal); } });
    $$('[data-modal-cerrar]', modal).forEach(function (b) { b.addEventListener('click', function () { cerrarModal(modal); }); });
    var form = $('form', modal);
    if (form) {
      form.addEventListener('submit', function (e) { e.preventDefault(); cerrarModal(modal); });
    }
  });
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') { $$('.modal-backdrop').forEach(cerrarModal); }
  });

  /* ---------- Buscador de tablas ---------- */
  $$('input[data-filtro]').forEach(function (input) {
    var tabla = document.getElementById(input.getAttribute('data-filtro'));
    if (!tabla) { return; }
    input.addEventListener('input', function () {
      var texto = input.value.trim().toLowerCase();
      $$('tbody tr', tabla).forEach(function (fila) {
        if (fila.querySelector('.empty-cell')) { return; }
        fila.hidden = texto !== '' && fila.textContent.toLowerCase().indexOf(texto) === -1;
      });
    });
  });

  /* ---------- Avisos y confirmaciones ---------- */
  $$('[data-aviso]').forEach(function (b) {
    b.addEventListener('click', function () { window.alert(b.getAttribute('data-aviso')); });
  });
  $$('[data-confirmar]').forEach(function (b) {
    b.addEventListener('click', function () {
      if (window.confirm(b.getAttribute('data-confirmar'))) {
        var fila = b.closest('tr');
        if (fila) { fila.remove(); }
      }
    });
  });

  /* ---------- Selector de cliente (recarga con ?cliente=ID) ---------- */
  $$('select[data-navegar]').forEach(function (select) {
    select.addEventListener('change', function () {
      var url = new URL(window.location.href);
      url.searchParams.set(select.getAttribute('data-navegar'), select.value);
      window.location.href = url.toString();
    });
  });

  /* ---------- Registro de pedidos ---------- */
  var formPedido = $('#form-pedido');
  if (formPedido) {
    var lista = $('#lista-items');
    var contador = $('#contador-items');
    var ubicacion = $('#ubicacion');
    var envio = $('#tipo-envio');
    var banner = $('#banner-exito');

    var actualizarItems = function () {
      var filas = $$('.item-row', lista);
      filas.forEach(function (fila, i) {
        $('.item-index', fila).textContent = ('0' + (i + 1)).slice(-2);
        $('.remove-item', fila).disabled = filas.length === 1;
      });
      contador.textContent = filas.length + (filas.length === 1 ? ' ítem' : ' ítems') + ' en el pedido';
    };

    $('#agregar-item').addEventListener('click', function () {
      var copia = $('.item-row', lista).cloneNode(true);
      $$('input', copia).forEach(function (i) { i.value = i.type === 'number' ? '1' : ''; });
      $$('select', copia).forEach(function (s) { s.selectedIndex = 0; });
      lista.appendChild(copia);
      actualizarItems();
    });

    lista.addEventListener('click', function (e) {
      var boton = e.target.closest('.remove-item');
      if (boton && !boton.disabled) {
        boton.closest('.item-row').remove();
        actualizarItems();
      }
    });

    // Provincia solo permite courier (Olva o Shalom): se oculta Motorizado.
    ubicacion.addEventListener('change', function () {
      var motorizado = $('option[value="Motorizado"]', envio);
      var provincia = ubicacion.value === 'Provincia';
      motorizado.hidden = provincia;
      motorizado.disabled = provincia;
      if (provincia && envio.value === 'Motorizado') { envio.value = 'Olva'; }
    });

    $('#dni').addEventListener('input', function (e) { e.target.value = e.target.value.replace(/\D/g, ''); });

    formPedido.addEventListener('submit', function (e) {
      e.preventDefault();
      banner.hidden = false;
      window.setTimeout(function () { banner.hidden = true; }, 3200);
      formPedido.reset();
      $$('.item-row', lista).slice(1).forEach(function (fila) { fila.remove(); });
      actualizarItems();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });

    actualizarItems();
  }
})();
