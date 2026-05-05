# Edytor Tekstu w Scali

Bogaty edytor tekstu napisany w Scali z wykorzystaniem Scala Swing. Obsługuje wiele zakładek, formatowanie tekstu, sprawdzanie pisowni przez Aspell oraz eksport plików do formatów RTF, PDF i DOCX.

## Funkcjonalnosci

- Edycja dokumentow w wielu zakładkach
- Formatowanie tekstu: pogrubienie, kursywa, podkreslenie, rozmiar czcionki, rodzina czcionek, kolor tekstu
- Obsługa cofania / ponawiania operacji (Undo / Redo)
- Znajdz i zamien z obsługa wyrazen regularnych
- Sprawdzanie pisowni przez Aspell (obsługa słownika polskiego)
- Zapisywanie / otwieranie plikow
- Eksport do: RTF, PDF, DOCX
- Operacje schowka: wytnij, kopiuj, wklej
- Skroty klawiszowe

## Wymagania

- JDK 11 lub nowszy
- Scala 2.13 / 3.x
- SBT (Scala Build Tool)
- [Aspell](http://aspell.net/) zainstalowany w `C:\Program Files (x86)\Aspell`

## Konfiguracja

### 1. Zależności SBT

Dodaj ponizsze zależnosci do pliku `build.sbt`:

```scala
// Zależności projektu
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

Po dodaniu zależnosci wykonaj `reload` w SBT, aby zastosowac zmiany.

### 2. Sprawdzanie pisowni Aspell

Pobierz i zainstaluj [Aspell](http://aspell.net/) (najlepiej z polskim słownikiem).

Program oczekuje, ze Aspell bedzie znajdował sie pod sciezką:

```
C:\Program Files (x86)\Aspell
```

Jest to domyslna sciezka instalacji rekomendowana przez tworców Aspella i uzywana przez instalator Windows. Mozna takze zainstalowac go w `C:\Program Files`, jednak `C:\Program Files (x86)\Aspell` jest preferowane.

## Uruchomienie

```bash
sbt run
```

## Struktura projektu

```
.
├── main.scala       # Pełny kod zrodłowy aplikacji
├── build.sbt        # Konfiguracja budowania SBT
├── README.md        # Dokumentacja po angielsku
└── README_PL.md     # Dokumentacja po polsku
```

## Uzyte technologie

| Biblioteka | Zastosowanie |
|---|---|
| scala-swing | Framework GUI |
| iTextPDF | Eksport do PDF |
| Apache POI | Eksport do DOCX |
| Play JSON | Obsługa formatu JSON |
| Logback / SLF4J | Logowanie |
| JNA | Integracja z systemem natywnym |
| Aspell | Sprawdzanie pisowni |

## Licencja

Projekt akademicki — Uniwersytet Mikołaja Kopernika (UMK), kierunek Programowanie Funkcyjne.
