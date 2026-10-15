package ke.ku.sco310.qr.test;

import ke.ku.sco310.qr.api.BoundPropertySource;
import ke.ku.sco310.qr.api.QrErrorCorrection;
import ke.ku.sco310.qr.api.QrGenerationResult;
import ke.ku.sco310.qr.components.OptionalComponentSupport;
import ke.ku.sco310.qr.components.PureJavaQrEncoderBean;
import ke.ku.sco310.qr.components.ZxingQrEncoderAdapterBean;
import ke.ku.sco310.qr.editor.HexColorPropertyEditor;
import ke.ku.sco310.qr.facade.AssemblyProfile;
import ke.ku.sco310.qr.facade.BeanPersistence;
import ke.ku.sco310.qr.facade.ComponentRegistry;
import ke.ku.sco310.qr.facade.QrAssembly;
import ke.ku.sco310.qr.log.AuditLogListenerBean;
import ke.ku.sco310.qr.render.AsciiRendererBean;
import ke.ku.sco310.qr.render.PngRendererBean;
import ke.ku.sco310.qr.render.SvgRendererBean;
import ke.ku.sco310.qr.store.FileSystemRepositoryBean;
import ke.ku.sco310.qr.store.QrHistoryBean;
import ke.ku.sco310.qr.facade.QrStudioFacadeBean;

import java.beans.BeanInfo;
import java.beans.IndexedPropertyChangeEvent;
import java.beans.Introspector;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.VetoableChangeListener;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verifies that the components honour the JavaBeans component model itself: the mandatory bean
 * characteristics, the four property kinds, the delegation event model, introspection through a
 * BeanInfo class and the two persistence mechanisms.
 */
final class BeanContractTest {

    private BeanContractTest() {
    }

    private static final List<Class<?>> BEAN_CLASSES = List.of(
            PureJavaQrEncoderBean.class,
            ZxingQrEncoderAdapterBean.class,
            PngRendererBean.class,
            SvgRendererBean.class,
            AsciiRendererBean.class,
            FileSystemRepositoryBean.class,
            QrHistoryBean.class,
            AuditLogListenerBean.class,
            QrStudioFacadeBean.class);

    static void run(TestHarness harness, File workDirectory) throws Exception {
        // ---------------------------------------------------------------- bean characteristics
        for (Class<?> beanClass : BEAN_CLASSES) {
            boolean noArgConstructor = false;
            for (Constructor<?> constructor : beanClass.getConstructors()) {
                if (constructor.getParameterCount() == 0) {
                    noArgConstructor = true;
                }
            }
            harness.check("beans", beanClass.getSimpleName()
                            + ": public class with a no-argument constructor",
                    java.lang.reflect.Modifier.isPublic(beanClass.getModifiers()) && noArgConstructor,
                    "public=" + java.lang.reflect.Modifier.isPublic(beanClass.getModifiers())
                            + " noArgConstructor=" + noArgConstructor);

            BeanInfo info = Introspector.getBeanInfo(beanClass);
            int writable = 0;
            for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
                if (descriptor.getWriteMethod() != null) {
                    writable++;
                }
            }
            harness.check("beans", beanClass.getSimpleName()
                            + ": exposes writable properties through the design patterns",
                    writable >= 2, writable + " writable properties of "
                            + info.getPropertyDescriptors().length + " discovered");
            harness.check("beans", beanClass.getSimpleName()
                            + ": implements Serializable and BoundPropertySource",
                    java.io.Serializable.class.isAssignableFrom(beanClass)
                            && BoundPropertySource.class.isAssignableFrom(beanClass),
                    "serializable=" + java.io.Serializable.class.isAssignableFrom(beanClass)
                            + " boundPropertySource="
                            + BoundPropertySource.class.isAssignableFrom(beanClass));
        }

        harness.checkEquals("beans", "encoder event set is visible without a BeanInfo",
                "propertyChange", Introspector.getBeanInfo(PureJavaQrEncoderBean.class)
                        .getEventSetDescriptors()[0].getName());

        // ---------------------------------------------------------------- bound property
        PngRendererBean png = new PngRendererBean();
        AtomicReference<PropertyChangeEvent> captured = new AtomicReference<>();
        png.addPropertyChangeListener(captured::set);
        png.setModuleSize(12);
        harness.check("beans.bound", "changing moduleSize fires a PropertyChangeEvent",
                captured.get() != null && "moduleSize".equals(captured.get().getPropertyName())
                        && Integer.valueOf(8).equals(captured.get().getOldValue())
                        && Integer.valueOf(12).equals(captured.get().getNewValue()),
                captured.get() == null ? "no event" : captured.get().getPropertyName() + " "
                        + captured.get().getOldValue() + " -> " + captured.get().getNewValue());

        // ---------------------------------------------------------------- constrained property
        QrHistoryBean history = new QrHistoryBean();
        history.setMaxEntries(5);
        AtomicInteger vetoCount = new AtomicInteger();
        VetoableChangeListener veto = event -> {
            if (((Integer) event.getNewValue()) < 5) {
                vetoCount.incrementAndGet();
                throw new java.beans.PropertyVetoException("capacity may not be reduced", event);
            }
        };
        history.addVetoableChangeListener(veto);
        harness.expectFailure("beans.constrained", "a listener can veto a capacity reduction",
                () -> history.setMaxEntries(2));
        harness.checkEquals("beans.constrained", "the vetoed value was not applied", 5, history.getMaxEntries());
        history.removeVetoableChangeListener(veto);
        history.setMaxEntries(7);
        harness.checkEquals("beans.constrained", "removing the listener allows the change again",
                7, history.getMaxEntries());

        // ---------------------------------------------------------------- indexed property
        QrHistoryBean indexed = new QrHistoryBean(3);
        List<PropertyChangeEvent> indexedEvents = new ArrayList<>();
        indexed.addPropertyChangeListener(event -> indexedEvents.add(event));
        indexed.addEntry(new ke.ku.sco310.qr.api.QrHistoryEntry(
                new PureJavaQrEncoderBean().encode("indexed property", QrErrorCorrection.LOW), "a.png"));
        indexed.addEntry(new ke.ku.sco310.qr.api.QrHistoryEntry(
                new PureJavaQrEncoderBean().encode("second entry", QrErrorCorrection.LOW), "b.png"));
        boolean indexedEventSeen = indexedEvents.stream()
                .anyMatch(event -> event instanceof IndexedPropertyChangeEvent
                        && "entries".equals(event.getPropertyName()));
        harness.check("beans.indexed", "addEntry fires an IndexedPropertyChangeEvent",
                indexedEventSeen, indexedEvents.size() + " events observed");
        harness.checkEquals("beans.indexed", "indexed accessor returns the stored element", 1,
                indexed.getEntries(1).getVersion() > 0 ? 1 : 0);
        indexed.addEntry(new ke.ku.sco310.qr.api.QrHistoryEntry(
                new PureJavaQrEncoderBean().encode("third", QrErrorCorrection.LOW), "c.png"));
        indexed.addEntry(new ke.ku.sco310.qr.api.QrHistoryEntry(
                new PureJavaQrEncoderBean().encode("fourth", QrErrorCorrection.LOW), "d.png"));
        harness.check("beans.indexed", "capacity policy discards the oldest entry",
                indexed.getEntryCount() == 3 && indexed.getDiscardedEntryCount() == 1,
                "count=" + indexed.getEntryCount() + " discarded=" + indexed.getDiscardedEntryCount());
        harness.checkEquals("beans.indexed", "overflow flag is exposed as a read-only property",
                true, indexed.isOverflowed());

        // ---------------------------------------------------------------- BeanInfo / property editor
        BeanInfo pngInfo = Introspector.getBeanInfo(PngRendererBean.class);
        String foregroundEditor = null;
        String quietZoneCategory = null;
        for (PropertyDescriptor descriptor : pngInfo.getPropertyDescriptors()) {
            if ("foregroundColor".equals(descriptor.getName()) && descriptor.getPropertyEditorClass() != null) {
                foregroundEditor = descriptor.getPropertyEditorClass().getSimpleName();
            }
            if ("quietZone".equals(descriptor.getName()) && descriptor.isExpert()) {
                quietZoneCategory = "expert";
            }
        }
        harness.checkEquals("beans.introspection", "BeanInfo attaches the custom colour editor",
                HexColorPropertyEditor.class.getSimpleName(), foregroundEditor);
        harness.checkEquals("beans.introspection", "BeanInfo marks quietZone as an expert feature",
                "expert", quietZoneCategory);
        harness.checkEquals("beans.introspection", "BeanInfo publishes a display name for builder tools",
                "PNG Renderer Bean", pngInfo.getBeanDescriptor().getDisplayName());

        // ---------------------------------------------------------------- event model
        AssemblyProfile profile = new AssemblyProfile();
        profile.setProperty("repository", "rootDirectory", new File(workDirectory, "bean-test").getPath());
        QrAssembly assembly = new ComponentRegistry().build(profile);
        AtomicInteger events = new AtomicInteger();
        AtomicReference<QrGenerationResult> lastResult = new AtomicReference<>();
        assembly.getFacade().addQrCodeGenerationListener(event -> events.incrementAndGet());
        lastResult.set(assembly.getFacade().generate("event model check", QrErrorCorrection.MEDIUM));
        harness.check("beans.events", "the facade notifies every connected listener",
                events.get() == 1 && assembly.getListener().getProcessedEventCount() > 0,
                "listeners notified=" + events.get() + " audit events="
                        + assembly.getListener().getProcessedEventCount());
        harness.check("beans.events", "the event object carries the symbol and the artifact",
                lastResult.get().getSymbol() != null && lastResult.get().getArtifact() != null,
                "symbol=" + lastResult.get().getSymbol() + " artifact="
                        + lastResult.get().getArtifact());

        // ---------------------------------------------------------------- XML persistence
        File xml = new File(workDirectory, "bean-graph.xml");
        BeanPersistence.save(assembly, xml);
        QrAssembly restored = BeanPersistence.load(xml, profile);
        harness.check("beans.persistence", "the assembled bean graph survives an XML round trip",
                restored.getEncoder().getComponentId().equals(assembly.getEncoder().getComponentId())
                        && restored.getRenderer().getRendererId().equals(assembly.getRenderer().getRendererId()),
                restored.getFacade().describeWiring());
        boolean configuredPropertySurvived = restored.getRenderer() instanceof PngRendererBean
                && ((PngRendererBean) restored.getRenderer()).getModuleSize()
                        == ((PngRendererBean) assembly.getRenderer()).getModuleSize();
        harness.check("beans.persistence", "design-time property values survive the round trip",
                configuredPropertySurvived, "moduleSize="
                        + (restored.getRenderer() instanceof PngRendererBean
                        ? ((PngRendererBean) restored.getRenderer()).getModuleSize() : -1)
                        + " xmlBytes=" + xml.length());
        harness.check("beans.persistence", "the restored assembly can generate without recompiling",
                restored.generate("persisted assembly", QrErrorCorrection.LOW).getStoredFile() != null,
                "artifact written by the restored graph");

        // ---------------------------------------------------------------- profile persistence
        File profileFile = new File(workDirectory, "assembly-roundtrip.xml");
        AssemblyProfile original = new AssemblyProfile();
        original.setProfileName("round-trip");
        original.setComponentId("renderer", "renderer.svg.v1");
        original.setProperty("renderer", "moduleSize", "9");
        original.save(profileFile);
        AssemblyProfile loaded = AssemblyProfile.load(profileFile);
        harness.check("beans.persistence", "the assembly profile survives an XML round trip",
                "renderer.svg.v1".equals(loaded.getComponentId("renderer"))
                        && "9".equals(loaded.getProperties("renderer").get("moduleSize")),
                loaded.describe().replace('\n', ' '));

        // ---------------------------------------------------------------- optional component
        boolean zxingPresent = OptionalComponentSupport.isZxingAvailable();
        harness.check("beans.optional", "the assembly detects the optional third-party component",
                zxingPresent, "com.google.zxing.core present=" + zxingPresent);
        if (zxingPresent) {
            ZxingQrEncoderAdapterBean adapter = new ZxingQrEncoderAdapterBean();
            harness.check("beans.optional", "the adapter reports itself as a third-party component",
                    adapter.getEncoderInfo().isThirdParty()
                            && adapter.getEncoderInfo().isRequiresOptionalLibrary(),
                    adapter.getEncoderInfo().toString());
        }
    }
}
