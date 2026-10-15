package ke.ku.sco310.qr.facade;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * XML persistence of an assembled bean graph (chapter 3, "Persistence" and "Overview of Bean
 * Builder").
 *
 * <p>Contrast with the repository component: the repository serialises <em>data</em> (history
 * records) with Java serialization, while this class persists the <em>structure and design-time
 * configuration</em> of the application as XML, which is readable, diff-able and editable by a
 * human.</p>
 */
public final class BeanPersistence {

    private BeanPersistence() {
    }

    /**
     * Writes the assembled bean graph to an XML file.
     *
     * @param assembly assembly to persist
     * @param file     destination file
     * @throws IOException when the file cannot be written
     */
    public static void save(QrAssembly assembly, File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (XMLEncoder encoder = new XMLEncoder(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            encoder.writeObject(AssemblyGraphBean.from(assembly));
        }
    }

    /**
     * Rebuilds an assembly from an XML file produced by {@link #save(QrAssembly, File)}.
     *
     * @param file    descriptor file
     * @param profile profile describing the roles (component ids are informational after loading)
     * @return the reconstructed assembly
     * @throws IOException when the file cannot be read or does not contain a bean graph
     */
    public static QrAssembly load(File file, AssemblyProfile profile) throws IOException {
        try (XMLDecoder decoder = new XMLDecoder(
                new BufferedInputStream(new FileInputStream(file)))) {
            Object root = decoder.readObject();
            if (!(root instanceof AssemblyGraphBean)) {
                throw new IOException("The XML file does not contain an assembly graph: " + file);
            }
            return ((AssemblyGraphBean) root).toAssembly(profile);
        }
    }
}
