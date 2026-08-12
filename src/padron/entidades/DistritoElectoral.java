package padron.entidades;

public class DistritoElectoral {

    private final String codigoElectoral;
    private final String provincia;
    private final String canton;
    private final String distrito;

    public DistritoElectoral(String codigoElectoral, String provincia, String canton, String distrito) {
        this.codigoElectoral = codigoElectoral;
        this.provincia = provincia;
        this.canton = canton;
        this.distrito = distrito;
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
