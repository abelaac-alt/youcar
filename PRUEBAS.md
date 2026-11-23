# Lista de pruebas manuales (a realizar tras compilar)

1. Abrir CarView en un emulador Android 8.0+, Android 13 y Android 15.
2. Comprobar que se carga `https://m.youtube.com/` y que el cuadro de direcciones funciona.
3. Buscar una palabra (p. ej. `concierto acústico`) y comprobar que lleva a resultados YouTube.
4. Abrir un vídeo y probar la reproducción y el modo pantalla completa.
5. Activar/desactivar modo escritorio y verificar la recarga.
6. Guardar la página como favorito y comprobar que persiste tras cerrar la app.
7. Abrir el menú de favoritos; abrir y eliminar una entrada.
8. Comprobar historial, borrar historial y comprobar que favoritos se mantienen.
9. Seleccionar un MP4 local desde el selector del sistema, reproducir, pausar y cerrar.
10. Rotar el dispositivo con un vídeo abierto, usar el botón Atrás y comprobar la barra del sistema.
11. Poner la app en segundo plano durante la reproducción y comprobar que se pausa.
12. Comprobar WebView con una conexión desconectada y con URLs no válidas.
13. Revisar accesibilidad, ajustes de tamaño de fuente y pantalla horizontal grande.
14. Para Android Automotive OS, probar el emulador específico y las restricciones del vehículo aparcado; no asumir funcionamiento en Android Auto normal.

Estado: **pendiente de ejecutar** (no hay SDK Android ni emulador en el entorno de generación).
