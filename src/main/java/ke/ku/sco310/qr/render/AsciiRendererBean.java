package ke.ku.sco310.qr.render;

import ke.ku.sco310.qr.api.BoundPropertySource;
import ke.ku.sco310.qr.api.QrCode;
import ke.ku.sco310.qr.api.QrCodeRenderer;
import ke.ku.sco310.qr.api.RenderedArtifact;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * Text renderer component: draws the symbol with characters so that a code can be scanned
 * straight from a terminal or embedded in a plain-text report.
 *
 * <p>Each module is drawn as two glyph characters, which keeps the printed aspect ratio square
 * (a terminal character cell is roughly twice as tall as it is wide).</p>
 */
public class AsciiRendererBean implements QrCodeRenderer, Serializable, BoundPropertySource {

    private static final long serialVersionUID = 1L;

    /** Component instance identifier. */
    public static final String COMPONENT_ID = "renderer.ascii.v1";

    private transient PropertyChangeSupport propertyChanges = new PropertyChangeSupport(this);

    private String darkGlyph = "\u2588\u2588";   // two full block characters
    private String lightGlyph = "  ";
    private int border = 2;

    public AsciiRendererBean() {
    }

    @Override
    public RenderedArtifact render(QrCode symbol) {
        String text = symbol.toAsciiArt(darkGlyph, lightGlyph, border);
        RenderedArtifact artifact = new RenderedArtifact();
        artifact.setContent(text.getBytes(StandardCharsets.UTF_8));
        artifact.setFormatName("ASCII");
        artifact.setFileExtension("txt");
        artifact.setMimeType("text/plain; charset=utf-8");
        artifact.setWidth(text.indexOf('\n'));
        artifact.setHeight((int) text.chars().filter(c -> c == '\n').count());
        artifact.setTextual(true);
        artifact.setRendererId(COMPONENT_ID);
        return artifact;
    }

    @Override
    public String getRendererId() {
        return COMPONENT_ID;
    }

    @Override
    public String getFormatName() {
        return "ASCII";
    }

    @Override
    public String getFileExtension() {
        return "txt";
    }

    @Override
    public String getMimeType() {
        return "text/plain; charset=utf-8";
    }

    @Override
    public String describeConfiguration() {
        return "ASCII: dark='" + darkGlyph + "', light='" + lightGlyph + "', border=" + border;
    }

    public String getDarkGlyph() {
        return darkGlyph;
    }

    public void setDarkGlyph(String darkGlyph) {
        String old = this.darkGlyph;
        this.darkGlyph = darkGlyph;
        propertyChanges.firePropertyChange("darkGlyph", old, darkGlyph);
    }

    public String getLightGlyph() {
        return lightGlyph;
    }

    public void setLightGlyph(String lightGlyph) {
        String old = this.lightGlyph;
        this.lightGlyph = lightGlyph;
        propertyChanges.firePropertyChange("lightGlyph", old, lightGlyph);
    }

    public int getBorder() {
        return border;
    }

    public void setBorder(int border) {
        int old = this.border;
        this.border = border;
        propertyChanges.firePropertyChange("border", old, border);
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
        return describeConfiguration();
    }
}
