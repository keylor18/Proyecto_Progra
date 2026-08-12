package padron.utilidades;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import padron.excepciones.DatosPadronException;

public final class PreparadorDatos {

    private static final Logger LOGGER = Logger.getLogger(PreparadorDatos.class.getName());
    private static final String NOMBRE_PADRON = "PADRON.txt";
    private static final String NOMBRE_COMPRIMIDO = "PADRON.zip";
    private static final long TAMANO_PADRON = 442_646_792L;
    private static final String SHA_256_PADRON
            = "3836a97d1b62e63d58b1779ddfbdefbe83d8351d8591bb40dc23285e5e79114a";

    private PreparadorDatos() {
    }

    public static Path asegurarPadron(Path rutaPadron) throws DatosPadronException {
        Path rutaNormalizada = rutaPadron.toAbsolutePath().normalize();
        Path rutaComprimida = rutaNormalizada.resolveSibling(NOMBRE_COMPRIMIDO);

        try {
            if (Files.isRegularFile(rutaNormalizada) && Files.size(rutaNormalizada) == TAMANO_PADRON) {
                return rutaNormalizada;
            }
            if (!Files.isRegularFile(rutaComprimida)) {
                return rutaNormalizada;
            }

            LOGGER.info("Preparando PADRON.txt desde " + rutaComprimida
                    + ". Esto solo ocurre en la primera ejecución.");
            extraerPadron(rutaComprimida, rutaNormalizada);
            LOGGER.info("PADRON.txt preparado correctamente en " + rutaNormalizada + ".");
            return rutaNormalizada;
        } catch (IOException ex) {
            throw new DatosPadronException("No se pudo preparar PADRON.txt desde " + rutaComprimida + ".", ex);
        }
    }

    private static void extraerPadron(Path rutaComprimida, Path destino)
            throws IOException, DatosPadronException {
        Files.createDirectories(destino.getParent());
        Path temporal = Files.createTempFile(destino.getParent(), "PADRON-", ".tmp");
        boolean completado = false;

        try {
            MessageDigest resumen = crearSha256();
            long bytesExtraidos = copiarEntradaPadron(rutaComprimida, temporal, resumen);
            String hashExtraido = HexFormat.of().formatHex(resumen.digest());

            if (bytesExtraidos != TAMANO_PADRON || !SHA_256_PADRON.equals(hashExtraido)) {
                throw new DatosPadronException("PADRON.zip no contiene la versión esperada de PADRON.txt.");
            }

            moverReemplazando(temporal, destino);
            completado = true;
        } finally {
            if (!completado) {
                Files.deleteIfExists(temporal);
            }
        }
    }

    private static long copiarEntradaPadron(Path rutaComprimida, Path temporal, MessageDigest resumen)
            throws IOException, DatosPadronException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(rutaComprimida));
                OutputStream salida = Files.newOutputStream(temporal,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) {
                if (!entrada.isDirectory() && NOMBRE_PADRON.equalsIgnoreCase(entrada.getName())) {
                    byte[] buffer = new byte[64 * 1024];
                    long total = 0;
                    int leidos;
                    while ((leidos = zip.read(buffer)) != -1) {
                        salida.write(buffer, 0, leidos);
                        resumen.update(buffer, 0, leidos);
                        total += leidos;
                    }
                    return total;
                }
                zip.closeEntry();
            }
        }

        throw new DatosPadronException("No se encontró PADRON.txt dentro de " + rutaComprimida + ".");
    }

    private static MessageDigest crearSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("La JVM no ofrece SHA-256.", ex);
        }
    }

    private static void moverReemplazando(Path origen, Path destino) throws IOException {
        try {
            Files.move(origen, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
