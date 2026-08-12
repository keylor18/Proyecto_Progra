package padron.excepciones;

public class PersonaNoEncontradaException extends PadronException {

    public PersonaNoEncontradaException(String mensaje) {
        super(404, mensaje);
    }
}
