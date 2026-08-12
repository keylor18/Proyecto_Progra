package padron.pruebas;

import padron.datos.RepositorioDistritos;
import padron.datos.RepositorioPadron;
import padron.dto.PersonaDTO;
import padron.entidades.DistritoElectoral;
import padron.entidades.Persona;
import padron.excepciones.PersonaNoEncontradaException;
import padron.excepciones.ValidacionException;
import padron.logica.ServicioPadron;
import padron.utilidades.Configuracion;

public class PruebasRepositorioYServicio {

    public static void main(String[] args) throws Exception {
        Configuracion configuracion = Configuracion.cargar();
        RepositorioPadron repositorioPadron = new RepositorioPadron(configuracion.getRutaPadron());
        RepositorioDistritos repositorioDistritos = new RepositorioDistritos(configuracion.getRutaDistritos());
        ServicioPadron servicioPadron = new ServicioPadron(repositorioPadron, repositorioDistritos);

        probarRepositorioPadron(repositorioPadron);
        probarRepositorioDistritos(repositorioDistritos);
        probarServicioPadron(servicioPadron);
        probarCedulaInexistente(servicioPadron);
        probarCedulaInvalida(servicioPadron);

        System.out.println("PruebasRepositorioYServicio: OK");
    }

    private static void probarRepositorioPadron(RepositorioPadron repositorioPadron) throws Exception {
        Persona persona = repositorioPadron.buscarPorCedula("115550555")
                .orElseThrow(() -> new AssertionError("La cédula 115550555 debía existir en PADRON.txt."));

        TestUtil.assertEquals("115550555", persona.getCedula(), "La cédula parseada es incorrecta.");
        TestUtil.assertEquals("401017", persona.getCodigoElectoral(), "El código electoral parseado es incorrecto.");
        TestUtil.assertEquals("JUAN CARLOS", persona.getNombre(), "El nombre parseado es incorrecto.");
        TestUtil.assertEquals("MOSCOSO", persona.getPrimerApellido(),
                "El primer apellido parseado es incorrecto.");
        TestUtil.assertEquals("AGUERO", persona.getSegundoApellido(),
                "El segundo apellido parseado es incorrecto.");
        TestUtil.assertTrue(repositorioPadron.totalRegistros() > 1_000_000,
                "Se esperaba un padrón de tamaño considerable.");
    }

    private static void probarRepositorioDistritos(RepositorioDistritos repositorioDistritos) {
        DistritoElectoral distrito = repositorioDistritos.buscarPorCodigoElectoral("401017")
                .orElseThrow(() -> new AssertionError("El código electoral 401017 debía existir en distelec.txt."));

        TestUtil.assertEquals("HEREDIA", distrito.getProvincia(), "La provincia obtenida es incorrecta.");
        TestUtil.assertEquals("CENTRAL", distrito.getCanton(), "El cantón obtenido es incorrecto.");
        TestUtil.assertEquals("GUARARI", distrito.getDistrito(), "El distrito obtenido es incorrecto.");
        TestUtil.assertTrue(repositorioDistritos.totalDistritos() >= 2000,
                "Se esperaba cargar todos los distritos en memoria.");
    }

    private static void probarServicioPadron(ServicioPadron servicioPadron) throws Exception {
        PersonaDTO persona = servicioPadron.consultarPorCedula("115550555");

        TestUtil.assertEquals("115550555", persona.getCedula(), "La cédula en el DTO es incorrecta.");
        TestUtil.assertEquals("JUAN CARLOS", persona.getNombre(), "El nombre en el DTO es incorrecto.");
        TestUtil.assertEquals("MOSCOSO", persona.getPrimerApellido(),
                "El primer apellido en el DTO es incorrecto.");
        TestUtil.assertEquals("AGUERO", persona.getSegundoApellido(),
                "El segundo apellido en el DTO es incorrecto.");
        TestUtil.assertEquals("401017", persona.getCodigoElectoral(),
                "El código electoral en el DTO es incorrecto.");
        TestUtil.assertEquals("HEREDIA", persona.getProvincia(), "La provincia en el DTO es incorrecta.");
        TestUtil.assertEquals("CENTRAL", persona.getCanton(), "El cantón en el DTO es incorrecto.");
        TestUtil.assertEquals("GUARARI", persona.getDistrito(), "El distrito en el DTO es incorrecto.");
    }

    private static void probarCedulaInexistente(ServicioPadron servicioPadron) throws Exception {
        try {
            servicioPadron.consultarPorCedula("999999999");
            TestUtil.fail("La consulta debía fallar para una cédula inexistente.");
        } catch (PersonaNoEncontradaException ex) {
            TestUtil.assertEquals(404, ex.getCodigo(), "El código para persona inexistente debe ser 404.");
        }
    }

    private static void probarCedulaInvalida(ServicioPadron servicioPadron) throws Exception {
        try {
            servicioPadron.consultarPorCedula("11A");
            TestUtil.fail("La consulta debía fallar para una cédula inválida.");
        } catch (ValidacionException ex) {
            TestUtil.assertEquals(400, ex.getCodigo(), "El código para una cédula inválida debe ser 400.");
        }
    }
}
