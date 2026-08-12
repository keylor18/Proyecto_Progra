package padron.excepciones;

public class DatosPadronException extends PadronException {

    public DatosPadronException(String mensaje) {
        super(500, mensaje);
    }

    public DatosPadronException(String mensaje, Throwable causa) {
        super(500, mensaje, causa);
    }
}
