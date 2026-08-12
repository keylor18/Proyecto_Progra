package padron.dto;

public class ErrorDTO {

    private final boolean error;
    private final int codigo;
    private final String mensaje;

    public ErrorDTO(int codigo, String mensaje) {
        this.error = true;
        this.codigo = codigo;
        this.mensaje = mensaje;
    }

    public boolean isError() {
        return error;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getMensaje() {
        return mensaje;
    }
}
