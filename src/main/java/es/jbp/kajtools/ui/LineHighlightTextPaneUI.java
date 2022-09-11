package es.jbp.kajtools.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import javax.swing.JTextPane;
import javax.swing.plaf.basic.BasicTextPaneUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position.Bias;
import org.apache.commons.lang3.tuple.Pair;

class LineHighlightTextPaneUI extends BasicTextPaneUI {

  private final JTextPane textPane;
  private final Collection<Pair<Integer, Integer>> positions;

  public LineHighlightTextPaneUI(JTextPane textPane, Collection<Pair<Integer, Integer>> positions) {
    this.textPane = textPane;
    this.positions = positions;
  }

  @Override
  public void paintBackground(Graphics g) {

    super.paintBackground(g);

    positions.forEach(l -> highlightLine(g, l));
  }

  private void highlightLine(Graphics graphics, Pair<Integer, Integer> position) {

    try {
      Rectangle2D rect1 = modelToView2D(textPane, position.getLeft(), Bias.Forward);
      Rectangle2D rect2 = modelToView2D(textPane, position.getRight(), Bias.Forward);
      int y = rect1.getBounds().y;
      int height = rect2.getBounds().y - y;
      if (height == 0) {
        height = rect1.getBounds().height;
      }
      graphics.setColor(Color.DARK_GRAY);
      graphics.fillRect(0, y, textPane.getWidth(), height);
    } catch (BadLocationException ex) {
      ex.printStackTrace();
    }
  }
}
