package padron.excepciones;

public class PadronException extends Exception {

    private final int codigo;

    public PadronException(int codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public PadronException(int codigo, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }
}
