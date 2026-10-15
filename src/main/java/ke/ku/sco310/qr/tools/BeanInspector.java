package ke.ku.sco310.qr.tools;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Introspection tool: prints everything a builder tool can discover about a bean (chapter 3,
 * "Introspector Class" and "Feature Descriptors").
 *
 * <p>Run it on any class of this suite to obtain the evidence table used in the report:</p>
 * <pre>
 * java -cp out/classes ke.ku.sco310.qr.tools.BeanInspector ke.ku.sco310.qr.render.PngRendererBean
 * </pre>
 *
 * <p>The tool also reports whether an explicit {@code ClassNameBeanInfo} class exists, which is how
 * a designer knows that a bean exposes curated property metadata rather than pure reflection
 * defaults.</p>
 */
public final class BeanInspector {

    private BeanInspector() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("usage: BeanInspector <fully.qualified.BeanClass> [more.Classes...]");
            System.exit(2);
        }
        for (String name : args) {
            try {
                System.out.println(inspect(Class.forName(name)));
                System.out.println();
            } catch (ClassNotFoundException e) {
                System.err.println("class not found: " + name);
            }
        }
    }

    /**
     * @param beanClass class to introspect
     * @return a printable report of the bean's features
     */
    public static String inspect(Class<?> beanClass) {
        StringBuilder sb = new StringBuilder();
        String simpleName = beanClass.getSimpleName();

        sb.append("==============================================================\n");
        sb.append("BEAN INSPECTION: ").append(beanClass.getName()).append('\n');
        sb.append("==============================================================\n");

        sb.append("\n[1] Required JavaBeans characteristics\n");
        sb.append(row("public class", Modifier.isPublic(beanClass.getModifiers()) ? "yes" : "NO"));
        sb.append(row("no-argument constructor", hasNoArgConstructor(beanClass) ? "yes" : "NO"));
        sb.append(row("implements Serializable",
                java.io.Serializable.class.isAssignableFrom(beanClass) ? "yes" : "no"));
        sb.append(row("superclass", String.valueOf(beanClass.getSuperclass().getName())));
        List<String> interfaces = new ArrayList<>();
        for (Class<?> iface : beanClass.getInterfaces()) {
            interfaces.add(iface.getName());
        }
        sb.append(row("directly implemented interfaces",
                interfaces.isEmpty() ? "-" : String.join(", ", interfaces)));

        String beanInfoName = beanClass.getName() + "BeanInfo";
        Class<?> beanInfoClass = null;
        try {
            beanInfoClass = Class.forName(beanInfoName);
        } catch (ClassNotFoundException ignored) {
            // no explicit BeanInfo: the Introspector will use the design patterns alone
        }
        sb.append(row("explicit BeanInfo", beanInfoClass == null ? "none (reflection only)"
                : beanInfoName));

        try {
            BeanInfo info = Introspector.getBeanInfo(beanClass);
            BeanDescriptor descriptor = info.getBeanDescriptor();
            if (descriptor != null) {
                sb.append(row("display name", String.valueOf(descriptor.getDisplayName())));
                sb.append(row("short description", String.valueOf(descriptor.getShortDescription())));
            }

            sb.append("\n[2] Properties discovered through the get/set design patterns\n");
            sb.append(String.format("%-24s %-32s %-6s %-6s %-9s %-9s %s%n",
                    "PROPERTY", "TYPE", "READ", "WRITE", "INDEXED", "CATEGORY", "PROPERTY EDITOR"));
            sb.append("-".repeat(140)).append('\n');
            int propertyCount = 0;
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                if ("class".equals(pd.getName())) {
                    continue;
                }
                propertyCount++;
                String category = "normal";
                if (pd.isExpert()) {
                    category = "expert";
                }
                if (pd.isHidden()) {
                    category = "hidden";
                }
                String editor = pd.getPropertyEditorClass() == null
                        ? "-" : pd.getPropertyEditorClass().getSimpleName();
                sb.append(String.format("%-24s %-32s %-6s %-6s %-9s %-9s %s%n",
                        pd.getName(),
                        pd.getPropertyType() == null ? "-" : pd.getPropertyType().getSimpleName(),
                        pd.getReadMethod() == null ? "-" : "yes",
                        pd.getWriteMethod() == null ? "-" : "yes",
                        pd instanceof java.beans.IndexedPropertyDescriptor ? "yes" : "-",
                        category,
                        editor));
            }
            sb.append(row("properties found", String.valueOf(propertyCount)));

            sb.append("\n[3] Event sets (discovered from add/remove listener methods)\n");
            EventSetDescriptor[] eventSets = info.getEventSetDescriptors();
            if (eventSets.length == 0) {
                sb.append("  (none)\n");
            }
            for (EventSetDescriptor esd : eventSets) {
                sb.append(String.format("  %-32s listener=%-42s add=%s%n",
                        esd.getName(), esd.getListenerType().getSimpleName(), esd.getAddListenerMethod().getName()));
            }
            sb.append(row("event sets", String.valueOf(eventSets.length)));

            sb.append("\n[4] Public methods that are neither accessors nor listener plumbing\n");
            for (MethodDescriptor md : info.getMethodDescriptors()) {
                Method method = md.getMethod();
                String name = method.getName();
                boolean accessor = name.startsWith("get") || name.startsWith("set") || name.startsWith("is");
                boolean plumbing = name.startsWith("add") || name.startsWith("remove");
                if (!accessor && !plumbing && Modifier.isPublic(method.getModifiers())) {
                    sb.append(String.format("  %-28s returns %s%n", name, method.getReturnType().getSimpleName()));
                }
            }
        } catch (IntrospectionException e) {
            sb.append("Introspection failed: ").append(e).append('\n');
        }

        sb.append("\n").append(summaryLine(beanClass)).append('\n');
        return sb.toString();
    }

    private static String summaryLine(Class<?> beanClass) {
        try {
            BeanInfo info = Introspector.getBeanInfo(beanClass);
            long propertyCount = Arrays.stream(info.getPropertyDescriptors())
                    .filter(pd -> !"class".equals(pd.getName())).count();
            return "Summary: " + propertyCount + " properties, " + info.getEventSetDescriptors().length
                    + " event sets, " + info.getMethodDescriptors().length + " methods";
        } catch (IntrospectionException e) {
            return "Summary: introspection failed";
        }
    }

    private static boolean hasNoArgConstructor(Class<?> beanClass) {
        for (Constructor<?> constructor : beanClass.getConstructors()) {
            if (constructor.getParameterCount() == 0) {
                return true;
            }
        }
        return false;
    }

    private static String row(String label, String value) {
        return String.format("  %-32s : %s%n", label, value);
    }

    /**
     * @param beanClass class to introspect
     * @return the same information as {@link #inspect(Class)} rendered as an HTML section, used to
     *         build the documentation appendix without copying tables by hand
     */
    public static String inspectHtml(Class<?> beanClass) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h4>").append(beanClass.getSimpleName()).append("</h4>\n");
        sb.append("<p class=\"bean-meta\">public class: ")
          .append(Modifier.isPublic(beanClass.getModifiers()) ? "yes" : "no")
          .append(" &middot; no-arg constructor: ").append(hasNoArgConstructor(beanClass) ? "yes" : "no")
          .append(" &middot; Serializable: ")
          .append(java.io.Serializable.class.isAssignableFrom(beanClass) ? "yes" : "no")
          .append("</p>\n");
        try {
            BeanInfo info = Introspector.getBeanInfo(beanClass);
            sb.append("<table><thead><tr><th>Property</th><th>Type</th><th>Read</th><th>Write</th>")
              .append("<th>Indexed</th><th>Editor</th></tr></thead><tbody>\n");
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                if ("class".equals(pd.getName())) {
                    continue;
                }
                sb.append("<tr><td><code>").append(pd.getName()).append("</code></td><td>")
                  .append(pd.getPropertyType() == null ? "-" : pd.getPropertyType().getSimpleName())
                  .append("</td><td>").append(pd.getReadMethod() == null ? "-" : "yes")
                  .append("</td><td>").append(pd.getWriteMethod() == null ? "-" : "yes")
                  .append("</td><td>")
                  .append(pd instanceof java.beans.IndexedPropertyDescriptor ? "yes" : "-")
                  .append("</td><td>")
                  .append(pd.getPropertyEditorClass() == null ? "default"
                          : pd.getPropertyEditorClass().getSimpleName().toLowerCase(Locale.ROOT))
                  .append("</td></tr>\n");
            }
            sb.append("</tbody></table>\n");
            if (info.getEventSetDescriptors().length > 0) {
                sb.append("<p><strong>Events:</strong> ");
                List<String> events = new ArrayList<>();
                for (EventSetDescriptor esd : info.getEventSetDescriptors()) {
                    events.add(esd.getName() + " (" + esd.getListenerType().getSimpleName() + ")");
                }
                sb.append(String.join(", ", events)).append("</p>\n");
            }
        } catch (IntrospectionException e) {
            sb.append("<p>introspection failed: ").append(e).append("</p>\n");
        }
        return sb.toString();
    }
}
