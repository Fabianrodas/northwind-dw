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
- Generar un QR local que contenga un resumen de los datos del gráfico y periodo seleccionados; permitir verlo y guardarlo como PNG. No requiere publicar datos ni acceder a servicios externos.
- Conservar los seis informes y la actualización al cambiar el periodo.

## Enfoques considerados

1. **Cambiar el contenido central en la misma ventana — recomendado.** Reutiliza la cabecera, los filtros, el tema y el gráfico existente; evita tener dos estados que sincronizar. La alternativa de una ventana separada imita mejor el primer proyecto, pero hace más fácil que el año o el tema se desincronicen. La alternativa de duplicar dos vistas completas mantiene independencia visual, pero duplica controles y lógica.

## Decisiones

- Se conserva la ventana única y se intercambia la cuadrícula por el detalle. El gráfico se reutiliza para conservar exactamente los datos, tooltips y estilos del tablero.
- El PDF del detalle contiene solo el informe seleccionado, incluida su gráfica visual y filas de datos; el botón de la cabecera sigue exportando el tablero completo.
- El QR codifica el título, contexto, periodo y valores del informe actualmente filtrado. Se muestra en un diálogo y puede guardarse como PNG; no codifica un enlace a Drive.
- El periodo «Todos los años» y cada año individual siguen disponibles desde la cabecera y actualizan el gráfico de detalle.
- El diseño continúa usando las paletas clara/oscura existentes y prioriza dimensiones adaptables, foco visible y navegación por teclado.
- Se podrá incorporar una biblioteca pequeña para codificar QR; sus términos se añadirán a los avisos de terceros.

## Pruebas de aceptación

- Las seis tarjetas abren el informe correcto, tanto con mouse como con teclado; regresar restituye la cuadrícula y su posición.
- Cambiar el periodo en el detalle actualiza el gráfico, el PDF y el contenido QR; el tema sigue visible y funcional.
- El PDF contiene la imagen, título, contexto, periodo y filas del informe elegido, sin incluir los otros cinco informes.
- El QR puede decodificarse y su contenido corresponde al resumen filtrado; guardarlo como PNG funciona.
- El FXML carga y el detalle conserva proporciones legibles al cambiar el tamaño de la ventana y el tema.
- La verificación estática y dinámica del proyecto termina sin fallos.
