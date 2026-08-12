package padron.utilidades;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class ConfiguradorLogs {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private ConfiguradorLogs() {
    }

    public static void configurar() {
        Logger raiz = LogManager.getLogManager().getLogger("");
        raiz.setLevel(Level.INFO);

        for (Handler handler : raiz.getHandlers()) {
            handler.setLevel(Level.INFO);
            handler.setFormatter(new FormatoConsola());
            try {
                handler.setEncoding(StandardCharsets.UTF_8.name());
            } catch (Exception ex) {
                // Si la consola no permite cambiar la codificación, mantenemos el log funcional.
            }
        }
    }

    private static final class FormatoConsola extends Formatter {

        @Override
        public String format(LogRecord record) {
            String hora = record.getInstant()
                    .atZone(ZoneId.systemDefault())
                    .format(FORMATO_HORA);

            StringBuilder salida = new StringBuilder();
            salida.append("[")
                    .append(hora)
                    .append("] ")
                    .append(record.getLevel().getName())
                    .append(" ")
                    .append(formatMessage(record))
                    .append(System.lineSeparator());

            if (record.getThrown() != null) {
                StringWriter buffer = new StringWriter();
                try (PrintWriter escritor = new PrintWriter(buffer)) {
                    record.getThrown().printStackTrace(escritor);
                }
                salida.append(buffer);
            }

            return salida.toString();
        }
    }
}
