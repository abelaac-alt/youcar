# CarView 1.1.0 — prototipo Android Auto (solo audio)

CarView es una implementación independiente inspirada en los reproductores de vídeo tipo CarStream. Esta versión ofrece navegador de YouTube y vídeo local en el **teléfono** (para uso con el vehículo estacionado), y un servicio de **audio local** para que Android Auto pueda mostrar una biblioteca de archivos musicales y controles de reproducción.

## Novedades

- Metadatos del manifiesto y `res/xml/automotive_app_desc.xml` para registrar una aplicación de medios en Android Auto.
- Servicio `CarAudioService` basado en `MediaBrowserServiceCompat` y `MediaSessionCompat` con controles de reproducción de audio, siguiente, anterior y notificación de servicio de primer plano.
- Importación de archivos de audio desde el selector de documentos Android y permisos persistentes de lectura. El usuario debe importar sus propios archivos.
- Aviso de seguridad modal en el **móvil** en el primer inicio, repetible desde el menú.
- Compilación automática del APK con GitHub Actions.

## Limitaciones importantes

- **No permite mostrar vídeo en la pantalla central durante la conducción.** No modifica ni elimina la detección de movimiento o las restricciones de Android Auto, Android Automotive OS o Apple CarPlay.
- Un diálogo de confirmación del móvil no puede sobreponerse arbitrariamente a la interfaz multimedia que dibuja Android Auto.
- La existencia del servicio multimedia no garantiza que la aplicación aparezca en todos los vehículos; además, la app combina un navegador de vídeo con un servicio de audio y puede requerir ajustes adicionales para superar las políticas de publicación de Google.
- El navegador de YouTube usa `WebView` y su funcionamiento está sujeto a cambios y políticas del sitio. El audio de YouTube **no** se envía a Android Auto: solo se comparten los archivos de audio importados.
- Esta entrega no incluye APK compilado ni pruebas reales con vehículos. La compilación está preparada para ejecutarse mediante GitHub Actions.

## Cómo usar

1. Compila desde [GitHub Actions](GITHUB_ACTIONS.md) o Android Studio (Java 17, Gradle 8.9, Android SDK 35).
2. Instala el APK, abre CarView en el móvil y confirma el aviso.
3. En el menú, pulsa **Importar audio para Android Auto** y selecciona uno o varios archivos MP3/M4A u otros formatos admitidos.
4. Conecta el móvil a Android Auto y busca CarView entre las aplicaciones de audio. Si aparece, selecciona el archivo y prueba los controles.
5. Abre la interfaz de vídeo en el móvil únicamente cuando el coche esté estacionado.

Consulta [ANDROID_AUTO.md](ANDROID_AUTO.md) y [GITHUB_ACTIONS.md](GITHUB_ACTIONS.md) para los detalles técnicos.

## Fuentes oficiales

- [Android Auto: medios](https://developer.android.com/training/cars/media/auto)
- [Declarar MediaBrowserService](https://developer.android.com/training/cars/media/configure-manifest)
- [Categorías admitidas para coches](https://developer.android.com/training/cars)
