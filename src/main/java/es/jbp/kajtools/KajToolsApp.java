package es.jbp.kajtools;

import javax.swing.JFrame;

public class KajToolsApp {

  private final JFrame mainFrame;

  public KajToolsApp(JFrame mainFrame) {
    this.mainFrame = mainFrame;
  }

  public void showWindow(String title) {

    if (title != null) {
      mainFrame.setTitle(title);
    }
    mainFrame.setVisible(true);
  }
}
