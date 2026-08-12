package padron.utilidades;

public class RespuestaJson {

    private final int codigo;
    private final String cuerpo;

    public RespuestaJson(int codigo, String cuerpo) {
        this.codigo = codigo;
        this.cuerpo = cuerpo;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getCuerpo() {
        return cuerpo;
    }
}
