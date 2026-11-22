-- Cambio de canal: los avisos pasan de WhatsApp a Telegram (Bot API).
-- Un bot de Telegram no puede escribirle a un número de teléfono: necesita el chat_id del usuario,
-- que existe recién cuando el usuario le manda /start al bot.

-- contacto: el teléfono no sirve para Telegram; se reemplaza por el chat_id.
ALTER TABLE contacto ADD COLUMN telegram_chat_id VARCHAR(32);
ALTER TABLE contacto DROP COLUMN telefono;
ALTER TABLE contacto RENAME COLUMN acepta_whatsapp TO acepta_telegram;
-- RN14: el consentimiento se había dado para WhatsApp y no se traslada a otro canal.
-- Cada usuario vuelve a aceptar al registrar su chat de Telegram.
UPDATE contacto SET acepta_telegram = FALSE;

-- notificacion: chat al que se envió cada aviso (foto del momento del envío).
-- Las filas anteriores a esta migración conservan el teléfono de WhatsApp al que se intentó enviar.
ALTER TABLE notificacion RENAME COLUMN telefono TO telegram_chat_id;
ALTER TABLE notificacion ALTER COLUMN telegram_chat_id SET DATA TYPE VARCHAR(32);
-- id_mensaje_proveedor pasa a guardar el message_id de Telegram (antes, el id del proveedor de WhatsApp).
