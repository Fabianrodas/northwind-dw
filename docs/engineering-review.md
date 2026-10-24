# Revisión de ingeniería de `NorthwindDW`

## Alcance y evidencia

Se revisaron las clases Java, los controladores de los seis gráficos, los FXML, CSS, configuración Maven, el flujo de descarga y el dump SQL. Se ejecutó `mvn test`: el proyecto compila, pero Maven informa que no hay pruebas. Se lanzó `mvn javafx:run`; tras 2,2 segundos, la carga intentó abrir el tablero y falló cuando PostgreSQL no estaba disponible. El error de conexión se convirtió en un `NullPointerException` al cerrar una conexión nula, por lo que no aparece un estado recuperable.

El SQL original se importó en un clúster PostgreSQL 18 temporal e independiente en el puerto 55432. El clúster conserva el dump bajo revisión y se eliminará al terminar las pruebas de la reconstrucción. El servidor PostgreSQL normal del equipo no estaba activo.

## Funcionalidad actual

| Área | Archivos y comportamiento | Hallazgos |
|---|---|---|
| Inicio | `App`, `CargaController`, `carga.fxml` | Pantalla animada de 2,2 s y transición adicional; el inicio no refleja trabajo real. Excepciones impresas y no comunicadas al usuario. |
| Acceso a datos | `DbConnection` | URL y base fijas; lee credenciales desde `postgresql_login.txt`; la conexión fallida retorna `false`, los llamadores lo ignoran y luego reciben una conexión nula. `desconectar()` también falla si no hubo conexión. |
| Ventana principal | `MainController`, `main.fxml`, `styles.css` | Carga seis reportes de forma síncrona durante la inicialización; seis controladores independientes abren conexiones. Ventana de tamaño fijo y controladores de gráficos repetidos. |
| Reporte 1 | `Grafico1Controller`, `Grafico1` | Suma ventas por año y trimestre; falta orden explícito del trimestre y los decimales se convierten a `int`. |
| Reporte 2 | `Grafico2Controller`, `Grafico2` | Participación por categoría con ventana SQL; usa join implícito y conexión separada. |
| Reporte 3 | `Grafico3Controller`, `Grafico3` | Países por ventas, primeros diez; join implícito. |
| Reporte 4 | `Grafico4Controller`, `Grafico4` | Cuenta filas de detalle y las llama clientes recurrentes; una fila por producto no equivale a una compra. |
| Reporte 5 | `Grafico5Controller`, `Grafico5` | Ejecuta cinco consultas para cinco empleados fijos; ignora otros empleados y trunca importes. |
| Reporte 6 | `Grafico6Controller`, `Grafico6` | Cuenta proveedores por país, pero calcula una suma adicional de cantidades que no se muestra. El gráfico añade cada punto dos veces. |
| Vista ampliada | `VentanaController`, `grafico.fxml`, singletons en `modelo` | Duplica consultas y renderizado. El singleton global guarda gráfico, ventana, imágenes y estado del flujo de exportación. |
| Exportación de gráficos | `VentanaController.descargarGrafico` | Depende de capturas hechas mediante tareas `Platform.runLater`; además bloquea el hilo gráfico con `Thread.sleep`, de modo que las imágenes pueden no estar listas al generar el PDF. |
| Exportación del tablero | `VentanaController.obtenerGraficos`, `OpcionesController.descargarPDF` | Programa capturas asíncronas y devuelve la lista antes de que se agreguen; el PDF puede salir vacío. Escribe directamente en una carpeta Descargas que podría no existir y deja archivos temporales si hay error. |
| SQL y QR | `OpcionesController` | La descarga SQL usa un selector de archivo. El QR apunta a un enlace de Drive codificado; no representa el tablero actual. |
| Modelo y estructura | `modelo/*.java`, `module-info.java`, `pom.xml`, `nbactions.xml` | Modelos mutables con nombres de datos poco claros, dependencias iText redundantes, versiones mezcladas de Java 21 y compilación release 11. No hay pruebas ni límites entre UI, consultas y exportación. |

## Incidente de credenciales

`postgresql_login.txt` está rastreado por el Git del proyecto de referencia. El repositorio nuevo no lo recibe. La reconstrucción usa `.env`, ignora ese archivo y ofrece `.env.example` con valores ficticios. Si la contraseña antigua se compartió fuera del equipo o se subió a un repositorio remoto, debe rotarse en PostgreSQL.

## Plan de verificación

La reconstrucción añade pruebas de configuración y exportación, una prueba integrada opcional contra PostgreSQL, un chequeo de compilación Maven, una inspección estática de secretos/rutas de configuración y capturas de estados real y sin conexión. La base de pruebas y el servidor PostgreSQL temporales se eliminan al finalizar.

## Resultado de la reconstrucción

El tablero conserva los seis análisis y el filtro por año. La lectura de configuración queda en `config`; siete consultas analíticas comparten una conexión por actualización y forman modelos inmutables; la interfaz presenta los resultados y delega las consultas a un hilo de fondo; `service` genera el PDF y copia el SQL incluido. Se usan consultas preparadas para el año y `BigDecimal` para importes. La pantalla animada, las conexiones duplicadas, la lista fija de cinco empleados y el enlace QR externo ya no forman parte del nuevo proyecto.

El archivo de preparación SQL se importó en una base temporal PostgreSQL 18. La integración confirmó ventas por 1.265.792,94, 89 clientes con ventas, 29 proveedores con ventas, tres años, diez países y diez clientes en los rankings, los empleados con datos y la selección del año 1997. Cada actualización abrió una sola conexión. El filtro conservó el total correcto del periodo y la participación de las categorías sumó aproximadamente 100 %.

Se abrió la aplicación con la base y también sin conexión: en el segundo caso la ventana permaneció disponible y mostró instrucciones de recuperación. Se generaron y revisaron visualmente las dos capturas de `docs/screenshots/`. Las pruebas de exportación generaron un PDF legible y comprobaron la copia del SQL portable. El comando `scripts/verify.ps1 -WithDatabaseIntegration` ejecutó las comprobaciones estáticas, 12 pruebas (11 aprobadas y la captura automática omitida en ese modo) y el empaquetado Maven sin fallos. La prueba de capturas se ejecutó por separado y produjo ambos PNG.

La base, el servidor y las carpetas temporales de verificación se eliminaron al cerrar las pruebas. El archivo `.env` usado para ellas también se borró; `.env.example` solo incluye valores ficticios y las reglas de Git siguen excluyendo `.env`.
