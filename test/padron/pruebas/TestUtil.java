package padron.pruebas;

public final class TestUtil {

    private TestUtil() {
    }

    public static void assertEquals(Object esperado, Object actual, String mensaje) {
        if (esperado == null ? actual != null : !esperado.equals(actual)) {
            throw new AssertionError(mensaje + " Esperado: " + esperado + ". Actual: " + actual + ".");
        }
    }

    public static void assertTrue(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }

    public static void fail(String mensaje) {
        throw new AssertionError(mensaje);
    }
}
