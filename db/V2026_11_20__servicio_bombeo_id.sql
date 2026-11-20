-- Referencia al catalogo de servicios de bombeo (clave+descripcion) en catalogo-service, igual de
-- laxa que producto_id: columna plana, sin FK cruzado entre microservicios, solo aplica cuando
-- tipo_linea=bombeo. Aditiva, no rompe nada existente.
-- Aplicar primero en local, verificar, luego sandbox y productivo (mismo orden de siempre).

ALTER TABLE `cotizacion_detalle`
  ADD COLUMN `servicio_bombeo_id` bigint(20) unsigned DEFAULT NULL COMMENT 'solo aplica cuando tipo_linea=bombeo' AFTER `producto_id`;

ALTER TABLE `pedido_detalle`
  ADD COLUMN `servicio_bombeo_id` bigint(20) unsigned DEFAULT NULL COMMENT 'solo aplica cuando tipo_linea=bombeo' AFTER `producto_id`;
