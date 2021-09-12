package es.jbp.kajtools.ui.interfaces;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Optional;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

public interface SearchablePanel {

  JTextField getSearchTextField();

  Optional<JTextComponent> getCurrentEditor();

  default void enableTextSearch(JTextField searchTextField, JTextComponent... editors) {
    searchTextField.addActionListener(e -> {
      findText(true, true);
      getCurrentEditor().ifPresent(JTextComponent::grabFocus);
    });
    searchTextField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        findText(true, true);
      }
    });
    Arrays.stream(editors).forEach(editor -> editor.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        super.keyPressed(e);
        if (e.getKeyCode() == KeyEvent.VK_F3) {
          if (e.isControlDown()) {
            searchTextField.setText(editor.getSelectedText());
          }
          findText(!e.isShiftDown(), false);
        }
      }
    }));
  }

  default void findText(boolean forward, boolean retryOppositeDirection) {
    String text = getSearchTextField().getText();
    boolean wasFound = findText(forward, text);
    if (!wasFound && retryOppositeDirection) {
      findText(!forward, text);
    }
  }

  default boolean findText(boolean forward, String text) {
    Optional<JTextComponent> editor = getCurrentEditor();
    if (!editor.isPresent() || !(editor.get() instanceof RSyntaxTextArea)) {
      return false;
    }
    SearchContext context = new SearchContext();
    context.setSearchFor(text);
    context.setMatchCase(false);
    context.setRegularExpression(false);
    context.setSearchForward(forward);
    context.setWholeWord(false);
    SearchResult result = SearchEngine.find((RSyntaxTextArea) editor.get(), context);
    return result.wasFound();
  }

}
