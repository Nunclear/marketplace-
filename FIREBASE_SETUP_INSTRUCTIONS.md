# Instrucciones para configurar Firebase Realtime Database

## 1. Importar estructura de datos

1. Ve a la consola de Firebase: https://console.firebase.google.com
2. Selecciona tu proyecto
3. En el menú lateral, selecciona "Realtime Database"
4. Haz clic en los tres puntos verticales (⋮) en la parte superior derecha
5. Selecciona "Importar JSON"
6. Sube el archivo `firebase-database-structure.json`
7. Confirma la importación

**Nota:** Este archivo contiene datos de ejemplo. Puedes eliminarlo después de la importación para empezar con una base de datos limpia.

## 2. Configurar reglas de seguridad

1. En la consola de Firebase Realtime Database
2. Ve a la pestaña "Reglas" (Rules)
3. Reemplaza el contenido actual con el contenido del archivo `firebase-realtime-database-rules.json`
4. Haz clic en "Publicar" (Publish)

## 3. Estructura de la base de datos

### Usuarios
\`\`\`
Usuarios/
  {uid}/
    - nombres: String
    - codigoTelefono: String
    - telefono: String
    - urlImagenPerfil: String
    - proveedor: String (Email, Google)
    - escribiendo: String
    - tiempo: Long (timestamp)
    - estado: String (online, offline)
    - email: String
    - uid: String
    - fecha_nac: String
    - fcmToken: String
    Favoritos/
      {id_anuncio}/
        - id_anuncio: String
        - tiempo: Long
    Comentarios/
      {id_comentario}/
        - id: String
        - tiempo: String
        - uid: String
        - uid_vendedor: String
        - comentario: String
\`\`\`

### Anuncios
\`\`\`
Anuncios/
  {id_anuncio}/
    - id: String
    - uid: String (del vendedor)
    - marca: String
    - categoria: String
    - condicion: String (Nuevo, Usado)
    - direccion: String
    - precio: String
    - titulo: String
    - descripcion: String
    - estado: String (Disponible, Vendido)
    - tiempo: Long (timestamp)
    - latitud: Double
    - longitud: Double
    - contadorVistas: Int
    Imagenes/
      {id_imagen}/
        - id: String
        - imagenUrl: String
\`\`\`

### Chats
\`\`\`
Chats/
  {uid_usuario1}_{uid_usuario2}/
    {mensaje_id}/
      - idMensaje: String
      - tipoMensaje: String (texto, imagen)
      - mensaje: String (texto o URL de imagen)
      - emisorUid: String
      - receptorUid: String
      - tiempo: Long (timestamp)
\`\`\`

## 4. Errores corregidos

Se corrigieron los siguientes errores en el código:

### FragmentChats.kt (Línea 89)
- **Error:** `kotlin.NotImplementedError: An operation is not implemented: Not yet implemented`
- **Solución:** Se implementó el manejo de errores en el método `onCancelled`

### AdaptadorChats.kt (Múltiples líneas)
- Se implementaron los métodos `onCancelled` en:
  - `cargarUltimoMensaje()` - línea 95
  - `cargarInfoUsuarioRecibimos()` - línea 146

### ChatActivity.kt (Múltiples líneas)
- Se implementaron los métodos `onCancelled` en:
  - `cargarMiInformacion()` - línea 100
  - `cargarMensajes()` - línea 140
  - `cargarInfoVendedor()` - línea 175

### CrearAnuncio.kt (Múltiples líneas)
- Se implementaron los métodos `onCancelled` en:
  - `cargarDetalles()` - línea 141 y 154

## 5. Permisos de las reglas

Las reglas configuradas permiten:

- **Usuarios:** Solo el propietario puede escribir en su perfil. Cualquier usuario autenticado puede leer perfiles.
- **Favoritos:** Solo el propietario puede leer y escribir sus favoritos.
- **Comentarios:** Cualquier usuario autenticado puede leer comentarios y escribir nuevos comentarios.
- **Anuncios:** Cualquier usuario autenticado puede leer anuncios. Solo el creador puede modificar sus anuncios.
- **Imágenes de anuncios:** Solo el creador del anuncio puede agregar/modificar imágenes.
- **Contador de vistas:** Cualquier usuario autenticado puede incrementar el contador.
- **Chats:** Solo los participantes del chat pueden leer y escribir mensajes.

## 6. Notas importantes

- Los UIDs en el archivo JSON son ejemplos. Firebase generará UIDs reales cuando los usuarios se registren.
- Los timestamps están en formato milisegundos (Long).
- Las rutas de chat siguen el formato: `{uid_usuario1}_{uid_usuario2}` donde los UIDs están ordenados alfabéticamente.
- El campo `tipoMensaje` puede ser "texto" o "imagen".
- El campo `estado` en Anuncios puede ser "Disponible" o "Vendido".
- El campo `proveedor` en Usuarios puede ser "Email" o "Google".
