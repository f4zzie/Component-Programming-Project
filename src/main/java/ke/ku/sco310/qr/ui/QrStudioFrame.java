package ke.ku.sco310.qr.ui;

import ke.ku.sco310.qr.api.BoundPropertySource;
import ke.ku.sco310.qr.api.QrCode;
import ke.ku.sco310.qr.api.QrCodeRenderer;
import ke.ku.sco310.qr.api.QrErrorCorrection;
import ke.ku.sco310.qr.api.QrGenerationResult;
import ke.ku.sco310.qr.api.RenderedArtifact;
import ke.ku.sco310.qr.editor.HexColorPropertyEditor;
import ke.ku.sco310.qr.facade.QrAssembly;
import ke.ku.sco310.qr.render.AsciiRendererBean;
import ke.ku.sco310.qr.render.PngRendererBean;
import ke.ku.sco310.qr.render.SvgRendererBean;
import ke.ku.sco310.qr.store.QrHistoryBean;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Swing assembly of the component suite (front end number one).
 *
 * <p>The frame owns no domain logic. It holds an {@link QrAssembly}, forwards user gestures to the
 * facade component and reacts to the components' <em>bound properties</em> and <em>events</em>:</p>
 * <ul>
 *   <li>the module-size spinner writes {@code PngRendererBean.moduleSize}; the change fires a
 *       {@code PropertyChangeEvent} that re-renders the preview;</li>
 *   <li>the history list is refreshed from the {@code QrHistoryBean} indexed property events;</li>
 *   <li>the audit listener prints to the status area through the generation event.</li>
 * </ul>
 *
 * <p>This is the visual-assembly part of chapter 3 ("Connection model" / "Techniques of COP with
 * JavaBeans") implemented with plain Swing instead of the original BeanBox.</p>
 */
public class QrStudioFrame extends JFrame implements PropertyChangeListener {

    private static final long serialVersionUID = 1L;

    private final transient QrAssembly assembly;

    private final JTextArea inputArea = new JTextArea("https://www.ku.ac.ke/sco310\nKenyatta University - Component Programming", 4, 34);
    private final JComboBox<QrErrorCorrection> levelCombo = new JComboBox<>(QrErrorCorrection.values());
    private final JComboBox<String> rendererCombo = new JComboBox<>(
            new DefaultComboBoxModel<>(new String[]{"PNG", "SVG", "ASCII"}));
    private final JSpinner moduleSizeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 64, 1));
    private final JTextField foregroundField = new JTextField("#101820", 8);
    private final JTextField backgroundField = new JTextField("#FFFFFF", 8);
    private final FitLabel previewLabel = new FitLabel();
    private final JTextArea previewText = new JTextArea(24, 60);
    private final CardLayout previewCards = new CardLayout();
    private final JPanel previewDeck = new JPanel(previewCards);
    private final JTextArea historyArea = new JTextArea(14, 60);
    private final JLabel statusLabel = new JLabel("ready");
    private final JLabel symbolLabel = new JLabel("-");
    private QrCode lastSymbol;
    private JSplitPane mainSplit;

    public QrStudioFrame(QrAssembly assembly) {
        super("SCO310 - QR Code Generator (component assembly)");
        this.assembly = assembly;
        // The combo box starts on the error-correction level the facade component was configured with.
        levelCombo.setSelectedItem(assembly.getFacade().getDefaultErrorCorrection());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildLayout();
        wireComponents();
        setSize(1220, 780);
        if (mainSplit != null) {
            mainSplit.setDividerLocation(590);   // property sheet on the left, artifact on the right
        }
    }

    private void buildLayout() {
        JPanel controls = new JPanel(new GridBagLayout());
        controls.setBorder(BorderFactory.createTitledBorder("Component properties"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 6, 3, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0;
        controls.add(new JLabel("Payload"), c);
        c.gridx = 1; c.weightx = 1; c.gridwidth = 3; c.fill = GridBagConstraints.BOTH;
        JScrollPane payloadScroll = new JScrollPane(inputArea);
        payloadScroll.setPreferredSize(new Dimension(430, 74));
        payloadScroll.setMinimumSize(new Dimension(430, 74));
        controls.add(payloadScroll, c);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridwidth = 1; c.weightx = 0; c.gridy = 1; c.gridx = 0;
        controls.add(new JLabel("Error correction"), c);
        c.gridx = 1; controls.add(levelCombo, c);
        c.gridx = 2; controls.add(new JLabel("Renderer component"), c);
        c.gridx = 3; controls.add(rendererCombo, c);

        c.gridy = 2; c.gridx = 0;
        controls.add(new JLabel("Module size (px)"), c);
        c.gridx = 1; controls.add(moduleSizeSpinner, c);
        c.gridx = 2; controls.add(new JLabel("Foreground / background"), c);
        JPanel colours = new JPanel(new GridLayout(1, 2, 4, 0));
        colours.add(foregroundField);
        colours.add(backgroundField);
        colours.setPreferredSize(new Dimension(180, 22));
        colours.setMinimumSize(new Dimension(180, 22));
        c.gridx = 3; controls.add(colours, c);

        JButton generate = new JButton("Generate");
        generate.addActionListener(e -> generate());
        JButton saveAs = new JButton("Save artifact as...");
        saveAs.addActionListener(e -> saveAs());
        JButton clearHistory = new JButton("Clear history");
        clearHistory.addActionListener(e -> assembly.getHistory().clear());
        JPanel buttons = new JPanel();
        buttons.add(generate);
        buttons.add(saveAs);
        buttons.add(clearHistory);
        c.gridy = 3; c.gridx = 0; c.gridwidth = 4;
        controls.add(buttons, c);

        previewText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        previewText.setEditable(false);
        historyArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        historyArea.setEditable(false);
        historyArea.setBorder(BorderFactory.createTitledBorder("History component (indexed property)"));

        JPanel preview = new JPanel(new BorderLayout());
        preview.setBorder(BorderFactory.createTitledBorder("Rendered artifact"));
        // A raster artifact is shown in the image card, a textual artifact (SVG source or ASCII
        // art) in the text card; exactly one of them is visible at a time.
        previewDeck.add(previewLabel, "raster");
        previewDeck.add(new JScrollPane(previewText), "text");
        preview.add(previewDeck, BorderLayout.CENTER);

        // The symbol identity sits on the WEST edge so that it stays visible even when the status
        // message is long enough to claim all the remaining width.
        symbolLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 10));
        symbolLabel.setFont(symbolLabel.getFont().deriveFont(Font.BOLD));
        JPanel status = new JPanel(new BorderLayout());
        status.add(symbolLabel, BorderLayout.WEST);
        status.add(statusLabel, BorderLayout.CENTER);

        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, preview, historyArea);
        vertical.setResizeWeight(0.72);
        // GridBagLayout centres its grid when the pane is taller than the content, which would
        // leave the property sheet floating in the middle of the window; hosting it in the NORTH
        // slot of a BorderLayout keeps it anchored under the titled border.
        JPanel controlsHost = new JPanel(new BorderLayout());
        controlsHost.add(controls, BorderLayout.NORTH);

        JSplitPane horizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlsHost, vertical);
        horizontal.setResizeWeight(0.0);
        mainSplit = horizontal;

        setLayout(new BorderLayout());
        add(horizontal, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    /** Connects this frame to the components as a listener of their properties and events. */
    private void wireComponents() {
        subscribe(rendererFor("PNG"));
        subscribe(assembly.getRenderer());
        assembly.getHistory().addPropertyChangeListener(this);
        assembly.getListener().addPropertyChangeListener(this);
        moduleSizeSpinner.addChangeListener(e -> applyModuleSize());
        rendererCombo.addActionListener(e -> applyRendererSelection());
        inputArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { schedulePreview(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { schedulePreview(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { schedulePreview(); }
        });
    }

    /** Subscribes to a component's bound properties through the smallest interface it publishes. */
    private void subscribe(QrCodeRenderer renderer) {
        if (renderer instanceof BoundPropertySource) {
            ((BoundPropertySource) renderer).addPropertyChangeListener(this);
        }
    }

    private void applyModuleSize() {
        int value = (Integer) moduleSizeSpinner.getValue();
        QrCodeRenderer renderer = assembly.getRenderer();
        if (renderer instanceof PngRendererBean) {
            ((PngRendererBean) renderer).setModuleSize(value);
        } else if (renderer instanceof SvgRendererBean) {
            ((SvgRendererBean) renderer).setModuleSize(value);
        }
        refreshPreviewFromLastSymbol();
    }

    private void applyRendererSelection() {
        String choice = (String) rendererCombo.getSelectedItem();
        QrCodeRenderer replacement;
        if ("SVG".equals(choice)) {
            replacement = svgRendererFor();
        } else if ("ASCII".equals(choice)) {
            replacement = asciiRendererFor();
        } else {
            replacement = pngRendererFor();
        }
        assembly.replaceRenderer(replacement);
        statusLabel.setText("renderer component replaced: " + replacement.getRendererId()
                + " (" + replacement.describeConfiguration() + ")");
        refreshPreviewFromLastSymbol();
    }

    private QrCodeRenderer pngRendererFor() {
        PngRendererBean bean = (PngRendererBean) rendererFor("PNG");
        bean.setModuleSize((Integer) moduleSizeSpinner.getValue());
        bean.setForegroundColor(HexColorPropertyEditor.fromHex(foregroundField.getText()));
        bean.setBackgroundColor(HexColorPropertyEditor.fromHex(backgroundField.getText()));
        bean.addPropertyChangeListener(this);
        return bean;
    }

    private QrCodeRenderer svgRendererFor() {
        SvgRendererBean bean = new SvgRendererBean();
        bean.setModuleSize((Integer) moduleSizeSpinner.getValue());
        bean.setForegroundColor(foregroundField.getText());
        bean.setBackgroundColor(backgroundField.getText());
        bean.setPhysicalWidthMillimetres(60);
        bean.addPropertyChangeListener(this);
        return bean;
    }

    private QrCodeRenderer asciiRendererFor() {
        AsciiRendererBean bean = new AsciiRendererBean();
        bean.addPropertyChangeListener(this);
        return bean;
    }

    private QrCodeRenderer rendererFor(String format) {
        if ("SVG".equals(format)) {
            return svgRendererFor();
        }
        if ("ASCII".equals(format)) {
            return asciiRendererFor();
        }
        return new PngRendererBean();
    }

    private void generate() {
        String payload = inputArea.getText();
        if (payload == null || payload.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a payload first", "Nothing to encode",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            QrErrorCorrection level = (QrErrorCorrection) levelCombo.getSelectedItem();
            QrGenerationResult result = assembly.generate(payload, level);
            lastSymbol = result.getSymbol();
            showArtifact(result.getArtifact(), result.getSymbol());
            statusLabel.setText("generated in " + result.getDurationMillis() + " ms - stored "
                    + (result.getStoredFile() == null ? "(nothing)" : result.getStoredFile().getName()));
            symbolLabel.setText("v" + result.getSymbol().getVersion() + "-"
                    + result.getSymbol().getErrorCorrection().getLabel()
                    + "  mask " + result.getSymbol().getMask()
                    + "  " + result.getSymbol().getEncodingMode()
                    + "  " + result.getSymbol().getSize() + "x" + result.getSymbol().getSize());
            refreshHistory();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Generation failed",
                    JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("error: " + e.getMessage());
        }
    }

    private void saveAs() {
        if (lastSymbol == null) {
            JOptionPane.showMessageDialog(this, "Generate a code first", "Nothing to save",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            RenderedArtifact artifact = assembly.getFacade().renderWith(lastSymbol, assembly.getRenderer());
            JFileChooser chooser = new JFileChooser(new File("."));
            chooser.setSelectedFile(new File("qr-code." + artifact.getFileExtension()));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File target = chooser.getSelectedFile();
                Files.write(target.toPath(), artifact.getContent());
                statusLabel.setText("written " + target.getAbsolutePath() + " ("
                        + artifact.getSizeInBytes() + " bytes)");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showArtifact(RenderedArtifact artifact, QrCode symbol) {
        if ("PNG".equals(artifact.getFormatName())) {
            ImageIcon icon = new ImageIcon(artifact.getContent());
            previewLabel.showImage(icon.getImage());
            previewLabel.setToolTipText(artifact.getWidth() + " x " + artifact.getHeight() + " pixels, "
                    + artifact.getSizeInBytes() + " bytes, " + artifact.getRendererId());
            previewText.setText("");
            previewCards.show(previewDeck, "raster");
        } else {
            previewLabel.clearImage();
            String text = artifact.getContentAsText();
            previewText.setText(text.length() > 20000 ? text.substring(0, 20000) + "..." : text);
            previewText.setCaretPosition(0);
            previewCards.show(previewDeck, "text");
        }
        previewDeck.revalidate();
        previewDeck.repaint();
        refreshHistory();
    }

    private void refreshPreviewFromLastSymbol() {
        if (lastSymbol == null) {
            return;
        }
        try {
            showArtifact(assembly.getFacade().renderWith(lastSymbol, assembly.getRenderer()), lastSymbol);
        } catch (Exception e) {
            statusLabel.setText("preview failed: " + e.getMessage());
        }
    }

    private void refreshHistory() {
        QrHistoryBean history = assembly.getHistory();
        historyArea.setText(history.describeEntries());
    }

    private void schedulePreview() {
        // Debounce: re-render the preview shortly after the user stops typing.
        SwingUtilities.invokeLater(() -> {
            if (lastSymbol != null) {
                refreshPreviewFromLastSymbol();
            }
        });
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        String name = event.getPropertyName();
        if ("entries".equals(name) || "entryCount".equals(name)) {
            refreshHistory();
        } else if ("processedEventCount".equals(name)) {
            statusLabel.setText("audit listener processed " + event.getNewValue() + " events");
        } else {
            statusLabel.setText("bound property changed: " + name + " = " + event.getNewValue()
                    + (event.getOldValue() == null ? "" : " (was " + event.getOldValue() + ")"));
            if ("moduleSize".equals(name) || "quietZone".equals(name)
                    || "foregroundColor".equals(name) || "backgroundColor".equals(name)) {
                refreshPreviewFromLastSymbol();
            }
        }
    }

    /** @return a one-line summary of the currently wired component graph. */
    public String describeWiring() {
        return assembly.getFacade().describeWiring()
                + " | renderer=" + assembly.getRenderer().describeConfiguration();
    }

    /**
     * Preview label that paints the rendered artifact scaled down to fit its panel, preserving the
     * aspect ratio. Nearest-neighbour interpolation is used so that module edges stay crisp, and the
     * image is never enlarged above 1:1 because a QR Code must stay pixel-exact when magnified.
     */
    private static final class FitLabel extends JLabel {

        private static final long serialVersionUID = 1L;

        private transient Image image;

        FitLabel() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setOpaque(true);
            setBackground(Color.WHITE);
        }

        void showImage(Image newImage) {
            this.image = newImage;
            setText("");
            repaint();
        }

        void clearImage() {
            this.image = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (image == null) {
                return;
            }
            int imageWidth = image.getWidth(this);
            int imageHeight = image.getHeight(this);
            int availableWidth = getWidth();
            int availableHeight = getHeight();
            if (imageWidth <= 0 || imageHeight <= 0 || availableWidth <= 0 || availableHeight <= 0) {
                return;
            }
            double scale = Math.min((double) availableWidth / imageWidth,
                    (double) availableHeight / imageHeight);
            if (scale > 1.0) {
                scale = 1.0;
            }
            int width = (int) Math.round(imageWidth * scale);
            int height = (int) Math.round(imageHeight * scale);
            Graphics2D scaled = (Graphics2D) graphics.create();
            try {
                scaled.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                scaled.drawImage(image, (availableWidth - width) / 2, (availableHeight - height) / 2,
                        width, height, this);
            } finally {
                scaled.dispose();
            }
        }
    }

    /** Renders the whole window into an image; used to capture the screenshots of this report. */
    public BufferedImage captureWindow() {
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** @return the Swing text component showing the history, used by the screenshot tool. */
    public JTextArea getHistoryArea() {
        return historyArea;
    }

    /** @return the artefact preview area. */
    public JTextArea getPreviewText() {
        return previewText;
    }

    /** Fills the payload area programmatically (screenshot tooling). */
    public void setPayload(String payload) {
        inputArea.setText(payload);
    }

    /** Selects a renderer in the combo box programmatically (screenshot tooling). */
    public void selectRenderer(String format) {
        rendererCombo.setSelectedItem(format.toUpperCase(Locale.ROOT));
    }

    /** Triggers a generation programmatically (screenshot tooling). */
    public void generateNow() {
        generate();
    }

    /** Applies colours programmatically (screenshot tooling). */
    public void setColours(String foregroundHex, String backgroundHex) {
        foregroundField.setText(foregroundHex);
        backgroundField.setText(backgroundHex);
        applyRendererSelection();
    }

    /** @return the colour currently used for the dark modules. */
    public Color getForegroundColour() {
        return HexColorPropertyEditor.fromHex(foregroundField.getText());
    }

    /** Re-subscribes the given listener to the currently wired renderer component. */
    public void reconnectToRenderer(PropertyChangeListener listener) {
        QrCodeRenderer renderer = assembly.getRenderer();
        if (renderer instanceof BoundPropertySource) {
            ((BoundPropertySource) renderer).addPropertyChangeListener(listener);
        }
    }
}
