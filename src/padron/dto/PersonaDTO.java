package padron.dto;

public class PersonaDTO {

    private final String cedula;
    private final String nombre;
    private final String primerApellido;
    private final String segundoApellido;
    private final String codigoElectoral;
    private final String provincia;
    private final String canton;
    private final String distrito;

    public PersonaDTO(String cedula, String nombre, String primerApellido, String segundoApellido,
            String codigoElectoral, String provincia, String canton, String distrito) {
        this.cedula = cedula;
        this.nombre = nombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
        this.codigoElectoral = codigoElectoral;
        this.provincia = provincia;
        this.canton = canton;
        this.distrito = distrito;
    }

    public String getCedula() {
        return cedula;
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

    public String getCodigoElectoral() {
        return codigoElectoral;
    }

    public String getProvincia() {
        return provincia;
    }

    public String getCanton() {
        return canton;
    }

    public String getDistrito() {
        return distrito;
    }
}
