# Plan: Onboarding de preferencias y feed personalizado (MotorMates)

> Estado: **implementado** en la rama `feat/onboarding` (fases 0 a 4). Cómo quedó, cómo se configura y qué falta: [onboarding.md](onboarding.md). El banner de AI Tuning (sección 4.5) queda pendiente hasta que esa funcionalidad llegue a `master`.

## 1. Contexto y problema

Hoy, al registrarse, el usuario cae directo al feed:

| Pestaña | Qué pasa hoy | Problema |
|---|---|---|
| Explorar | Los 8 vehículos del catálogo, siempre en el mismo orden | No hay ningún criterio |
| Siguiendo | Reseñas de las personas que sigues | **Vacía para un usuario nuevo**: es lo primero que ve |
| Mapa | Reseñas de las últimas 24 h | Sin filtro y sin centrar en la ciudad del usuario |

No existe onboarding ni preferencias guardadas. En el perfil, la ciudad es
texto libre.

**Objetivo:** en menos de un minuto, saber qué le gusta al usuario y usarlo
para:
- ordenar Explorar,
- llenar Siguiendo desde el primer momento,
- centrar el mapa,
- destacar las funciones que le interesan (por ejemplo, AI Tuning).

**Criterios de evaluación:**
- **Dificultad técnica media:** flujo de varios pasos, una API externa, un ranking probado y cambios en la navegación.
- **Valor de negocio alto:** retención del usuario nuevo y solución del "cold start".
- **Se puede demostrar en clase sin carro.**

## 2. Preguntas del onboarding

Son 6 pantallas cortas con una barra de progreso, y **todas se pueden
saltar**. Si el usuario no responde, la app funciona como hoy.

| # | Pregunta | Tipo de respuesta | Para qué se usa |
|---|---|---|---|
| 1 | ¿Qué carros te mueven? | Selección múltiple de las categorías existentes (Deportivo, SUV, Clásico, Eléctrico, Pickup), con foto | Ranking de Explorar y de reseñas; filtro del mapa |
| 2 | Marcas favoritas | Chips con buscador. Las marcas salen del catálogo y, si la API responde, de la API de marcas | Dar más prioridad a esas marcas |
| 3 | ¿Tienes carro? ¿Cuál? | Sí/No; marca, modelo y año con autocompletado desde la **API NHTSA vPIC** | Crear el carro en el garaje del perfil; banner "Modifica tu {carro} con AI" |
| 4 | ¿Qué buscas en MotorMates? | Selección múltiple: reseñas antes de comprar · modificar mi carro · conocer gente · fotografía | Decidir qué funciones se destacan en el feed |
| 5 | Tu ciudad | Lista de ciudades o GPS | Centrar el mapa; dar puntos al contenido cercano; rellenar `location` del perfil si está vacío |
| 6 | Personas que te pueden interesar | Lista de usuarios sugeridos con botón Seguir | Llenar Siguiendo desde el primer minuto |

### API real para el paso 3: NHTSA vPIC

- Es gratis y no necesita API key. Base: `https://vpic.nhtsa.dot.gov/api/vehicles/`.
- Endpoints útiles:
  - `GetMakesForVehicleType/car?format=json`
  - `GetModelsForMakeYear/make/{marca}/modelyear/{año}?format=json`
- **Antes de implementar hay que verificar:**
  - que cubra las marcas comunes en Colombia: Chevrolet, Renault, Mazda, Kia, Toyota, Nissan, Suzuki, Hyundai;
  - cuánto tarda en responder.
- Si la API falla, el usuario puede escribir marca y modelo a mano.
- Se llama con Retrofit, que el proyecto ya usa. Va en su propio data source, con un timeout corto.

## 3. Modelo de datos

Las preferencias se guardan en el documento del usuario, `users/{uid}`, en un
mapa nuevo:

```
preferences: {
  categories: ["SUV", "Deportivo"],        // mismas etiquetas que vehicles.category
  brands: ["Toyota", "Porsche"],
  interests: ["BUY_RESEARCH", "MODIFY", "SOCIAL", "PHOTO"],
  city: "Bogotá",
  car: { brand: "Mazda", model: "3", year: 2020 } | null,
  completed: true,
  skipped: false,
  updatedAt: <epoch ms>
}
```

- **Reglas de Firestore:** solo el dueño puede escribir `preferences`. Se valida que las listas tengan un tamaño razonable, por ejemplo hasta 10 elementos.
- **Carro del usuario:** con "Tengo carro", se crea un documento en `vehicles` con `ownerId`, igual que el garaje actual. Así funciona el botón "Modificar con AI" de dueño.
  - Solo se crea al terminar el onboarding y con confirmación del usuario.
  - **Pendiente de decidir:** si se crea con la foto de la marca, o si se le pide al usuario una foto.

## 4. Cambios en la app

### 4.1 Navegación

| Situación | Antes | Ahora |
|---|---|---|
| Registro | Va al Feed | Va al **Onboarding** y después al Feed |
| Splash o login | Va al Feed | Si `preferences.completed` es falso y `skipped` también, va al Onboarding |
| Usuarios que ya existen | — | Se les ofrece **una vez** (se puede saltar) |
| Perfil | — | Nueva opción **"Editar preferencias"**, que abre la misma pantalla con los datos actuales |

### 4.2 Explorar

- **Carrusel "Para ti":** arriba, los 5 vehículos con mayor puntaje. Cada tarjeta muestra el motivo ("Porque te gustan los SUV", "Es de Toyota", "Popular en Bogotá").
- **Lista completa** debajo, ordenada por el mismo puntaje.

### 4.3 Siguiendo

- **Sin nadie seguido:** se muestran las sugerencias de personas y un botón "Seguir a todos".
- **Feed corto (menos de 5 reseñas):** se completa con "Reseñas destacadas de tus categorías", con su propia etiqueta, para que el feed nunca esté vacío.

### 4.4 Mapa

- Arranca centrado en la ciudad del usuario, con coordenadas fijas para las 8 ciudades del seed.
- Nuevo chip **"Solo mis categorías"**.

### 4.5 Otras funciones

- **AI Tuning:**
  - Si el usuario eligió "modificar mi carro" **y** registró su carro, aparece un banner en Explorar: "Modifica tu {marca} {modelo} con AI".
  - Si solo eligió "modificar mi carro", el banner abre el catálogo.
- **Búsqueda:** el chip inicial es la categoría favorita en lugar de "Todos". Es un cambio menor.

## 5. Ranking: `FeedRanker` (Kotlin puro, con pruebas)

Puntaje de cada vehículo:

| Criterio | Puntos |
|---|---|
| Su categoría está en `preferences.categories` | +3 |
| Su marca está en `preferences.brands` | +2 |
| Tiene reseñas de las últimas 72 h hechas en la ciudad del usuario | +1 |
| Popularidad: `log(1 + reseñas) + rating promedio / 5` | 0 a ~2 |
| Es el carro del usuario | Se excluye de "Para ti" |

- **Desempate:** por popularidad, luego por id. Así el orden es estable y se puede probar.
- **Motivo de la recomendación:** se muestra el criterio que más puntos aportó.
- **Reseñas:** el puntaje del vehículo, más +1 si la reseña es de las últimas 24 h, más `likes / 10` con un tope de +1.
  - Las reseñas no guardan la categoría del vehículo. Se cruzan en memoria con el catálogo, que es pequeño. No hace falta migrar datos.
- **Sugerencias de personas (paso 6):**
  - Se toman los autores de reseñas sobre vehículos de las categorías o marcas elegidas.
  - Se ordenan por likes recibidos y luego por cantidad de reseñas.
  - Se excluye al propio usuario y a quienes ya sigue. Máximo 8.
- **Sin preferencias:** el ranking queda solo por popularidad, que es casi el comportamiento de hoy.

Todo se calcula en el teléfono y no hacen falta índices compuestos nuevos.
Las fuentes ya existen: `getVehicles`, `observeReviewsByUsers` y
`observeRecentReviews`.

## 6. Arquitectura (sigue los patrones existentes)

```
ui/onboarding/      OnboardingScreen · OnboardingContent · OnboardingState · OnboardingViewModel
                    components/ (CategoryPicker, BrandPicker, CarPicker, InterestPicker, CityPicker, SuggestedUsers)
data/model/         Preferences.kt (UserPreferences, Interest, CarRef)
data/dto/           PreferencesDto (mapa dentro de UserDto)
data/source/        PreferencesDataSource (Firestore) · VpicDataSource (Retrofit)
data/repository/    PreferencesRepository · CarCatalogRepository (vPIC con caché y respaldo local)
data/util/          FeedRanker · UserSuggester · CityCoordinates
navigation/         ScreenRoute.Onboarding (+ modo edición), cambios en AppNavigation y SplashViewModel
ui/feed/            FeedViewModel usa FeedRanker y PreferencesRepository; carrusel "Para ti", banner AI
ui/reviewsMap/      centro inicial y chip "Solo mis categorías"
ui/user/            opción "Editar preferencias"
```

- Inyección con Hilt: un `@Binds` para las implementaciones de Firebase, igual que `AiTuningDataModule`.
- Repositorios con `Result<T>` y mensajes en español.

## 7. Contenido para que se note en la demo

**Con 8 vehículos, la personalización casi no se ve.** Hay que ampliar el seed:
- **unos 30 vehículos**, 6 por categoría, con marcas comunes en Colombia
  (Mazda, Chevrolet, Renault, Kia, Toyota, Hyundai, Nissan, BMW,
  Mercedes-Benz, Ford, Jeep, Tesla, BYD, Porsche);
- reseñas repartidas entre las 8 ciudades, para que se note "Popular en tu ciudad";
- **unos 15 usuarios del seed**, con gustos marcados (fans de SUV, de clásicos, etc.), para que las sugerencias tengan sentido.

Se actualizan `seed/seed.js` y `data/seed/SeedData.kt` para que sigan iguales.

## 8. Fases

| Fase | Alcance | Esfuerzo |
|---|---|---|
| 0. Contenido | Ampliar el seed (vehículos, reseñas, usuarios); verificar la cobertura de vPIC | ½ sprint |
| 1. Onboarding | Pantallas 1, 2, 4 y 5, guardar preferencias, navegación (registro, splash, editar desde el perfil) | 1 sprint |
| 2. Personalización | `FeedRanker` con pruebas, carrusel "Para ti" con motivos, orden de Explorar, mapa centrado y filtro | ½ sprint |
| 3. Cold start | Paso 6 (`UserSuggester` y "Seguir a todos"), Siguiendo con sugerencias y reseñas destacadas | ½ sprint |
| 4. Tu carro | Paso 3 con vPIC, creación del vehículo propio, banner de AI Tuning | ½ sprint |

**Reparto sugerido para 4 personas:**
1. Pantallas del onboarding y navegación.
2. `FeedRanker`, `UserSuggester` y sus pruebas.
3. Seed ampliado y vPIC.
4. Cambios en Feed, mapa y perfil, y el guion de la demo.

## 9. Riesgos

| Riesgo | Mitigación |
|---|---|
| vPIC no cubre bien el mercado colombiano o se cae | Entrada manual y lista local de respaldo con las marcas del seed |
| Onboarding largo, el usuario abandona | Todo se puede saltar, hay barra de progreso, máximo unos 45 s |
| Usuarios existentes molestos por el onboarding | Se ofrece una sola vez y se marca `skipped` |
| Ranking "raro" o difícil de explicar | Motivo visible en cada tarjeta y pruebas con casos fijos |
| Personalización imperceptible | Seed ampliado (sección 7) |
| Las reglas de `users` rechazan el nuevo campo | Actualizar `firestore.rules` y desplegar antes de probar |

## 10. Verificación

- **Pruebas unitarias:**
  - `FeedRankerTest`: puntajes, desempate, motivo, sin preferencias, carro propio excluido.
  - `UserSuggesterTest`.
  - `OnboardingViewModelTest`: pasos, saltar, guardar, error de red, modo edición.
  - Pruebas del repositorio de preferencias y de vPIC con mocks.
- **Pruebas de UI (Compose):** el onboarding completo con datos falsos, y el carrusel "Para ti" con un estado armado a mano.
- **E2E, ya hay infraestructura en `androidTest/e2e`:** registro → onboarding → feed muestra "Para ti" con la categoría elegida.
- **Manual en emulador:**
  1. Cuenta A elige SUV y Toyota; cuenta B elige Deportivo y Porsche. Los dos feeds se ven distintos.
  2. Editar preferencias desde el perfil reordena el feed al instante.
  3. Con una cuenta nueva, Siguiendo muestra sugerencias en vez de quedar vacía.

## 11. Guion de demo en clase (2 minutos)

1. Registrar una cuenta nueva en vivo y mostrar el onboarding: categorías, marcas, "tengo un Mazda 3" (autocompletado real) y ciudad.
2. Seguir a 3 personas sugeridas con un toque.
3. El feed muestra "Para ti" con sus motivos y Siguiendo ya tiene reseñas.
4. Poner al lado un segundo emulador con otros gustos: el mismo catálogo se ve distinto.
5. El banner "Modifica tu Mazda 3 con AI" enlaza con la funcionalidad de AI Tuning.
