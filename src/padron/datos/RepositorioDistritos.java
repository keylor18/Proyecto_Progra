package padron.datos;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import padron.entidades.DistritoElectoral;
import padron.excepciones.DatosPadronException;

public class RepositorioDistritos {

    private final Path rutaDistritos;
    private final Map<String, DistritoElectoral> distritosPorCodigo;

    public RepositorioDistritos(Path rutaDistritos) throws DatosPadronException {
        this.rutaDistritos = rutaDistritos;
        this.distritosPorCodigo = Collections.unmodifiableMap(cargarDistritos());
    }

    public Optional<DistritoElectoral> buscarPorCodigoElectoral(String codigoElectoral) {
        return Optional.ofNullable(distritosPorCodigo.get(codigoElectoral));
    }

    public int totalDistritos() {
        return distritosPorCodigo.size();
    }

    public Path getRutaDistritos() {
        return rutaDistritos;
    }

    private Map<String, DistritoElectoral> cargarDistritos() throws DatosPadronException {
        Map<String, DistritoElectoral> distritos = new LinkedHashMap<>();

        try (BufferedReader lector = Files.newBufferedReader(rutaDistritos, StandardCharsets.ISO_8859_1)) {
            String linea;
            int numeroLinea = 0;
            while ((linea = lector.readLine()) != null) {
                numeroLinea++;
                if (linea.isBlank()) {
                    continue;
                }

                String[] partes = linea.split(",", 4);
                if (partes.length != 4) {
                    throw new DatosPadronException("Formato inválido en distelec.txt, línea " + numeroLinea + ".");
                }

                String codigo = partes[0].trim();
                String provincia = partes[1].trim();
                String canton = partes[2].trim();
                String distrito = partes[3].trim();

                if (!codigo.matches("\\d{6}")) {
                    throw new DatosPadronException("Código electoral inválido en distelec.txt, línea "
                            + numeroLinea + ": " + codigo);
                }

                if (distritos.containsKey(codigo)) {
                    throw new DatosPadronException("Código electoral duplicado en distelec.txt: " + codigo);
                }

                distritos.put(codigo, new DistritoElectoral(codigo, provincia, canton, distrito));
            }
        } catch (IOException ex) {
            throw new DatosPadronException("No se pudo leer distelec.txt desde " + rutaDistritos + ".", ex);
        }

        if (distritos.isEmpty()) {
            throw new DatosPadronException("El archivo distelec.txt no contiene registros utilizables.");
        }

        return distritos;
    }
}
