package es.jbp.kajtools.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import es.jbp.kajtools.Environment;
import es.jbp.kajtools.KajException;
import es.jbp.kajtools.configuration.Configuration;
import es.jbp.kajtools.i18n.I18nService;
import es.jbp.kajtools.ksqldb.KSqlDbService;
import es.jbp.kajtools.ui.interfaces.InfoReportable;
import es.jbp.kajtools.util.ResourceUtil;
import lombok.Getter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class KSqlDbPanel extends BasePanel {

    private final KSqlDbService kSqlDbService;
    @Getter
    private JPanel contentPane;
    private JButton buttonExecute;
    private JTabbedPane tabbedPane;
    private JPanel tabInfo;
    private JPanel tabScript;
    @Getter
    private InfoTextPane infoTextPane;
    private JButton cleanButton;
    private JButton copyButton;
    @Getter
    private JTextField searchTextField;
    private RTextScrollPane scriptScrollPane;
    private JButton buttonStop;
    private JComboBox comboScript;
    private JButton buttonOpenFileScript;
    private JComboBox comboEnvironment;
    private RSyntaxTextArea scriptEditor;

    public KSqlDbPanel(KSqlDbService kSqlDbService,
                       UiComponentCreator componentFactory) {
        super(componentFactory);

        this.kSqlDbService = kSqlDbService;

        $$$setupUI$$$();

        super.initialize();

        // Combo Entorno
        Configuration.getEnvironmentList().forEach(comboEnvironment::addItem);

        buttonExecute.addActionListener(e -> asyncExecute());

        buttonStop.addActionListener(e -> stopAsyncTasks());

        cleanButton.addActionListener(e -> cleanEditor());
        copyButton.addActionListener(e -> copyToClipboard());

        enableTextSearch(searchTextField, scriptEditor);

        List<String> availableScripts = ResourceUtil.getResourceFileNames("")
                .stream()
                .filter(s -> s.toLowerCase().endsWith(".ksql"))
                .collect(Collectors.toList());
        comboScript.addItem("");
        availableScripts.forEach(comboScript::addItem);

        comboScript.addActionListener(e -> loadResourceForScript());
        buttonOpenFileScript.addActionListener(e -> openFileForScript());
    }

    protected Environment getEnvironment() {
        return (Environment) comboEnvironment.getSelectedItem();
    }

    private void openFileForScript() {
        File file = chooseAndReadFile();
        if (file != null) {
            loadTextFromFile(file, scriptEditor);
        }
    }

    private void asyncExecute() {
        var sentence = extractCaretSentence();
        if (sentence == null) {
            printMessage(InfoReportable.buildActionMessage("No hay sentencia para ejecutar"));
            return;
        }
        buttonExecute.setEnabled(false);
        buttonStop.setEnabled(true);
        printLink(InfoDocument.simpleDocument(extractReducedSentence(sentence), InfoDocument.Type.INFO, sentence));
        executeAsyncTask(() -> executeScript(sentence));
    }

    private String extractReducedSentence(String sentence) {
        var splitSentence = sentence.split("\n");
        return Arrays.stream(splitSentence)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .map(s -> s.length() > 100 ? s.substring(0, 100) + "..." : s)
                .orElse("No sentence");
    }

    private String extractCaretSentence() {
        String scriptCode = scriptEditor.getText();
        int position = scriptEditor.getCaretPosition();

        var sentences = scriptCode.split(";");

        int counter = 0;
        for (var sentence : sentences) {
            int length = sentence.length();
            if (position < counter + length + 1) {
                scriptEditor.select(counter, counter + length + 1);
                return scriptEditor.getSelectedText();
            }
            counter += length + 1;
        }

        return null;
    }

    private void loadResourceForScript() {
        String path = Optional.ofNullable(comboScript.getSelectedItem()).map(Object::toString).orElse("");
        loadTextFromResource(path, scriptEditor);
    }

    private Void executeScript(String scriptCode) {

        try {
            var result = kSqlDbService.executeScript(scriptCode, getEnvironment());

            if (result.isOk()) {
                enqueueMessage(InfoReportable.buildSuccessfulMessage("Ejecución correcta"));
            } else {
                enqueueMessage(InfoReportable.buildErrorMessage("Ejecución con errores"));
            }
            enqueueLink(InfoDocument.simpleDocument("Response", InfoDocument.Type.JSON, result.getResponse()));
        } catch (KajException e) {
            enqueueException(e);
        }

        SwingUtilities.invokeLater(() -> {
            buttonStop.setEnabled(false);
        });
        return null;
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(5, 2, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        buttonExecute = new JButton();
        buttonExecute.setIcon(new ImageIcon(getClass().getResource("/images/execute.png")));
        this.$$$loadButtonText$$$(buttonExecute, this.$$$getMessageFromBundle$$$("messages", "button.execute"));
        panel1.add(buttonExecute, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 2, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        buttonStop = new JButton();
        buttonStop.setEnabled(false);
        buttonStop.setIcon(new ImageIcon(getClass().getResource("/images/stop.png")));
        buttonStop.setText("");
        buttonStop.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.stop.script"));
        panel1.add(buttonStop, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel2, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(615, 40), null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("messages", "label.script"));
        panel2.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboScript = new JComboBox();
        panel2.add(comboScript, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonOpenFileScript = new JButton();
        Font buttonOpenFileScriptFont = this.$$$getFont$$$(null, -1, -1, buttonOpenFileScript.getFont());
        if (buttonOpenFileScriptFont != null) buttonOpenFileScript.setFont(buttonOpenFileScriptFont);
        buttonOpenFileScript.setIcon(new ImageIcon(getClass().getResource("/images/folder.png")));
        buttonOpenFileScript.setText("");
        buttonOpenFileScript.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.open.script.file"));
        panel2.add(buttonOpenFileScript, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(24, 24), new Dimension(24, 24), new Dimension(24, 24), 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Environment:");
        panel2.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboEnvironment = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        comboEnvironment.setModel(defaultComboBoxModel1);
        panel2.add(comboEnvironment, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        contentPane.add(panel3, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setIcon(new ImageIcon(getClass().getResource("/images/search.png")));
        label3.setText("");
        panel3.add(label3, BorderLayout.WEST);
        searchTextField = new JTextField();
        searchTextField.setText("");
        panel3.add(searchTextField, BorderLayout.CENTER);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        contentPane.add(panel4, new GridConstraints(2, 0, 2, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        tabbedPane = new JTabbedPane();
        panel4.add(tabbedPane, BorderLayout.CENTER);
        tabInfo = new JPanel();
        tabInfo.setLayout(new BorderLayout(0, 0));
        tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "tab.info"), tabInfo);
        final JScrollPane scrollPane1 = new JScrollPane();
        tabInfo.add(scrollPane1, BorderLayout.CENTER);
        infoTextPane = new InfoTextPane();
        infoTextPane.setBackground(new Color(-13948117));
        infoTextPane.setCaretColor(new Color(-1));
        infoTextPane.setEditable(false);
        infoTextPane.setEnabled(true);
        Font infoTextPaneFont = this.$$$getFont$$$(null, -1, -1, infoTextPane.getFont());
        if (infoTextPaneFont != null) infoTextPane.setFont(infoTextPaneFont);
        infoTextPane.setForeground(new Color(-1));
        scrollPane1.setViewportView(infoTextPane);
        tabScript = new JPanel();
        tabScript.setLayout(new BorderLayout(0, 0));
        tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "tab.script"), tabScript);
        tabScript.add(scriptScrollPane, BorderLayout.CENTER);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel5, BorderLayout.EAST);
        cleanButton = new JButton();
        cleanButton.setIcon(new ImageIcon(getClass().getResource("/images/rubber.png")));
        cleanButton.setText("");
        cleanButton.setToolTipText("Limpiar");
        panel5.add(cleanButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        copyButton = new JButton();
        copyButton.setIcon(new ImageIcon(getClass().getResource("/images/copy.png")));
        copyButton.setText("");
        copyButton.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.copy.clipboard"));
        panel5.add(copyButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel5.add(spacer2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel5.add(spacer3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 30), null, 0, false));
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

    private static Method $$$cachedGetBundleMethod$$$ = null;

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    private void createUIComponents() {
        scriptEditor = createSqlEditor();
        scriptScrollPane = componentFactory.createEditorScroll(scriptEditor);
    }

    @Override
    public Optional<JTextComponent> getCurrentEditor() {
        int index = tabbedPane.getSelectedIndex();
        return getUmpteenthEditor(index, infoTextPane, scriptEditor);
    }

    public InfoTextPane getInfoTextPane() {
        return (InfoTextPane) infoTextPane;
    }

    @Override
    protected void asyncTaskFinished() {
        super.asyncTaskFinished();
        buttonExecute.setEnabled(true);
        buttonStop.setEnabled(false);
    }

}

