package es.jbp.kajtools.ui.interfaces;

import javax.swing.JDialog;
import javax.swing.JPanel;

public interface DialogueablePanel {
  JPanel getMainPanel();
  void bindDialog(JDialog dialog);
}
