package controllers;

import javax.swing.table.DefaultTableModel;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import gui.MainWindow;
import java.util.Locale;
import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class EventController {
    private MainWindow view;
    private final SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");
    private final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");

    // --- CAJA ---
    private final String CAJA_FILE = "caja.csv";

    public boolean isCajaAbiertaHoy() {
        String fechaHoy = sdfDate.format(new Date());
        try (BufferedReader br = new BufferedReader(new FileReader(CAJA_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3 && parts[0].equals(fechaHoy) && parts[1].equals("abierta")) {
                    return true;
                }
            }
        } catch (IOException e) { /* archivo puede no existir */ }
        return false;
    }

    public double getValorAperturaHoy() {
        String fechaHoy = sdfDate.format(new Date());
        try (BufferedReader br = new BufferedReader(new FileReader(CAJA_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3 && parts[0].equals(fechaHoy) && parts[1].equals("abierta")) {
                    return Double.parseDouble(parts[2]);
                }
            }
        } catch (IOException e) { }
        return 0.0;
    }

    public void abrirCaja(double valor) {
        String fechaHoy = sdfDate.format(new Date());
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(CAJA_FILE, true))) {
            bw.write(fechaHoy + ",abierta," + valor);
            bw.newLine();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void cerrarCaja() {
        String fechaHoy = sdfDate.format(new Date());
        // Marcar como cerrada
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(CAJA_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3 && parts[0].equals(fechaHoy) && parts[1].equals("abierta")) {
                    lines.add(fechaHoy + ",cerrada," + parts[2]);
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) { }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(CAJA_FILE))) {
            for (String l : lines) {
                bw.write(l); bw.newLine();
            }
        } catch (IOException e) { }
    }

    // --- ESTADÍSTICAS ---
    public EstadisticasDiarias calcularEstadisticasHoy() {
        EstadisticasDiarias est = new EstadisticasDiarias();
        String fechaHoy = sdfDate.format(new Date());
        // Prestamos vigentes (no finalizados), sin filtrar por fecha (incluye atrasos)
        try (BufferedReader br = new BufferedReader(new FileReader("prestamos.csv"))) {
            String line; br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length < 8) continue;
                String horaFinal = p[3];
                if (horaFinal != null && !horaFinal.equals("---") && !horaFinal.trim().isEmpty()) continue; // solo los que NO han sido finalizados
                String tipo = p[1].toUpperCase();
                if (tipo.equals("MANDIL")) est.mandiles++;
                else if (tipo.equals("GAFAS")) est.gafas++;
                else if (tipo.equals("CALCULADORA")) est.calculadoras++;
                else if (tipo.equals("AMBOS")) { est.mandiles++; est.gafas++; }
            }
        } catch (IOException e) { }
        // Total dinero de préstamos (todos los del día, finalizados o no)
        try (BufferedReader br = new BufferedReader(new FileReader("prestamos.csv"))) {
            String line; br.readLine();
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length < 8) continue;
                if (!p[7].equals(fechaHoy)) continue;
                est.totalPrestamos += Double.parseDouble(p[6].replace("---", "0").replace("$", "").replace(",", "."));
            }
        } catch (IOException e) { }
        // Ventas
        try (BufferedReader br = new BufferedReader(new FileReader("ventas.csv"))) {
            String line; br.readLine();
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                if (v.length < 6) continue;
                if (!v[5].equals(fechaHoy)) continue;
                est.totalVentas += Double.parseDouble(v[4].replace("---", "0").replace("$", "").replace(",", "."));
                est.cantVentas++;
            }
        } catch (IOException e) { }
        est.valorApertura = getValorAperturaHoy();
        return est;
    }

    public static class EstadisticasDiarias {
        public double valorApertura = 0;
        public double totalPrestamos = 0;
        public double totalVentas = 0;
        public int mandiles = 0, gafas = 0, calculadoras = 0, cantVentas = 0;
    }

    public EventController(MainWindow view) {
        this.view = view;
    }

    public void registrarPrestamo(String nombre, String prestamo, String horaInicio, String garantia, String observacion) {
        // Si algún campo es nulo o vacío, poner '---'
        String nombreFinal = (nombre == null || nombre.trim().isEmpty()) ? "---" : nombre;
        String prestamoFinal = (prestamo == null || prestamo.trim().isEmpty()) ? "---" : prestamo;
        String horaInicioFinal = (horaInicio == null || horaInicio.trim().isEmpty()) ? "---" : horaInicio;
        String horaFinal = "---";
        String duracion = "---";
        String garantiaFinal = (garantia == null || garantia.trim().isEmpty()) ? "---" : garantia;
        String vFinal = "---";
        String fechaActual = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        // Orden: NOMBRE, PRESTAMO, HORA INICIAL, HORA FINAL, DURACION, GARANTIA, V.FINAL, FECHA
        String linea = String.join(",",
            nombreFinal,
            prestamoFinal,
            horaInicioFinal,
            horaFinal,
            duracion,
            garantiaFinal,
            vFinal,
            fechaActual
        );
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("prestamos.csv", true))) {
            bw.write(linea);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        loadData();
    }

    public void registrarVenta(String producto, String hora, int cantidad, double precioUnitario, double total) {
        String fechaActual = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String horaActual = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String linea = String.join(",",
            producto,
            horaActual,
            String.valueOf(cantidad),
            String.format(Locale.US, "%.2f", precioUnitario), // <-- Fuerza punto decimal
            String.format(Locale.US, "%.2f", total),          // <-- Fuerza punto decimal
            fechaActual
        );
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("ventas.csv", true))) {
            bw.write(linea);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        loadData();
    }

    public void finalizarPrestamo(DefaultTableModel model, int selectedRow) {
        // Obtener todos los valores de la fila seleccionada
        int columnCount = model.getColumnCount();
        String[] selectedValues = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            Object value = model.getValueAt(selectedRow, i);
            selectedValues[i] = value != null ? value.toString() : "";
        }

        // Leer todos los registros del archivo
        String[][] prestamos = leerCSV("prestamos.csv");
        int filaCsv = -1;
        // Buscar la fila en el CSV que coincida con todos los valores de la fila seleccionada
        outer:
        for (int i = 1; i < prestamos.length; i++) {
            String[] p = prestamos[i];
            if (p.length != selectedValues.length) continue;
            for (int j = 0; j < selectedValues.length; j++) {
                if (!p[j].equals(selectedValues[j])) {
                    continue outer;
                }
            }
            filaCsv = i;
            break;
        }
        if (filaCsv == -1) {
            JOptionPane.showMessageDialog(null, "No se encontró el registro correspondiente en el archivo.\n\nValores buscados:\n" + String.join(", ", selectedValues));
            return;
        }
        String[] prestamo = prestamos[filaCsv];
        String horaInicio = prestamo[2];
        if (horaInicio == null || horaInicio.equals("---") || horaInicio.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "No se puede finalizar: hora de inicio vacía o inválida. Valor leído: '" + horaInicio + "'");
            return;
        }
        // Normalizar hora de inicio
        String horaInicioNormalizada = horaInicio;
        try {
            sdfTime.parse(horaInicio);
        } catch (Exception e) {
            try {
                String[] partes = horaInicio.split(":");
                if (partes.length == 2) {
                    horaInicioNormalizada = String.format("%02d:%02d:00", Integer.parseInt(partes[0]), Integer.parseInt(partes[1]));
                } else if (partes.length == 3) {
                    horaInicioNormalizada = String.format("%02d:%02d:%02d", Integer.parseInt(partes[0]), Integer.parseInt(partes[1]), Integer.parseInt(partes[2]));
                }
                sdfTime.parse(horaInicioNormalizada);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Hora de inicio inválida o no reconocida: '" + horaInicio + "'.\nFormato esperado: HH:mm:ss");
                return;
            }
        }
        try {
            // Usar LocalDateTime para máxima precisión
            LocalDate fechaIni = LocalDate.parse(prestamo[7]); // yyyy-MM-dd
            LocalTime horaIni = LocalTime.parse(horaInicioNormalizada); // HH:mm:ss
            LocalDateTime inicio = LocalDateTime.of(fechaIni, horaIni);
            LocalDateTime fin = LocalDateTime.now();
            String horaFin = fin.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String fechaFin = fin.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            // Calcular días de diferencia
            long diasDiferencia = ChronoUnit.DAYS.between(fechaIni, fin.toLocalDate());
            // Calcular duración real
            Duration duracion = Duration.between(inicio, fin);
            long horas = duracion.toHours();
            long minutos = duracion.toMinutes() % 60;
            long segs = duracion.getSeconds() % 60;
            String duracionStr = String.format("%02d:%02d:%02d", horas, minutos, segs);
            // Valor base
            double valorBase = 0.5;
            if (prestamo[1] != null && prestamo[1].equalsIgnoreCase("AMBOS")) valorBase = 1.0;
            // Calcular multa
            double multa = 0.0;
            if (diasDiferencia == 0) {
                // Mismo día: multa por horas extra
                long minutosTotales = duracion.toMinutes();
                long minutosBase = 150; // 2h 30min
                if (minutosTotales > minutosBase) {
                    long horasExtra = (long) Math.ceil((minutosTotales - minutosBase) / 60.0);
                    multa = Math.min(0.25 * horasExtra, 1.0);
                }
            } else if (diasDiferencia > 0) {
                // Días extra: 1 por cada día
                multa = diasDiferencia * 1.0;
            }
            // Panel principal
            JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
            panel.add(new JLabel("Hora inicial:")); panel.add(new JLabel(horaInicioNormalizada));
            panel.add(new JLabel("Hora final:")); panel.add(new JLabel(horaFin));
            panel.add(new JLabel("Duración:")); panel.add(new JLabel(duracionStr));
            panel.add(new JLabel("Fecha de alquiler:")); panel.add(new JLabel(prestamo[7]));
            panel.add(new JLabel("Fecha de finalización:")); panel.add(new JLabel(fechaFin));
            panel.add(new JLabel("Días de diferencia:")); panel.add(new JLabel(String.valueOf(diasDiferencia)));
            panel.add(new JLabel("Multa recomendada:"));
            JLabel multaLabel = new JLabel(String.format("$%.2f", multa));
            multaLabel.setForeground(Color.RED);
            panel.add(multaLabel);
            JRadioButton rbRecomendada = new JRadioButton("Usar MULTA RECOMENDADA", true);
            JRadioButton rbPersonalizada = new JRadioButton("Ingresar MULTA PERSONALIZADA");
            ButtonGroup group = new ButtonGroup();
            group.add(rbRecomendada);
            group.add(rbPersonalizada);
            JTextField multaPersonalizada = new JTextField();
            multaPersonalizada.setEnabled(false);
            rbPersonalizada.addActionListener(_ -> multaPersonalizada.setEnabled(true));
            rbRecomendada.addActionListener(_ -> multaPersonalizada.setEnabled(false));
            panel.add(rbRecomendada); panel.add(rbPersonalizada);
            panel.add(new JLabel("Multa personalizada:")); panel.add(multaPersonalizada);
            int result = JOptionPane.showConfirmDialog(null, panel, "Finalizar Préstamo", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;
            double multaElegida = multa;
            if (rbPersonalizada.isSelected()) {
                String texto = multaPersonalizada.getText().replace(',', '.').replaceAll("[^0-9.\\-+()]", "");
                try {
                    multaElegida = Double.parseDouble(texto);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Multa personalizada inválida.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            double totalPagar = valorBase + multaElegida;
            // Si el registro es un atraso (fecha original distinta a la actual), cambiar fecha y duración
            String fechaSistema = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String nuevaDuracion = duracionStr;
            if (!prestamo[7].equals(fechaSistema)) {
                // Es un atraso finalizado
                fechaFin = fechaSistema;
                nuevaDuracion = "ATRASO";
            }
            // Actualizar el registro en prestamos.csv
            String[] actualizado = new String[] {
                prestamo[0],
                prestamo[1],
                horaInicioNormalizada,
                horaFin,
                nuevaDuracion,
                prestamo[5],
                String.format(Locale.US, "%.2f", totalPagar),
                fechaFin
            };
            prestamos[filaCsv] = actualizado;
            // Sobrescribir el archivo con los datos actualizados
            try (BufferedWriter bw = new BufferedWriter(new FileWriter("prestamos.csv"))) {
                bw.write("NOMBRE,PRESTAMO,HORA INICIAL,HORA FINAL,DURACION,GARANTIA,V.FINAL,FECHA");
                bw.newLine();
                for (String[] fila : prestamos) {
                    if (fila == null) continue;
                    bw.write(String.join(",", fila));
                    bw.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Mostrar resumen final
            JPanel resumen = new JPanel(new GridLayout(0, 2, 5, 5));
            resumen.add(new JLabel("Multa seleccionada:")); resumen.add(new JLabel(String.format("$%.2f", multaElegida)));
            resumen.add(new JLabel("Valor del préstamo:")); resumen.add(new JLabel(String.format("$%.2f", valorBase)));
            resumen.add(new JLabel("Valor total a pagar:")); resumen.add(new JLabel(String.format("$%.2f", totalPagar)));
            resumen.add(new JLabel("Garantía: ")); resumen.add(new JLabel(prestamo[5]));
            JOptionPane.showMessageDialog(null, resumen, "Resumen Final", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al calcular la duración o las fechas.\n" + e.getMessage());
            return;
        }
        loadData();
    }

    public void eliminarRegistro(int selectedRow, DefaultTableModel model, String tabla) {
        // Obtener los valores de la fila seleccionada
        int columnCount = model.getColumnCount();
        String[] selectedValues = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            Object value = model.getValueAt(selectedRow, i);
            selectedValues[i] = value != null ? value.toString() : "";
        }
        String archivo = tabla.equals("ventas") ? "ventas.csv" : "prestamos.csv";
        String[][] registros = leerCSV(archivo);
        // Buscar la fila en el CSV que coincida con todos los valores de la fila seleccionada
        int filaCsv = -1;
        outer:
        for (int i = 1; i < registros.length; i++) { // Saltar encabezado
            String[] p = registros[i];
            int compararHasta = Math.min(selectedValues.length, p.length);
            for (int j = 0; j < compararHasta; j++) {
                if (!p[j].equals(selectedValues[j])) {
                    continue outer;
                }
            }
            filaCsv = i;
            break;
        }
        if (filaCsv == -1) {
            JOptionPane.showMessageDialog(null, "No se encontró el registro correspondiente en el archivo para eliminar.\n\nValores buscados:\n" + String.join(", ", selectedValues));
            return;
        }
        // Eliminar la fila del array
        List<String[]> nuevaLista = new ArrayList<>();
        for (int i = 0; i < registros.length; i++) {
            if (i != filaCsv) nuevaLista.add(registros[i]);
        }
        // Sobrescribir el archivo con los datos actualizados
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivo))) {
            for (String[] fila : nuevaLista) {
                bw.write(String.join(",", fila));
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        loadData();
    }

    public void loadData() {
        loadPrestamos();
        loadVentas();
        loadAtrasos();
    }

    private void loadAtrasos() {
        DefaultTableModel atrasosModel = view.getAtrasosModel();
        atrasosModel.setRowCount(0);
        String fechaActual = sdfDate.format(new Date());
        List<String> registrosUnicos = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get("prestamos.csv"));
            for (int i = 1; i < lines.size(); i++) { // Saltar encabezado
                String line = lines.get(i);
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",");
                if (data.length < 8) continue;
                String horaFinal = data[3];
                String fecha = data[7];
                // Mostrar solo préstamos NO finalizados y cuya fecha sea DIFERENTE a la actual
                if ((horaFinal.equals("---") || horaFinal.isEmpty()) && !fecha.equals(fechaActual)) {
                    String[] rowData = new String[8];
                    for (int j = 0; j < 8; j++) {
                        rowData[j] = data[j];
                    }
                    String clave = String.join("|", rowData);
                    if (!registrosUnicos.contains(clave)) {
                        atrasosModel.addRow(rowData);
                        registrosUnicos.add(clave);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPrestamos() {
        DefaultTableModel prestamosModel = view.getPrestamosModel();
        prestamosModel.setRowCount(0);
        String fechaActual = sdfDate.format(new Date());
        List<String> registrosUnicos = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get("prestamos.csv"));
            for (int i = 1; i < lines.size(); i++) { // Saltar encabezado
                String line = lines.get(i);
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",");
                // Rellenar con '---' si faltan columnas
                String[] rowData = new String[8];
                for (int j = 0; j < 8; j++) {
                    rowData[j] = (j < data.length && data[j] != null && !data[j].trim().isEmpty()) ? data[j] : "---";
                }
                // Solo mostrar los de la fecha actual
                if (rowData[7].equals(fechaActual)) {
                    // Evitar duplicados usando la línea completa como clave
                    String clave = String.join("|", rowData);
                    if (!registrosUnicos.contains(clave)) {
                        prestamosModel.addRow(rowData);
                        registrosUnicos.add(clave);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadVentas() {
        DefaultTableModel ventasModel = view.getVentasModel();
        ventasModel.setRowCount(0);
        String fechaActual = sdfDate.format(new Date());
        try {
            List<String> lines = Files.readAllLines(Paths.get("ventas.csv"));
            for (int i = 1; i < lines.size(); i++) { // Saltar encabezado
                String line = lines.get(i);
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",");
                if (data.length < 6) continue;
                if (!data[5].equals(fechaActual)) continue; // Solo mostrar ventas del día actual
                // Mostrar solo las primeras 6 columnas
                ventasModel.addRow(new Object[] {
                    data[0], // PRODUCTO
                    data[1], // HORA
                    data[2], // CANTIDAD
                    data[3], // PRECIO UNIT.
                    data[4], // TOTAL
                    data[5]  // FECHA
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[][] leerCSV(String archivo) {
        List<String[]> lineas = new ArrayList<>();
        int columnasEsperadas = 8; // Para prestamos.csv
        if (archivo.equals("ventas.csv")) columnasEsperadas = 6;
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(",");
                // Solo tomar las primeras columnasEsperadas columnas
                String[] nuevo = new String[columnasEsperadas];
                for (int i = 0; i < columnasEsperadas; i++) {
                    if (i < partes.length) {
                        nuevo[i] = partes[i];
                    } else {
                        nuevo[i] = "---";
                    }
                }
                lineas.add(nuevo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lineas.toArray(new String[0][]);
    }

    public void mostrarVentanaFinalizacion(String horaInicio, String garantia, String tipoPrestamo, String fechaInicio) {
        // Hora y fecha actuales
        LocalTime horaFinal = LocalTime.now();
        String horaFinalStr = horaFinal.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        LocalTime horaInicial = LocalTime.parse(horaInicio, DateTimeFormatter.ofPattern("HH:mm:ss"));

        // Calcular duración
        long segundos = ChronoUnit.SECONDS.between(horaInicial, horaFinal);
        long horas = segundos / 3600;
        long minutos = (segundos % 3600) / 60;
        long segs = segundos % 60;
        String duracionStr = String.format("%02d:%02d:%02d", horas, minutos, segs);

        // Valor base
        double valorBase = tipoPrestamo.equalsIgnoreCase("AMBOS") ? 1.00 : 0.50;

        // Calcular días transcurridos
        LocalDate fechaIni = LocalDate.parse(fechaInicio);
        LocalDate fechaFin = LocalDate.now();
        long diasTranscurridos = ChronoUnit.DAYS.between(fechaIni, fechaFin);

        // Multa horaria (solo si devolución el mismo día)
        double multaRecomendada = 0.0;
        if (diasTranscurridos == 0) {
            Duration duracion = Duration.between(horaInicial, horaFinal);
            Duration maxDuracion = Duration.ofHours(2).plusMinutes(30);
            if (duracion.compareTo(maxDuracion) > 0) {
                long horasExtra = (long) Math.ceil((double) (duracion.minus(maxDuracion).toMinutes()) / 60.0);
                multaRecomendada = Math.min(0.25 * horasExtra, 1.00);
            }
        }

        // Multa por días extra
        double multaDias = 0.0;
        if (diasTranscurridos > 0) {
            multaDias = diasTranscurridos * 1.00;
        }

        double multaTotal = multaRecomendada + multaDias;

        // Panel principal
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Hora inicial:")); panel.add(new JLabel(horaInicio));
        panel.add(new JLabel("Hora final:")); panel.add(new JLabel(horaFinalStr));
        panel.add(new JLabel("Duración:")); panel.add(new JLabel(duracionStr));
        panel.add(new JLabel("Días transcurridos:")); panel.add(new JLabel(String.valueOf(diasTranscurridos)));
        panel.add(new JLabel("Valor del préstamo:")); panel.add(new JLabel(String.format("$%.2f", valorBase)));
        panel.add(new JLabel("MULTA RECOMENDADA:"));
        JLabel multaLabel = new JLabel(String.format("$%.2f", multaTotal));
        multaLabel.setForeground(Color.RED);
        panel.add(multaLabel);

        // Opción de multa personalizada
        JRadioButton rbRecomendada = new JRadioButton("Usar MULTA RECOMENDADA", true);
        JRadioButton rbPersonalizada = new JRadioButton("Ingresar MULTA PERSONALIZADA");
        ButtonGroup group = new ButtonGroup();
        group.add(rbRecomendada);
        group.add(rbPersonalizada);

        JTextField multaPersonalizada = new JTextField();
        multaPersonalizada.setEnabled(false);

        rbPersonalizada.addActionListener(_ -> multaPersonalizada.setEnabled(true));
        rbRecomendada.addActionListener(_ -> multaPersonalizada.setEnabled(false));

        panel.add(rbRecomendada); panel.add(rbPersonalizada);
        panel.add(new JLabel("Multa personalizada:")); panel.add(multaPersonalizada);

        // Mostrar ventana
        int result = JOptionPane.showConfirmDialog(null, panel, "Finalizar Préstamo", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            double multaElegida = multaTotal;
            if (rbPersonalizada.isSelected()) {
                String texto = multaPersonalizada.getText().replace(',', '.').replaceAll("[^0-9.\\-+()]", "");
                try {
                    multaElegida = Double.parseDouble(texto);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Multa personalizada inválida.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Segunda ventana resumen
            double totalPagar = valorBase + multaElegida;
            JPanel resumen = new JPanel(new GridLayout(0, 2, 5, 5));
            resumen.add(new JLabel("Multa seleccionada:")); resumen.add(new JLabel(String.format("$%.2f", multaElegida)));
            resumen.add(new JLabel("Valor del préstamo:")); resumen.add(new JLabel(String.format("$%.2f", valorBase)));
            resumen.add(new JLabel("Valor total a pagar:")); resumen.add(new JLabel(String.format("$%.2f", totalPagar)));
            resumen.add(new JLabel("Garantía:")); resumen.add(new JLabel(garantia));

            JOptionPane.showMessageDialog(null, resumen, "Resumen Final", JOptionPane.INFORMATION_MESSAGE);

            // Aquí actualizas el registro en el CSV:
            // - HORA FINAL: horaFinalStr
            // - DURACION: duracionStr
            // - V.FINAL: totalPagar
            // (El resto de columnas igual)
            // ...actualiza el CSV aquí...
        }
    }
}