package ke.ku.sco310.qr.cli;

import ke.ku.sco310.qr.api.QrErrorCorrection;
import ke.ku.sco310.qr.api.QrGenerationResult;
import ke.ku.sco310.qr.api.RenderedArtifact;
import ke.ku.sco310.qr.facade.AssemblyProfile;
import ke.ku.sco310.qr.facade.BeanPersistence;
import ke.ku.sco310.qr.facade.ComponentRegistry;
import ke.ku.sco310.qr.facade.QrAssembly;
import ke.ku.sco310.qr.tools.BeanInspector;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

/**
 * Command line assembly of the component suite (front end number two).
 *
 * <p>It contains no encoding, drawing or storage logic whatsoever: it parses arguments into an
 * {@link AssemblyProfile}, asks the {@link ComponentRegistry} to build the assembly and calls the
 * facade. Re-implementing none of the behaviour is the whole point of the exercise.</p>
 */
public final class QrCliAssembly {

    private QrCliAssembly() {
    }

    public static void main(String[] args) {
        try {
            System.exit(run(args));
        } catch (Exception e) {
            System.err.println("fatal: " + e);
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static int run(String[] args) throws Exception {
        String text = null;
        String textFile = null;
        String outputDirectory = null;
        String profileFile = AssemblyProfile.DEFAULT_RESOURCE;
        String rendererId = null;
        String encoderId = null;
        QrErrorCorrection level = null;
        Integer moduleSize = null;
        Integer quietZone = null;
        boolean showAssembly = false;
        boolean listComponents = false;
        boolean asciiPreview = false;
        String inspectClass = null;
        String saveAssembly = null;
        String loadAssembly = null;
        boolean quietZoneSet = false;

        for (int i = 0; i < args.length; i++) {
            String option = args[i];
            switch (option) {
                case "--text": text = args[++i]; break;
                case "--file": textFile = args[++i]; break;
                case "--ec": level = QrErrorCorrection.fromLabel(args[++i]); break;
                case "--format": rendererId = rendererIdFor(args[++i]); break;
                case "--encoder": encoderId = encoderIdFor(args[++i]); break;
                case "--module-size": moduleSize = Integer.parseInt(args[++i]); break;
                case "--quiet-zone": quietZone = Integer.parseInt(args[++i]); quietZoneSet = true; break;
                case "--out": outputDirectory = args[++i]; break;
                case "--profile": profileFile = args[++i]; break;
                case "--list-components": listComponents = true; break;
                case "--show-assembly": showAssembly = true; break;
                case "--ascii": asciiPreview = true; break;
                case "--inspect": inspectClass = args[++i]; break;
                case "--save-assembly": saveAssembly = args[++i]; break;
                case "--load-assembly": loadAssembly = args[++i]; break;
                case "--help":
                case "-h": printUsage(); return 0;
                default:
                    System.err.println("Unknown option: " + option);
                    printUsage();
                    return 2;
            }
        }

        if (text == null && textFile != null) {
            text = new String(Files.readAllBytes(new File(textFile).toPath()), StandardCharsets.UTF_8);
        }

        AssemblyProfile profile = loadProfile(profileFile);
        if (rendererId != null) {
            profile.setComponentId("renderer", rendererId);
        }
        if (encoderId != null) {
            profile.setComponentId("encoder", encoderId);
        }
        if (level != null) {
            profile.setProperty("facade", "defaultErrorCorrection", level.getLabel());
            profile.setProperty("encoder", "defaultErrorCorrection", level.getLabel());
        }
        if (moduleSize != null) {
            profile.setProperty("renderer", "moduleSize", String.valueOf(moduleSize));
        }
        if (quietZoneSet) {
            profile.setProperty("renderer", "quietZone", String.valueOf(quietZone));
        }
        if (outputDirectory != null) {
            profile.setProperty("repository", "rootDirectory", outputDirectory);
        }

        ComponentRegistry registry = new ComponentRegistry();

        if (listComponents) {
            printComponentCatalogue(registry);
            return 0;
        }
        if (inspectClass != null) {
            System.out.println(BeanInspector.inspect(Class.forName(inspectClass)));
            return 0;
        }

        System.out.println("=== SCO310 QR Component Suite - command line assembly ===");
        QrAssembly assembly;
        if (loadAssembly != null) {
            System.out.println("[cli] loading persisted bean graph " + loadAssembly);
            assembly = BeanPersistence.load(new File(loadAssembly), profile);
        } else {
            assembly = registry.build(profile);
        }
        System.out.println();
        System.out.println(assembly.describeAssembly());

        if (saveAssembly != null) {
            BeanPersistence.save(assembly, new File(saveAssembly));
            System.out.println("[cli] persisted the assembled bean graph to " + saveAssembly);
        }

        if (text == null) {
            System.err.println("Nothing to do: supply --text or --file (see --help)");
            return 2;
        }

        QrGenerationResult result = assembly.generate(text, level);
        System.out.println();
        System.out.println("--- generation result ---");
        System.out.println("payload           : " + preview(text));
        System.out.println("symbol            : v" + result.getSymbol().getVersion()
                + " (" + result.getSymbol().getSize() + "x" + result.getSymbol().getSize() + " modules)"
                + ", level " + result.getSymbol().getErrorCorrection().getLabel()
                + ", mode " + result.getSymbol().getEncodingMode()
                + ", mask " + result.getSymbol().getMask());
        System.out.println("codewords         : " + result.getSymbol().getDataCodewordCount()
                + " data + " + result.getSymbol().getEccCodewordCount() + " error-correction");
        System.out.println("artifact          : " + result.getArtifact().getFormatName()
                + ", " + result.getArtifact().getWidth() + "x" + result.getArtifact().getHeight()
                + ", " + result.getArtifact().getSizeInBytes() + " bytes");
        System.out.println("stored            : " + (result.getStoredFile() == null
                ? "(not stored)" : result.getStoredFile().getAbsolutePath()));
        System.out.println("elapsed           : " + result.getDurationMillis() + " ms");
        System.out.println("history entries   : " + assembly.getHistory().getEntryCount()
                + " (capacity " + assembly.getHistory().getMaxEntries() + ")");
        System.out.println("audit events      : " + assembly.getListener().getProcessedEventCount());

        if (asciiPreview) {
            System.out.println();
            System.out.println(result.getSymbol().toAsciiArt());
        }
        if (showAssembly && result.getArtifact().isTextual()
                && !"ASCII".equals(result.getArtifact().getFormatName())) {
            System.out.println();
            System.out.println(result.getArtifact().getContentAsText());
        }
        if (result.getStoredFile() != null) {
            System.out.println();
            System.out.println("Reproduce with: java -cp out/classes ke.ku.sco310.qr.cli.QrCliAssembly --text \""
                    + preview(text) + "\" --format " + result.getArtifact().getFormatName().toLowerCase(Locale.ROOT)
                    + " --ec " + result.getSymbol().getErrorCorrection().getLabel());
        }
        return 0;
    }

    private static AssemblyProfile loadProfile(String profileFile) {
        File file = new File(profileFile);
        if (!file.isFile()) {
            System.out.println("[cli] profile " + profileFile + " not found, using built-in defaults");
            return new AssemblyProfile();
        }
        try {
            AssemblyProfile profile = AssemblyProfile.load(file);
            System.out.println("[cli] loaded assembly profile from " + file.getAbsolutePath());
            return profile;
        } catch (Exception e) {
            System.out.println("[cli] could not read " + profileFile + " (" + e.getMessage()
                    + "), using built-in defaults");
            return new AssemblyProfile();
        }
    }

    private static String rendererIdFor(String format) {
        switch (format.toLowerCase(Locale.ROOT)) {
            case "png": return "renderer.png.v1";
            case "svg": return "renderer.svg.v1";
            case "ascii":
            case "text": return "renderer.ascii.v1";
            default: throw new IllegalArgumentException("Unknown format '" + format + "'; use png, svg or ascii");
        }
    }

    private static String encoderIdFor(String encoder) {
        switch (encoder.toLowerCase(Locale.ROOT)) {
            case "in-house":
            case "native":
            case "pure-java": return "encoder.pure-java.v1";
            case "zxing": return "encoder.zxing-adapter.v1";
            default: throw new IllegalArgumentException(
                    "Unknown encoder '" + encoder + "'; use pure-java or zxing");
        }
    }

    private static String preview(String text) {
        String flat = text.replaceAll("\\s+", " ");
        return flat.length() <= 60 ? flat : flat.substring(0, 60) + "...";
    }

    private static void printComponentCatalogue(ComponentRegistry registry) {
        System.out.println("Available components in this deployment:");
        System.out.printf("%-11s %-30s %-14s %-46s %s%n",
                "ROLE", "COMPONENT ID", "STATE", "PROVIDED INTERFACE", "REQUIRED INTERFACE");
        System.out.println("-".repeat(150));
        for (ke.ku.sco310.qr.facade.ComponentDescriptor descriptor : registry.listComponents()) {
            System.out.println(descriptor.toTableRow());
            if (!descriptor.isAvailable()) {
                System.out.println("            reason: " + descriptor.getUnavailableReason());
            }
        }
    }

    private static void printUsage() {
        System.out.println("SCO310 QR Code generator - component based assembly");
        System.out.println();
        System.out.println("Usage: qr-cli --text <payload> [options]");
        System.out.println();
        System.out.println("  --text <payload>        payload to encode");
        System.out.println("  --file <path>           read the payload from a UTF-8 text file");
        System.out.println("  --ec <L|M|Q|H>          error-correction level (default from profile)");
        System.out.println("  --format <png|svg|ascii>  renderer component to use");
        System.out.println("  --encoder <pure-java|zxing>  encoder component to use");
        System.out.println("  --module-size <px>      size of one module in pixels");
        System.out.println("  --quiet-zone <modules>  light margin around the symbol");
        System.out.println("  --out <directory>       storage directory of the repository component");
        System.out.println("  --profile <path>        assembly profile (default config/assembly.xml)");
        System.out.println("  --list-components       print the component catalogue and exit");
        System.out.println("  --inspect <class>       introspection dump of a bean class and exit");
        System.out.println("  --show-assembly         print the XML bean graph after generation");
        System.out.println("  --save-assembly <path>  persist the assembled bean graph as XML");
        System.out.println("  --load-assembly <path>  rebuild the assembly from a persisted XML graph");
        System.out.println("  --ascii                 print an ASCII preview of the symbol");
        System.out.println("  --help                  this message");
    }

    /** Keeps the unused import warning away while documenting the artifact type used above. */
    static RenderedArtifact unusedDocumentationHook() {
        return null;
    }
}
