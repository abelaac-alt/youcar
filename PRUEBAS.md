# Comprobaciones de CarView 1.1.0

- XML y manifiesto: verificados como documentos XML bien formados.
- GitHub Actions: workflow incluido para Java 17, Android SDK 35 y Gradle 8.9.
- Integración Android Auto: declarado descriptor `media`, servicio `MediaBrowserServiceCompat` y sesión `MediaSessionCompat`.
- Audio local: importación `ACTION_OPEN_DOCUMENT`, persistencia de permisos y biblioteca consultable.
- Emergente: implementado solo en la app del teléfono.

**Pendiente:** ejecutar `assembleDebug` en Android Studio/GitHub Actions y probar Android Auto real. Sin esas pruebas, no se puede garantizar compilación, reproducción ni visibilidad en el lanzador del coche.

**No se ha implementado** desactivación del detector de movimiento del sistema ni proyección de vídeo durante la conducción.
