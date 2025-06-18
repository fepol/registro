package gui.components;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

public class TableUtils {
    public static void autoResizeColumns(JTable table) {
        TableModel model = table.getModel();
        TableColumnModel columnModel = table.getColumnModel();
        for (int col = 0; col < columnModel.getColumnCount(); col++) {
            int maxWidth = 50; // Mínimo
            TableColumn column = columnModel.getColumn(col);
            // Encabezado
            TableCellRenderer headerRenderer = column.getHeaderRenderer();
            if (headerRenderer == null) headerRenderer = table.getTableHeader().getDefaultRenderer();
            Component headerComp = headerRenderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, 0, col);
            maxWidth = Math.max(maxWidth, headerComp.getPreferredSize().width + 10);
            // Filas
            for (int row = 0; row < model.getRowCount(); row++) {
                TableCellRenderer cellRenderer = table.getCellRenderer(row, col);
                Object value = model.getValueAt(row, col);
                Component comp = cellRenderer.getTableCellRendererComponent(table, value, false, false, row, col);
                maxWidth = Math.max(maxWidth, comp.getPreferredSize().width + 10);
            }
            column.setPreferredWidth(maxWidth);
        }
    }
}
