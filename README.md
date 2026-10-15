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
| Code | 48 Java source files, ~6 500 lines, zero warnings under `-Xlint:all` |

## Quick start

```bash
./build.sh all        # clean, compile, verify, package -> out/dist/qr-component-suite-1.0.0.jar
./run-tests.sh        # verification only (rewrites docs/evidence/)
./run-cli.sh --text "https://www.ku.ac.ke/sco310" --ec M --format png
./run-cli.sh --list-components
./run-gui.sh          # desktop assembly
./run-web.sh --port 8099   # then: curl 'http://localhost:8099/qr?text=HELLO&format=svg'
./build.sh docs       # rebuild the HTML / DOCX / PDF versions of the report
```

Requirements: a JDK 11 or newer (`javac` and `jar` only — no Maven or Gradle needed).
Python 3 with `markdown`, `python-docx`, `Pillow` is needed only to regenerate the documentation
images and the report formats.

## Where to look

| Path | Contents |
|---|---|
| `docs/Documentation.md` | the full report (master document) |
| `docs/SCO310-QR-Component-Suite-Report.html/.pdf/.docx` | the report in submission formats, with every figure and screenshot embedded |
| `docs/screenshots/` | 27 screenshots of the source code and of the running programs |
| `docs/diagrams/` | the six component-model, connection-model, deployment and pipeline figures |
| `docs/evidence/` | verification transcript, CSVs, artifact gallery, persisted bean graph |
| `config/assembly.xml` | the deployment descriptor: component selection and property values |
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

## Red-team / security note

Nothing in this repository performs offensive operations; the security-adjacent tooling
(`tools/MaskComparison.java`) only compares mask selection against ZXing. ZXing jars in `lib/` are
used for verification and as an optional replaceable component.
