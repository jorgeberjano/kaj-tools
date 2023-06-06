package es.jbp.kajtools;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;
import es.jbp.kajtools.ui.KafkaProducerPanel;
import es.jbp.kajtools.ui.MainForm;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

@Slf4j
public class KajToolsApp {

    private JFrame mainFrame;

    @Getter
    private final MainForm mainForm;

    public KajToolsApp(MainForm mainForm) {
        this.mainForm = mainForm;
    }

    static public void initializeLookAndFeel() {
        try {
            UIManager.setLookAndFeel(lookAndFeel());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }
        Locale.setDefault(Locale.ENGLISH);
        UIManager.put("OptionPane.yesButtonText", "Sí");
    }

    public void createMainFrame() {

        ImageIcon icono = null;
        try {
            URL url = KafkaProducerPanel.class.getResource("/images/icon.png");
            if (url != null) {
                icono = new ImageIcon(ImageIO.read(url));
            }
        } catch (IOException e) {
            log.error("No de ha podido cargar el icono de la aplicación");
        }

        mainFrame = new JFrame("MainForm");
        mainFrame.setTitle("KAJ Tools");
        if (icono != null) {
            mainFrame.setIconImage(icono.getImage());
        }
        mainFrame.setContentPane(mainForm.getContentPane());
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setSize(1000, 600);
    }

    private static LookAndFeel lookAndFeel() {
        boolean light = es.jbp.kajtools.configuration.Configuration.isLightTheme();
        try {
            return light ? new FlatLightLaf() : new FlatDarculaLaf();
        } catch (Exception ex) {
            log.error("Failed to initialize FlatLaf");
            return null;
        }
    }

    public void addPanel(String title, String iconResourceName, JPanel panel) {
        mainForm.addPanel(title, iconResourceName, panel);
    }

    public void showWindow(String title) {

        if (title != null) {
            mainFrame.setTitle(title);
        }
        mainFrame.setVisible(true);
    }
}
