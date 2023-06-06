package es.jbp.kajtools.configuration;

import es.jbp.kajtools.IMessageClient;
import es.jbp.kajtools.KajToolsApp;
import es.jbp.kajtools.i18n.I18nService;
import es.jbp.kajtools.kafka.KafkaAdminService;
import es.jbp.kajtools.ksqldb.KSqlDbService;
import es.jbp.kajtools.schemaregistry.SchemaRegistryService;
import es.jbp.kajtools.ui.MainForm;
import es.jbp.kajtools.ui.UiComponentCreator;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Configuration
public class SpringConfig {

  @Autowired
  private SchemaRegistryService schemaRegistryService;

  @Autowired
  private KafkaAdminService kafkaAdminService;

  @Autowired
  private List<IMessageClient> clientList;

  @Autowired
  private I18nService i18nService;

  @Autowired
  private KSqlDbService kSqlDbService;

  @Bean
  public MainForm mainForm() {
    return new MainForm(new UiComponentCreator(theme()),
            schemaRegistryService,
            kafkaAdminService,
            clientList,
            i18nService,
            kSqlDbService);
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

//  @Bean
//  public JFrame mainFrame() {
//
//    try {
//      UIManager.setLookAndFeel(lookAndFeel());
//    } catch (Exception ex) {
//      System.err.println("Failed to initialize FlatLaf");
//    }
//    Locale.setDefault(Locale.ENGLISH);
//    UIManager.put("OptionPane.yesButtonText", "Sí");
//
//    ImageIcon icono = null;
//    try {
//      URL url = KafkaProducerPanel.class.getResource("/images/icon.png");
//      if (url != null) {
//        icono = new ImageIcon(ImageIO.read(url));
//      }
//    } catch (IOException e) {
//      System.err.println("No de ha podido cargar el icono de la aplicación");
//    }
//
//    JFrame frame = new JFrame("MainForm");
//    frame.setTitle("KAJ Tools");
//    if (icono != null) {
//      frame.setIconImage(icono.getImage());
//    }
//    frame.setContentPane(mainForm().getContentPane());
//    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//    frame.pack();
//    frame.setLocationRelativeTo(null);
//    frame.setSize(1000, 600);
//    return frame;
//  }


}
