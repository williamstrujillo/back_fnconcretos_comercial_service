# comercial-service

El flujo de venta completo de FN Concretos: clientes, contactos, obras
(compartibles entre clientes), asesores comerciales, agenda, cotizaciones
(con reciclaje y descuento especial) y su conversion a pedido con las
autorizaciones de pago y logistica.

No emite tokens — solo los valida, con el mismo `JWT_SECRET` que
`auth-service`. Ver el catalogo completo en el artifact "fnconcretos.app —
Catalogo de endpoints" (pestana `02 comercial-service`).

## Requisitos

- **Java 21** (forzar `JAVA_HOME`, ver README de auth-service)
- `fndatabase`, `auth-service` y `catalogo-service` corriendo (este
  servicio referencia `planta_id`/`producto_id`/`zona_id` de catalogo-service
  y `usuario_id` de auth-service como IDs sueltos, no como relaciones JPA:
  cada microservicio es dueno solo de sus propias tablas)

## Correr en local

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
mvn clean package
java -jar target/comercial-service.jar
```

Puerto por defecto: `8083`. Swagger UI: http://localhost:8083/api/comercial/swagger-ui.html

## Reglas de negocio implementadas

- **Descuento especial en cotizacion**: si el `porcentajeDescuento` excede
  el maximo estandar (`5%` sin factura / `12%` con factura, configurable
  por variable de entorno), se exige el permiso
  `cotizaciones.aplicar_descuento_especial` (Ventas).
- **Convertir cotizacion a pedido**: solo permitido si la cotizacion esta
  en estatus `listo`; evita la doble captura trayendo los datos ya
  capturados en la cotizacion.
- **Autorizacion de pago del pedido**: si `condicionPago == credito`, se
  exige el permiso `pedidos.autorizar_credito` (Direccion). Los demas
  casos (liquidado, anticipo) no requieren permiso especial.
- **Autorizacion de logistica**: siempre requiere el permiso
  `pedidos.autorizar_logistica` (Produccion).
- **Ruta diaria de asesores**: minimo de visitas configurable
  (`VISITAS_MINIMO_DIARIO`, default 10).
