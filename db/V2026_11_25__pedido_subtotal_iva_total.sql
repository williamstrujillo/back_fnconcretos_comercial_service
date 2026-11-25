ALTER TABLE pedidos
  ADD COLUMN subtotal DECIMAL(14,2) NOT NULL DEFAULT 0 AFTER condiciones_snapshot,
  ADD COLUMN iva DECIMAL(14,2) NOT NULL DEFAULT 0 AFTER subtotal,
  ADD COLUMN total DECIMAL(14,2) NOT NULL DEFAULT 0 AFTER iva;

-- Backfill de pedidos ya existentes: tipo_linea='otro' se usa EXCLUSIVAMENTE para la linea
-- sintetica de iva que agrega CotizacionService.convertirAPedido (nunca para otra cosa), asi que
-- sirve para separar subtotal/iva de lo que ya habia quedado guardado solo como suma de lineas.
UPDATE pedidos p
SET subtotal = COALESCE((SELECT SUM(pd.precio_total) FROM pedido_detalle pd WHERE pd.pedido_id = p.id AND pd.tipo_linea <> 'otro'), 0),
    iva      = COALESCE((SELECT SUM(pd.precio_total) FROM pedido_detalle pd WHERE pd.pedido_id = p.id AND pd.tipo_linea = 'otro'), 0);

UPDATE pedidos SET total = subtotal + iva;
