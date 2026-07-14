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
| Sistema operativo | Android (minSdk 28) |
| Lenguaje | Kotlin |
| UI | Jetpack Compose |

## Cómo correr el proyecto

1. Abrir la carpeta del proyecto en Android Studio.
2. Dejar que Gradle sincronice.
3. Conectar un dispositivo físico o levantar un emulador (API 28 o superior).
4. Correr la app ( Run 'app').

## Estado actual del proyecto

- [x] App base que compila y muestra un primer Composable
- [ ] Detección de frecuencia por micrófono
- [ ] Comparación contra afinación seleccionada
- [ ] Indicador visual de afinado / grave / agudo

## Equipo

- Brian Bernabe Bolaños Arreaga — 24846
- Dostyn López Bautista — 251404
- Eduardo Isaí López Sutuc — 251832
- Maria A. Flores — 251646

Proyecto para el curso *Panorama del desarrollo móvil 2026*, Universidad del
Valle de Guatemala.