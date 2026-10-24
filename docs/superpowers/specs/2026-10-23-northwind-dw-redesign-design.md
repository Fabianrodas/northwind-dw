# Rediseño de Northwind DW

## Intención

Reconstruir en el proyecto vacío una aplicación local para explorar el data warehouse Northwind. Debe conservar los seis análisis que ya ofrece, proteger las credenciales PostgreSQL fuera de Git y ser más fácil de mantener, ejecutar y ampliar.

## Metas

- Mantener Java 21, JavaFX y PostgreSQL.
- Separar la configuración, el acceso a datos, los modelos, las exportaciones y la interfaz.
- Cargar los seis análisis con una conexión por actualización y sin bloquear la interfaz.
- Leer la conexión desde `.env` y variables de entorno; ignorar los secretos con Git e incluir `.env.example`.
- Retirar la pantalla inicial animada y el QR que apunta a un enlace externo fijo.
- Conservar la actualización, las seis visualizaciones, la exportación PDF del tablero y la descarga del SQL.
- Mostrar estados claros de carga, conexión, error y datos vacíos.
- Entregar instrucciones en español y capturas de la aplicación.

## Fuera de alcance

- Cambiar el proyecto de referencia `NorthwindDW`.
- Publicar o desplegar el programa, crear un servicio web o hacer push.
- Cambiar el origen público de datos o diseñar un almacén distinto.

## Enfoques considerados

1. **Mantener JavaFX y separar capas — elegido.** Conserva la forma local de uso del proyecto y permite corregir el acoplamiento sin cambiar plataforma. El archivo `.env` protege las credenciales del repositorio; sigue siendo una aplicación local y el archivo no debe distribuirse junto con un ejecutable a usuarios no confiables.
2. **Migrar a una aplicación web con backend.** Permitiría centralizar credenciales y atender varios usuarios, pero exigiría un servidor, despliegue y un flujo operativo que no forman parte del uso descrito.
3. **Parchear los controladores actuales.** Requeriría menos cambios, pero mantendría consultas duplicadas, ventanas acopladas y una base difícil de probar y extender.

## Arquitectura y decisiones

- `config`: lectura y validación de `.env` con precedencia para variables del sistema.
- `data`: consultas PostgreSQL preparadas y mapeo de resultados a modelos tipados.
- `service`: coordinación de informes y generación de archivos.
- `ui`: JavaFX con tarjetas que se ajustan al ancho de la ventana; las consultas corren fuera del hilo gráfico.
- Un único tablero contiene seis visualizaciones: ventas por trimestre y año, participación por categoría, ventas por país, ventas por cliente, ventas por empleado y proveedores por país.
- Los importes se mantienen como `BigDecimal`; los filtros se parametrizan.
- El SQL de descarga será un único recurso portable, sin borrar ni crear bases de datos ni asignar objetos a un usuario local específico.
- La exportación PDF describe el tablero actual y su filtro. La exportación SQL permite preparar la base aparte.
- Las pruebas integradas usarán una instancia PostgreSQL efímera y aislada; al terminar se borrará la base y se apagará el servidor.

## Restricciones

- Todo el trabajo va en `main`, sin ramas ni worktrees.
- No se copiará `postgresql_login.txt` al repositorio nuevo.
- `.env` debe estar ignorado y `.env.example` no contiene credenciales reales.
- Hacer commits por hitos revisables.
- El README va dirigido a una persona que quiere abrir y usar el programa, no a una persona que desarrolla Java.

## Criterios de aceptación

- `mvn test` y `mvn package` terminan correctamente.
- Las seis consultas funcionan sobre el SQL de referencia, preservan decimales y admiten todos los empleados del almacén.
- La interfaz carga datos sin congelarse y explica qué hacer cuando no hay conexión.
- PDF y SQL se guardan mediante una ubicación elegida o un destino que exista.
- El repositorio no rastrea `.env`, contraseñas ni el antiguo archivo de acceso.
- El README incluye capturas reales del tablero.
