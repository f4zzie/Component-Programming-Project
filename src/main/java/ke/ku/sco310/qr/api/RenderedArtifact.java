package ke.ku.sco310.qr.api;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * Result of a rendering component: the bytes of the finished image/document plus the
 * metadata needed to name, store and serve it.
 *
 * <p>Modelled as a JavaBeans "simple property" bean: every attribute is reachable through
 * {@code get}/{@code set} pairs so that builder tools can inspect it and serialization can
 * persist it.</p>
 */
public class RenderedArtifact implements Serializable {

    private static final long serialVersionUID = 1L;

    private byte[] content = new byte[0];
    private String formatName = "";
    private String fileExtension = "";
    private String mimeType = "";
    private int width;
    private int height;
    private boolean textual;
    private String rendererId = "";

    public RenderedArtifact() {
    }

    public byte[] getContent() {
        return content.clone();
    }

    public void setContent(byte[] content) {
        this.content = content == null ? new byte[0] : content.clone();
    }

    /** Interprets the payload as UTF-8 text; useful for SVG and ASCII artifacts. */
    public String getContentAsText() {
        return new String(content, StandardCharsets.UTF_8);
    }

    public String getFormatName() {
        return formatName;
    }

    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isTextual() {
        return textual;
    }

    public void setTextual(boolean textual) {
        this.textual = textual;
    }

    public String getRendererId() {
        return rendererId;
    }

    public void setRendererId(String rendererId) {
        this.rendererId = rendererId;
    }

    /** @return number of bytes in the artifact body. */
    public int getSizeInBytes() {
        return content.length;
    }

    @Override
    public String toString() {
        return "RenderedArtifact{format=" + formatName + ", " + width + "x" + height
                + ", bytes=" + content.length + ", by=" + rendererId + "}";
    }
}
