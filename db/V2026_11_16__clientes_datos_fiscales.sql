ALTER TABLE clientes
    ADD COLUMN razon_social VARCHAR(150) NULL AFTER rfc,
    ADD COLUMN codigo_postal VARCHAR(5) NULL AFTER razon_social,
    ADD COLUMN regimen_fiscal VARCHAR(10) NULL AFTER codigo_postal,
    ADD COLUMN uso_cfdi_preferido VARCHAR(10) NULL AFTER regimen_fiscal;
