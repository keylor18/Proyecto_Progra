package padron.presentacion;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import padron.dto.PersonaDTO;
import padron.excepciones.PadronException;
import padron.logica.ServicioPadron;
import padron.utilidades.RespuestaJson;
import padron.utilidades.RespuestaUtil;

public class ServidorHTTP {

    private static final Logger LOGGER = Logger.getLogger(ServidorHTTP.class.getName());

    private final int puerto;
    private final ServicioPadron servicioPadron;
    private final ExecutorService executor;
    private HttpServer servidor;

    public ServidorHTTP(int puerto, ServicioPadron servicioPadron) {
        this.puerto = puerto;
        this.servicioPadron = servicioPadron;
        this.executor = Executors.newFixedThreadPool(
                Math.max(4, Runtime.getRuntime().availableProcessors() * 2));
    }

    public void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        servidor.createContext("/", this::manejarSolicitud);
        servidor.setExecutor(executor);
        servidor.start();
        LOGGER.info(() -> "Servidor HTTP escuchando en el puerto " + puerto);
    }

    public void detener() {
        if (servidor != null) {
            servidor.stop(1);
        }
        executor.shutdownNow();
    }

    private void manejarSolicitud(HttpExchange intercambio) throws IOException {
        RespuestaJson respuesta;

        try {
            respuesta = resolverSolicitud(intercambio);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error inesperado procesando la solicitud HTTP.", ex);
            respuesta = RespuestaUtil.errorInterno();
        }

        byte[] cuerpo = respuesta.getCuerpo().getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        intercambio.sendResponseHeaders(respuesta.getCodigo(), cuerpo.length);

        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(cuerpo);
        } finally {
            intercambio.close();
        }
    }

    private RespuestaJson resolverSolicitud(HttpExchange intercambio) {
        if (!"GET".equalsIgnoreCase(intercambio.getRequestMethod())) {
            return RespuestaUtil.error(405, "El método HTTP solicitado no está permitido.");
        }

        String ruta = intercambio.getRequestURI().getPath();
        if ("/padron".equals(ruta) || "/padron/".equals(ruta)) {
            return RespuestaUtil.error(400, "Debe indicar la cédula en la ruta /padron/{cedula}.");
        }

        if (!ruta.startsWith("/padron/")) {
            return RespuestaUtil.error(404, "La ruta HTTP solicitada no existe.");
        }

        String cedula = ruta.substring("/padron/".length()).trim();
        if (cedula.isEmpty()) {
            return RespuestaUtil.error(400, "Debe indicar la cédula en la ruta /padron/{cedula}.");
        }
        if (cedula.contains("/")) {
            return RespuestaUtil.error(404, "La ruta HTTP solicitada no existe.");
        }

        try {
            PersonaDTO persona = servicioPadron.consultarPorCedula(cedula);
            return RespuestaUtil.exito(persona);
        } catch (PadronException ex) {
            return RespuestaUtil.error(ex);
        }
    }
}
