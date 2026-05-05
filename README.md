# Scala Text Editor

A rich text editor built in Scala using Scala Swing. Supports multiple tabs, text formatting, spell checking via Aspell, and file export to RTF, PDF, and DOCX formats.

## Features

- Multi-tab document editing
- Text formatting: bold, italic, underline, font size, font family, text color
- Undo / Redo support
- Find & Replace with regex support
- Spell checking via Aspell (Polish dictionary supported)
- Save / Open files
- Export to: RTF, PDF, DOCX
- Clipboard operations: cut, copy, paste
- Keyboard shortcuts

## Requirements

- JDK 11 or newer
- Scala 2.13 / 3.x
- SBT (Scala Build Tool)
- [Aspell](http://aspell.net/) installed at `C:\Program Files (x86)\Aspell`

## Setup

### 1. SBT Dependencies

Add the following to your `build.sbt`:

```scala
// Project dependencies
libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-swing" % "3.0.0",
  "com.itextpdf" % "itextpdf" % "5.5.13.4",
  "org.apache.poi" % "poi" % "5.3.0",
  "org.apache.poi" % "poi-ooxml" % "5.3.0",
  "com.itextpdf" % "itext-pdfa" % "5.5.13.3",
  "com.itextpdf" % "itext-xtra" % "5.5.13.4",
  "com.typesafe.play" %% "play-json" % "2.10.5",
  "org.slf4j" % "slf4j-api" % "2.0.12",
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  "net.java.dev.jna" % "jna" % "5.14.0",
  "net.java.dev.jna" % "jna-platform" % "5.14.0"
)
```

After adding the dependencies, run `reload` in SBT to apply the changes.

### 2. Aspell Spell Checker

Download and install [Aspell](http://aspell.net/) (preferably with the Polish dictionary).

The program expects Aspell to be located at:

```
C:\Program Files (x86)\Aspell
```

This is the default installation path recommended by the Aspell authors and used by the Windows installer. Alternatively, you can install it to `C:\Program Files`, but `C:\Program Files (x86)\Aspell` is preferred.

## Running

```bash
sbt run
```

## Project Structure

```
.
├── main.scala       # Full application source code
├── build.sbt        # SBT build configuration
├── README.md        # English documentation
└── README_PL.md     # Polish documentation
```

## Technologies Used

| Library | Purpose |
|---|---|
| scala-swing | GUI framework |
| iTextPDF | PDF export |
| Apache POI | DOCX export |
| Play JSON | JSON handling |
| Logback / SLF4J | Logging |
| JNA | Native system integration |
| Aspell | Spell checking |

## License

Academic project — University of Nicolaus Copernicus (UMK), Functional Programming course.
