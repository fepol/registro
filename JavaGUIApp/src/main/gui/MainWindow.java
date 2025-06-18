package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import controllers.EventController;
import utils.Constants;
import gui.components.CustomComponents;
import gui.components.TableUtils;
import gui.components.LoadingDialog;

public class MainWindow extends JFrame {
    private JTable prestamosTable, ventasTable, atrasosTable;
    private DefaultTableModel prestamosModel, ventasModel, atrasosModel;
    private JButton registrarButton, finalizarButton, eliminarButton, abrirCajaButton, cerrarCajaButton;
    private JLabel resumenAtrasosLabel;
    private EventController controller;
    private JPanel estadisticasPanel;
    private JLabel estadisticasLabel;
    private boolean eliminarSinPassword = false;

    public MainWindow() {
        controller = new EventController(this);
        initializeWindow();
        initializeComponents();
        setupLayout();
        setupEventListeners();
        controller.loadData();
        setupEstadisticasPanel();
        checkAbrirCajaAlInicio();
        setVisible(true);
        // Atajo secreto: Alt+E para eliminar sin contraseña
        KeyStroke ks = KeyStroke.getKeyStroke('E', InputEvent.ALT_DOWN_MASK, false);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, "eliminarSinPassword");
        getRootPane().getActionMap().put("eliminarSinPassword", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                eliminarSinPassword = true;
                handleEliminar();
                eliminarSinPassword = false;
            }
        });
    }

    private void initializeWindow() {
        setTitle(Constants.APP_TITLE);
        setSize(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(null);
        setResizable(false); // No permite modificar el tamaño
        // Elimina decoraciones personalizadas para evitar doble barra de título
        // setUndecorated(false); // innecesario, por defecto es false
        // setDefaultLookAndFeelDecorated(true); // QUITADO
        // getRootPane().setWindowDecorationStyle(JRootPane.FRAME); // QUITADO
        setExtendedState(JFrame.NORMAL); // Asegura que no esté maximizada
        // Nota: setMaximizable solo existe en JInternalFrame, no JFrame
    }

    private void initializeComponents() {
        prestamosModel = new DefaultTableModel(Constants.PRESTAMOS_COLUMNS, 0);
        prestamosTable = new JTable(prestamosModel);

        atrasosModel = new DefaultTableModel(Constants.ATRASOS_COLUMNS, 0);
        atrasosTable = new JTable(atrasosModel);

        String[] ventasVisibleColumns = {"PRODUCTO", "CANTIDAD", "TOTAL"};
        ventasModel = new DefaultTableModel(Constants.VENTAS_COLUMNS, 0);
        ventasTable = new JTable(ventasModel);
        for (int i = ventasModel.getColumnCount() - 1; i >= 0; i--) {
            String colName = ventasModel.getColumnName(i);
            boolean visible = false;
            for (String vcol : ventasVisibleColumns) {
                if (colName.equals(vcol)) { visible = true; break; }
            }
            if (!visible) {
                ventasTable.getColumnModel().removeColumn(ventasTable.getColumnModel().getColumn(i));
            }
        }

        registrarButton = new JButton("REGISTRAR");
        finalizarButton = new JButton("FINALIZAR");
        eliminarButton = new JButton("ELIMINAR");
        abrirCajaButton = new JButton("ABRIR CAJA");
        cerrarCajaButton = new JButton("CERRAR CAJA");
        resumenAtrasosLabel = new JLabel("Multas atrasos: $0.00 | Valores iniciales atrasos: $0.00");
    }

    private void setupLayout() {
        // Usar layout absoluto para mayor control
        // Tabla principal ocupa todo el alto (antes era 200+200)
        add(CustomComponents.createTitledPanel("PRESTAMOS Y ALQUILERES", prestamosTable,
            new Rectangle(20, 20, 900, 410)));
        add(CustomComponents.createTitledPanel("ATRASOS", atrasosTable,
            new Rectangle(20, 440, 900, 200)));
        // Tabla de ventas a la derecha, más angosta
        add(CustomComponents.createTitledPanel("VENTAS", ventasTable,
            new Rectangle(940, 20, 350, 620)));

        resumenAtrasosLabel.setBounds(20, 650, 500, 20);
        add(resumenAtrasosLabel);

        registrarButton.setBounds(1300, 50, 120, 30);
        finalizarButton.setBounds(1300, 90, 120, 30);
        eliminarButton.setBounds(1300, 130, 120, 30);
        abrirCajaButton.setBounds(1300, 250, 120, 30);
        cerrarCajaButton.setBounds(1300, 290, 120, 30);

        add(registrarButton);
        add(finalizarButton);
        add(eliminarButton);
        add(abrirCajaButton);
        add(cerrarCajaButton);
    }

    private void setupEstadisticasPanel() {
        estadisticasPanel = new JPanel();
        estadisticasPanel.setLayout(new BorderLayout());
        estadisticasLabel = new JLabel();
        estadisticasPanel.add(estadisticasLabel, BorderLayout.CENTER);
        estadisticasPanel.setBounds(1300, 350, 270, 180);
        add(estadisticasPanel);
        actualizarEstadisticas();
    }

    public void actualizarEstadisticas() {
        EventController.EstadisticasDiarias est = controller.calcularEstadisticasHoy();
        String html = "<html>" +
            "<span style='font-weight:bold; font-size:13pt;'>ESTADÍSTICAS DEL DÍA</span><br><br>" +
            "Apertura caja: <b>$" + String.format("%.2f", est.valorApertura) + "</b><br>" +
            "Total préstamos: <b>$" + String.format("%.2f", est.totalPrestamos) + "</b><br>" +
            "Total ventas: <b>$" + String.format("%.2f", est.totalVentas) + "</b><br>" +
            "<br><span style='font-weight:bold; font-size:12pt;'>POR DEVOLVER:</span><br>" +
            "Mandiles: <b>" + est.mandiles + "</b><br>" +
            "Gafas: <b>" + est.gafas + "</b><br>" +
            "Calculadoras: <b>" + est.calculadoras + "</b><br>" +
            "# Ventas: <b>" + est.cantVentas + "</b><br>" +
            "</html>";
        estadisticasLabel.setText(html);
        // Ajustar columnas de todas las tablas
        TableUtils.autoResizeColumns(prestamosTable);
        TableUtils.autoResizeColumns(atrasosTable);
        TableUtils.autoResizeColumns(ventasTable);
    }

    private void checkAbrirCajaAlInicio() {
        if (!controller.isCajaAbiertaHoy()) {
            while (true) {
                String input = JOptionPane.showInputDialog(this, "Ingrese el valor de apertura de caja para hoy:", "Apertura de Caja", JOptionPane.QUESTION_MESSAGE);
                if (input == null) System.exit(0);
                try {
                    double valor = Double.parseDouble(input.replace(",", "."));
                    controller.abrirCaja(valor);
                    break;
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Valor inválido. Intente de nuevo.");
                }
            }
        }
        actualizarEstadisticas();
    }

    /**
     * Muestra la pantalla de carga DESPUÉS de ejecutar la acción principal.
     * Úsalo tras actualizar la lista, eliminar, registrar, etc.
     * @param postAction Mensaje a mostrar en el loading (ej: "Actualizando lista...")
     */
    private void showLoadingAfterAction(String postAction) {
        LoadingDialog.showLoading(this, postAction, null);
    }

    private void setupEventListeners() {
        registrarButton.addActionListener(_ -> {
            mostrarVentanaRegistro();
            showLoadingAfterAction("Actualizando lista...");
        });
        finalizarButton.addActionListener(_ -> {
            int selectedRow = prestamosTable.getSelectedRow();
            if (selectedRow != -1) {
                controller.finalizarPrestamo(prestamosModel, selectedRow);
                actualizarEstadisticas();
                showLoadingAfterAction("Actualizando lista...");
                return;
            }
            selectedRow = atrasosTable.getSelectedRow();
            if (selectedRow != -1) {
                controller.finalizarPrestamo(atrasosModel, selectedRow);
                actualizarEstadisticas();
                showLoadingAfterAction("Actualizando lista...");
                return;
            }
            JOptionPane.showMessageDialog(MainWindow.this, "Seleccione un préstamo o atraso para finalizar.");
        });
        eliminarButton.addActionListener(_ -> {
            handleEliminar();
            actualizarEstadisticas();
            showLoadingAfterAction("Actualizando lista...");
        });
        abrirCajaButton.addActionListener(_ -> {
            if (controller.isCajaAbiertaHoy()) {
                JOptionPane.showMessageDialog(this, "La caja ya está abierta para hoy.");
                return;
            }
            String input = JOptionPane.showInputDialog(this, "Ingrese el valor de apertura de caja para hoy:", "Apertura de Caja", JOptionPane.QUESTION_MESSAGE);
            if (input == null) return;
            try {
                double valor = Double.parseDouble(input.replace(",", "."));
                controller.abrirCaja(valor);
                actualizarEstadisticas();
                showLoadingAfterAction("Abriendo caja...");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Valor inválido.");
            }
        });
        cerrarCajaButton.addActionListener(_ -> {
            controller.cerrarCaja();
            EventController.EstadisticasDiarias est = controller.calcularEstadisticasHoy();
            String resumen = "<html><b>CIERRE DE CAJA</b><br><br>" +
                "Apertura: $" + String.format("%.2f", est.valorApertura) + "<br>" +
                "Total préstamos: $" + String.format("%.2f", est.totalPrestamos) + "<br>" +
                "Total ventas: $" + String.format("%.2f", est.totalVentas) + "<br>" +
                "Mandiles: " + est.mandiles + "<br>" +
                "Gafas: " + est.gafas + "<br>" +
                "Calculadoras: " + est.calculadoras + "<br>" +
                "# Ventas: " + est.cantVentas + "<br>" +
                "<b>Total en caja: $" + String.format("%.2f", (est.valorApertura + est.totalPrestamos + est.totalVentas)) + "</b></html>";
            JOptionPane.showMessageDialog(this, resumen, "Cierre de Caja", JOptionPane.INFORMATION_MESSAGE);
            actualizarEstadisticas();
            showLoadingAfterAction("Cerrando caja...");
        });
        setupTableSelectionListeners();
    }

    private void setupTableSelectionListeners() {
        prestamosTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                ventasTable.clearSelection();
                atrasosTable.clearSelection();
            }
        });

        ventasTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                prestamosTable.clearSelection();
                atrasosTable.clearSelection();
            }
        });

        atrasosTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                prestamosTable.clearSelection();
                ventasTable.clearSelection();
            }
        });
    }

    private void handleEliminar() {
        int selectedRow = prestamosTable.getSelectedRow();
        String tabla = null;
        DefaultTableModel model = null;
        if (selectedRow != -1) {
            tabla = "prestamos";
            model = prestamosModel;
        } else {
            selectedRow = atrasosTable.getSelectedRow();
            if (selectedRow != -1) {
                tabla = "prestamos"; // atrasos también se eliminan de prestamos.csv
                model = atrasosModel;
            } else {
                selectedRow = ventasTable.getSelectedRow();
                if (selectedRow != -1) {
                    tabla = "ventas";
                    model = ventasModel;
                }
            }
        }
        if (tabla == null || model == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una fila para eliminar.");
            return;
        }
        if (!eliminarSinPassword) {
            // Restaurar a JOptionPane estándar
            JPasswordField pwd = new JPasswordField();
            // Atajo Alt+E para saltar contraseña en el dialogo
            final boolean[] skip = {false};
            JDialog dialog = new JDialog(this, "Ingrese la contraseña para eliminar", true);
            dialog.setLayout(new BorderLayout());
            dialog.add(new JLabel("Contraseña:"), BorderLayout.NORTH);
            dialog.add(pwd, BorderLayout.CENTER);
            JPanel btnPanel = new JPanel();
            JButton okBtn = new JButton("OK");
            JButton cancelBtn = new JButton("Cancelar");
            btnPanel.add(okBtn); btnPanel.add(cancelBtn);
            dialog.add(btnPanel, BorderLayout.SOUTH);
            final boolean[] accepted = {false};
            okBtn.addActionListener(e -> { accepted[0] = true; dialog.dispose(); });
            cancelBtn.addActionListener(e -> { dialog.dispose(); });
            // Atajo Alt+E
            KeyStroke ks = KeyStroke.getKeyStroke('E', InputEvent.ALT_DOWN_MASK, false);
            dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, "eliminarSinPassword");
            dialog.getRootPane().getActionMap().put("eliminarSinPassword", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    eliminarSinPassword = true;
                    dialog.dispose();
                }
            });
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            if (!accepted[0] && !eliminarSinPassword) return;
            if (!eliminarSinPassword) {
                String pass = new String(pwd.getPassword());
                if (!"FEPOL2025".equals(pass)) {
                    JOptionPane.showMessageDialog(this, "Contraseña incorrecta. No se eliminará la fila.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }
        controller.eliminarRegistro(selectedRow, model, tabla);
    }

    private void mostrarVentanaRegistro() {
        JDialog dialog = new JDialog(this, "Nuevo Registro", true);
        dialog.setSize(400, 350);
        dialog.setResizable(false); // Fijo e inmodificable
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Préstamo/Alquiler", createAlquilerPanel(dialog));
        tabbedPane.addTab("Ventas", createVentasPanel(dialog));
        dialog.add(tabbedPane, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private JPanel createAlquilerPanel(JDialog dialog) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField nombreField = new JTextField(15);
        JComboBox<String> prestamoCombo = new JComboBox<>(Constants.PRESTAMO_OPTIONS);
        JComboBox<String> garantiaCombo = new JComboBox<>(Constants.GARANTIA_OPTIONS);
        JTextField objetoValorField = new JTextField(15);
        JLabel objetoLabel = new JLabel("¿Cuál objeto?:");

        Dimension fieldSize = new Dimension(150, 25);
        nombreField.setPreferredSize(fieldSize);
        prestamoCombo.setPreferredSize(fieldSize);
        garantiaCombo.setPreferredSize(fieldSize);
        objetoValorField.setPreferredSize(fieldSize);

        CustomComponents.setUpperCaseFilter(nombreField);
        CustomComponents.setUpperCaseFilter(objetoValorField);

        gbc.gridx = 0; gbc.gridy = 0;
        fieldsPanel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        fieldsPanel.add(nombreField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        fieldsPanel.add(new JLabel("Préstamo:"), gbc);
        gbc.gridx = 1;
        fieldsPanel.add(prestamoCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        fieldsPanel.add(new JLabel("Garantía:"), gbc);
        gbc.gridx = 1;
        fieldsPanel.add(garantiaCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        fieldsPanel.add(objetoLabel, gbc);
        gbc.gridx = 1;
        fieldsPanel.add(objetoValorField, gbc);

        // Siempre visible, pero inhabilitado si no es OBJETO DE VALOR
        objetoLabel.setVisible(true);
        objetoValorField.setVisible(true);
        objetoValorField.setEnabled(garantiaCombo.getSelectedItem().equals("OBJETO DE VALOR"));
        garantiaCombo.addActionListener(_ -> {
            boolean isObjetoValor = garantiaCombo.getSelectedItem().equals("OBJETO DE VALOR");
            objetoValorField.setEnabled(isObjetoValor);
            if (!isObjetoValor) objetoValorField.setText("");
        });

        mainPanel.add(fieldsPanel);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton registrarButton = new JButton("Registrar");
        registrarButton.setPreferredSize(new Dimension(90, 30));
        registrarButton.addActionListener(_ -> {
            handleRegistroAlquiler(dialog, nombreField, prestamoCombo, garantiaCombo, objetoValorField);
        });
        buttonPanel.add(registrarButton);
        mainPanel.add(Box.createVerticalGlue());
        mainPanel.add(buttonPanel);
        return mainPanel;
    }

    private JPanel createVentasPanel(JDialog dialog) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel para los campos
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JComboBox<String> productoCombo = new JComboBox<>(Constants.PRODUCTOS_VENTA);
        JSpinner cantidadSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        JLabel precioLabel = new JLabel("Precio: $0.25");
        JLabel totalLabel = new JLabel("Total: $0.25");
        
        // Establecer tamaños
        Dimension fieldSize = new Dimension(150, 25);
        productoCombo.setPreferredSize(fieldSize);
        cantidadSpinner.setPreferredSize(fieldSize);

        // Agregar listener para actualizar precios
        productoCombo.addActionListener(_ -> {
            String producto = (String) productoCombo.getSelectedItem();
            double precio = Constants.PRECIOS_PRODUCTOS.get(producto);
            int cantidad = (Integer) cantidadSpinner.getValue();
            precioLabel.setText(String.format("Precio: $%.2f", precio));
            totalLabel.setText(String.format("Total: $%.2f", precio * cantidad));
        });
        cantidadSpinner.addChangeListener(_ -> {
            String producto = (String) productoCombo.getSelectedItem();
            double precio = Constants.PRECIOS_PRODUCTOS.get(producto);
            int cantidad = (Integer) cantidadSpinner.getValue();
            totalLabel.setText(String.format("Total: $%.2f", precio * cantidad));
        });

        // Agregar componentes
        gbc.gridx = 0; gbc.gridy = 0;
        fieldsPanel.add(new JLabel("Producto:"), gbc);
        gbc.gridx = 1;
        fieldsPanel.add(productoCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        fieldsPanel.add(new JLabel("Cantidad:"), gbc);
        gbc.gridx = 1;
        fieldsPanel.add(cantidadSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        fieldsPanel.add(precioLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        fieldsPanel.add(totalLabel, gbc);

        mainPanel.add(fieldsPanel);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton registrarButton = new JButton("Registrar");
        registrarButton.setPreferredSize(new Dimension(90, 30));
        registrarButton.addActionListener(_ -> {
            handleRegistroVenta(dialog, productoCombo, cantidadSpinner);
        });
        buttonPanel.add(registrarButton);

        mainPanel.add(Box.createVerticalGlue());
        mainPanel.add(buttonPanel);

        return mainPanel;
    }

    private void handleRegistroAlquiler(JDialog dialog, JTextField nombreField,
                                      JComboBox<String> prestamoCombo,
                                      JComboBox<String> garantiaCombo,
                                      JTextField objetoValorField) {
        String nombre = nombreField.getText().trim();
        String prestamo = (String) prestamoCombo.getSelectedItem();
        String garantia = (String) garantiaCombo.getSelectedItem();
        String objetoValor = objetoValorField.getText().trim();

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "El nombre es obligatorio.");
            return;
        }
        if (garantia.equals("OBJETO DE VALOR") && objetoValor.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Debe especificar el objeto de valor.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (prestamo.equals("CALCULADORA")) {
            JLabel label = new JLabel("Debe hacer firmar el acta de compromiso.");
            label.setForeground(Color.RED);
            int res = JOptionPane.showOptionDialog(dialog, label, "Acta de Compromiso",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new Object[] {"Sí, firmó", "No firmó"}, "Sí, firmó");
            if (res != 0) return; // Si no firmó, cancelar registro
        }
        String horaInicio = new SimpleDateFormat("HH:mm:ss").format(new Date());
        // Guardar el objeto de valor en la columna GARANTIA si aplica, si no, solo "CÉDULA"
        String garantiaFinal = garantia.equals("OBJETO DE VALOR") ? objetoValor : "CÉDULA";
        controller.registrarPrestamo(nombre, prestamo, horaInicio, garantiaFinal, "");
        dialog.dispose();
    }

    private void handleRegistroVenta(JDialog dialog, JComboBox<String> productoCombo, JSpinner cantidadSpinner) {
        String producto = (String) productoCombo.getSelectedItem();
        int cantidad = (Integer) cantidadSpinner.getValue();
        double precioUnitario = Constants.PRECIOS_PRODUCTOS.get(producto);
        double total = precioUnitario * cantidad;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String hora = sdf.format(new Date());
        controller.registrarVenta(producto, hora, cantidad, precioUnitario, total);
        dialog.dispose();
    }

    // Getters for models
    public DefaultTableModel getPrestamosModel() { return prestamosModel; }
    public DefaultTableModel getVentasModel() { return ventasModel; }
    public DefaultTableModel getAtrasosModel() { return atrasosModel; }

}