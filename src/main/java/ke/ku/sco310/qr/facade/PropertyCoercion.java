package ke.ku.sco310.qr.facade;

import ke.ku.sco310.qr.api.QrErrorCorrection;
import ke.ku.sco310.qr.editor.HexColorPropertyEditor;

import java.awt.Color;

/**
 * Converts the textual values of the assembly descriptor into the Java types declared by bean
 * setter methods.
 *
 * <p>This is the small piece of glue that makes the assembly <em>declarative</em>: the
 * configuration file says {@code moduleSize=10}, and this class decides that the bean property
 * of type {@code int} must receive {@code 10}. Without it, every new configurable property would
 * need new parsing code in the assembly.</p>
 */
final class PropertyCoercion {

    private PropertyCoercion() {
    }

    /**
     * @param value      textual value from the descriptor
     * @param targetType declared type of the bean property
     * @return the coerced value
     * @throws IllegalArgumentException when the text cannot be represented in the target type
     */
    static Object coerce(String value, Class<?> targetType) {
        String text = value == null ? "" : value.trim();
        if (targetType == String.class) {
            return text;
        }
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(text);
        }
        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(text);
        }
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(text);
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(text);
        }
        if (targetType == Color.class) {
            return HexColorPropertyEditor.fromHex(text);
        }
        if (targetType == QrErrorCorrection.class) {
            return QrErrorCorrection.fromLabel(text);
        }
        if (targetType == java.io.File.class) {
            return new java.io.File(text);
        }
        throw new IllegalArgumentException("No coercion from text to " + targetType.getName()
                + " (value '" + text + "')");
    }
}
