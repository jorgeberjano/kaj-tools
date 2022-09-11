package es.jbp.kajtools.ui;

import es.jbp.kajtools.ui.InfoDocument.InfoDocumentBuilder;
import es.jbp.kajtools.ui.InfoMessage.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import org.apache.commons.lang3.StringUtils;

public class TextComparator {

  public InfoDocument compare(String leftTitle, String leftText, String rightTitle, String rightText) {

    InfoDocumentBuilder infoDocumentBuilder = InfoDocument.builder().type(InfoDocument.Type.DIFF).title("diferencias");

    infoDocumentBuilder.left(new InfoMessage(" ", Type.ADDED));
    infoDocumentBuilder.left(new InfoMessage(leftTitle + "\n", Type.TRACE));
    infoDocumentBuilder.right(new InfoMessage(" ", Type.DELETED));
    infoDocumentBuilder.right(new InfoMessage(rightTitle + "\n", Type.TRACE));

    diff_match_patch difference = new diff_match_patch();
    LinkedList<Diff> deltas = difference.diff_main(rightText, leftText);

    int lines;
    for (Diff delta : deltas) {
      switch (delta.operation) {
        case EQUAL:
          infoDocumentBuilder.left(new InfoMessage(delta.text, Type.TRACE));
          infoDocumentBuilder.right(new InfoMessage(delta.text, Type.TRACE));
          break;
        case INSERT:
          infoDocumentBuilder.left(new InfoMessage(delta.text, Type.ADDED));
          lines = StringUtils.countMatches(delta.text, '\n');
          if (lines > 0) {
            infoDocumentBuilder.right(new InfoMessage(StringUtils.repeat("\n", lines), Type.MISSING));
          }
          break;
        case DELETE:
          infoDocumentBuilder.right(new InfoMessage(delta.text, Type.DELETED));
          lines = StringUtils.countMatches(delta.text, '\n');
          if (lines > 0) {
            infoDocumentBuilder.left(new InfoMessage(StringUtils.repeat("\n", lines), Type.MISSING));
          }
          break;
      }
    }
    return infoDocumentBuilder.build();
  }
}
