# CarView 1.1.0 — integración real de audio con Android Auto

## Alcance

**Esta versión NO habilita vídeo en la pantalla del coche en movimiento.**
La detección y las restricciones de movimiento pertenecen al sistema de Android Auto/vehículo y no son desactivables por el código de CarView. Apple CarPlay es otro sistema y no se puede modificar desde una app Android.

Esta variante añade:

- `CarAudioService`, servicio `MediaBrowserServiceCompat` con `MediaSessionCompat`, navegación de archivos de audio, controles de reproducción y notificación de primer plano.
- Descriptor `res/xml/automotive_app_desc.xml` con `uses name="media"` y metadatos de Android Auto en el manifiesto.
- Importación de archivos de audio desde el móvil (`ACTION_OPEN_DOCUMENT`) y permisos persistentes de lectura. Android Auto solo reproduce los archivos que agregue el usuario.
- Diálogo de aceptación en **la pantalla del móvil**, en el primer inicio o desde el menú "Mostrar aviso de seguridad". No puede aparecer como diálogo arbitrario sobre la pantalla Android Auto, cuya interfaz la dibuja el host.
- Conserva el navegador de YouTube y reproductor local para uso con el vehículo estacionado; **no se proyecta vídeo a Android Auto**.

## Cómo probarlo

1. Abre el APK en el **móvil** y acepta el aviso. Importa uno o más archivos de audio desde el menú **Importar audio para Android Auto**.
2. Conecta el móvil con Android Auto; permite las apps de terceros cuando corresponda. Busca **CarView** en el selector de aplicaciones de audio.
3. En Android Auto, entra en CarView, selecciona un archivo de audio y prueba reproducción, pausa y saltos de pista.
4. Si no aparece, verifica el estado de compatibilidad, la fuente de instalación, permisos, la conexión del teléfono y revisa logs de `CarAudioService`. La presencia en el lanzador **no está garantizada** solo por permitir apps externas.

**Nota sobre el alcance de Android Auto:** este proyecto es principalmente un navegador de vídeo con un servicio separado de audio local. La distribución como app de medios y la presencia en el lanzador pueden estar sujetas a los criterios de calidad y revisión de Google.

## Compilación

Incluye `.github/workflows/build-apk.yml`. Publícalo como repositorio GitHub, visita Actions → Compilar APK Android → Run workflow y descarga el artifact `CarView-debug-APK` si se completa correctamente.

Esta entrega contiene **código fuente sin compilar**, ya que este entorno no proporciona Android SDK y no se ha ejecutado la integración en un vehículo.

## Fuentes oficiales

- https://developer.android.com/training/cars/media/auto
- https://developer.android.com/training/cars/media/configure-manifest
- https://developer.android.com/training/cars/media/create-media-browser
- https://developer.android.com/training/cars/platforms/android-auto
- https://developer.android.com/training/cars
