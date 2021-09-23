package es.jbp.kajtools.ui.interfaces;

import java.awt.Window;
import javax.swing.JDialog;
import javax.swing.JPanel;

public interface DialogueablePanel {
  JPanel getMainPanel();
  void bindDialog(Window dialog);
}
