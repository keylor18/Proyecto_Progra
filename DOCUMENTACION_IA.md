# Evidencia de uso de Inteligencia Artificial

## Implementación de arquitectura

Problema:
Se necesitaba construir desde cero un servidor por capas, separando presentación, lógica, repositorios, DTO y utilidades.

Respuesta/propuesta:
Se propuso una estructura con paquetes `entidades`, `dto`, `datos`, `logica`, `presentacion` y `utilidades`, más un `Main` responsable únicamente del arranque.

Parte utilizada:
Se utilizaron las clases creadas en `src/padron/` para materializar esa separación de responsabilidades.

Modificaciones realizadas:
Se crearon las entidades `Persona` y `DistritoElectoral`, los DTO `PersonaDTO` y `ErrorDTO`, el `ServicioPadron`, los repositorios, los servidores TCP y HTTP, las utilidades de configuración y JSON, y el punto de entrada principal.

## Análisis real de PADRON.txt y distelec.txt

Problema:
No era válido inventar el formato de los archivos; había que inspeccionar los datos reales antes de programar los parsers.

Respuesta/propuesta:
Se inspeccionaron los archivos reales disponibles en `C:\Users\KEYLOR\Desktop\Llave\PADRON.txt` y `C:\Users\KEYLOR\Desktop\Llave\distelec.txt`.

Parte utilizada:
El análisis determinó que `PADRON.txt` está ordenado por cédula y usa registros fijos de 118 bytes con `CRLF`, mientras que `distelec.txt` es un CSV pequeño con código electoral, provincia, cantón y distrito.

Modificaciones realizadas:
El parser de `RepositorioPadron` se implementó con posiciones fijas reales. El parser de `RepositorioDistritos` se implementó leyendo el CSV real y cargándolo en memoria.

## Estrategia de eficiencia y concurrencia

Problema:
`PADRON.txt` tiene cientos de megabytes y el servidor debía soportar consultas concurrentes sin una base de datos.

Respuesta/propuesta:
Se eligió búsqueda binaria con `RandomAccessFile` para `PADRON.txt`, evitando cargarlo completo en memoria, y un `Map` inmutable para `distelec.txt` por su tamaño pequeño.

Parte utilizada:
La búsqueda binaria quedó implementada en `RepositorioPadron`; la carga única de distritos quedó en `RepositorioDistritos`.

Modificaciones realizadas:
Se agregó concurrencia con `ExecutorService` tanto en `ServidorTCP` como en `ServidorHTTP`, manteniendo la lógica compartida en `ServicioPadron`.

## Implementación de TCP, HTTP y JSON

Problema:
El proyecto debía responder por socket TCP y por HTTP usando exclusivamente JSON.

Respuesta/propuesta:
Se implementó `ServidorTCP` para el protocolo `GET|cedula` y `ServidorHTTP` para `GET /padron/{cedula}`. Las respuestas se centralizaron con `JsonUtil` y `RespuestaUtil`.

Parte utilizada:
Los servidores llaman al mismo `ServicioPadron` y convierten el resultado o el error en JSON.

Modificaciones realizadas:
Se añadieron validaciones de protocolo, respuestas `400`, `404`, `405` y `500`, y serialización JSON manual sin agregar frameworks innecesarios.

## Pruebas y documentación

Problema:
Era necesario verificar funcionamiento, errores controlados y concurrencia, además de dejar documentación académica clara.

Respuesta/propuesta:
Se crearon pruebas automáticas en `test/padron/pruebas/` para repositorios, servicio e integración TCP/HTTP/concurrencia.

Parte utilizada:
Se ejecutaron `PruebasRepositorioYServicio` y `PruebasIntegracion`, además de una ejecución manual del servidor principal con consultas reales por TCP y HTTP.

Modificaciones realizadas:
Se agregaron `README.md`, `.gitignore`, `config.properties` y este archivo para dejar constancia del trabajo realizado en esta ejecución.
