package ke.ku.sco310.qr.log;

import ke.ku.sco310.qr.api.BoundPropertySource;
import ke.ku.sco310.qr.api.QrCode;
import ke.ku.sco310.qr.api.RenderedArtifact;
import ke.ku.sco310.qr.event.QrCodeGeneratedEvent;
import ke.ku.sco310.qr.event.QrCodeGenerationListener;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Listener component of the JavaBeans connection model.
 *
 * <p>It is not referenced by the facade at all: the assembly wires it with
 * {@code facade.addQrCodeGenerationListener(auditLogBean)}. Because the source only knows the
 * {@link QrCodeGenerationListener} interface, this logger can be replaced by any other listener
 * (for example a statistics bean) without recompiling the facade - the delegation model in
 * practice.</p>
 */
public class AuditLogListenerBean implements QrCodeGenerationListener, Serializable, BoundPropertySource {

    private static final long serialVersionUID = 1L;

    /** Component instance identifier. */
    public static final String COMPONENT_ID = "listener.audit-log.v1";

    private transient PropertyChangeSupport propertyChanges = new PropertyChangeSupport(this);

    private File logFile;
    private boolean echoToConsole = true;
    private boolean verbose = false;
    private int processedEventCount = 0;
    private final ArrayList<String> recentLines = new ArrayList<>();

    public AuditLogListenerBean() {
    }

    public String getComponentId() {
        return COMPONENT_ID;
    }

    @Override
    public void qrCodeGenerated(QrCodeGeneratedEvent event) {
        QrCode symbol = event.getSymbol();
        RenderedArtifact artifact = event.getArtifact();
        String line = String.format(Locale.ROOT,
                "%s | %s | v%d-%s | mask %d | %s | %d bytes | %d ms | %s",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT).format(new Date()),
                abbreviate(symbol.getContent(), 28),
                symbol.getVersion(), symbol.getErrorCorrection().getLabel(), symbol.getMask(),
                artifact == null ? "no artifact" : artifact.getFormatName(),
                artifact == null ? 0 : artifact.getSizeInBytes(),
                event.getDurationMillis(),
                event.getStoredFile() == null ? "(not stored)" : event.getStoredFile().getName());
        if (verbose && artifact != null && artifact.isTextual()) {
            line = line + System.lineSeparator() + indent(abbreviate(artifact.getContentAsText(), 400));
        }
        processedEventCount++;
        remember(line);
        if (echoToConsole) {
            System.out.println("[audit] " + line);
        }
        if (logFile != null) {
            appendToFile(line);
        }
        propertyChanges.firePropertyChange("processedEventCount", processedEventCount - 1, processedEventCount);
    }

    private void appendToFile(String line) {
        try {
            File parent = logFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                writer.println(line);
            }
        } catch (IOException e) {
            System.err.println("[audit] could not write " + logFile + ": " + e.getMessage());
        }
    }

    private void remember(String line) {
        recentLines.add(line);
        while (recentLines.size() > 20) {
            recentLines.remove(0);
        }
    }

    private static String abbreviate(String text, int max) {
        String flat = text == null ? "" : text.replaceAll("\\s+", " ");
        return flat.length() <= max ? flat : flat.substring(0, max) + "...";
    }

    private static String indent(String text) {
        StringBuilder sb = new StringBuilder();
        for (String part : text.split("\\R")) {
            sb.append("        ").append(part).append(System.lineSeparator());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ properties

    public File getLogFile() {
        return logFile;
    }

    /** Simple property: when {@code null} only the console echo is produced. */
    public void setLogFile(File logFile) {
        File old = this.logFile;
        this.logFile = logFile;
        propertyChanges.firePropertyChange("logFile", old, logFile);
    }

    public boolean isEchoToConsole() {
        return echoToConsole;
    }

    public void setEchoToConsole(boolean echoToConsole) {
        boolean old = this.echoToConsole;
        this.echoToConsole = echoToConsole;
        propertyChanges.firePropertyChange("echoToConsole", old, echoToConsole);
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        boolean old = this.verbose;
        this.verbose = verbose;
        propertyChanges.firePropertyChange("verbose", old, verbose);
    }

    /** @return how many generation events this listener has processed. */
    public int getProcessedEventCount() {
        return processedEventCount;
    }

    /** @return the most recently produced log lines, oldest first. */
    public List<String> getRecentLines() {
        return new ArrayList<>(recentLines);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChanges.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChanges.removePropertyChangeListener(listener);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        propertyChanges = new PropertyChangeSupport(this);
    }

    @Override
    public String toString() {
        return "AuditLogListenerBean{file=" + logFile + ", console=" + echoToConsole
                + ", events=" + processedEventCount + "}";
    }
}
