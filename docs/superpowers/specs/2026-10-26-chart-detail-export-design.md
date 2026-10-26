# Detalle de gráficos y exportación

## Intención

Permitir abrir cualquiera de los seis gráficos del tablero en una vista amplia y volver sin perder el filtro de año, el tema elegido ni la posición de la cuadrícula. La vista de detalle debe explicar brevemente el informe y ofrecer exportaciones específicas para el gráfico seleccionado.

## Metas y límites

- Abrir el detalle al seleccionar una tarjeta de gráfico con mouse o teclado.
- Mantener la misma ventana y la cabecera compartida con el tema, periodo, actualización, exportación completa a PDF y descarga SQL.
- Ocultar los indicadores resumen mientras se consulta un gráfico para dar más espacio al detalle.
- Incluir un botón para regresar a la cuadrícula conservando periodo, tema y desplazamiento anterior.
- Mostrar el título, una explicación breve y el gráfico seleccionado en tamaño adaptable.
- Exportar el gráfico seleccionado a un PDF propio con título, contexto, periodo, imagen del gráfico y sus datos actuales.
- Generar un QR que permita descargar el gráfico PNG desde un teléfono conectado a la misma red local; permitir ver y guardar el QR como PNG.
- Mostrar el logotipo de Northwind en el encabezado y como icono de la ventana, y mantener el botón «Regresar» sin símbolos decorativos.
- Explicar al inicio del README que esta versión parte del sistema académico anterior para corregir problemas de optimización y cuidado de credenciales, e incluir el enlace a su video.
- Conservar los seis informes y la actualización al cambiar el periodo.

## Enfoques considerados

1. **Descarga temporal en la red local — elegida.** El QR apunta a un archivo servido por el computador durante un periodo corto. No necesita cuenta ni servicio externo y Safari recibe el PNG como adjunto. Un enlace alojado fuera del equipo subiría el gráfico a un tercero; incluir los bytes de la imagen en el QR supera su capacidad práctica.
2. **Cambiar el contenido central en la misma ventana.** Reutiliza la cabecera, los filtros, el tema y el gráfico existente; evita tener dos estados que sincronizar. La alternativa de una ventana separada imita mejor el primer proyecto, pero hace más fácil que el año o el tema se desincronicen. La alternativa de duplicar dos vistas completas mantiene independencia visual, pero duplica controles y lógica.

## Decisiones

- Se conserva la ventana única y se intercambia la cuadrícula por el detalle. El gráfico se reutiliza para conservar exactamente los datos, tooltips y estilos del tablero.
- El PDF del detalle contiene solo el informe seleccionado, incluida su gráfica visual y filas de datos; el botón de la cabecera sigue exportando el tablero completo.
- El QR contiene una dirección privada temporal con un token aleatorio. El computador sirve únicamente la imagen PNG del gráfico actual, durante 10 minutos; el teléfono debe estar en la misma red local y la aplicación debe seguir abierta. No se envían datos a un servicio de Internet.
- El servidor de descarga se limita a una interfaz local y a una ruta única, usa lecturas con tiempo y tamaño máximos, rechaza otros métodos y se cierra al expirar o al cerrar la aplicación.
- El logotipo adjunto se usa en el encabezado y como icono de la ventana; «Regresar» conserva su nombre accesible sin la flecha visual.
- El README atribuye esta versión al proyecto académico anterior, resume la motivación y enlaza el video proporcionado.
- El periodo «Todos los años» y cada año individual siguen disponibles desde la cabecera y actualizan el gráfico de detalle.
- El diseño continúa usando las paletas clara/oscura existentes y prioriza dimensiones adaptables, foco visible y navegación por teclado.
- Se podrá incorporar una biblioteca pequeña para codificar QR; sus términos se añadirán a los avisos de terceros.

## Pruebas de aceptación

- Las seis tarjetas abren el informe correcto, tanto con mouse como con teclado; regresar restituye la cuadrícula y su posición.
- Cambiar el periodo en el detalle actualiza el gráfico, el PDF y el contenido QR; el tema sigue visible y funcional.
- El PDF contiene la imagen, título, contexto, periodo y filas del informe elegido, sin incluir los otros cinco informes.
- El QR puede decodificarse como una URL de descarga; una petición válida recibe el PNG como adjunto, una ruta o método ajeno es rechazado y cerrar el servidor invalida el enlace.
- El QR se genera para los seis informes, se puede guardar como PNG y explica que requiere la misma red y expira en 10 minutos.
- El logo se carga desde los recursos de la aplicación, aparece en el encabezado y está instalado en la ventana; el botón visible dice «Regresar» sin flecha.
- El FXML carga y el detalle conserva proporciones legibles al cambiar el tamaño de la ventana y el tema.
- La verificación estática y dinámica del proyecto termina sin fallos.
