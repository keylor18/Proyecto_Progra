package padron.utilidades;

import padron.dto.ErrorDTO;
import padron.dto.PersonaDTO;

public final class JsonUtil {

    private JsonUtil() {
    }

    public static String convertirPersona(PersonaDTO persona) {
        StringBuilder json = new StringBuilder();
        json.append("{")
                .append("\"cedula\":\"").append(escapar(persona.getCedula())).append("\",")
                .append("\"nombre\":\"").append(escapar(persona.getNombre())).append("\",")
                .append("\"primerApellido\":\"").append(escapar(persona.getPrimerApellido())).append("\",")
                .append("\"segundoApellido\":\"").append(escapar(persona.getSegundoApellido())).append("\",")
                .append("\"codigoElectoral\":\"").append(escapar(persona.getCodigoElectoral())).append("\",")
                .append("\"provincia\":\"").append(escapar(persona.getProvincia())).append("\",")
                .append("\"canton\":\"").append(escapar(persona.getCanton())).append("\",")
                .append("\"distrito\":\"").append(escapar(persona.getDistrito())).append("\"")
                .append("}");
        return json.toString();
    }

    public static String convertirError(ErrorDTO error) {
        StringBuilder json = new StringBuilder();
        json.append("{")
                .append("\"error\":").append(error.isError()).append(",")
                .append("\"codigo\":").append(error.getCodigo()).append(",")
                .append("\"mensaje\":\"").append(escapar(error.getMensaje())).append("\"")
                .append("}");
        return json.toString();
    }

    private static String escapar(String valor) {
        StringBuilder escapado = new StringBuilder();
        for (int i = 0; i < valor.length(); i++) {
            char caracter = valor.charAt(i);
            switch (caracter) {
                case '\\' -> escapado.append("\\\\");
                case '"' -> escapado.append("\\\"");
                case '\n' -> escapado.append("\\n");
                case '\r' -> escapado.append("\\r");
                case '\t' -> escapado.append("\\t");
                default -> {
                    if (caracter < 32) {
                        escapado.append(String.format("\\u%04x", (int) caracter));
                    } else {
                        escapado.append(caracter);
                    }
                }
            }
        }
        return escapado.toString();
    }
}
