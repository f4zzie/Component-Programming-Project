# SCO 310 / SIT 300 — Component Programming project

## `qr-component-suite`: a QR Code generator assembled from interchangeable JavaBeans components

This project builds a working QR Code generator the way component programming requires: the
application is **assembled** from nine independent JavaBeans components (plus two infrastructure
classes) instead of being written as one program. The same compiled component set drives three
different front ends — a Swing desktop application, a command-line tool and an HTTP service — and
the encoder or any renderer can be replaced at run time by editing one XML file.

The QR encoding itself (numeric, alphanumeric and byte modes, Reed–Solomon over GF(2⁸), all eight
mask patterns, versions 1–40) is implemented from **ISO/IEC 18004 with no third-party dependency**.
A second encoder component wraps ZXing behind the *same* provided interface, and ZXing is also used
as an independent oracle by the verification suite.

| | |
|---|---|
| Automated verification | **108 checks, 108 passed, 0 failed** |
| Parameter tables vs ZXing | 160/160 version-level combinations identical |
| Round-trip decode | 60/60 payloads recovered exactly (accents, CJK, emoji) |
| Capacity sweep | 160/160 maximum-capacity symbols decode |
| Code | 49 Java source files, ~6 800 lines, zero warnings under `-Xlint:all` |

## Quick start

There are no shell scripts anywhere in this project: the build tool is a Java class, and every
other entry point is an ordinary `main` class.

```bash
# 1. bootstrap compile (a single javac call; produces the build tool together with the components)
javac -cp lib/core-3.5.3.jar -d out/classes src/main/java/ke/ku/sco310/qr/*/*.java

# 2. compile, verify, compare and package - all in Java
java -cp out/classes ke.ku.sco310.qr.tools.ProjectBuilder all

# running the three assemblies
java -cp out/classes:lib/* ke.ku.sco310.qr.ui.QrStudioGuiAssembly            # desktop
java -cp out/classes:lib/* ke.ku.sco310.qr.cli.QrCliAssembly --list-components   # components
java -cp out/classes:lib/* ke.ku.sco310.qr.cli.QrCliAssembly --text "SCO310" --ec H --format png
java -cp out/classes:lib/* ke.ku.sco310.qr.web.QrHttpServiceComponent --port 8099
# then: curl 'http://localhost:8099/qr?text=HELLO&format=svg'
```

On Windows use `;` instead of `:` in the class path.

Requirements: a **JDK 11 or newer** for the project itself. Python 3 with `markdown`,
`python-docx` and `Pillow` is needed only if you want to regenerate the report images and the
report formats from `docs/report-tooling/`; the project does not use it.

## Where to look

| Path | Contents |
|---|---|
| `docs/Documentation.md` | the full report (master document) |
| `docs/SCO310-QR-Component-Suite-Report.html/.pdf/.docx` | the report in submission formats, with every figure and screenshot embedded |
| `docs/screenshots/` | 27 screenshots of the source code and of the running programs |
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
| encoder | `PureJavaQrEncoderBean` (`encoder.pure-java.v1`) | `QrCodeEncoder`, `BoundPropertySource` | — |
| encoder | `ZxingQrEncoderAdapterBean` (`encoder.zxing-adapter.v1`) | `QrCodeEncoder`, `BoundPropertySource` | ZXing core (optional) |
| renderer | `PngRendererBean` (`renderer.png.v1`) | `QrCodeRenderer` | — |
| renderer | `SvgRendererBean` (`renderer.svg.v1`) | `QrCodeRenderer` | — |
| renderer | `AsciiRendererBean` (`renderer.ascii.v1`) | `QrCodeRenderer` | — |
| repository | `FileSystemRepositoryBean` (`repository.file-system.v1`) | `QrCodeRepository` | file system |
| history | `QrHistoryBean` (`history.bounded-memory.v1`) | indexed + constrained properties | — |
| listener | `AuditLogListenerBean` (`listener.audit-log.v1`) | `QrCodeGenerationListener` | `QrCodeGeneratedEvent` |
| facade | `QrStudioFacadeBean` (`facade.qr-studio.v1`) | `generate(...)`, `renderWith(...)`, delegation | encoder, renderer, repository, history |

## Reusing the components

```java
QrAssembly assembly = new ComponentRegistry().build(AssemblyProfile.load(new File("config/assembly.xml")));
QrGenerationResult result = assembly.generate("https://www.ku.ac.ke/sco310", QrErrorCorrection.QUARTILE);

assembly.replaceRenderer(new SvgRendererBean());    // no caller recompiles
System.out.println(result.getStoredFile());         // written by the repository component
```

## Note on the third-party library

Nothing in this project performs offensive operations. The ZXing jars in `lib/` are used only as an
independent verification oracle and as an optional, replaceable encoder component; the engine
itself has no third-party dependency.
