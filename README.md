## Setup Instructions

To ensure the program works correctly, follow the steps below:

### 1. Add Required Dependencies

Add the following dependencies to your `build.sbt` file:

```scala
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

After adding the dependencies, reload the `build.sbt` project.

---

### 2. Install Aspell

Install **Aspell** (preferably with the Polish dictionary).

Recommended installation path:

```text
C:\Program Files (x86)\Aspell
```

Alternative installation path:

```text
C:\Program Files\Aspell
```

> **Note:**  
> The preferred installation directory is `C:\Program Files (x86)\Aspell`,  
> as recommended by the Aspell developers and commonly used by Windows installers.

---
