package es.jbp.kajtools.ui;

import es.jbp.kajtools.ui.InfoDocument.InfoDocumentBuilder;
import es.jbp.kajtools.ui.InfoMessage.Type;
import org.apache.commons.lang3.StringUtils;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;

import java.util.LinkedList;

public class TextComparator {

  public InfoDocument compare(String leftTitle, String leftText, String rightTitle, String rightText) {

    InfoDocumentBuilder infoDocumentBuilder = InfoDocument.builder().type(InfoDocument.Type.DIFF).title("diferencias");

    infoDocumentBuilder.left(new InfoMessage(" ", Type.ADDED));
    infoDocumentBuilder.left(new InfoMessage(leftTitle + "\n", Type.TRACE));
    infoDocumentBuilder.right(new InfoMessage(" ", Type.DELETED));
    infoDocumentBuilder.right(new InfoMessage(rightTitle + "\n", Type.TRACE));



    var difference = new DiffMatchPatch();
    LinkedList<DiffMatchPatch.Diff> deltas = difference.diffMain(rightText, leftText);

    int lines;
    for (var delta : deltas) {
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
