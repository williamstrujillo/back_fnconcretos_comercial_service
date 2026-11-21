-- Nuevo tipo_linea 'vacio_bombeo' (ver catalogo-service V2026_11_21__planta_vacio_bombeo.sql para
-- los campos de configuracion por planta: capacidad_minima_bombeo_m3/precio_por_m3_vacio_bombeo).
-- Solo actualiza el COLUMN COMMENT documental (no es un ENUM real); no requiere cambio de tipo ni
-- de datos existentes. Aplicar primero en local, verificar, luego sandbox y productivo.

ALTER TABLE `cotizacion_detalle`
  MODIFY COLUMN `tipo_linea` varchar(20) NOT NULL DEFAULT 'producto'
    COMMENT 'producto, bombeo, flete_vacio, vacio_bombeo, otro';

ALTER TABLE `pedido_detalle`
  MODIFY COLUMN `tipo_linea` varchar(20) NOT NULL DEFAULT 'producto'
    COMMENT 'producto, bombeo, flete_vacio, vacio_bombeo, otro';
