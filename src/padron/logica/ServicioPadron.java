package padron.logica;

import padron.datos.RepositorioDistritos;
import padron.datos.RepositorioPadron;
import padron.dto.PersonaDTO;
import padron.entidades.DistritoElectoral;
import padron.entidades.Persona;
import padron.excepciones.DatosPadronException;
import padron.excepciones.PersonaNoEncontradaException;
import padron.excepciones.ValidacionException;

public class ServicioPadron {

    private final RepositorioPadron repositorioPadron;
    private final RepositorioDistritos repositorioDistritos;

    public ServicioPadron(RepositorioPadron repositorioPadron, RepositorioDistritos repositorioDistritos) {
        this.repositorioPadron = repositorioPadron;
        this.repositorioDistritos = repositorioDistritos;
    }

    public PersonaDTO consultarPorCedula(String cedula)
            throws ValidacionException, PersonaNoEncontradaException, DatosPadronException {
        String cedulaNormalizada = validarCedula(cedula);

        Persona persona = repositorioPadron.buscarPorCedula(cedulaNormalizada)
                .orElseThrow(() -> new PersonaNoEncontradaException(
                "No se encontró una persona con la cédula indicada."));

        DistritoElectoral distrito = repositorioDistritos.buscarPorCodigoElectoral(persona.getCodigoElectoral())
                .orElseThrow(() -> new DatosPadronException("No se encontró la división territorial para el código "
                + persona.getCodigoElectoral() + "."));

        return new PersonaDTO(
                persona.getCedula(),
                persona.getNombre(),
                persona.getPrimerApellido(),
                persona.getSegundoApellido(),
                persona.getCodigoElectoral(),
                distrito.getProvincia(),
                distrito.getCanton(),
                distrito.getDistrito()
        );
    }

    private String validarCedula(String cedula) throws ValidacionException {
        if (cedula == null || cedula.isBlank()) {
            throw new ValidacionException("La cédula es obligatoria.");
        }

        String cedulaNormalizada = cedula.trim();
        if (!cedulaNormalizada.matches("\\d{9}")) {
            throw new ValidacionException("La cédula debe contener exactamente 9 dígitos.");
        }

        return cedulaNormalizada;
    }
}
