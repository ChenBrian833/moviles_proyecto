#  Eztunez

Afinador de guitarra nativo para Android, offline, sin anuncios y con soporte
para distintas afinaciones.

## El problema

Los músicos principiantes e intermedios necesitan afinar su instrumento de
forma rápida y confiable antes de tocar, pero muchos afinadores disponibles
hoy tienen anuncios invasivos, detección de tono poco precisa, o dependen de
internet para algo que debería funcionar 100% offline.

## Propuesta de valor

Una app simple que escucha la cuerda a través del micrófono del teléfono,
detecta la nota más cercana y le dice al usuario si tiene que tensar o
destensar la cuerda — sin conexión a internet, sin pagos y sin publicidad
que interrumpa el proceso de afinación.

## Usuarios objetivo

Estudiantes de guitarra, músicos principiantes, guitarristas aficionados y
profesores de música que necesitan una herramienta rápida durante ensayos o
presentaciones.

## Plataforma y stack técnico

| | |
|---|---|
| Sistema operativo | Android (minSdk 28, compileSdk 36) |
| Lenguaje | Kotlin |
| UI | Jetpack Compose |
| Detección | AudioRecord + TarsosDSP YIN |
| Preferencias | Preferences DataStore |

## Cómo correr el proyecto

1. Abrir la carpeta del proyecto en Android Studio.
2. Dejar que Gradle sincronice.
3. Conectar un dispositivo físico o levantar un emulador (API 28 o superior).
4. Correr la app ( Run 'app').

Desde terminal, con `JAVA_HOME` apuntando al JBR/JDK compatible:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Estado actual del proyecto

- [x] Captura real del micrófono con `AudioRecord`
- [x] Detección de pitch con TarsosDSP/YIN
- [x] Auto Tune y Manual Tune para E2, A2, D3, G3, B3 y E4
- [x] Aguja, cents y mensajes Tune Up / Tune Down / In Tune
- [x] Ajustes persistentes de sensibilidad, A4, Hz, notación y tema
- [x] Estados básicos de permiso, inicialización e interrupción del micrófono
- [x] Presets Standard, Drop D, Open G, DADGAD y Half Step Down conectados al motor
- [ ] FFT avanzada (no disponible; la interfaz la muestra deshabilitada)

## Verificación manual de la fase Manual Tune + Settings

En un teléfono físico con una fuente de sonido adecuada:

1. Conceder el permiso de micrófono y comprobar Auto Tune en las seis cuerdas.
2. Abrir Manual Tune, seleccionar cada objetivo y verificar que no cambie automáticamente.
3. Cambiar de cuerda y confirmar que se borren la lectura y la aguja anteriores.
4. Probar notas bajas/altas y confirmar Tune Up, Tune Down e In Tune.
5. Cambiar sensibilidad y A4; volver al afinador y comprobar su efecto.
6. Ocultar Hz, cambiar entre Standard/Solfège y alternar el tema.
7. Cerrar por completo y reabrir la app para confirmar la persistencia.
8. Denegar el permiso, reintentar y probar el acceso a ajustes cuando quede bloqueado.

La compilación automatizada no sustituye esta verificación acústica en hardware real.

## Arquitectura

- `MainActivity` crea los ViewModels con factories e inyección manual. La captura
  utiliza el contexto de aplicación; el ViewModel no conserva una Activity.
- `TunerRoute` conecta permisos, ajustes de Android y duración de la pantalla con
  `TunerViewModel`. `TuneScreen` recibe estado de solo lectura y callbacks.
- `TunerViewModel` coordina `TuningTracker` y la interfaz `PitchCapture` mediante
  `StateFlow`. `GuitarPitchDetector` implementa la captura; YIN y sus umbrales no
  cambian. Los callbacks de capturas anteriores se descartan al detener o reintentar.
- `QueuedPitchCaptureFactory` ejecuta la creación, inicio y parada del driver en
  una cola de trabajo compartida. La UI solicita la parada y limpia su lectura sin
  esperar operaciones nativas ni `join`. La siguiente captura espera a que termine
  la limpieza anterior, incluso si el ViewModel ya fue eliminado. El hilo lector
  libera su `AudioRecord` en `finally`, también si falla la recuperación.
- `TuningPresetsRoute` conecta la pantalla con `TuningPresetsViewModel`; el catálogo
  local vive en `TuningPresetRepository`. Cada preset define seis notas MIDI con
  octavas. La selección compartida configura Home, Tune, el tracker y las referencias
  del analizador. Los objetivos se recalculan según el A4 elegido en Settings.
- `SettingsRoute` conecta `SettingsScreen` con el ViewModel y el repositorio
  DataStore existentes. Home conserva únicamente estado visual local.

La pantalla actual y el modo de afinación usan `rememberSaveable`. Al rotar, los
ViewModels conservan el objetivo manual y los filtros/selección de presets; el
registro anterior del micrófono se detiene y la nueva pantalla inicia otra captura.
La sesión del afinador se reinicia al navegar a otra pantalla, conservando el preset
activo. Salir de Presets reinicia solo su filtro. La selección dura durante la sesión
de la aplicación y la rotación; una apertura nueva usa Standard. No se guardan
muestras de audio en el estado de Android.

El indicador de carga aparece únicamente mientras llega la primera lectura de
DataStore, sin temporizador ni calibración de micrófono simulada. Los ajustes ya
cargados se conservan durante la recreación. La captura solo se inicia con permiso
y con el afinador en estado `RESUMED`. Al pausar, bloquear o dejar la aplicación se
solicita liberar el micrófono en segundo plano; al volver se comprueba el permiso y se inicia una nueva captura
con el mismo objetivo. Los reintentos usan la misma condición de ciclo de vida.
Al detenerse la captura o recibir un error de micrófono se borran inmediatamente
la medición, los cents y el historial de suavizado, conservando la cuerda elegida.
Un callback tardío no puede restablecer una lectura de una captura detenida. Las
pausas breves de señal durante una captura activa mantienen su tolerancia habitual.
La restauración de selecciones de los ViewModels
después de que Android termine el proceso también queda pendiente.

Los ajustes se aplican inmediatamente en memoria. La sensibilidad se guarda al
terminar el gesto (también al salir o pasar a segundo plano si se interrumpe).
Los cambios pendientes se agrupan y guardan en orden mediante una edición atómica
de DataStore, omitiendo valores sin cambios. Si falla una escritura, Settings
muestra un error persistente con reintento; las opciones siguen activas en la sesión.

Los textos visibles, mensajes de micrófono, etiquetas accesibles y descripciones de
presets integrados se definen en `app/src/main/res/values/strings.xml`. Los valores
variables usan marcadores indexados; los presets personalizados mantienen su texto.
La notación musical y las claves persistidas no se traducen.

Home y Tune usan contenido desplazable con ancho máximo centrado. Sus encabezados,
tarjetas y controles crecen con el texto; las cuerdas se distribuyen en varias filas
cuando no caben y el medidor respeta tanto su ancho como su altura.
La navegación compartida muestra solo iconos si las etiquetas no caben con el
tamaño de texto elegido, conservando sus nombres accesibles. Las pruebas
`ResponsiveScreensTest` comprueban ventanas de 800×360 y 280×360 dp, además de
320×480 dp con texto al 200 %, sin modificar la configuración del teléfono.

Las pruebas de ViewModels usan una captura falsa y un reloj controlado para
verificar selección manual/automática, cambios de sensibilidad y A4, reintentos,
callbacks tardíos, liberación de recursos y filtros de presets. Ejecutar:

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
```

`AppStateRestorationTest` verifica el destino, modo manual, objetivo, presets y
reemplazo de captura al recrear la composición con estado guardado. Usa un
micrófono falso; requiere dispositivo o emulador para ejecutarse:

```bash
./gradlew connectedDebugAndroidTest
```

## Equipo

- Brian Bernabe Bolaños Arreaga — 24846
- Dostyn López Bautista — 251404
- Eduardo Isaí López Sutuc — 251832
- Maria A. Flores — 251646

Proyecto para el curso *Panorama del desarrollo móvil 2026*, Universidad del
Valle de Guatemala.
