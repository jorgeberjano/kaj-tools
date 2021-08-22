package es.jbp.kajtools;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;
import es.jbp.kajtools.ui.KafkaProducerPanel;
import es.jbp.kajtools.ui.MainForm;
import es.jbp.kajtools.util.SchemaRegistryService;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.UIManager;
import lombok.Getter;
import org.apache.avro.generic.GenericRecord;
import org.fife.ui.rsyntaxtextarea.Theme;

public class KajToolsApp {

    @Getter
    private static KajToolsApp instance;
    @Getter

    private final List<IMessageClient> clientList;
    @Getter
    private final SchemaRegistryService schemaRegistryService;
    @Getter
    private Theme theme;

    public KajToolsApp(List<IMessageClient> clientList,
        SchemaRegistryService schemaRegistryService) {
        this.instance  = this;
        this.clientList = clientList;
        this.schemaRegistryService = schemaRegistryService;
    }

    public void showWindow(String[] args) {

        boolean dark = args.length > 0 && "dark".equals(args[0]);
        try {
            if (dark) {
                UIManager.setLookAndFeel(new FlatDarculaLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }
        UIManager.put("OptionPane.yesButtonText", "Sí");

        if (dark) {
            InputStream in = KajToolsApp.class.getResourceAsStream("/dark.xml");
            try {
                theme = Theme.load(in);
            } catch (IOException ioe) {
            }
        }

        ImageIcon icono = null;
        try {
            URL url = KafkaProducerPanel.class.getResource("/images/icon.png");
            if (url != null) {
                icono = new ImageIcon(ImageIO.read(url));
            }
        } catch (IOException e) {
        }

        JFrame frame = new JFrame("MainForm");
        frame.setTitle("KAJ Tools");
        if (icono != null) {
            frame.setIconImage(icono.getImage());
        }
        frame.setContentPane(new MainForm().getContentPane());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setSize(1000, 600);
        frame.setVisible(true);
    }
}
