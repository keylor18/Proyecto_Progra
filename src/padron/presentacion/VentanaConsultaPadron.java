package padron.presentacion;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/**
 * Interfaz gráfica para consultar el padrón mediante los servidores locales.
 * Permite elegir el protocolo de comunicación, validar la cédula y mostrar la
 * información recibida sin bloquear la ventana mientras se realiza la consulta.
 */
public class VentanaConsultaPadron extends JFrame {

    private static final String HOST = "127.0.0.1";
    private static final Pattern CAMPO_JSON = Pattern.compile("\\\"([^\\\"]+)\\\":(?:\\\"((?:\\\\.|[^\\\"])*)\\\"|(true|false|\\d+))");

    private final int puertoTcp;
    private final int puertoHttp;
    private final JTextField campoCedula = new JTextField(12);
    private final JComboBox<String> selectorProtocolo = new JComboBox<>(new String[]{"HTTP", "TCP"});
    private final JTextArea areaResultado = new JTextArea();
    private final JButton botonConsultar = new JButton("Ejecutar consulta");

    public VentanaConsultaPadron(int puertoTcp, int puertoHttp) {
        this.puertoTcp = puertoTcp;
        this.puertoHttp = puertoHttp;
        configurarVentana();
    }

    private void configurarVentana() {
        setTitle("Consulta del Padrón Electoral");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(650, 430));
        setLocationByPlatform(true);

        JPanel contenido = new JPanel(new BorderLayout(10, 10));
        contenido.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        contenido.add(crearPanelConsulta(), BorderLayout.NORTH);

        areaResultado.setEditable(false);
        areaResultado.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        areaResultado.setLineWrap(true);
        areaResultado.setWrapStyleWord(true);
        areaResultado.setText("Ingrese una cédula, seleccione HTTP o TCP y ejecute la consulta.");
        contenido.add(new JScrollPane(areaResultado), BorderLayout.CENTER);
        contenido.add(crearPanelBotones(), BorderLayout.SOUTH);
        setContentPane(contenido);

        botonConsultar.addActionListener(evento -> consultar());
        campoCedula.addActionListener(evento -> consultar());
    }

    private JPanel crearPanelConsulta() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Datos de la consulta"));
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(5, 5, 5, 5);
        restricciones.anchor = GridBagConstraints.WEST;

        restricciones.gridx = 0;
        restricciones.gridy = 0;
        panel.add(new JLabel("Ingresar el número de cédula:"), restricciones);
        restricciones.gridx = 1;
        restricciones.fill = GridBagConstraints.HORIZONTAL;
        restricciones.weightx = 1;
        panel.add(campoCedula, restricciones);

        restricciones.gridx = 0;
        restricciones.gridy = 1;
        restricciones.weightx = 0;
        restricciones.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Protocolo de comunicacion:"), restricciones);
        restricciones.gridx = 1;
        restricciones.fill = GridBagConstraints.HORIZONTAL;
        panel.add(selectorProtocolo, restricciones);
        return panel;
    }

    private JPanel crearPanelBotones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton botonLimpiar = new JButton("Limpiar");
        JButton botonSalir = new JButton("Salir");
        botonLimpiar.addActionListener(evento -> limpiar());
        botonSalir.addActionListener(evento -> System.exit(0));
        panel.add(botonConsultar);
        panel.add(botonLimpiar);
        panel.add(botonSalir);
        return panel;
    }

    private void consultar() {
        String cedula = campoCedula.getText().trim();
        if (cedula.isEmpty()) {
            mostrarValidacion("La cédula no puede estar vacía.");
            return;
        }
        if (!cedula.matches("\\d{9}")) {
            mostrarValidacion("La cédula debe contener exactamente 9 dígitos.");
            return;
        }

        String protocolo = (String) selectorProtocolo.getSelectedItem();
        botonConsultar.setEnabled(false);
        areaResultado.setText("Consultando por " + protocolo + "...");
        // La comunicación de red se ejecuta fuera del hilo de la interfaz para
        // que la ventana pueda seguir respondiendo a las acciones del usuario.
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return "HTTP".equals(protocolo) ? consultarHttp(cedula) : consultarTcp(cedula);
            }

            @Override
            protected void done() {
                botonConsultar.setEnabled(true);
                try {
                    areaResultado.setText(formatearJson(get()));
                } catch (Exception ex) {
                    areaResultado.setText("No fue posible completar la consulta.\n" + mensajeCausa(ex));
                }
            }
        }.execute();
    }

    private String consultarHttp(String cedula) throws IOException {
        // HTTP utiliza la ruta definida por el servidor: /padron/{cedula}.
        HttpURLConnection conexion = (HttpURLConnection) URI.create("http://" + HOST + ":" + puertoHttp
                + "/padron/" + cedula).toURL().openConnection();
        conexion.setRequestMethod("GET");
        conexion.setConnectTimeout(5000);
        conexion.setReadTimeout(5000);
        int codigo = conexion.getResponseCode();
        InputStream entrada = codigo >= 400 ? conexion.getErrorStream() : conexion.getInputStream();
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            return lector.readLine();
        } finally {
            conexion.disconnect();
        }
    }

    private String consultarTcp(String cedula) throws IOException {
        // TCP envía una única línea con el formato GET|cedula y recibe JSON.
        try (Socket socket = new Socket(HOST, puertoTcp);
                BufferedWriter escritor = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader lector = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            socket.setSoTimeout(5000);
            escritor.write("GET|" + cedula);
            escritor.newLine();
            escritor.flush();
            return lector.readLine();
        }
    }

    private String formatearJson(String json) {
        if (json == null || json.isBlank()) {
            return "El servidor no envio una respuesta.";
        }
        // Se extraen los campos de la respuesta JSON para presentarlos con
        // etiquetas legibles para la persona usuaria.
        Map<String, String> campos = new LinkedHashMap<>();
        Matcher matcher = CAMPO_JSON.matcher(json);
        while (matcher.find()) {
            campos.put(matcher.group(1), matcher.group(2) != null ? desescapar(matcher.group(2)) : matcher.group(3));
        }
        if (campos.containsKey("error")) {
            return "Error " + campos.getOrDefault("codigo", "") + ": " + campos.getOrDefault("mensaje", "Error desconocido.");
        }
        if (!campos.containsKey("cedula")) {
            return json;
        }
        return "Cédula: " + campos.get("cedula")
                + "\nNombre: " + campos.get("nombre")
                + "\nPrimer apellido: " + campos.get("primerApellido")
                + "\nSegundo apellido: " + campos.get("segundoApellido")
                + "\nCodigo electoral: " + campos.get("codigoElectoral")
                + "\nProvincia: " + campos.get("provincia")
                + "\nCanton: " + campos.get("canton")
                + "\nDistrito: " + campos.get("distrito");
    }

    private String desescapar(String valor) {
        return valor.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String mensajeCausa(Exception ex) {
        Throwable causa = ex.getCause() == null ? ex : ex.getCause();
        return causa.getMessage() == null ? causa.getClass().getSimpleName() : causa.getMessage();
    }

    private void mostrarValidacion(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Datos invalidos", JOptionPane.WARNING_MESSAGE);
        campoCedula.requestFocusInWindow();
    }

    private void limpiar() {
        // Restaura el formulario a su estado inicial para una nueva consulta.
        campoCedula.setText("");
        selectorProtocolo.setSelectedIndex(0);
        areaResultado.setText("Ingrese una cédula, seleccione HTTP o TCP y ejecute la consulta.");
        campoCedula.requestFocusInWindow();
    }
}
