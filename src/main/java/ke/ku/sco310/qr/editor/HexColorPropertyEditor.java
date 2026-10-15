package ke.ku.sco310.qr.editor;

import java.awt.Color;
import java.beans.PropertyEditorSupport;

/**
 * Custom property editor for {@link Color} valued bean properties.
 *
 * <p>Demonstrates the "explicit association via a BeanInfo object" technique of chapter 3: the
 * {@link ke.ku.sco310.qr.render.PngRendererBeanInfo} class attaches this editor to the
 * {@code foregroundColor} and {@code backgroundColor} properties, so a builder tool displays a
 * hex string such as {@code #1B5E20} instead of the raw {@code java.awt.Color} object.</p>
 */
public class HexColorPropertyEditor extends PropertyEditorSupport {

    @Override
    public String getAsText() {
        Object value = getValue();
        return value instanceof Color ? toHex((Color) value) : "";
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Colour must not be empty; expected #RRGGBB");
        }
        setValue(fromHex(text.trim()));
    }

    @Override
    public String[] getTags() {
        return new String[]{"#000000", "#1B5E20", "#0D47A1", "#B71C1C", "#FFFFFF", "#F5F5F5"};
    }

    @Override
    public boolean supportsCustomEditor() {
        return false;
    }

    /** @param color colour to convert @return {@code #RRGGBB} representation. */
    public static String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * @param text {@code #RGB}, {@code #RRGGBB} or a {@code java.awt.Color} constant name
     * @return the parsed colour
     * @throws IllegalArgumentException when the text is not a valid colour
     */
    public static Color fromHex(String text) {
        String value = text.trim();
        if (value.equalsIgnoreCase("black")) return Color.BLACK;
        if (value.equalsIgnoreCase("white")) return Color.WHITE;
        if (value.equalsIgnoreCase("red")) return Color.RED;
        if (value.equalsIgnoreCase("green")) return new Color(0x1B5E20);
        if (value.equalsIgnoreCase("blue")) return new Color(0x0D47A1);
        if (!value.startsWith("#")) {
            value = "#" + value;
        }
        if (value.length() == 4) {
            char r = value.charAt(1), g = value.charAt(2), b = value.charAt(3);
            value = "#" + r + r + g + g + b + b;
        }
        if (value.length() != 7) {
            throw new IllegalArgumentException("Expected #RRGGBB but received '" + text + "'");
        }
        try {
            return new Color(Integer.parseInt(value.substring(1), 16));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text + "' is not a hexadecimal colour", e);
        }
    }
}
