package padron.entidades;

public class Persona {

    private final String cedula;
    private final String codigoElectoral;
    private final String fechaVencimiento;
    private final String numeroJunta;
    private final String nombre;
    private final String primerApellido;
    private final String segundoApellido;

    public Persona(String cedula, String codigoElectoral, String fechaVencimiento,
            String numeroJunta, String nombre, String primerApellido, String segundoApellido) {
        this.cedula = cedula;
        this.codigoElectoral = codigoElectoral;
        this.fechaVencimiento = fechaVencimiento;
        this.numeroJunta = numeroJunta;
        this.nombre = nombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
    }

    public String getCedula() {
        return cedula;
    }

    public String getCodigoElectoral() {
        return codigoElectoral;
    }

    public String getFechaVencimiento() {
        return fechaVencimiento;
    }

    public String getNumeroJunta() {
        return numeroJunta;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }
}
