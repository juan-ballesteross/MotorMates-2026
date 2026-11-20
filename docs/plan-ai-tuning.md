# Plan: AI Tuning (Modificaciones con AI) — MotorMates

> **Estado (2026-11-17):** fases 0-4 implementadas en código (ver
> [ai-tuning.md](ai-tuning.md) para la guía de configuración y pruebas).
> Pendiente fuera del código: activar Blaze, AI Logic, App Check y las reglas
> de Storage en la consola; reemplazar los precios estimados del catálogo por
> datos reales de tiendas y agregar fotos de las piezas.

## Contexto
MotorMates necesita funcionalidades que destaquen en la sustentación (dificultad técnica + valor de negocio) y que se puedan **demostrar dentro del aula**, sin carro real. "AI Tuning" es la funcionalidad estrella: el usuario elige piezas reales de un catálogo (rines, alerón, escape, color…) y Gemini genera la foto de su carro modificado, junto con precio total en COP y cambios estimados de rendimiento.

Principio de diseño: **la AI dibuja y explica; el código calcula; el catálogo garantiza.** Precios y deltas de rendimiento salen de datos controlados por el equipo, no inventados por el modelo.

App solo para uso académico → se permiten fotos reales de tiendas con crédito (`sourceUrl`), no publicar en Play Store con ellas.

## Decisiones tomadas
| Tema | Decisión |
|---|---|
| Edición de imagen | `gemini-nano-banana-2.1` vía Firebase AI Logic (requiere plan **Blaze**, feature en Preview) |
| Ficha técnica (JSON) | `gemini-3.8-flash` con `responseSchema` |
| Precio en vivo (opcional) | `gemini-3.8-flash` + `Tool.googleSearch()`, llamada separada; mostrar fuentes y `searchEntryPoint` (obligatorio) |
| Nombres de modelo | En **Remote Config** (`ai_image_model`, `ai_text_model`), con default en código |
| Selección | Catálogo predefinido (una pieza por categoría) + paquetes Street/Track/Show; modos extra: "Trae tu pieza" (cámara) y "Describe tu idea" (solo imagen) |
| Imágenes de piezas | Fotos reales de tiendas colombianas, curadas a mano, con `sourceUrl`; fondo removido si hace falta |
| Precios | Rango `precioMin–precioMax` COP + fecha de consulta; opcional precio en vivo con fuentes |
| Rendimiento | Delta declarado por fabricante en catálogo + `SpecCalculator` propio (Kotlin puro) |

## Arquitectura (sigue los patrones existentes)
```
ui/aiTuning/            AiTuningScreen, AiTuningContent, AiTuningState, AiTuningViewModel, components/
data/model/             Part.kt, Build.kt, TunedSpecs.kt
data/dto/               PartFirestoreDto.kt, BuildFirestoreDto.kt (constructores vacíos + toDomain(), como VehicleFirestoreDto)
data/source/            PartsCatalogDataSource (+ FirestorePartsCatalogDataSource)
                        GeminiTuningDataSource (+ FirebaseAiTuningDataSource)  ← único punto que toca firebase-ai
                        BuildDataSource (+ FirestoreBuildDataSource, sube imagen con putBytes)
data/repository/        PartsCatalogRepository, AiTuningRepository, BuildRepository (Result<T> + mensajes en español, como ProfileImageRepository)
data/util/              SpecCalculator.kt, SpecParser.kt ("502 hp" → 502, "3.4 s" → 3.4)
data/injection/         AiTuningDataModule.kt (abstract @Binds, patrón de StoryDataModule; solo Firebase, sin pick())
```

### Reutilizar
- Ruta con argumento: patrón de `ScreenRoute.VehicleDetail` / `NewReview` en `navigation/ScreenRoute.kt`; registro con `navArgument` en `navigation/AppNavigation.kt` (args vía `entry.arguments?.getString(...)` → parámetro del Screen, el ViewModel recibe `load(vehicleId)` como `VehicleDetailViewModel`).
- Subida a Storage: forma de `FirebaseProfileImageDataSource.uploadProfileImage` (`data/source/ProfileImageDataSource.kt`) pero con `putBytes`; instancia `FirebaseStorage` ya provista en `di/ProfileImageModule.kt`.
- Firestore: `FirestoreModule.provideFirestore()` (DB `motormatesdb`); añadir `PARTS = "parts"`, `BUILDS = "builds"` a `data/source/FirestoreCollections.kt`.
- Usuario actual: `AuthRepository.getCurrentUser()`.
- Tests: `MainDispatcherRule`, mockk + Truth como en `app/src/test/.../ui/review/NewReviewViewModelTest.kt`.
- Seeder: `seed/seed.js` (`commitInBatches`, ids fijos).

### Modelo de datos
**`parts/{id}`**: `name, brand, category (COLOR|RINES|AERO|SUSPENSION|ESCAPE|ADMISION|ECU|LUCES), imageUrl, sourceUrl, priceMinCop, priceMaxCop, priceCheckedAt, hpDeltaPct, torqueDeltaPct, weightDeltaKg, heightDeltaMm, compatibleCategories[], visualPrompt, perfSource ("fabricante"|"sin dato")`
Colores: categoría `COLOR` con `hex`, `finish` (brillante/mate/satinado/cromado) y precio por tipo de trabajo (wrap parcial/completo, pintura).

**`builds/{id}`**: `vehicleId, ownerId, baseImageUrl, resultImageUrl, partIds[], mode (CATALOG|OWN_PART|FREE), totalMinCop, totalMaxCop, specsBefore{hp,torqueNm,weightKg,zeroTo100}, specsAfter{…}, insights{resumen, manejo, consumo, garantia, legal[]}, sources[], published, createdAt`
**`aiUsage/{uid}`**: `date, count` → límite 5 generaciones/día.

Storage: `builds/{uid}/{buildId}.jpg`.

### Vehículo
`Vehicle` tiene specs como strings ("502 hp"). No se migra: `SpecParser` extrae números; si falta un dato (p. ej. peso), se usa un valor por categoría marcado "estimado".

## Flujo de usuario
1. `VehicleDetailContent` → nuevo botón **"Modificar con AI ✨"** junto a "Escribir reseña". Texto "Imagínalo modificado" si el usuario no es dueño (`isOwner = vehicle.ownerId == currentUser.id`; los vehículos del seed no tienen `ownerId`, así que el botón siempre aparece). Callback `onAiTuningClick` → Screen → lambda en `AppNavigation`.
2. **Configurador** (`aiTuning/{vehicleId}`): foto del carro arriba; paquetes rápidos; chips de categoría; cuadrícula de tarjetas (imagen, nombre, rango de precio, insignia `+12 HP`); una por categoría; solo compatibles. Barra inferior fija: nº piezas · total COP · Δ HP (calculado al instante, sin AI).
3. **Generar**: en paralelo (`async`) → imagen (foto carro + fotos piezas + prompt armado desde `visualPrompt`s: "instala exactamente estas piezas… mismo ángulo, fondo e iluminación, no cambies nada más") y ficha JSON (recibe specs ya calculadas, solo explica). Animación de carga 10–20 s.
4. **Resultado**: slider antes/después, tarjeta de specs antes→después, desglose de costo, insights + legal con fuentes, botón "Ver precio actual" (grounding).
5. **Iterar**: chat multi-turno de Nano Banana ("más bajo", "otro color").
6. **Guardar / Publicar** el Build (Storage + Firestore); pestaña "Builds" en perfil (fase 3).

Modos extra: **Trae tu pieza** (Photo Picker/cámara → ML Kit Subject Segmentation quita fondo → misma edición; opcional identificar pieza y precio con grounding). **Describe tu idea** (texto libre → solo imagen, etiqueta "sin precio ni specs verificados").

## SpecCalculator (Kotlin puro, testeable)
- HP/torque: aplicar `hpDeltaPct` acumulado de piezas del catálogo (ECU y escape/admisión no se suman linealmente sin tope: limitar suma total a lo declarado).
- Peso: `peso + Σ weightDeltaKg`.
- 0–100: `t_nuevo ≈ t_actual × ((peso/HP)_nuevo ÷ (peso/HP)_actual)^0.8` → etiquetado "estimado".
- Precio: `Σ priceMin`, `Σ priceMax`.

## Dependencias y config
- `libs.versions.toml` / `app/build.gradle.kts`: `firebase-ai`, `firebase-config`, `firebase-appcheck-playintegrity` (+ `-debug` en debug), ML Kit subject segmentation (fase 3).
- Consola Firebase: activar plan Blaze + **alerta de presupuesto**, habilitar Firebase AI Logic (Gemini Developer API), App Check, parámetros de Remote Config.
- Comprimir imágenes antes de enviar (<7 MB c/u).

## Catálogo (contenido)
~24 piezas: 3–4 por categoría (rines, aero, escape, suspensión, admisión, ECU, luces) + paleta de ~8 colores con acabados. Proceso: hoja compartida → buscar en tiendas colombianas (foto + precio + dato de fabricante) → descargar a `seed/parts/images/` → `seed/parts/parts.json` → nueva función `seedParts()` en `seed/seed.js` que sube imágenes a Storage (`parts/{id}.png`) y escribe la colección `parts` con ids fijos. Claude puede hacer la búsqueda y armar la tabla; descargar imágenes requiere aprobación explícita.

## Fases
| Fase | Alcance | Esfuerzo |
|---|---|---|
| 0. Preparación | Blaze + alerta presupuesto, dependencias, Remote Config, App Check, catálogo + `seedParts()` | ½ sprint |
| 1. MVP | Ruta + botón en VehicleDetail, configurador, `SpecCalculator`/`SpecParser` con tests, generación de imagen, slider antes/después | 1 sprint |
| 2. Insights | Ficha JSON, desglose COP, guardar Build, límite diario | ½ sprint |
| 3. Wow | Edición multi-turno, precio en vivo con fuentes, "Trae tu pieza" + ML Kit, publicar Build en feed, pestaña Builds | 1 sprint |
| 4. Libre | "Describe tu idea" | bajo |

Reparto sugerido (4 integrantes): (1) UI configurador/resultado, (2) AiTuningRepository + Gemini + Remote Config/App Check, (3) catálogo + seeder + Firestore/Storage de Builds, (4) SpecCalculator, tests y guion de demo.

## Riesgos
- Costo por imagen → límite diario, alerta de presupuesto, cachear Builds.
- Latencia / caída de internet en clase → 2–3 Builds precargadas como respaldo.
- API de imagen en Preview y nombres de modelo cambiantes → Remote Config.
- Logos/detalles finos deformados → comunicar "visualización aproximada".
- Storage no emulado en androidTest → probar repositorio de Builds con mocks.

## Verificación
- **Unit tests** (`app/src/test`): `SpecParserTest`, `SpecCalculatorTest` (deltas, tope, 0–100, totales), `AiTuningViewModelTest` con repos mockk (selección una por categoría, totales en vivo, estados loading/éxito/error, límite diario).
- **Build**: `./gradlew :app:assembleDebug` y `./gradlew :app:testDebugUnitTest`.
- **Integración**: con emulador Firestore, correr `seedParts()` y verificar que el configurador lista las piezas.
- **Manual en dispositivo**: VehicleDetail → Modificar con AI → paquete Street → Generar → ver slider, specs y costo → iterar "más bajo" → guardar → ver Build en Firestore/Storage.
- **Ensayo de demo**: Hot Wheels/foto en proyector con "Trae tu pieza"; Builds de respaldo cargadas.
