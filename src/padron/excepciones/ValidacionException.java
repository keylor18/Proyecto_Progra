package padron.excepciones;

public class ValidacionException extends PadronException {

    public ValidacionException(String mensaje) {
        super(400, mensaje);
    }
}
