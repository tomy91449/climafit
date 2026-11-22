-- Vinculación automática con Telegram.
-- El usuario ya no tiene que averiguar su chat_id: al aceptar avisos recibe un link
-- t.me/<bot>?start=<codigo>. Cuando toca "Iniciar", el bot recibe "/start <codigo>",
-- busca el contacto con ese código y guarda el chat_id solo.

-- Código de un solo uso para el link. Es aleatorio y largo: quien lo tenga puede vincular su chat
-- a ese usuario, por eso vence y se borra apenas se usa.
ALTER TABLE contacto ADD COLUMN codigo_vinculacion VARCHAR(64);
ALTER TABLE contacto ADD COLUMN codigo_vence TIMESTAMP;
CREATE UNIQUE INDEX ux_contacto_codigo_vinculacion ON contacto (codigo_vinculacion);
