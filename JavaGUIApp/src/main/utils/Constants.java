package utils;
import java.util.HashMap;
import java.util.Map;

public class Constants {
    // Window configuration
    public static final String APP_TITLE = "Registro FEPOL";
    public static final int WINDOW_WIDTH = 1500;
    public static final int WINDOW_HEIGHT = 750;
    
    // Table columns
    public static final String[] PRESTAMOS_COLUMNS = {
        "NOMBRE", "PRESTAMO", "HORA INICIAL", "HORA FINAL", "DURACION", "GARANTIA", "V.FINAL", "FECHA"
    };
    
    public static final String[] ATRASOS_COLUMNS = {
        "NOMBRE", "PRESTAMO", "HORA INICIAL", "HORA FINAL", "DURACION", "GARANTIA", "V.FINAL", "FECHA"
    };
    
    public static final String[] VENTAS_COLUMNS = {
        "PRODUCTO", "HORA", "CANTIDAD", "PRECIO UNIT.", "TOTAL", "FECHA"
    };

    // Productos y precios
    public static final String[] PRODUCTOS_VENTA = {"MASCARILLAS", "GUANTES", "CURITAS"};
    public static final Map<String, Double> PRECIOS_PRODUCTOS = new HashMap<>() {{
        put("MASCARILLAS", 0.25);
        put("GUANTES", 0.25);
        put("CURITAS", 0.10);
    }};

    // File names
    public static final String PRESTAMOS_FILE = "prestamos.csv";
    public static final String VENTAS_FILE = "ventas.csv";
    
    // Préstamos options
    public static final String[] PRESTAMO_OPTIONS = {"MANDIL", "GAFAS", "AMBOS", "CALCULADORA"};
    public static final String[] GARANTIA_OPTIONS = {"CÉDULA", "OBJETO DE VALOR"};
}