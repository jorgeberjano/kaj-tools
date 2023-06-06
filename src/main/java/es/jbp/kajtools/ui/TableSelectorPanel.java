package es.jbp.kajtools.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import es.jbp.kajtools.ui.interfaces.DialogueablePanel;
import es.jbp.tabla.ModeloTablaGenerico;
import es.jbp.tabla.TablaGenerica;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import lombok.Getter;

public class TableSelectorPanel<T> implements DialogueablePanel {

    private JTable table;
    @Getter
    private JPanel mainPanel;
    private JTextField textFieldFilter;
    private JButton buttonOk;
    private JButton buttonUpdate;

    private boolean okButtonPressed;

    public JTable getTable() {
        return table;
    }

    public interface ModelProvider<T> {

        ModeloTablaGenerico<T> getModel(boolean update);
    }

    private ModeloTablaGenerico<T> tableModel;
    private ModelProvider<T> modelProvider;

    public TableSelectorPanel(
            ModelProvider<T> modelProvider) {
        this.modelProvider = modelProvider;
        tableModel = modelProvider.getModel(false);

        $$$setupUI$$$();

        textFieldFilter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });

        buttonUpdate.addActionListener(e -> updateModel());
    }

    private void updateModel() {
        tableModel = modelProvider.getModel(true);
        table.setModel(tableModel);
    }

    private void applyFilter() {
        String filterText = textFieldFilter.getText().toLowerCase();

        List<String> filterList = Arrays.asList(filterText.split("\\s"));

        tableModel.filtrarPorPredicado(e -> containsAll(e.toString().toLowerCase(), filterList));
    }

    private boolean containsAll(String s, List<String> filterList) {
        return filterList.stream().allMatch(s::contains);
    }

    public void bindDialog(Window dialog) {
        mainPanel.getRootPane().setDefaultButton(buttonOk);
        buttonOk.addActionListener(e -> {
            okButtonPressed = true;
            dialog.setVisible(false);
            dialog.dispose();
        });
    }


    private void createUIComponents() {
        table = new TablaGenerica();
        table.setModel(tableModel);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(3, 2, new Insets(8, 8, 8, 8), -1, -1));
        final JScrollPane scrollPane1 = new JScrollPane();
        mainPanel.add(scrollPane1, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane1.setViewportView(table);
        textFieldFilter = new JTextField();
        mainPanel.add(textFieldFilter, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonOk = new JButton();
        buttonOk.setText("Aceptar");
        mainPanel.add(buttonOk, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonUpdate = new JButton();
        Font buttonUpdateFont = this.$$$getFont$$$(null, -1, -1, buttonUpdate.getFont());
        if (buttonUpdateFont != null) buttonUpdate.setFont(buttonUpdateFont);
        buttonUpdate.setIcon(new ImageIcon(getClass().getResource("/images/update.png")));
        buttonUpdate.setText("");
        buttonUpdate.setToolTipText("Actualizar");
        mainPanel.add(buttonUpdate, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(24, 24), new Dimension(24, 24), new Dimension(24, 24), 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }


    public T getAcceptedItem() {
        if (!okButtonPressed) {
            return null;
        }
        return getSelectedItem();
    }

    public T getSelectedItem() {
        int index = table.getSelectedRow();
        return tableModel.getFila(index);
    }

    public void setTablePopupMenu(JPopupMenu popupMenu) {
        table.setComponentPopupMenu(popupMenu);
    }
}
