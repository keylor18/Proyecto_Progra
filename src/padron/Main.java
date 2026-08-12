package padron;

import java.io.IOException;
import java.net.BindException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import padron.datos.RepositorioDistritos;
import padron.datos.RepositorioPadron;
import padron.excepciones.DatosPadronException;
import padron.logica.ServicioPadron;
import padron.presentacion.ServidorHTTP;
import padron.presentacion.ServidorTCP;
import padron.utilidades.Configuracion;
import padron.utilidades.ConfiguradorLogs;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        ConfiguradorLogs.configurar();
        try {
            Configuracion configuracion = Configuracion.cargar();
            RepositorioPadron repositorioPadron = new RepositorioPadron(configuracion.getRutaPadron());
            RepositorioDistritos repositorioDistritos = new RepositorioDistritos(configuracion.getRutaDistritos());
            ServicioPadron servicioPadron = new ServicioPadron(repositorioPadron, repositorioDistritos);

            ServidorTCP servidorTcp = new ServidorTCP(configuracion.getPuertoTcp(), servicioPadron);
            ServidorHTTP servidorHttp = new ServidorHTTP(configuracion.getPuertoHttp(), servicioPadron);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                servidorTcp.detener();
                servidorHttp.detener();
            }, "shutdown-padron"));

            try {
                servidorHttp.iniciar();
            } catch (BindException ex) {
                informarPuertoEnUso("HTTP", configuracion.getPuertoHttp());
                System.exit(1);
                return;
            }
            Thread hiloTcp = Thread.ofPlatform()
                    .name("servidor-tcp")
                    .start(() -> iniciarTcp(servidorTcp, configuracion.getPuertoTcp()));

            System.out.println("Servidor Padrón Electoral iniciado.");
            System.out.println("PADRON.txt: " + configuracion.getRutaPadron());
            System.out.println("distelec.txt: " + configuracion.getRutaDistritos());
            System.out.println("TCP: puerto " + configuracion.getPuertoTcp());
            System.out.println("HTTP: puerto " + configuracion.getPuertoHttp());
            System.out.println("Hilo TCP: " + hiloTcp.getName());

            new CountDownLatch(1).await();
        } catch (DatosPadronException ex) {
            LOGGER.log(Level.SEVERE, "No fue posible iniciar el proyecto por un problema de datos/configuración.", ex);
            System.exit(1);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "No fue posible iniciar uno de los servidores.", ex);
            System.exit(1);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "La ejecución principal fue interrumpida.", ex);
            System.exit(1);
        }
    }

    private static void iniciarTcp(ServidorTCP servidorTcp, int puertoTcp) {
        try {
            servidorTcp.iniciar();
        } catch (BindException ex) {
            informarPuertoEnUso("TCP", puertoTcp);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "El servidor TCP se detuvo por un error de E/S.", ex);
        }
    }

    private static void informarPuertoEnUso(String protocolo, int puerto) {
        LOGGER.severe("No se pudo iniciar el servidor " + protocolo
                + " porque el puerto " + puerto
                + " ya está en uso. Cierre el otro proceso o cambie el puerto en config.properties.");
    }
}
