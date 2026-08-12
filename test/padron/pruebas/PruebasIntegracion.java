package padron.pruebas;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import padron.datos.RepositorioDistritos;
import padron.datos.RepositorioPadron;
import padron.logica.ServicioPadron;
import padron.presentacion.ServidorHTTP;
import padron.presentacion.ServidorTCP;
import padron.utilidades.Configuracion;

public class PruebasIntegracion {

    private static final int PUERTO_TCP_PRUEBA = 5500;
    private static final int PUERTO_HTTP_PRUEBA = 8081;

    public static void main(String[] args) throws Exception {
        Configuracion base = Configuracion.cargar();
        Configuracion configuracion = base.conPuertos(PUERTO_TCP_PRUEBA, PUERTO_HTTP_PRUEBA);
        RepositorioPadron repositorioPadron = new RepositorioPadron(configuracion.getRutaPadron());
        RepositorioDistritos repositorioDistritos = new RepositorioDistritos(configuracion.getRutaDistritos());
        ServicioPadron servicioPadron = new ServicioPadron(repositorioPadron, repositorioDistritos);

        ServidorTCP servidorTcp = new ServidorTCP(configuracion.getPuertoTcp(), servicioPadron);
        ServidorHTTP servidorHttp = new ServidorHTTP(configuracion.getPuertoHttp(), servicioPadron);
        Thread hiloTcp = Thread.ofPlatform().name("tcp-pruebas").start(() -> iniciarTcp(servidorTcp));

        try {
            servidorHttp.iniciar();
            Thread.sleep(600);

            probarTcpExitoso();
            probarTcpMalFormadoSinDetenerServidor();
            probarHttpExitoso();
            probarHttpInexistente();
            probarConcurrencia();

            System.out.println("PruebasIntegracion: OK");
        } finally {
            servidorTcp.detener();
            servidorHttp.detener();
            hiloTcp.join(2000);
        }
    }

    private static void iniciarTcp(ServidorTCP servidorTcp) {
        try {
            servidorTcp.iniciar();
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo iniciar el servidor TCP de pruebas.", ex);
        }
    }

    private static void probarTcpExitoso() throws Exception {
        String respuesta = consultarTcp("GET|115550555");
        TestUtil.assertTrue(respuesta.contains("\"cedula\":\"115550555\""),
                "La respuesta TCP debe incluir la cédula consultada.");
        TestUtil.assertTrue(respuesta.contains("\"provincia\":\"HEREDIA\""),
                "La respuesta TCP debe incluir la relación territorial.");
    }

    private static void probarTcpMalFormadoSinDetenerServidor() throws Exception {
        String error = consultarTcp("BAD|115550555");
        TestUtil.assertTrue(error.contains("\"error\":true"),
                "La respuesta TCP inválida debe devolverse en JSON.");
        TestUtil.assertTrue(error.contains("\"codigo\":400"),
                "La solicitud TCP mal formada debe devolver 400.");

        String respuestaPosterior = consultarTcp("GET|115550555");
        TestUtil.assertTrue(respuestaPosterior.contains("\"cedula\":\"115550555\""),
                "El servidor TCP debe seguir respondiendo después de un error.");
    }

    private static void probarHttpExitoso() throws Exception {
        HttpResponse<String> respuesta = consultarHttp("/padron/115550555");
        TestUtil.assertEquals(200, respuesta.statusCode(), "La consulta HTTP válida debe devolver 200.");
        TestUtil.assertTrue(respuesta.body().contains("\"codigoElectoral\":\"401017\""),
                "La respuesta HTTP debe incluir el código electoral.");
    }

    private static void probarHttpInexistente() throws Exception {
        HttpResponse<String> respuesta = consultarHttp("/padron/999999999");
        TestUtil.assertEquals(404, respuesta.statusCode(), "La persona inexistente debe devolver 404 en HTTP.");
        TestUtil.assertTrue(respuesta.body().contains("\"error\":true"),
                "Los errores HTTP deben devolverse en JSON.");
    }

    private static void probarConcurrencia() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                resultados.add(executor.submit(() -> consultarTcp("GET|115550555")
                        .contains("\"cedula\":\"115550555\"")));
                resultados.add(executor.submit(() -> consultarHttp("/padron/115550555")
                        .body().contains("\"distrito\":\"GUARARI\"")));
            }

            for (Future<Boolean> resultado : resultados) {
                try {
                    TestUtil.assertTrue(resultado.get(),
                            "La prueba de concurrencia esperaba una respuesta correcta.");
                } catch (ExecutionException ex) {
                    throw new AssertionError("Falló una ejecución concurrente.", ex.getCause());
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static String consultarTcp(String solicitud) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", PUERTO_TCP_PRUEBA);
                BufferedWriter escritor = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader lector = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            socket.setSoTimeout(5000);
            escritor.write(solicitud);
            escritor.newLine();
            escritor.flush();
            return lector.readLine();
        }
    }

    private static HttpResponse<String> consultarHttp(String ruta) throws Exception {
        HttpClient cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest solicitud = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + PUERTO_HTTP_PRUEBA + ruta))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        return cliente.send(solicitud, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
