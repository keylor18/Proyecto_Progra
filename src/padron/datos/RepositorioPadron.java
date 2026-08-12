package padron.datos;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import padron.entidades.Persona;
import padron.excepciones.DatosPadronException;

public class RepositorioPadron {

    private static final Charset CHARSET_PADRON = StandardCharsets.ISO_8859_1;
    private static final int LONGITUD_REGISTRO_SIN_SALTO = 116;
    private static final int LONGITUD_CEDULA = 9;
    private static final int INICIO_CODIGO_ELECTORAL = 10;
    private static final int FIN_CODIGO_ELECTORAL = 16;
    private static final int INICIO_FECHA_VENCIMIENTO = 17;
    private static final int FIN_FECHA_VENCIMIENTO = 25;
    private static final int INICIO_NUMERO_JUNTA = 26;
    private static final int FIN_NUMERO_JUNTA = 31;
    private static final int INICIO_NOMBRE = 32;
    private static final int FIN_NOMBRE = 62;
    private static final int INICIO_PRIMER_APELLIDO = 63;
    private static final int FIN_PRIMER_APELLIDO = 89;
    private static final int INICIO_SEGUNDO_APELLIDO = 90;
    private static final int FIN_SEGUNDO_APELLIDO = 116;

    private final Path rutaPadron;
    private final long totalRegistros;
    private final int bytesPorRegistro;

    public RepositorioPadron(Path rutaPadron) throws DatosPadronException {
        this.rutaPadron = rutaPadron;
        AnalisisPadron analisis = analizarArchivo();
        this.bytesPorRegistro = analisis.bytesPorRegistro();
        this.totalRegistros = analisis.totalRegistros();
    }

    public Optional<Persona> buscarPorCedula(String cedula) throws DatosPadronException {
        byte[] buffer = new byte[bytesPorRegistro];

        try (RandomAccessFile archivo = new RandomAccessFile(rutaPadron.toFile(), "r")) {
            long inferior = 0;
            long superior = totalRegistros - 1;

            while (inferior <= superior) {
                long medio = inferior + ((superior - inferior) / 2);
                archivo.seek(medio * bytesPorRegistro);
                archivo.readFully(buffer);

                String cedulaActual = new String(buffer, 0, LONGITUD_CEDULA, CHARSET_PADRON);
                int comparacion = cedulaActual.compareTo(cedula);

                if (comparacion == 0) {
                    return Optional.of(parsearPersona(buffer));
                }
                if (comparacion < 0) {
                    inferior = medio + 1;
                } else {
                    superior = medio - 1;
                }
            }

            return Optional.empty();
        } catch (IOException ex) {
            throw new DatosPadronException("No se pudo consultar PADRON.txt desde " + rutaPadron + ".", ex);
        }
    }

    public long totalRegistros() {
        return totalRegistros;
    }

    public Path getRutaPadron() {
        return rutaPadron;
    }

    private AnalisisPadron analizarArchivo() throws DatosPadronException {
        try {
            long tamano = Files.size(rutaPadron);
            byte[] primerRegistro = leerPrimerRegistro();
            int longitudContenido = calcularLongitudContenido(primerRegistro);

            if (longitudContenido != LONGITUD_REGISTRO_SIN_SALTO) {
                throw new DatosPadronException("El formato real de PADRON.txt no coincide con la longitud analizada.");
            }

            if (tamano % primerRegistro.length != 0) {
                throw new DatosPadronException("PADRON.txt no tiene un tamaño compatible con registros fijos.");
            }

            long total = tamano / primerRegistro.length;
            if (total <= 0) {
                throw new DatosPadronException("PADRON.txt no contiene registros.");
            }

            return new AnalisisPadron(primerRegistro.length, total);
        } catch (IOException ex) {
            throw new DatosPadronException("No se pudo analizar PADRON.txt desde " + rutaPadron + ".", ex);
        }
    }

    private byte[] leerPrimerRegistro() throws IOException, DatosPadronException {
        try (InputStream entrada = Files.newInputStream(rutaPadron)) {
            byte[] buffer = new byte[256];
            int indice = 0;
            int byteLeido;

            while ((byteLeido = entrada.read()) != -1) {
                if (indice == buffer.length) {
                    throw new DatosPadronException("No se pudo determinar el tamaño del registro de PADRON.txt.");
                }
                buffer[indice++] = (byte) byteLeido;
                if (byteLeido == '\n') {
                    break;
                }
            }

            if (indice == 0) {
                throw new DatosPadronException("PADRON.txt está vacío.");
            }

            byte[] primerRegistro = new byte[indice];
            System.arraycopy(buffer, 0, primerRegistro, 0, indice);
            return primerRegistro;
        }
    }

    private int calcularLongitudContenido(byte[] registro) {
        int longitud = registro.length;
        if (longitud >= 2 && registro[longitud - 2] == '\r' && registro[longitud - 1] == '\n') {
            return longitud - 2;
        }
        if (longitud >= 1 && registro[longitud - 1] == '\n') {
            return longitud - 1;
        }
        return longitud;
    }

    private Persona parsearPersona(byte[] registro) throws DatosPadronException {
        String linea = new String(registro, 0, LONGITUD_REGISTRO_SIN_SALTO, CHARSET_PADRON);
        if (linea.length() != LONGITUD_REGISTRO_SIN_SALTO) {
            throw new DatosPadronException("Registro inválido al parsear PADRON.txt.");
        }

        return new Persona(
                linea.substring(0, LONGITUD_CEDULA).trim(),
                linea.substring(INICIO_CODIGO_ELECTORAL, FIN_CODIGO_ELECTORAL).trim(),
                linea.substring(INICIO_FECHA_VENCIMIENTO, FIN_FECHA_VENCIMIENTO).trim(),
                linea.substring(INICIO_NUMERO_JUNTA, FIN_NUMERO_JUNTA).trim(),
                linea.substring(INICIO_NOMBRE, FIN_NOMBRE).trim(),
                linea.substring(INICIO_PRIMER_APELLIDO, FIN_PRIMER_APELLIDO).trim(),
                linea.substring(INICIO_SEGUNDO_APELLIDO, FIN_SEGUNDO_APELLIDO).trim()
        );
    }

    private record AnalisisPadron(int bytesPorRegistro, long totalRegistros) {
    }
}
