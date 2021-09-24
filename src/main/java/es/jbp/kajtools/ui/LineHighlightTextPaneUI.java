package es.jbp.kajtools.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import javax.swing.JTextPane;
import javax.swing.plaf.basic.BasicTextPaneUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position.Bias;
import org.apache.commons.lang3.tuple.Pair;

class LineHighlightTextPaneUI extends BasicTextPaneUI {

  private final JTextPane textPane;
  private final Collection<Pair<Integer, Integer>> lines;

  public LineHighlightTextPaneUI(JTextPane textPane, Collection<Pair<Integer, Integer>> lines) {
    this.textPane = textPane;
    this.lines = lines;
  }

  @Override
  public void paintBackground(Graphics g) {

    super.paintBackground(g);

    lines.forEach(l -> highlightLine(g, l));
  }

  private void highlightLine(Graphics g, Pair<Integer, Integer> position) {

    try {
      Rectangle2D rect1 = modelToView2D(textPane, position.getLeft(), Bias.Forward);
      Rectangle2D rect2 = modelToView2D(textPane, position.getRight(), Bias.Forward);
      int y = rect1.getBounds().y;
      int h = rect2.getBounds().y - y;// + rect2.getBounds().height;
      g.setColor(Color.DARK_GRAY);
      g.fillRect(0, y, textPane.getWidth(), h);
    } catch (BadLocationException ex) {
      ex.printStackTrace();
    }
  }
}
