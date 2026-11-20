# Plan de captura — video promocional MotorMates

Generado tras leer `Color.kt`, `Type.kt`, `Theme.kt`, `navigation/ScreenRoute.kt`
y las pantallas de `ui/`. **No se usan los mockups de `imgs/`.**

## Datos confirmados en el código

### Paleta (`ui/theme/Color.kt`) — verificada
| Token | Hex | Uso |
|---|---|---|
| `MotorMatesBackground` | `#0A0A0A` | fondo de la app y del video |
| `MotorMatesSurface` | `#1E1E1E` | tarjetas |
| `MotorMatesRed` | `#B3282D` | primario, resplandor del video |
| `MotorMatesRedLight` | `#E05257` | acento de los textos |
| `md3_dark_tertiary` | `#E0B84E` | ámbar terciario |
| `MotorMatesTextSecondary` | `#9B9B9B` | texto secundario |

Colores de marcador del mapa (no son roles de tema, no cambian con la paleta):
Car Meet `#3D7BD6`, pique `#36A85D`, gasolinera `#8C5BD6`, carga `#14A6A6`.

### Tipografía (`ui/theme/Type.kt`)
`FontFamily.Default` en todos los estilos → Roboto en Android. En el video: **Inter o Roboto**.
Escala: displayLarge 50sp Bold, headlineLarge 32sp Bold, titleLarge 22sp SemiBold.

### Tema (`ui/theme/Theme.kt`) — ojo
El comentario del archivo dice que el modo oscuro del sistema muestra la paleta clara,
pero el código es `if (darkTheme) DarkColorScheme`, y `md3_dark_*` **son** los colores de
marca. El comentario está desactualizado: **el emulador va en tema oscuro** para obtener
`#0A0A0A` + rojo. Se verifica en la captura 01 y se invierte si saliera claro.

### Navegación relevante (`navigation/ScreenRoute.kt`)
`splash` → `login` → `onboarding` (se salta solo si ya se contestó) → `feed`.
**Explorar y Mapa son pestañas dentro del Feed**, no rutas aparte
(`feed_tab_explore` = "Explorar", `feed_tab_following` = "Siguiendo",
`feed_tab_map` = "Mapa", `feed_tab_news` = "Noticias").
Rutas con argumento: `vehicle/{id}`, `compare/{leftId}?rightId=`, `aiTuning?vehicleId=`,
`build/{id}`, `event/{id}`, `story/{index}`.

### Cómo se toca la UI
`testTagsAsResourceId` **no** está activado, así que los `testTag` no aparecen como
`resource-id` en `uiautomator dump`. Se toca por **`text` y `content-desc`** leídos del
dump, nunca por coordenadas fijas. Textos de `res/values/strings.xml`.

## Reglas de seguridad aplicadas a este recorrido
Firebase es real. **Solo navegar y mirar.** Prohibido en esta sesión:
publicar reseñas/historias/eventos/builds, dar like (`likeButton_*`), seguir
(`followButton`, `follow_*`), mandar mensajes (`storyReplySend`, chat),
unirse a eventos ("Me interesa" / "Voy a asistir"), borrar, y
**no tocar `Publicar` / `publishBuildButton` / `garagePublishButton`** en AI Tuning.
Si ya hay un Build guardado en el perfil, se muestra ese y no se genera nada nuevo.
Sin commits.

## Recorrido, pantalla por pantalla

| NN | Pantalla | Qué tocar | Qué debe verse en la captura |
|---|---|---|---|
| 01 | Splash | — (abrir la app) | logo sobre `#0A0A0A`, sin spinner |
| 02 | Login | escribir correo y contraseña demo en los campos "Correo electrónico" / "Contraseña"; **no** enviar aún | "Bienvenido de nuevo", campos llenos, teclado **cerrado** |
| 03 | Feed | tocar "Iniciar sesión"; saltar onboarding con "Saltar"/`onboardingSkip` si aparece | historias arriba cargadas (sin círculos grises) + una reseña con foto grande |
| 04 | Historia | tocar el primer círculo de historia | historia a pantalla completa, imagen cargada, barra de progreso visible |
| 05 | Explorar | cerrar historia; pestaña "Explorar"; tocar categoría "Deportivos" | grilla filtrada, todas las fotos cargadas, chip "Deportivos" activo |
| 06 | VehicleDetail | tocar un deportivo de la grilla | foto grande + ficha técnica (Potencia, Vel. máxima, Tracción) + "Reseñas de la comunidad" |
| 07 | Comparador | tocar "Comparar con otro carro"; elegir el segundo del selector | dos carros lado a lado + "Especificaciones" + "Comunidad" |
| 08 | AI Tuning — piezas | volver al detalle; tocar "Modificar con AI"; marcar 2–3 piezas | "Paquetes rápidos" / "Piezas" con selección y "Costo estimado" |
| 09 | AI Tuning — antes/después | si hay Build guardado, abrirlo desde el perfil; si no, **una** generación | slider con "ANTES" / "DESPUÉS", ambas imágenes nítidas |
| 10 | AI Tuning — Build | bajar al resultado | "Costo estimado" en COP + "Rendimiento" (Potencia, 0-100, Peso) + "Ficha técnica AI" |
| 11 | Mapa | pestaña "Mapa"; permitir ubicación; "Ver todas las reseñas" | varios marcadores de distintos colores, tiles cargados, sin `reviewsMapLoading` |
| 12 | EventDetail | tocar un marcador de Car Meet → "Ver evento" | portada del evento, fecha, "Organiza", características; **sin** tocar "Voy" |
| 13 | Perfil | pestaña de perfil | portada + avatar + pestaña "Garaje" con los carros |
| 14 | Garaje/Builds | pestaña de builds del perfil | builds guardados con imagen |

## Capturas y clips
- Barra de estado: demo mode de SystemUI (hora **9:41**, batería **100 %**, wifi lleno,
  sin notificaciones). Se sale del demo mode al terminar.
- Antes de cada captura: esperar imágenes y mapa; repetir si hay placeholder gris,
  spinner, teclado abierto, error, vacío o texto cortado. Cada captura se revisa a ojo.
- Capturas: `promo-assets/shots/NN-nombre.png` (1080×2400).
- Clips: `adb shell screenrecord --bit-rate 8000000 --time-limit 8` → `promo-assets/clips/NN-nombre.mp4`,
  con scrolls lentos y suaves (los clips van dentro del video).

## Video (Paso 4, skill `hyperframes` v0.8.61)
1080×1920, 30 fps, **36 s** (tope 40 s), sin audio. Teléfono Android sobre `#0A0A0A`
con resplandor `#B3282D` suave. Textos en español, máx. 6 palabras, blancos con acento
`#E05257`, **márgenes ≥120 px arriba y abajo**. 4 actos; el teléfono cambia de posición
entre actos. Transiciones 200–400 ms dentro del acto, 500 ms al cambiar de acto.
Ninguna escena por debajo de 2 s.

Exportes: `promo-assets/video-final.mp4` (1080×1920) y
`promo-assets/video-final-4x5.mp4` (1080×1350), H.264, `-pix_fmt yuv420p`, `+faststart`.
