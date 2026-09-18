# API de IA privada de Trivium

Esta carpeta implementa la capa **server-side** para Gemini. La tablet y el
panel no deben llevar claves Gemini cuando esta API esté en producción.

## Operaciones disponibles

- `generateReading`: lectura dinámica con tres preguntas para Historia, Cívica,
  Ciencias, etc.
- `generateExercise`: ejercicio contextualizado y variado.
- `evaluateSummary`: evaluación segura de una respuesta abierta.
- `getEducationalImage`: consulta una imagen educativa ya almacenada.

Todas son [callable functions](https://firebase.google.com/docs/functions/callable):
requieren Firebase Auth y validan que padre/alumno corresponda al `childId`.
Las lecturas y ejercicios se guardan en `aiCache` para reutilizarlos y después
poder descargarlos al banco offline.

## Antes de desplegar

Cloud Functions de segunda generación se ejecuta sobre Cloud Run, por lo que el
proyecto normalmente debe estar en **Blaze**. No es necesario hacerlo para
compilar ni revisar el código.

Desde la raíz del proyecto, una vez habilitada la facturación y con Firebase CLI
iniciada en `trivium-ecc25`:

```powershell
cd C:\Users\fcoba.FRANKPC\Downloads\maui-kiosk-parent-control-companion
firebase functions:secrets:set GEMINI_API_KEY_FREE
firebase functions:secrets:set GEMINI_API_KEY_BILLING
firebase deploy --only functions
```

Pega cada clave únicamente cuando la CLI lo solicite. No las pongas en
`firebase.json`, `local.properties`, Git ni el dashboard.

## Imágenes

`getEducationalImage` ya define el contrato y conserva la prioridad correcta:
imagen en caché → imagen incluida en APK → emoji. Generar imágenes nuevas y
guardarlas requiere activar Firebase Storage; se deja deshabilitado hasta que el
proyecto esté listo para usar Storage y controlar su presupuesto.

## Siguiente integración

1. Agregar `firebase-functions` al APK y llamar estas funciones solo si hay red.
2. Guardar lectura/ejercicio devuelto en la memoria local para uso offline.
3. Mantener `ChallengeEngine` como fallback local, sin cambiar su algoritmo.
