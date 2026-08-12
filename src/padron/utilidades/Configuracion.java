package padron.utilidades;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import padron.excepciones.DatosPadronException;

public class Configuracion {

    private static final int PUERTO_TCP_PREDETERMINADO = 5000;
    private static final int PUERTO_HTTP_PREDETERMINADO = 8080;
    private static final String ARCHIVO_CONFIGURACION = "config.properties";

    private final int puertoTcp;
    private final int puertoHttp;
    private final Path rutaPadron;
    private final Path rutaDistritos;

    public Configuracion(int puertoTcp, int puertoHttp, Path rutaPadron, Path rutaDistritos) {
        validarPuerto("TCP", puertoTcp);
        validarPuerto("HTTP", puertoHttp);
        this.puertoTcp = puertoTcp;
        this.puertoHttp = puertoHttp;
        this.rutaPadron = Objects.requireNonNull(rutaPadron, "La ruta de PADRON.txt es obligatoria")
                .toAbsolutePath().normalize();
        this.rutaDistritos = Objects.requireNonNull(rutaDistritos, "La ruta de distelec.txt es obligatoria")
                .toAbsolutePath().normalize();
    }

    public static Configuracion cargar() throws DatosPadronException {
        Properties propiedades = new Properties();
        Path archivoConfiguracion = Paths.get(ARCHIVO_CONFIGURACION).toAbsolutePath().normalize();

        if (Files.exists(archivoConfiguracion)) {
            try (Reader lector = Files.newBufferedReader(archivoConfiguracion, StandardCharsets.UTF_8)) {
                propiedades.load(lector);
            } catch (IOException ex) {
                throw new DatosPadronException("No se pudo leer el archivo de configuración: "
                        + archivoConfiguracion, ex);
            }
        }

        int puertoTcp = leerPuerto("tcp.port", "TCP_PORT", propiedades, PUERTO_TCP_PREDETERMINADO);
        int puertoHttp = leerPuerto("http.port", "HTTP_PORT", propiedades, PUERTO_HTTP_PREDETERMINADO);
        Path rutaPadron = resolverRutaDatos("padron.path", "PADRON_PATH", "PADRON.txt", propiedades);
        Path rutaDistritos = resolverRutaDatos("distelec.path", "DISTELEC_PATH", "distelec.txt", propiedades);

        return new Configuracion(puertoTcp, puertoHttp, rutaPadron, rutaDistritos);
    }

    public Configuracion conPuertos(int nuevoPuertoTcp, int nuevoPuertoHttp) {
        return new Configuracion(nuevoPuertoTcp, nuevoPuertoHttp, rutaPadron, rutaDistritos);
    }

    public int getPuertoTcp() {
        return puertoTcp;
    }

    public int getPuertoHttp() {
        return puertoHttp;
    }

    public Path getRutaPadron() {
        return rutaPadron;
    }

    public Path getRutaDistritos() {
        return rutaDistritos;
    }

    private static int leerPuerto(String propiedad, String variableEntorno, Properties propiedades, int valorDefault)
            throws DatosPadronException {
        String valor = System.getProperty(propiedad);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(variableEntorno);
        }
        if (valor == null || valor.isBlank()) {
            valor = propiedades.getProperty(propiedad);
        }
        if (valor == null || valor.isBlank()) {
            return valorDefault;
        }

        try {
            int puerto = Integer.parseInt(valor.trim());
            validarPuerto(propiedad, puerto);
            return puerto;
        } catch (NumberFormatException ex) {
            throw new DatosPadronException("El puerto configurado en " + propiedad + " no es válido: " + valor, ex);
        }
    }

    private static Path resolverRutaDatos(String propiedad, String variableEntorno, String nombreArchivo,
            Properties propiedades) throws DatosPadronException {
        List<Path> candidatos = new ArrayList<>();
        String valor = System.getProperty(propiedad);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(variableEntorno);
        }
        if (valor == null || valor.isBlank()) {
            valor = propiedades.getProperty(propiedad);
        }
        if (valor != null && !valor.isBlank()) {
            candidatos.add(resolverContraDirectorioTrabajo(valor.trim()));
        }

        candidatos.add(resolverContraDirectorioTrabajo("data/" + nombreArchivo));
        candidatos.add(resolverContraDirectorioTrabajo(nombreArchivo));

        for (Path candidato : candidatos) {
            if (Files.exists(candidato) && Files.isRegularFile(candidato)) {
                return candidato.toAbsolutePath().normalize();
            }
        }

        throw new DatosPadronException("No se encontró el archivo " + nombreArchivo
                + ". Ajuste la propiedad " + propiedad + " o ubique el archivo en una ruta conocida.");
    }

    private static Path resolverContraDirectorioTrabajo(String ruta) {
        Path path = Paths.get(ruta);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Paths.get("").toAbsolutePath().resolve(path).normalize();
    }

    private static void validarPuerto(String nombre, int puerto) {
        if (puerto < 1 || puerto > 65535) {
            throw new IllegalArgumentException("El puerto " + nombre + " debe estar entre 1 y 65535.");
        }
    }
}
