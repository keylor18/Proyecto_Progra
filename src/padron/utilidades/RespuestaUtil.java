package padron.utilidades;

import padron.dto.ErrorDTO;
import padron.dto.PersonaDTO;
import padron.excepciones.PadronException;

public final class RespuestaUtil {

    private RespuestaUtil() {
    }

    public static RespuestaJson exito(PersonaDTO persona) {
        return new RespuestaJson(200, JsonUtil.convertirPersona(persona));
    }

    public static RespuestaJson error(int codigo, String mensaje) {
        return new RespuestaJson(codigo, JsonUtil.convertirError(new ErrorDTO(codigo, mensaje)));
    }

    public static RespuestaJson error(PadronException ex) {
        return error(ex.getCodigo(), ex.getMessage());
    }

    public static RespuestaJson errorInterno() {
        return error(500, "Ocurrió un error interno del servidor.");
    }
}
