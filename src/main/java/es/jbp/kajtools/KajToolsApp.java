package es.jbp.kajtools;

import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

public class KajToolsApp {

  private final JFrame mainFrame;
  private LookAndFeel lookAndField;

  public KajToolsApp(JFrame mainFrame,
      LookAndFeel lookAndField) {
    this.mainFrame = mainFrame;
    this.lookAndField = lookAndField;
  }

  public void showWindow(String title) {

    if (title != null) {
      mainFrame.setTitle(title);
    }
    mainFrame.setVisible(true);
  }
}
