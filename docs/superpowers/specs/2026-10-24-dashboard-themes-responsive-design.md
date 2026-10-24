# Temas y proporciones adaptables para Northwind DW

## Intención

Acercar el aspecto del nuevo tablero al original y permitir leer sus informes en ventanas de distintos tamaños. El original usa un fondo índigo oscuro (`#17103A`), tarjetas (`#272952`) y seis gráficos en tres columnas por dos filas.

## Metas y límites

- Ofrecer modo oscuro y claro; al abrir por primera vez se usa el oscuro para recuperar la apariencia del proyecto de referencia.
- Guardar localmente el último tema elegido para conservarlo al volver a abrir la aplicación.
- Mantener todos los datos y acciones actuales.
- Adaptar la cuadrícula a tres, dos o una columna según el ancho y dimensionar cada gráfico junto con su tarjeta.
- Reacomodar los controles de cabecera cuando no caben en una fila y permitir ventanas más angostas.
- No añadir dependencias ni cambiar la base de datos.

## Enfoques considerados

1. **Variantes CSS en la misma vista — elegido.** Cambiar una clase del contenedor permite compartir controles, datos y FXML entre los dos temas. `Preferences` guarda el ajuste local. El cálculo de la cuadrícula queda en una clase pequeña y comprobable.
2. **Dos vistas FXML separadas.** Ofrece libertad de composición, pero duplica controles y obliga a mantener dos copias de cada cambio.
3. **Mantener dimensiones fijas y solo reducir la ventana.** Es la modificación más pequeña, pero conserva el problema de proporciones que se quiere resolver.

## Decisiones de diseño

- El modo oscuro recupera el fondo y las tarjetas del original, con ejes, leyendas y texto de contraste suficiente; el modo claro usa una paleta clara coordinada.
- Un botón con nombre accesible cambia entre los temas. Se muestra en la cabecera, junto al estado de conexión.
- La cuadrícula usa tres columnas en ventanas anchas, dos en medianas y una en estrechas. El ancho y la altura de tarjetas y gráficos se calculan en conjunto.
- La cabecera usa controles que pueden pasar a una segunda fila. La ventana inicial se limita al espacio disponible en pantalla y su tamaño mínimo baja.
- Las pruebas unitarias verifican selección/persistencia de tema y puntos de corte; la prueba visual captura ambos temas y los estados de conexión.

## Criterios de aceptación

- Al primer inicio aparece el modo oscuro con la paleta del original; el botón conmuta el tema y la elección sobrevive a reiniciar.
- Los controles, textos de ejes, leyendas, tarjetas y gráficos siguen legibles en ambos temas.
- Al reducir y ampliar la ventana, las seis tarjetas fluyen en 1, 2 o 3 columnas sin desbordamiento horizontal y los gráficos conservan proporción útil.
- Las funciones de actualizar, filtrar, exportar PDF y descargar SQL continúan disponibles.
- Las pruebas Maven y el empaquetado pasan; las capturas actualizadas documentan los dos temas.
