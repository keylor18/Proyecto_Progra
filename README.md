# Servidor Padrón Electoral

Proyecto de Programación III, Etapa 1. Implementa un servidor Java que consulta personas del padrón electoral de Costa Rica usando exclusivamente `PADRON.txt` y `distelec.txt`, sin base de datos.

## Requisitos

- Java 25
- PowerShell en Windows para los comandos documentados
- Archivos de datos reales:
  - `PADRON.txt`
  - `distelec.txt`

## Arquitectura

El proyecto está organizado por capas:

- `presentacion`: `ServidorTCP` y `ServidorHTTP`
- `logica`: `ServicioPadron`
- `datos`: `RepositorioPadron` y `RepositorioDistritos`
- `entidades`: `Persona` y `DistritoElectoral`
- `dto`: `PersonaDTO` y `ErrorDTO`
- `utilidades`: configuración y serialización JSON

Flujo:

`TCP/HTTP -> ServicioPadron -> RepositorioPadron -> PADRON.txt -> RepositorioDistritos -> distelec.txt -> PersonaDTO -> JSON`

## Archivos de datos

Análisis real realizado:

- `PADRON.txt`
  - Tamaño analizado: 442,646,792 bytes
  - Registros: 3,751,244
  - Ordenado por cédula
  - Longitud fija: 118 bytes por registro con `CRLF`
  - Formato real:
    - cédula: 9
    - código electoral: 6
    - fecha vencimiento: 8
    - número junta: 5
    - nombre: 30
    - primer apellido: 26
    - segundo apellido: 26
- `distelec.txt`
  - Registros útiles cargados: 2,179
  - Formato CSV: `codigoElectoral,provincia,canton,distrito`

## Configuración

El archivo [config.properties](config.properties) centraliza:

- `tcp.port`
- `http.port`
- `padron.path`
- `distelec.path`

Valores por defecto en este proyecto:

- TCP: `5000`
- HTTP: `8080`
- `padron.path=../Llave/PADRON.txt`
- `distelec.path=../Llave/distelec.txt`

También se pueden sobreescribir con propiedades del sistema o variables de entorno.

## Decisión de eficiencia

`PADRON.txt` no se carga completo en memoria. Se consulta con búsqueda binaria usando `RandomAccessFile`, porque el archivo real está ordenado por cédula y cada registro tiene tamaño fijo.

`distelec.txt` sí se carga una sola vez en un `Map` inmutable porque es pequeño.

## Ejecución

Compilar:

```powershell
New-Item -ItemType Directory -Force -Path build\classes | Out-Null
$src = Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d build\classes $src
```

Ejecutar:

```powershell
java --class-path "build\classes" padron.Main
```

## Consulta TCP

Solicitud:

```text
GET|115550555
```

Prueba rápida desde PowerShell:

```powershell
$client = New-Object System.Net.Sockets.TcpClient('127.0.0.1', 5000)
try {
    $stream = $client.GetStream()
    $writer = New-Object System.IO.StreamWriter($stream, [System.Text.ASCIIEncoding]::new())
    $reader = New-Object System.IO.StreamReader($stream, [System.Text.ASCIIEncoding]::new())
    $writer.AutoFlush = $true
    $writer.WriteLine('GET|115550555')
    $reader.ReadLine()
}
finally {
    $client.Close()
}
```

## Consulta HTTP

Solicitud:

```text
GET /padron/115550555
```

Prueba rápida:

```powershell
(Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:8080/padron/115550555").Content
```

## Respuesta JSON

Ejemplo correcto:

```json
{"cedula":"115550555","nombre":"JUAN CARLOS","primerApellido":"MOSCOSO","segundoApellido":"AGUERO","codigoElectoral":"401017","provincia":"HEREDIA","canton":"CENTRAL","distrito":"GUARARI"}
```

Ejemplo de error:

```json
{"error":true,"codigo":404,"mensaje":"No se encontró una persona con la cédula indicada."}
```

## Errores

Se controlan, entre otros, estos casos:

- solicitud TCP vacía
- comando TCP desconocido
- cédula faltante
- cédula inválida
- persona inexistente
- ruta HTTP inexistente
- método HTTP no permitido
- errores internos de lectura o configuración

## Concurrencia

- TCP usa `ServerSocket` con `ExecutorService`
- HTTP usa `HttpServer` con `ExecutorService`
- la lógica de negocio es compartida y sin estado mutable compartido peligroso

## Estructura del proyecto

```text
src/
  padron/
    Main.java
    entidades/
    dto/
    datos/
    logica/
    presentacion/
    utilidades/
test/
  padron/pruebas/
```

## Pruebas

Compilar fuentes y pruebas:

```powershell
New-Item -ItemType Directory -Force -Path build\classes,build\test-classes | Out-Null
$src = Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d build\classes $src
$test = Get-ChildItem -Recurse -Filter *.java test | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -cp build\classes -d build\test-classes $test
```

Ejecutar pruebas automáticas:

```powershell
java --class-path "build\classes;build\test-classes" padron.pruebas.PruebasRepositorioYServicio
java --class-path "build\classes;build\test-classes" padron.pruebas.PruebasIntegracion
```
