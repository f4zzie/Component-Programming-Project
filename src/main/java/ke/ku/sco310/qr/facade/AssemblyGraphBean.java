package ke.ku.sco310.qr.facade;

import ke.ku.sco310.qr.api.QrCodeEncoder;
import ke.ku.sco310.qr.api.QrCodeRepository;
import ke.ku.sco310.qr.api.QrCodeRenderer;
import ke.ku.sco310.qr.log.AuditLogListenerBean;
import ke.ku.sco310.qr.store.QrHistoryBean;


/**
 * Root bean used by {@link BeanPersistence} when a complete assembled bean graph is written to XML.
 *
 * <p>Note: this holder is not itself Serializable. The assembled graph is persisted through
 * {@code java.beans.XMLEncoder}, which records structure and properties; Java serialization is
 * reserved for the data records of the repository component.</p>
 *
 * <p>The JavaBeans specification calls this "persistence": {@code java.beans.XMLEncoder} records
 * the class names, the property values and the object graph in a human readable XML file, and
 * {@code java.beans.XMLDecoder} rebuilds the very same assembly later. The Bean Builder tool of
 * the original JavaBeans Development Kit worked exactly this way.</p>
 */
public class AssemblyGraphBean {

    private static final long serialVersionUID = 1L;

    private QrCodeEncoder encoder;
    private QrCodeRenderer renderer;
    private QrCodeRepository repository;
    private QrHistoryBean history;
    private AuditLogListenerBean listener;
    private QrStudioFacadeBean facade;

    public AssemblyGraphBean() {
    }

    public static AssemblyGraphBean from(QrAssembly assembly) {
        AssemblyGraphBean graph = new AssemblyGraphBean();
        graph.setEncoder(assembly.getEncoder());
        graph.setRenderer(assembly.getRenderer());
        graph.setRepository(assembly.getRepository());
        graph.setHistory(assembly.getHistory());
        graph.setListener(assembly.getListener());
        graph.setFacade(assembly.getFacade());
        return graph;
    }

    public QrAssembly toAssembly(AssemblyProfile profile) {
        QrAssembly assembly = new QrAssembly(profile);
        assembly.setEncoder(encoder);
        assembly.setRenderer(renderer);
        assembly.setRepository(repository);
        assembly.setHistory(history);
        assembly.setListener(listener);
        assembly.setFacade(facade);
        ComponentRegistry registry = new ComponentRegistry();
        assembly.setComponents(registry.listComponents());
        if (listener != null && facade != null) {
            facade.addQrCodeGenerationListener(listener);
        }
        return assembly;
    }

    public QrCodeEncoder getEncoder() {
        return encoder;
    }

    public void setEncoder(QrCodeEncoder encoder) {
        this.encoder = encoder;
    }

    public QrCodeRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(QrCodeRenderer renderer) {
        this.renderer = renderer;
    }

    public QrCodeRepository getRepository() {
        return repository;
    }

    public void setRepository(QrCodeRepository repository) {
        this.repository = repository;
    }

    public QrHistoryBean getHistory() {
        return history;
    }

    public void setHistory(QrHistoryBean history) {
        this.history = history;
    }

    public AuditLogListenerBean getListener() {
        return listener;
    }

    public void setListener(AuditLogListenerBean listener) {
        this.listener = listener;
    }

    public QrStudioFacadeBean getFacade() {
        return facade;
    }

    public void setFacade(QrStudioFacadeBean facade) {
        this.facade = facade;
    }

    /** @return a short description of the persisted graph, used by the verification report. */
    public String describeGraph() {
        return "XML bean graph{encoder=" + (encoder == null ? "-" : encoder.getComponentId())
                + ", renderer=" + (renderer == null ? "-" : renderer.getRendererId())
                + ", repository=" + (repository == null ? "-" : repository.getComponentId())
                + ", history=" + (history == null ? "-" : history.getComponentId())
                + ", listener=" + (listener == null ? "-" : listener.getComponentId())
                + ", facade=" + (facade == null ? "-" : facade.getComponentId()) + "}";
    }
}
