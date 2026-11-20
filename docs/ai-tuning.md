# AI Tuning: implementación y puesta en marcha

Guía técnica de la funcionalidad **AI Tuning** (Modificaciones con AI). El
diseño y las decisiones están en [plan-ai-tuning.md](plan-ai-tuning.md); aquí
está lo que quedó implementado, cómo configurarlo y cómo probarlo.

> Principio de diseño: **la AI dibuja y explica; el código calcula; el
> catálogo garantiza.** Precio y rendimiento nunca salen de la respuesta del
> modelo.

## 1. Qué hace

Desde el detalle de un vehículo, el botón **"Modificar con AI"** (o
**"Imagínalo modificado"** si el carro no es tuyo) abre el configurador:

| Modo | Qué elige el usuario | Qué genera |
|---|---|---|
| **Catálogo** | Paquetes (Street, Track, Show, Off-road) o una pieza por categoría: color, rines, aero, suspensión, escape, admisión, motor (ECU), luces y vidrios | Imagen + rendimiento antes/después + costo en COP + ficha técnica AI + precio en vivo con Google |
| **Tu pieza** | Foto (cámara o galería) de una pieza real; ML Kit le quita el fondo en el teléfono | Imagen con esa pieza instalada; botón para identificar la pieza y su precio con Google |
| **Tu idea** | Texto libre | Solo imagen, marcada como "sin precio ni specs verificados" |

En el resultado hay:
- un **deslizador antes/después** sobre la foto,
- **ajustes conversacionales** ("más bajo", "otro color") sobre la misma imagen,
- y los botones **Guardar** (privado) o **Publicar** (visible para la comunidad).

Los **Builds** guardados aparecen:
- en la pestaña **Builds** del perfil (con candado o globo según sean privados o públicos),
- en el perfil público del autor (solo los publicados),
- y en el carrusel **"Builds de la comunidad"** del detalle del vehículo.

## 2. Arquitectura

```
ui/aiTuning/       AiTuningScreen · AiTuningContent · AiTuningState · AiTuningViewModel
                   components/ (configurador, resultado, comparador, tarjetas)
ui/buildDetail/    BuildDetailScreen · BuildDetailViewModel
data/model/Tuning.kt                 Part, PartCategory, TuningPackage, TuningSummary, Build…
data/dto/TuningDtos.kt               DTOs para Firestore y Gson
data/source/
  PartsCatalogDataSource.kt          Firestore ("parts", "tuningPackages") + assets/parts_catalog.json
  GeminiTuningDataSource.kt          ÚNICO punto que usa firebase-ai
  TuningImageSource.kt               Coil, BitmapFactory, ML Kit (reduce a 1536 px, JPEG)
  AiUsageDataSource.kt               límite diario (transacción en aiUsage/{uid})
  BuildDataSource.kt                 Storage builds/{uid}/{id}.jpg + colección "builds"
  AiModelConfig.kt                   nombres de modelo y límite en Remote Config
data/repository/   PartsCatalogRepository · AiTuningRepository · BuildRepository
data/util/         SpecParser · SpecCalculator · CopFormat · TuningPromptBuilder
data/injection/AiTuningDataModule.kt
src/debug|release/AppCheckSetup.kt   App Check (depuración en debug, Play Integrity en release)
```

Llamadas a Gemini (Firebase AI Logic, backend Gemini Developer API):

| Llamada | Modelo por defecto | Configuración |
|---|---|---|
| Editar la foto | `gemini-3.1-flash-image` (Nano Banana 2) | `responseModalities = TEXT + IMAGE`, 4:3, chat multi-turno |
| Ficha técnica | `gemini-3.8-flash` (respaldo: `gemini-3.5-flash-lite`) | `responseMimeType = application/json` + `responseSchema` |
| Precio en vivo / identificar pieza | `gemini-3.8-flash` (respaldo: `gemini-3.5-flash-lite`) | `Tool.googleSearch()`, en una llamada aparte |

**Fórmulas** (en `SpecCalculator`, con pruebas):
- **Potencia:** se suman los porcentajes declarados, con un tope de 35 %.
- **Peso:** base típica por categoría (estimada) más la suma de los deltas de cada pieza.
- **0-100:** `t × ((peso/HP)nuevo ÷ (peso/HP)actual)^0.8` (estimado).
- **Costo:** suma de los mínimos y suma de los máximos.

## 3. Configuración en la consola de Firebase (una sola vez)

1. **Plan Blaze.** Los modelos de imagen (Nano Banana) lo exigen. Crea antes una
   **alerta de presupuesto** en Google Cloud > Facturación > Presupuestos.
2. **Firebase AI Logic.** En la consola: AI Logic > Comenzar, con el proveedor
   **Gemini Developer API**.
3. **Remote Config** (opcional; la app trae valores por defecto):

   | Parámetro | Tipo | Valor por defecto |
   |---|---|---|
   | `ai_image_model` | String | `gemini-3.1-flash-image` |
   | `ai_text_model` | String | `gemini-3.8-flash` |
   | `ai_text_fallback_model` | String | `gemini-3.5-flash-lite` |
   | `ai_daily_limit` | Number | `5` |
   | `ai_backend` | String | `vertex_ai` (o `google_ai` para la Gemini Developer API) |

   Con `vertex_ai` el uso se cobra a la cuenta de facturación de Google Cloud
   (sirven los créditos de la prueba gratuita, si aplican). Con `google_ai` se
   necesitan créditos de prepago comprados en AI Studio.

   Si el modelo de texto responde 503 (saturado), la app reintenta dos veces
   y luego usa el modelo de respaldo. Si Google cambia o retira un modelo, se actualiza aquí sin publicar otra
   versión de la app.
4. **App Check.** Registra la app con Play Integrity. En debug la app imprime
   en logcat un token (busca `DebugAppCheckProvider`); agrégalo en App Check >
   Administrar tokens de depuración. **No actives la aplicación forzosa**
   (enforcement) para AI Logic hasta haber registrado el token de cada
   celular de prueba.
5. **Reglas de Firestore.** Ya están en `firestore.rules` (parts,
   tuningPackages, builds y aiUsage). Despliégalas con:
   ```bash
   firebase deploy --only firestore:rules
   ```
6. **Reglas de Storage.** Este repositorio no versiona las reglas de Storage,
   así que agrega estas en la consola (Storage > Reglas) junto a las que ya
   tengas:
   ```
   match /builds/{uid}/{file} {
     allow read: if request.auth != null;
     allow write: if request.auth != null && request.auth.uid == uid
                  && request.resource.size < 10 * 1024 * 1024
                  && request.resource.contentType.matches('image/.*');
     allow delete: if request.auth != null && request.auth.uid == uid;
   }
   match /parts/{file} {
     allow read: if request.auth != null;
   }
   ```

## 4. Catálogo de piezas

- **La fuente única es `app/src/main/assets/parts_catalog.json`:** 28 piezas y 4
  paquetes. La app lo usa como respaldo si Firestore está vacío o no hay red,
  así que **la demo funciona aunque no se haya sembrado nada**.
- **Los precios y deltas actuales son estimados de referencia**
  (`priceSource: "estimado"`), y la app lo indica en pantalla. Antes de la
  sustentación conviene reemplazarlos por datos reales de tiendas colombianas:
  `priceMinCop`, `priceMaxCop`, `sourceUrl`, `priceCheckedAt` y
  `priceSource: "referencia"`.
- **Fotos:** guarda cada una en `seed/parts/images/<id>.jpg|png|webp` y corre:
  ```bash
  node seed.js --parts
  ```
  El script la sube a Storage y le pone su `imageUrl`. Una pieza con foto se
  envía a Gemini como imagen de referencia, así la AI instala ese diseño
  exacto. Una pieza sin foto se dibuja solo a partir de su `visualPrompt`.
- **Validación:** `PartsCatalogFileTest` revisa el JSON: ids únicos, que los
  paquetes apunten a piezas existentes, rangos de precio válidos, colores con
  su hex y compatibilidades existentes.

## 5. Pruebas

```bash
./gradlew :app:testDebugUnitTest
```
Son 73 pruebas nuevas en la JVM:

| Prueba | Pruebas |
|---|---|
| SpecParser | 6 |
| SpecCalculator | 8 |
| CopFormat | 5 |
| TuningPromptBuilder | 6 |
| Catálogo real | 9 |
| AiTuningRepository | 16 |
| PartsCatalogRepository | 5 |
| BuildRepository | 6 |
| AiTuningViewModel | 16 |

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.motormates.ui.aiTuning.AiTuningContentTest
```
Son 4 pruebas de UI en Compose, con el catálogo real y sin red ni AI: el
configurador, la selección, los clics y la pantalla de resultado.

**Prueba manual en un celular:**
1. Entra a Vehículo > **Modificar con AI** > paquete **Street** > **Generar**.
2. Revisa el deslizador, las specs y el costo.
3. Escribe "más bajo" en el ajuste.
4. **Publicar**.
5. Verifica que el build aparezca en Perfil > Builds y en "Builds de la
   comunidad" del vehículo.

## 6. Demo en el aula

- Cada imagen cuesta dinero y tarda unos 10-20 s. Genera y **guarda 2 o 3
  builds antes de la clase**: si falla el internet, se muestran desde Perfil >
  Builds.
- **"Tu pieza":** apunta la cámara a un carro de juguete o a una foto en el
  proyector; luego **Identificar pieza y precio**.
- El límite diario (5 por defecto) se puede subir para la demo con
  `ai_daily_limit` en Remote Config. Las reglas imponen un techo duro de 50.

## 7. Limitaciones conocidas

- La imagen es una **visualización aproximada**: los logos y detalles finos
  pueden salir deformados, y no garantiza compatibilidad física de la pieza.
- El peso base y el 0-100 son **estimados**: el catálogo de vehículos no
  guarda el peso.
- El límite diario se cuenta en el cliente, con reglas que solo permiten
  subirlo de a uno. Para algo en producción habría que moverlo a una Cloud
  Function.
- Los vehículos del seed no tienen `ownerId`, así que el botón dice
  "Imagínalo modificado" para todos.
