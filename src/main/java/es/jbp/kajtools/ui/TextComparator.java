package es.jbp.kajtools.ui;

import es.jbp.kajtools.ui.InfoDocument.InfoDocumentBuilder;
import es.jbp.kajtools.ui.InfoMessage.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;

public class TextComparator {

  public InfoDocument compare(String leftTitle, String textLeft, String rightTitle, String textRight) {

    List<InfoMessage> result = new ArrayList<>();

    result.add(new InfoMessage(" ", Type.ADDED));
    result.add(new InfoMessage(leftTitle + "\n", Type.TRACE));
    result.add(new InfoMessage(" ", Type.DELETED));
    result.add(new InfoMessage(rightTitle + "\n", Type.TRACE));

    diff_match_patch difference = new diff_match_patch();
    LinkedList<Diff> deltas = difference.diff_main(textLeft, textRight);


    for (Diff delta : deltas) {
      switch (delta.operation) {
        case EQUAL:
          result.add(new InfoMessage(delta.text, Type.TRACE));

          break;
        case INSERT:
          result.add(new InfoMessage(delta.text, Type.ADDED));
          break;
        case DELETE:
          result.add(InfoMessage.builder().mensaje(delta.text).type(Type.DELETED).build());
          break;
      }
    }
    InfoDocumentBuilder infoDocumentBuilder = InfoDocument.builder();
    result.forEach(infoDocumentBuilder::message);
    return infoDocumentBuilder.build();
  }
}
