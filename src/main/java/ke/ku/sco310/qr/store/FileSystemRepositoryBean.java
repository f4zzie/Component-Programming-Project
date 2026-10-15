package ke.ku.sco310.qr.store;

import ke.ku.sco310.qr.api.BoundPropertySource;
import ke.ku.sco310.qr.api.QrCodeRepository;
import ke.ku.sco310.qr.api.QrHistoryEntry;
import ke.ku.sco310.qr.api.QrStorageException;
import ke.ku.sco310.qr.api.RenderedArtifact;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Persistence component: writes rendered artifacts and the generation history to the file system.
 *
 * <p>Two persistence mechanisms are exercised by the suite, exactly as described in the course
 * material: <em>serialization</em> for the history records (this class) and <em>XML
 * persistence</em> for the assembled bean graph
 * ({@code ke.ku.sco310.qr.facade.BeanPersistence}).</p>
 *
 * <p>Deployment model hint (D = (C, E, P, L)): nothing in this class knows whether it runs on a
 * desktop or on a server - the difference is the {@code rootDirectory} property, i.e. the
 * deployment parameter P, not the implementation.</p>
 */
public class FileSystemRepositoryBean implements QrCodeRepository, Serializable, BoundPropertySource {

    private static final long serialVersionUID = 1L;

    /** Component instance identifier. */
    public static final String COMPONENT_ID = "repository.file-system.v1";

    private transient PropertyChangeSupport propertyChanges = new PropertyChangeSupport(this);

    private File rootDirectory = new File("out/generated");
    private String historyFileName = "history.ser";
    private int storedArtifactCount = 0;

    public FileSystemRepositoryBean() {
    }

    public FileSystemRepositoryBean(File rootDirectory) {
        setRootDirectory(rootDirectory);
    }

    @Override
    public File getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public File storeArtifact(RenderedArtifact artifact, String baseName) throws QrStorageException {
        if (artifact == null) {
            throw new QrStorageException(COMPONENT_ID, "Cannot store a null artifact");
        }
        String safeName = sanitise(baseName) + "." + artifact.getFileExtension();
        File target = new File(ensureDirectory(), safeName);
        try (FileOutputStream out = new FileOutputStream(target)) {
            out.write(artifact.getContent());
        } catch (IOException e) {
            throw new QrStorageException(COMPONENT_ID, "Could not write " + target.getAbsolutePath(), e);
        }
        storedArtifactCount++;
        propertyChanges.firePropertyChange("storedArtifactCount", storedArtifactCount - 1, storedArtifactCount);
        return target;
    }

    @Override
    public void appendHistory(QrHistoryEntry entry) throws QrStorageException {
        List<QrHistoryEntry> history = loadHistory();
        history.add(entry);
        writeHistory(history);
        propertyChanges.firePropertyChange("historyCount", history.size() - 1, history.size());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<QrHistoryEntry> loadHistory() throws QrStorageException {
        File file = historyFile();
        if (!file.isFile()) {
            return new ArrayList<>();
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            Object read = in.readObject();
            if (!(read instanceof List)) {
                throw new QrStorageException(COMPONENT_ID,
                        "History file " + file + " does not contain a list of entries");
            }
            return new ArrayList<>((List<QrHistoryEntry>) read);
        } catch (IOException | ClassNotFoundException e) {
            throw new QrStorageException(COMPONENT_ID,
                    "Could not read the history file " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public int getHistoryCount() throws QrStorageException {
        return loadHistory().size();
    }

    @Override
    public void clearHistory() throws QrStorageException {
        File file = historyFile();
        if (file.isFile() && !file.delete()) {
            throw new QrStorageException(COMPONENT_ID, "Could not delete " + file.getAbsolutePath());
        }
        propertyChanges.firePropertyChange("historyCount", -1, 0);
    }

    @Override
    public String getComponentId() {
        return COMPONENT_ID;
    }

    private void writeHistory(List<QrHistoryEntry> history) throws QrStorageException {
        File file = historyFile();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(new ArrayList<>(history));
        } catch (IOException e) {
            throw new QrStorageException(COMPONENT_ID,
                    "Could not write the history file " + file.getAbsolutePath(), e);
        }
    }

    private File ensureDirectory() throws QrStorageException {
        if (!rootDirectory.isDirectory() && !rootDirectory.mkdirs()) {
            throw new QrStorageException(COMPONENT_ID,
                    "Could not create the storage directory " + rootDirectory.getAbsolutePath());
        }
        return rootDirectory;
    }

    private File historyFile() throws QrStorageException {
        return new File(ensureDirectory(), historyFileName);
    }

    private static String sanitise(String baseName) {
        String cleaned = baseName == null ? "qr-code" : baseName.trim().toLowerCase(Locale.ROOT);
        cleaned = cleaned.replaceAll("[^a-z0-9._-]+", "-");
        cleaned = cleaned.replaceAll("^-+|-+$", "");
        return cleaned.isEmpty() ? "qr-code" : cleaned;
    }

    /** @return the directory plus the two configuration files, for the report appendix. */
    public String describeConfiguration() {
        return "FileSystemRepository{root=" + rootDirectory.getAbsolutePath()
                + ", historyFile=" + historyFileName + ", storedArtifacts=" + storedArtifactCount + "}";
    }

    // ------------------------------------------------------------------ properties

    public void setRootDirectory(File rootDirectory) {
        File old = this.rootDirectory;
        this.rootDirectory = rootDirectory;
        propertyChanges.firePropertyChange("rootDirectory", old, rootDirectory);
    }

    public String getHistoryFileName() {
        return historyFileName;
    }

    public void setHistoryFileName(String historyFileName) {
        String old = this.historyFileName;
        this.historyFileName = historyFileName;
        propertyChanges.firePropertyChange("historyFileName", old, historyFileName);
    }

    /** @return number of artifacts written during this session (read-only simple property). */
    public int getStoredArtifactCount() {
        return storedArtifactCount;
    }

    @Override
    public List<QrHistoryEntry> getHistorySnapshot() {
        try {
            return Collections.unmodifiableList(loadHistory());
        } catch (QrStorageException e) {
            return Collections.emptyList();
        }
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
