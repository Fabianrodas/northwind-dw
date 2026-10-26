# Seguridad

Northwind DW es un proyecto académico de escritorio que usa datos públicos de ejemplo. No procesa información privada ni ofrece un servicio de red.

## Alcance y mantenimiento

No hay versiones con soporte ni un compromiso de respuesta o de publicar futuras correcciones. No uses el proyecto para decisiones de producción ni con datos confidenciales. Para un uso real, se requiere revisar el código, las dependencias y la configuración de PostgreSQL para ese entorno.

## Protección local

- La contraseña se configura en `.env` o en variables del sistema; ese archivo está excluido de Git.
- La aplicación necesita una contraseña explícita y usa el usuario `northwind_dw_app`, pensado para consultas de solo lectura.
- El archivo `src/main/resources/db/create_readonly_user.sql` crea ese usuario con permisos de lectura. Define su contraseña por separado en pgAdmin; no la escribas en un script SQL.
- Las consultas de la aplicación usan parámetros y las conexiones JDBC se marcan como solo lectura.
- La base incluida contiene datos de ejemplo públicos; no copies información real o privada allí.

## Descarga mediante QR

Al generar un QR, la aplicación comparte únicamente la imagen PNG del gráfico mediante una dirección temporal en la red local. El enlace incluye un token aleatorio, dura 10 minutos y deja de funcionar al cerrar la aplicación; el servicio solo responde en la dirección privada elegida y rechaza otras rutas o métodos. El teléfono y el computador deben estar conectados a la misma red local. El archivo no se sube a Internet. Usa esta función únicamente con la información de ejemplo incluida y en una red de confianza.

## Si una contraseña se expone

Cámbiala en PostgreSQL de inmediato. Si llegó a Git, quitar el archivo en un commit nuevo no borra la copia de los commits anteriores: cambia la contraseña y limpia el historial publicado antes de compartir ese repositorio.

No publiques contraseñas, archivos `.env`, capturas con datos privados ni detalles que permitan usar una cuenta. Para avisar sobre un problema, contacta directamente a quien mantiene el proyecto; no se garantiza una respuesta.
