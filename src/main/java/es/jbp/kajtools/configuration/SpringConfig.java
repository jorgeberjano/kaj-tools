package es.jbp.kajtools.configuration;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;
import es.jbp.kajtools.IMessageClient;
import es.jbp.kajtools.KajToolsApp;
import es.jbp.kajtools.ui.ComponentFactory;
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
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringConfig {
  @Autowired
  private ComponentFactory componentFactory;

  @Autowired
  private SchemaRegistryService schemaRegistryService;

  @Autowired
  private List<IMessageClient> clientList;

  @Bean
  public JFrame mainFrame() {

    try {
      UIManager.setLookAndFeel(lookAndFeel());
    } catch (Exception ex) {
      System.err.println("Failed to initialize FlatLaf");
    }
    UIManager.put("OptionPane.yesButtonText", "Sí");

    ImageIcon icono = null;
    try {
      URL url = KafkaProducerPanel.class.getResource("/images/icon.png");
      if (url != null) {
        icono = new ImageIcon(ImageIO.read(url));
      }
    } catch (IOException e) {
      System.err.println("No de ha podido cargar el icono de la aplicación");
    }

    JFrame frame = new JFrame("MainForm");
    frame.setTitle("KAJ Tools");
    if (icono != null) {
      frame.setIconImage(icono.getImage());
    }
    frame.setContentPane(mainForm().getContentPane());
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setSize(1000, 600);
    return frame;
  }

  @Bean MainForm mainForm() {
    return new MainForm(componentFactory, schemaRegistryService, clientList);
  }

  @Bean
  public LookAndFeel lookAndFeel() {
    boolean light = es.jbp.kajtools.configuration.Configuration.isLightTheme();
    try {
      return light ? new FlatLightLaf() : new FlatDarculaLaf();
    } catch (Exception ex) {
      System.err.println("Failed to initialize FlatLaf");
      return null;
    }
  }

  @Bean
  public Theme theme() {
    if (!es.jbp.kajtools.configuration.Configuration.isLightTheme()) {
      InputStream in = KajToolsApp.class.getResourceAsStream("/dark.xml");
      try {
        return Theme.load(in);
      } catch (IOException ioe) {
        System.err.println("No de ha podido cargar el tema dark");
      }
    }
    return null;
  }
}
