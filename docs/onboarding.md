# Onboarding y feed personalizado: implementación

El diseño y las decisiones están en [plan-onboarding.md](plan-onboarding.md).
Aquí está lo que quedó, cómo ponerlo a funcionar y cómo probarlo.

## 1. Qué hace

Después del **login, el registro o el splash** (con sesión abierta), la app pasa
por el onboarding.

- **Usuario que ya contestó o saltó antes:** la pantalla sigue de una vez al feed.
- **Usuario nuevo:** ve 5 pasos, y todos se pueden saltar con **"Ahora no"**.

| # | Paso | Se guarda en `preferences` |
|---|---|---|
| 1 | ¿Qué carros te mueven? (fotos del catálogo) | `categories` |
| 2 | Marcas favoritas (con buscador) | `brands` |
| 3 | ¿Qué buscas en MotorMates? | `interests` |
| 4 | Tu ciudad (lista o GPS) | `city` |
| 5 | Personas que te pueden interesar (seguir / seguir a todos) | — (sigue de una vez) |

El onboarding **no pregunta por el carro** ni agrega nada al garaje: el
usuario publica sus carros desde Perfil > Garaje > **Agregar vehículo**. Si
alguien lo había contestado en una versión anterior, `car` se conserva.

**Editar:** Editar perfil > **Editar mis gustos** abre los mismos pasos, sin el
5. Lo mismo hace la tarjeta "Personaliza tu feed" que aparece en Explorar
cuando el usuario no ha dicho qué le gusta.

### Cambios en la app

- **Explorar**
  - Arriba, un carrusel **"Para ti"** que abre con las herramientas AI (**Modificar con AI** y **Encontrar pieza**) y sigue con hasta 5 vehículos que coinciden con los gustos y el motivo de cada uno.
  - Debajo, la lista completa, ordenada por el mismo puntaje.
- **Siguiendo:** si hay menos de 5 reseñas, se completa con:
  - **Personas que te pueden interesar** (con botón Seguir);
  - **Destacadas para ti**: reseñas de los carros que le gustan, con su corazón en vivo.
- **Mapa**
  - El primer encuadre es la ciudad del usuario: sus reseñas, o el centro de la ciudad si no hay.
  - Nuevo chip **"Mis categorías"**.

## 2. Arquitectura

```
ui/onboarding/      OnboardingScreen · OnboardingContent · OnboardingState · OnboardingViewModel
                    components/OnboardingComponents.kt
data/model/Preferences.kt              UserPreferences, CarRef, Interest
data/dto/PreferencesDto.kt             mapa `preferences` dentro de users/{uid}
data/source/PreferencesDataSource.kt   Firestore (mergeFields: no toca el resto del perfil)
data/repository/   PreferencesRepository
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
3. **Nada más:** no hay llaves nuevas.

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
| `OnboardingViewModelTest` (12) | Pasos (sin carro), tope, sugerencias, seguir, guardar, editar, GPS |
| `FeedPersonalizationTest` (5) | Orden, "Para ti", reordenar al editar, Siguiendo |
| `ReviewsMapViewModelTest` (+3) | Ciudad inicial y chip "Mis categorías" |

**Pruebas de UI** (Compose, sin Firebase): `OnboardingContentTest` (5). Para
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

1. Registra una cuenta nueva en vivo y elige **SUV** y **Toyota**.
2. Elige ciudad con **Usar mi ubicación** y sigue a 3 personas sugeridas.
3. El feed abre con **"Para ti"** y sus motivos, y Siguiendo ya tiene reseñas.
4. En otro emulador, una cuenta con **Deportivo** y **Porsche** ve el mismo
   catálogo en otro orden.
5. Editar perfil > **Editar mis gustos**: cambia de categoría y el feed se
   reordena al volver.

## 6. Pendiente y limitaciones

- **Sugerencias:** salen de las reseñas de los últimos 30 días, con un máximo
  de 200 por consulta. Quien no ha reseñado no aparece.
