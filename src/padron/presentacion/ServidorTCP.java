package padron.presentacion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import padron.dto.PersonaDTO;
import padron.excepciones.PadronException;
import padron.excepciones.ValidacionException;
import padron.logica.ServicioPadron;
import padron.utilidades.RespuestaJson;
import padron.utilidades.RespuestaUtil;

public class ServidorTCP {

    private static final Logger LOGGER = Logger.getLogger(ServidorTCP.class.getName());
    private static final int TIEMPO_ESPERA_SOCKET_MS = 5000;

    private final int puerto;
    private final ServicioPadron servicioPadron;
    private final ExecutorService poolClientes;
    private volatile boolean activo;
    private ServerSocket serverSocket;

    public ServidorTCP(int puerto, ServicioPadron servicioPadron) {
        this.puerto = puerto;
        this.servicioPadron = servicioPadron;
        this.poolClientes = Executors.newFixedThreadPool(
                Math.max(4, Runtime.getRuntime().availableProcessors() * 2));
    }

    public void iniciar() throws IOException {
        serverSocket = new ServerSocket(puerto);
        activo = true;
        LOGGER.info(() -> "Servidor TCP escuchando en el puerto " + puerto);

        try {
            while (activo) {
                Socket cliente = serverSocket.accept();
                poolClientes.submit(() -> atenderCliente(cliente));
            }
        } catch (IOException ex) {
            if (activo) {
                throw ex;
            }
        } finally {
            detener();
        }
    }

    public void detener() {
        activo = false;
        cerrarServidor();
        poolClientes.shutdown();
        try {
            if (!poolClientes.awaitTermination(5, TimeUnit.SECONDS)) {
                poolClientes.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            poolClientes.shutdownNow();
        }
    }

    private void atenderCliente(Socket cliente) {
        try (Socket socket = cliente;
                BufferedReader lector = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter escritor = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            socket.setSoTimeout(TIEMPO_ESPERA_SOCKET_MS);

            String solicitud = lector.readLine();
            RespuestaJson respuesta = procesarSolicitud(solicitud);
            escritor.write(respuesta.getCuerpo());
            escritor.newLine();
            escritor.flush();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error atendiendo un cliente TCP.", ex);
        }
    }

    private RespuestaJson procesarSolicitud(String solicitud) {
        try {
            String cedula = extraerCedula(solicitud);
            PersonaDTO persona = servicioPadron.consultarPorCedula(cedula);
            return RespuestaUtil.exito(persona);
        } catch (PadronException ex) {
            return RespuestaUtil.error(ex);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error inesperado procesando la solicitud TCP.", ex);
            return RespuestaUtil.errorInterno();
        }
    }

    private String extraerCedula(String solicitud) throws ValidacionException {
        if (solicitud == null || solicitud.isBlank()) {
            throw new ValidacionException("La solicitud TCP no puede estar vacía.");
        }

        String[] partes = solicitud.split("\\|", -1);
        if (partes.length != 2) {
            throw new ValidacionException("Formato TCP inválido. Use GET|cedula.");
        }

        String comando = partes[0].trim();
        if (!"GET".equalsIgnoreCase(comando)) {
            throw new ValidacionException("Comando TCP desconocido. Use GET|cedula.");
        }

        String cedula = partes[1].trim();
        if (cedula.isEmpty()) {
            throw new ValidacionException("Debe indicar una cédula en la solicitud TCP.");
        }

        return cedula;
    }

    private void cerrarServidor() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                LOGGER.log(Level.FINE, "No se pudo cerrar el ServerSocket TCP limpiamente.", ex);
            }
        }
    }
}
