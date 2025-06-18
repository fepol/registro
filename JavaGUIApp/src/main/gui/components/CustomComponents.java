// This file contains custom GUI components that can be reused throughout the application.

package gui.components;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;

public class CustomComponents {
    public static JPanel createTitledPanel(String title, JTable table, Rectangle bounds) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK), 
            title, 
            TitledBorder.CENTER, 
            TitledBorder.TOP
        ));
        panel.setBounds(bounds);
        
        // Centrar texto en todas las columnas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        // Ajustar ancho de columnas
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            column.setPreferredWidth(100);  // ancho predeterminado
        }
        
        panel.add(new JScrollPane(table));
        return panel;
    }

    public static void setUpperCaseFilter(JTextField textField) {
        ((AbstractDocument) textField.getDocument()).setDocumentFilter(
            new DocumentFilter() {
                @Override
                public void replace(FilterBypass fb, int offset, int length, 
                                 String text, AttributeSet attrs)
                        throws BadLocationException {
                    fb.replace(offset, length, text.toUpperCase(), attrs);
                }
            }
        );
    }
}