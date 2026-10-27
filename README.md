# SCO 310 / SIT 300 - Component Programming project

## `qr-component-suite`: a QR Code generator assembled from interchangeable JavaBeans components

This project builds a working QR Code generator the way component programming requires: the
application is **assembled** from interchangeable JavaBeans components (plus infrastructure
classes) instead of being written as one program. The same compiled component set drives three
front ends - a Swing desktop application, an HTTP service and a CLI - and the encoder or any
renderer can be replaced at run time by editing one XML file.

The QR encoding itself (numeric, alphanumeric, byte, Kanji, ECI and optimal mixed modes, Reed-Solomon
over GF(2^8), all eight mask patterns, versions 1-40) is implemented from **ISO/IEC 18004 with no
third-party dependency**. A separate Micro QR M1-M4 path is exposed through its own interface.
A second encoder component wraps ZXing behind the *same* provided interface, and ZXing is also used
as an independent oracle by the verification suite.

## Quick start

Double-click **`QrStudio.jar`** (Windows / macOS), or run:

```bash
java -jar QrStudio.jar
```

`QrStudio.jar` is the self-contained desktop application: no scripts, no classpath setup and no
separate library folder. It needs **Java 11 or newer**.

Manual build (only needed after changing the source):

```bash
# 1. bootstrap compile (a single javac call; produces the build tool together with the components)
javac -cp lib/core-3.5.3.jar -d out/classes src/main/java/ke/ku/sco310/qr/*/*.java

# 2. compile, verify, compare and package
java -cp out/classes ke.ku.sco310.qr.tools.ProjectBuilder all

# 3. run the desktop application
java -jar QrStudio.jar

# optional HTTP service (needs the source class path)
java -cp "out/classes:lib/*" ke.ku.sco310.qr.web.QrHttpServiceComponent --port 8099
# then: curl 'http://localhost:8099/qr?text=HELLO&format=svg'

# CLI
java -cp "out/classes:lib/*" ke.ku.sco310.qr.cli.QrCli generate --text HELLO --renderer svg --ec M --out hello.svg
java -cp "out/classes:lib/*" ke.ku.sco310.qr.cli.QrCli decode --in hello.svg
java -cp "out/classes:lib/*" ke.ku.sco310.qr.cli.QrCli batch --csv jobs.csv --out-dir out/batch
java -cp "out/classes:lib/*" ke.ku.sco310.qr.cli.QrCli list-components
```

On Windows use `;` instead of `:` in the class path of the HTTP service.

Requirements: **Java 11 or newer** (a JRE is enough to run the JAR; a JDK is needed to build).
Python 3 with `markdown`, `python-docx` and `Pillow` is needed only if you want to regenerate the
report images and the report formats from `docs/report-tooling/`; the project does not use it.

## Where to look

| Path | Contents |
|---|---|
| `QrStudio.jar` | standalone desktop application (double-click or `java -jar`) |
| `docs/Documentation.md` | the full report (master document) |
| `docs/SCO310-QR-Component-Suite-Report.html/.pdf/.docx` | the report in submission formats, with every figure and screenshot embedded |
| `docs/screenshots/` | 22 screenshots of the source code and of the running programs |
| `docs/report-tooling/` | the helpers that produced the figures and screenshots (optional, not part of the build) |
| `docs/diagrams/` | the six component-model, connection-model, deployment and pipeline figures |
| `docs/evidence/` | verification transcript, CSVs, artifact gallery, persisted bean graph |
| `config/assembly.xml` | the deployment descriptor: component selection and property values |
| `src/main/java/ke/ku/sco310/qr/tools/ProjectBuilder.java` | the Java build tool (compile, verify, package) |
| `src/main/java/ke/ku/sco310/qr/` | the components, the engine and the three front ends |
| `src/test/java/ke/ku/sco310/qr/test/` | the in-house verification suite and the ZXing oracle |

## The components

| Role | Component (id) | Provides | Requires |
|---|---|---|---|
| encoder | `PureJavaQrEncoderBean` (`encoder.pure-java.v1`) | `QrCodeEncoder`, `BoundPropertySource` | - |
| encoder | `ZxingQrEncoderAdapterBean` (`encoder.zxing-adapter.v1`) | `QrCodeEncoder`, `BoundPropertySource` | ZXing core (optional) |
| micro-encoder | `MicroQrEncoderBean` (`encoder.micro-qr.v1`) | `QrMicroEncoder`, `BoundPropertySource` | - |
| renderer | `PngRendererBean` (`renderer.png.v1`) | `QrCodeRenderer` | - |
| renderer | `SvgRendererBean` (`renderer.svg.v1`) | `QrCodeRenderer` | - |
| renderer | `AsciiRendererBean` (`renderer.ascii.v1`) | `QrCodeRenderer` | - |
| renderer | `RoundedModuleRendererBean` (`renderer.rounded-png.v1`) | `QrCodeRenderer`, `BoundPropertySource` | - |
| renderer | `PdfRendererBean` (`renderer.pdf.v1`) | `QrCodeRenderer`, `BoundPropertySource` | - |
| decoder | `PureJavaQrDecoderBean` (`decoder.pure-java.v1`) | `QrCodeDecoder`, `BoundPropertySource` | - |
| decoder | `ZxingQrDecoderAdapterBean` (`decoder.zxing-adapter.v1`) | `QrCodeDecoder`, `BoundPropertySource` | ZXing core (optional) |
| repository | `FileSystemRepositoryBean` (`repository.file-system.v1`) | `QrCodeRepository` | file system |
| history | `QrHistoryBean` (`history.bounded-memory.v1`) | indexed + constrained properties | - |
| listener | `AuditLogListenerBean` (`listener.audit-log.v1`) | `QrCodeGenerationListener` | `QrCodeGeneratedEvent` |
| facade | `QrStudioFacadeBean` (`facade.qr-studio.v1`) | `generate(...)`, `renderWith(...)`, delegation | encoder, renderer, repository, history |

## Reusing the components

```java
QrAssembly assembly = new ComponentRegistry().build(AssemblyProfile.load(new File("config/assembly.xml")));
QrGenerationResult result = assembly.generate("https://www.ku.ac.ke/sco310", QrErrorCorrection.QUARTILE);

assembly.replaceRenderer(new SvgRendererBean());    // no caller recompiles
System.out.println(result.getStoredFile());         // written by the repository component
```

## SPI discovery and deployment

Encoders, renderers, decoders and the Micro QR encoder are discovered with Java's
`ServiceLoader`. Their provider files live under `src/main/resources/META-INF/services/`, and
`ProjectBuilder` copies those resources into verification output and packaged JARs. The registry
selects providers by their existing component IDs, so the XML shape in `config/assembly.xml` remains
backward compatible. Repository, history, listener and facade assembly roles continue to use the
existing descriptor and wiring model.

## CLI

`QrCli generate` accepts `--text`, `--renderer`, `--ec` and `--out`; without `--out`, textual
artifacts are printed and binary artifacts are written as `qr-code.<extension>`. `decode` reads PNG,
SVG or ASCII artifacts with the pure-Java decoder. `batch` reads CSV rows in the form
`text,filename,ec,format`, continues after row failures, writes `batch-summary.txt`, and returns
exit code 10 when any row fails. `list-components` prints the live SPI catalogue.

## HTTP endpoints

The HTTP assembly provides `GET /qr?text=...&format=png|svg|ascii&ec=L|M|Q|H`, `GET /components`,
`GET /stats`, `GET /history?limit=n`, `GET /health` and a small HTML form at `/`. The service
limits UTF-8 payload bytes (default 4096, configurable with `setMaxPayloadBytes` or
`--max-payload-bytes`), logs one line per response, reuses configured renderer instances, and can
send `Access-Control-Allow-Origin` with `setCorsOrigin` or `--cors-origin`.

## Note on the third-party library

The ZXing jars in `lib/` are used only as an independent verification oracle and as an optional,
replaceable encoder component; the engine itself has no third-party dependency.
