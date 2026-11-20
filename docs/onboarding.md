# Onboarding y feed personalizado: implementación

El diseño y las decisiones están en [plan-onboarding.md](plan-onboarding.md).
Aquí está lo que quedó, cómo ponerlo a funcionar y cómo probarlo.

## 1. Qué hace

Después del **login, el registro o el splash** (con sesión abierta), la app pasa
por el onboarding.

- **Usuario que ya contestó o saltó antes:** la pantalla sigue de una vez al feed.
- **Usuario nuevo:** ve 6 pasos, y todos se pueden saltar con **"Ahora no"**.

| # | Paso | Se guarda en `preferences` |
|---|---|---|
| 1 | ¿Qué carros te mueven? (fotos del catálogo) | `categories` |
| 2 | Marcas favoritas (con buscador) | `brands` |
| 3 | ¿Tienes carro? Marca, modelo, año y tipo | `car` (y se crea en el garaje) |
| 4 | ¿Qué buscas en MotorMates? | `interests` |
| 5 | Tu ciudad (lista o GPS) | `city` |
| 6 | Personas que te pueden interesar (seguir / seguir a todos) | — (sigue de una vez) |

**Editar:** Editar perfil > **Editar mis gustos** abre los mismos pasos, sin el
6. Lo mismo hace la tarjeta "Personaliza tu feed" que aparece en Explorar
cuando el usuario no ha dicho qué le gusta.

### Cambios en la app

- **Explorar**
  - Arriba, un carrusel **"Para ti"** con hasta 5 vehículos que coinciden con los gustos y el motivo de cada uno.
  - Debajo, la lista completa, ordenada por el mismo puntaje.
- **Siguiendo:** si hay menos de 5 reseñas, se completa con:
  - **Personas que te pueden interesar** (con botón Seguir);
  - **Destacadas para ti**: reseñas de los carros que le gustan, con su corazón en vivo.
- **Mapa**
  - El primer encuadre es la ciudad del usuario: sus reseñas, o el centro de la ciudad si no hay.
  - Nuevo chip **"Mis categorías"**.
- **Garaje:** si el usuario dijo que tiene carro, se publica en su garaje sin foto. La foto se agrega después desde Editar perfil.

## 2. Arquitectura

```
ui/onboarding/      OnboardingScreen · OnboardingContent · OnboardingState · OnboardingViewModel
                    components/OnboardingComponents.kt
data/model/Preferences.kt              UserPreferences, CarRef, Interest
data/dto/PreferencesDto.kt             mapa `preferences` dentro de users/{uid}
data/source/PreferencesDataSource.kt   Firestore (mergeFields: no toca el resto del perfil)
data/source/CarCatalogDataSource.kt    assets/car_models_co.json + vPIC (Retrofit)
data/remote/service/VpicApiService.kt  API pública de la NHTSA, sin llave
data/repository/   PreferencesRepository · CarCatalogRepository
data/util/         FeedRanker · UserSuggester · CityCoordinates      (Kotlin puro, con pruebas)
data/injection/OnboardingDataModule.kt
```

**Puntaje de un vehículo** (`FeedRanker`):

| Criterio | Puntos |
|---|---|
| Categoría favorita | +3 |
| Marca favorita | +2 |
| Reseñas de las últimas 72 h en la ciudad del usuario (a menos de 30 km) | +1 |
| Popularidad | `ln(1 + reseñas) + rating promedio / 5` |

El motivo que se muestra es el criterio que más pesó. El cálculo se hace en
el teléfono con las reseñas de los últimos 30 días, así que no hacen falta
índices nuevos.

**Por qué una lista local además de vPIC:** se verificó la API el 2026-11-19.
- Cubre Mazda, Chevrolet, Kia, Toyota, Hyundai y Nissan, y responde en unos 0,3-1,5 s.
- Para 2020 no trae **ningún modelo de Renault**, de BYD solo trae buses y camiones, y de Suzuki devuelve motos.

Por eso se consulta con `vehicletype` car/mpv/truck y se combina con
`car_models_co.json`, que tiene 26 marcas del mercado colombiano. Si vPIC
falla o tarda más de 6 s, quedan los modelos locales, y el usuario siempre
puede escribir el suyo.

## 3. Puesta en marcha

1. **Desplegar las reglas de Firestore.** `firestore.rules` ahora valida el
   campo `preferences` de `users`:
   ```bash
   firebase deploy --only firestore:rules
   ```
   El comentario al inicio de ese archivo es importante: el repo es la única
   fuente de verdad, y desplegar borra lo que se haya editado a mano en la consola.
2. **Sembrar el catálogo ampliado:**
   - 29 vehículos, con fotos de Wikimedia Commons;
   - 15 usuarios con gustos y su `preferences`;
   - unas 146 reseñas repartidas en las 8 ciudades.
   ```bash
   cd seed
   node seed.js
   ```
   Usa `serviceAccountKey.json`, que nunca se sube al repo. Sobrescribe los
   documentos `seed_*` y los vehículos `"1".."29"`. Los datos de usuarios
   reales no se tocan.
3. **Nada más:** no hay llaves nuevas. vPIC es pública.

## 4. Pruebas

```bash
./gradlew :app:testDebugUnitTest
```

| Prueba | Qué cubre |
|---|---|
| `FeedRankerTest` (12) | Puntajes, desempate, ciudad, "Para ti", destacadas, textos |
| `UserSuggesterTest` (5) | Relevancia, excluidos, marca y ciudad, límite |
| `CityCoordinatesTest` (4) | Tildes, ciudad dentro de un texto libre, la más cercana |
| `PreferencesRepositoryImplTest` (8) | Conversión, guardar, saltar, errores, listener |
| `CarCatalogRepositoryImplTest` (6) | Local + vPIC, caída, timeout, caché, archivo real |
| `OnboardingViewModelTest` (16) | Pasos, tope, carro, sugerencias, seguir, guardar, editar, GPS |
| `FeedPersonalizationTest` (5) | Orden, "Para ti", reordenar al editar, Siguiendo |
| `ReviewsMapViewModelTest` (+3) | Ciudad inicial y chip "Mis categorías" |

**Pruebas de UI** (Compose, sin Firebase): `OnboardingContentTest` (6). Para
correrlas sin desinstalar la app del emulador, lo que borraría la sesión y el
token de App Check:

```bash
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.example.motormates.ui.onboarding.OnboardingContentTest \
  com.example.motormates.test/com.example.motormates.e2e.EmulatorTestRunner
```

**E2E:** las tres pruebas existentes tocan "Ahora no" con
`enterFeedSkippingOnboarding()` (en `E2EWaits.kt`) y siguen como antes.

## 5. Demo en clase

1. Registra una cuenta nueva en vivo y elige **SUV** y **Toyota**. Escribe
   "Mazda" en tu carro y muestra cómo llegan los modelos de vPIC.
2. Elige ciudad con **Usar mi ubicación** y sigue a 3 personas sugeridas.
3. El feed abre con **"Para ti"** y sus motivos, y Siguiendo ya tiene reseñas.
4. En otro emulador, una cuenta con **Deportivo** y **Porsche** ve el mismo
   catálogo en otro orden.
5. Editar perfil > **Editar mis gustos**: cambia de categoría y el feed se
   reordena al volver.

## 6. Pendiente y limitaciones

- **Banner de AI Tuning:** "Modifica tu {carro} con AI" no está, porque AI Tuning
  vive en `feat/ai-tuning` y todavía no está en `master`. Se agrega al unir
  las dos ramas. El interés `MODIFY` ya queda guardado.
- **Sugerencias:** salen de las reseñas de los últimos 30 días, con un máximo
  de 200 por consulta. Quien no ha reseñado no aparece.
- **El carro del onboarding se crea sin foto ni especificaciones.** Se
  completan desde Editar perfil > garaje.
