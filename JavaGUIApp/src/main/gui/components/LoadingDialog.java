package gui.components;

import javax.swing.*;
import java.awt.*;

public class LoadingDialog extends JDialog {
    public LoadingDialog(Frame parent, String message) {
        super(parent, true);
        setUndecorated(true);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        JLabel label = new JLabel(message);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(bar);
        setContentPane(panel);
        pack();
        setLocationRelativeTo(parent);
    }
    public static void showLoading(Frame parent, String message, Runnable after) {
        LoadingDialog dialog = new LoadingDialog(parent, message);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                Thread.sleep(1000);
                return null;
            }
            @Override
            protected void done() {
                dialog.dispose();
                if (after != null) after.run();
            }
        };
        worker.execute();
        dialog.setVisible(true);
    }
}
