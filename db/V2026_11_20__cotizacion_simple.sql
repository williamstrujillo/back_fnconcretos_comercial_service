-- Cotizacion "simple": permite capturar cliente/obra solo como texto libre, sin tener que dar de
-- alta un cliente/obra real en el catalogo primero -- pensado para prospectar rapido. No se puede
-- convertir a pedido hasta que se vincule un cliente real (ver CotizacionService.convertirAPedido).

ALTER TABLE cotizaciones
  MODIFY COLUMN cliente_id BIGINT(20) UNSIGNED NULL,
  ADD COLUMN cliente_nombre_libre VARCHAR(150) DEFAULT NULL AFTER cliente_id,
  ADD COLUMN obra_nombre_libre VARCHAR(150) DEFAULT NULL AFTER obra_id;
