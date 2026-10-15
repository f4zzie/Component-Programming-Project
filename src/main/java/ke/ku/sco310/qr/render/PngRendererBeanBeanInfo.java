package ke.ku.sco310.qr.render;

import ke.ku.sco310.qr.editor.HexColorPropertyEditor;

import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * Explicit introspection description of {@link PngRendererBean} (chapter 3, "5 Steps to Creating
 * a BeanInfo Class").
 *
 * <p>Why an explicit BeanInfo instead of relying on reflection alone?</p>
 * <ul>
 *   <li>the two {@code Color} properties are associated with the custom
 *       {@link HexColorPropertyEditor} through {@code setPropertyEditorClass};</li>
 *   <li>each property receives a human readable display name and a short description, which is
 *       what a property sheet shows to a designer;</li>
 *   <li>{@code quietZone} is marked as an <em>expert</em> feature, so a builder tool can hide it
 *       behind an "advanced" switch;</li>
 *   <li>the bean descriptor carries a display name for the palette.</li>
 * </ul>
 *
 * <p>The remaining properties are still discovered automatically by
 * {@code java.beans.Introspector}, because the class follows the {@code get}/{@code set} design
 * patterns.</p>
 *
 * <p>Naming rule: a builder tool looks for class {@code <BeanClass>BeanInfo}, so the descriptor of
 * {@code PngRendererBean} is {@code PngRendererBeanBeanInfo} - appending "BeanInfo" to the bean
 * class name, not replacing the trailing "Bean".</p>
 */
public class PngRendererBeanBeanInfo extends SimpleBeanInfo {

    @Override
    public BeanDescriptor getBeanDescriptor() {
        BeanDescriptor descriptor = new BeanDescriptor(PngRendererBean.class);
        descriptor.setDisplayName("PNG Renderer Bean");
        descriptor.setShortDescription("Renders an encoded QR Code symbol as a PNG image.");
        return descriptor;
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            PropertyDescriptor moduleSize = new PropertyDescriptor("moduleSize", PngRendererBean.class);
            moduleSize.setDisplayName("Module size (px)");
            moduleSize.setShortDescription("Pixel size of a single QR module; 8 px gives a 200 dpi friendly symbol.");

            PropertyDescriptor quietZone = new PropertyDescriptor("quietZone", PngRendererBean.class);
            quietZone.setDisplayName("Quiet zone (modules)");
            quietZone.setShortDescription("Light margin around the symbol; 4 modules is the ISO minimum.");
            quietZone.setExpert(true);

            PropertyDescriptor foreground = new PropertyDescriptor("foregroundColor", PngRendererBean.class);
            foreground.setDisplayName("Module colour");
            foreground.setShortDescription("Colour of the dark modules, edited as #RRGGBB.");
            foreground.setPropertyEditorClass(HexColorPropertyEditor.class);

            PropertyDescriptor background = new PropertyDescriptor("backgroundColor", PngRendererBean.class);
            background.setDisplayName("Background colour");
            background.setShortDescription("Colour of the light modules, edited as #RRGGBB.");
            background.setPropertyEditorClass(HexColorPropertyEditor.class);

            return new PropertyDescriptor[]{moduleSize, quietZone, foreground, background};
        } catch (IntrospectionException e) {
            return null; // let the Introspector fall back to the default reflection based search
        }
    }
}
