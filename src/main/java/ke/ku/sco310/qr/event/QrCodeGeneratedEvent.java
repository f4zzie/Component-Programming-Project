package ke.ku.sco310.qr.event;

import ke.ku.sco310.qr.api.QrCode;
import ke.ku.sco310.qr.api.RenderedArtifact;

import java.io.File;
import java.util.EventObject;

/**
 * Event object fired by the facade component after a successful generation.
 *
 * <p>JavaBeans connection model: the facade is the <em>event source</em>, the audit logger
 * and the GUI are <em>listeners</em>, and this class is the <em>event object</em> that
 * carries the state change. The delegation model delivers it to every registered listener.</p>
 */
public class QrCodeGeneratedEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    private final QrCode symbol;
    private final RenderedArtifact artifact;
    private final File storedFile;
    private final long durationMillis;
    private final String rendererId;

    public QrCodeGeneratedEvent(Object source, QrCode symbol, RenderedArtifact artifact,
                                File storedFile, String rendererId, long durationMillis) {
        super(source);
        this.symbol = symbol;
        this.artifact = artifact;
        this.storedFile = storedFile;
        this.rendererId = rendererId;
        this.durationMillis = durationMillis;
    }

    public QrCode getSymbol() {
        return symbol;
    }

    public RenderedArtifact getArtifact() {
        return artifact;
    }

    /** @return the file the artifact was written to, or {@code null} when nothing was stored. */
    public File getStoredFile() {
        return storedFile;
    }

    public String getRendererId() {
        return rendererId;
    }

    /** @return wall-clock time spent encoding and rendering the symbol. */
    public long getDurationMillis() {
        return durationMillis;
    }

    @Override
    public String toString() {
        return "QrCodeGeneratedEvent{version=" + symbol.getVersion()
                + ", ec=" + symbol.getErrorCorrection().getLabel()
                + ", renderer=" + rendererId
                + ", millis=" + durationMillis + "}";
    }
}
