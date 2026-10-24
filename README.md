# Northwind DW

Northwind DW reúne los datos comerciales de ejemplo de Northwind en una ventana sencilla. Permite revisar las ventas a lo largo del tiempo, comparar productos, países, clientes y empleados, y consultar cuántos proveedores hay por país.

## Así se ve

![Tablero de ventas conectado a la base Northwind](docs/screenshots/dashboard.png)

Si la aplicación no encuentra PostgreSQL, informa qué revisar y mantiene la ventana abierta:

![Tablero cuando no hay conexión con PostgreSQL](docs/screenshots/connection-error.png)

## Qué incluye

- Ventas por trimestre y año.
- Participación de ventas por categoría de producto.
- Los diez países con más ventas.
- Los diez clientes con más ventas.
- Ventas de cada empleado por año.
- Cantidad de proveedores por país.
- Filtro por año, resumen de ventas y descarga de informes en PDF o del archivo para preparar la base.

## Preparar la aplicación

Necesitas Java 21, Maven y PostgreSQL instalados. El proyecto incluye los datos públicos de Northwind en `src/main/resources/db/northwind_dw.sql`.

1. En PostgreSQL, crea una base vacía llamada `northwind_pentaho`.
2. Carga el archivo `src/main/resources/db/northwind_dw.sql` en esa base. Si usas pgAdmin, abre la base, abre ese archivo en Query Tool y ejecútalo. La base debe estar vacía.
3. En la carpeta principal del proyecto, crea tu configuración local copiando el ejemplo. En PowerShell:

   ```powershell
   Copy-Item .env.example .env
   ```

4. Abre `.env` y escribe el nombre de usuario y la contraseña que usas en PostgreSQL. Si tu instalación usa otro puerto o nombre de base, cambia también esos datos.
5. Desde PowerShell, en la carpeta principal del proyecto, inicia la aplicación:

   ```powershell
   mvn javafx:run
   ```

Al abrirse, el tablero consulta la base. Usa **Periodo** para elegir un año y **Actualizar** para volver a consultar. **Exportar PDF** guarda un informe con el resumen y los datos de los seis análisis; **Descargar SQL** guarda una copia del archivo de preparación de la base.

Si ves **Sin conexión**, revisa que PostgreSQL esté abierto, que la base se llame como indica `.env` y que el usuario y la contraseña sean correctos. Cierra la ventana para salir.

## Cuidar la contraseña

La contraseña va en el archivo local `.env`. Git lo ignora para evitar que se publique por accidente; `.env.example` contiene valores de muestra y sí se comparte. No publiques ni envíes tu `.env`. Si la contraseña del proyecto anterior se compartió o se subió a un repositorio, cámbiala en PostgreSQL.

## Revisión del proyecto

Para ejecutar las comprobaciones automáticas en Windows PowerShell:

```powershell
.\scripts\verify.ps1
```

Estas comprobaciones revisan el código, ejecutan las pruebas y preparan la aplicación. La prueba adicional de PostgreSQL es opcional: necesita que la base de prueba ya esté cargada y que indiques `NORTHWIND_TEST_DB_PORT` antes de ejecutar el script con `-WithDatabaseIntegration`. La prueba consulta datos, pero no los modifica.

La revisión funcional del proyecto anterior y los cambios realizados se describen en [`docs/engineering-review.md`](docs/engineering-review.md).
