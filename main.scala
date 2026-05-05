import scala.swing.*
import scala.swing.event.*
import javax.swing.{AbstractAction, JFileChooser, JTextPane, KeyStroke, undo}
import javax.swing.text.{SimpleAttributeSet, StyleConstants, StyleContext, StyledDocument}
import java.io.*
import javax.swing.Icon
import javax.swing.undo.UndoManager
import scala.swing.Dialog
import java.awt.{Color, Dimension, Graphics, Graphics2D, Toolkit}
import java.awt.event.{ActionEvent, InputEvent, KeyEvent}
import java.awt.datatransfer.*
import java.util.regex.Pattern
import javax.swing.text.rtf.RTFEditorKit
import scala.swing.event.{Event, SelectionChanged}
import scala.swing.TabbedPane
import scala.swing._
import javax.swing.text._
import java.awt.Color
import java.io._

// Klasa reprezentująca pojedynczą zakładkę
class EditorTab(val title: String = "Nowy dokument") {
  val textPane = new StyledTextPane()
  var filePath: Option[String] = None
  var isModified: Boolean = false
}

// Klasa reprezentująca ikonę koloru
class ColorIcon(color: Color, width: Int = 16, height: Int = 16) extends Icon {
  def getIconWidth: Int = width
  def getIconHeight: Int = height

  def paintIcon(c: java.awt.Component, g: Graphics, x: Int, y: Int): Unit = {
    val g2d = g.asInstanceOf[Graphics2D]
    g2d.setColor(color)
    g2d.fillOval(x, y, width - 1, height - 1)
    g2d.setColor(Color.BLACK)
    g2d.drawOval(x, y, width - 1, height - 1)
  }
}

// Wrapper dla JTextPane z rozszerzoną funkcjonalnością stylizacji
class StyledTextPane extends Component {
  // Nadpisanie (override) pola peer jako "leniwe" (lazy) // lazy oznacza, że JTextPane zostanie utworzony dopiero przy pierwszym użyciu
  // Jest to wzorzec lazy initialization - oszczędza pamięć, gdy komponent nie jest używany
  override lazy val peer: JTextPane = new JTextPane()

  // Metoda pomocnicza zwracająca dokument z formatowaniem // StyledDocument to interfejs reprezentujący dokument, który może zawierać formatowany tekst
  // getStyledDocument zwraca obiekt umożliwiający manipulację formatowaniem tekstu // (np. kolory, czcionki, style) w dokumencie
  def getStyledDoc: StyledDocument = peer.getStyledDocument

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  // UndoManager do obsługi cofania zmian . Utworzenie instancji UndoManager, która będzie zarządzać historią zmian w dokumencie
  // UndoManager automatycznie śledzi wszystkie edycje i umożliwia ich cofanie/ponowne wykonanie
  val undoManager = new UndoManager()

  // Dodanie listenera do dokumentu, który będzie przechwytywał wszystkie zdarzenia edycji
  // Każda zmiana w dokumencie (wpisanie tekstu, formatowanie, etc.) zostanie zarejestrowana
  getStyledDoc.addUndoableEditListener(undoManager)

  // Definicja akcji cofania (Undo)
  // AbstractAction to klasa bazowa Swinga do tworzenia akcji, które mogą być przypisane do różnych zdarzeń
  val undoAction = new AbstractAction("Undo") {
    override def actionPerformed(e: ActionEvent): Unit = {
      // Sprawdzenie czy istnieje jakaś akcja do cofnięcia
      if (undoManager.canUndo) undoManager.undo()
    }
  }

  // Konfiguracja mapy wejściowej komponentu // WHEN_FOCUSED oznacza, że skrót będzie działał tylko gdy komponent ma focus
  // KeyStroke.getKeyStroke tworzy kombinację klawiszy (tutaj Ctrl+Z)
  peer.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
    .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "Undo")

  // Powiązanie nazwy akcji ("Undo") z faktyczną implementacją (undoAction)
  // ActionMap mapuje nazwy akcji na ich implementacje
  peer.getActionMap().put("Undo", undoAction)

  // Definicja akcji ponownego wykonania (Redo)
  // Działa analogicznie do Undo, ale przywraca cofnięte zmiany
  val redoAction = new AbstractAction("Redo") {
    override def actionPerformed(e: ActionEvent): Unit = {
      // Sprawdzenie czy istnieje jakaś akcja do ponownego wykonania
      if (undoManager.canRedo) undoManager.redo()
    }
  }

  // Konfiguracja skrótu klawiszowego dla Redo (Ctrl+Y)
  // Analogiczna do konfiguracji Undo
  peer.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
    .put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "Redo")

  // Powiązanie akcji Redo z jej implementacją w ActionMap
  peer.getActionMap().put("Redo", redoAction)

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  // Style dla tekstu
  private val styleContext = new StyleContext()
  private def createStyle(color: Color = Color.BLACK,  // DOMYSLNY KOLOR CZCIONKI
                          fontSize: Int = 12, // DOMYSLNY ROZMIAR CZCIONKI
                          underline: Boolean = false): SimpleAttributeSet = {  // DOMYSLNIE BRAK PODKRESLENIA
    val style = new SimpleAttributeSet()
    StyleConstants.setForeground(style, color)
    StyleConstants.setFontSize(style, fontSize)
    StyleConstants.setUnderline(style, underline)
    style
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  // Dodanie obsługi schowka
  private val clipboard = Toolkit.getDefaultToolkit.getSystemClipboard

  // Akcja kopiowania
  val copyAction = new AbstractAction("Copy") {
    override def actionPerformed(e: ActionEvent): Unit = {
      if (peer.getSelectedText != null) {
        val selection = peer.getSelectedText
        val start = peer.getSelectionStart
        val end = peer.getSelectionEnd

        val rtfKit = new RTFEditorKit()
        // Tworzymy nowy dokument tylko dla zaznaczonego tekstu
        val tempDoc = new javax.swing.text.DefaultStyledDocument()

        // Kopiujemy zaznaczony fragment z zachowaniem formatowania
        val source = peer.getStyledDocument
        tempDoc.insertString(0, selection, null)

        // Kopiujemy atrybuty dla każdego znaku
        for (i <- 0 until selection.length) {
          val attrs = source.getCharacterElement(start + i).getAttributes
          tempDoc.setCharacterAttributes(i, 1, attrs, true)
        }

        val baos = new ByteArrayOutputStream()

        try {
          rtfKit.write(baos, tempDoc, 0, tempDoc.getLength)
          val rtfContent = baos.toString("UTF-8")

          val transferable = new Transferable {
            def getTransferDataFlavors: Array[DataFlavor] = Array(
              DataFlavor.stringFlavor,
              new DataFlavor("text/rtf", "RTF")
            )

            def isDataFlavorSupported(flavor: DataFlavor): Boolean =
              flavor.equals(DataFlavor.stringFlavor) ||
                flavor.getPrimaryType.equals("text") && flavor.getSubType.equals("rtf")

            def getTransferData(flavor: DataFlavor): AnyRef = {
              if (flavor.equals(DataFlavor.stringFlavor)) selection
              else if (flavor.getPrimaryType.equals("text") && flavor.getSubType.equals("rtf")) rtfContent
              else throw new UnsupportedFlavorException(flavor)
            }
          }

          clipboard.setContents(transferable, null)
        } catch {
          case e: Exception =>
            e.printStackTrace()
            peer.copy()
        }
      }
    }
  }

  // Akcja wklejania
  val pasteAction = new AbstractAction("Paste") {
    override def actionPerformed(e: ActionEvent): Unit = {
      try {
        val contents = clipboard.getContents(null)
        if (contents != null) {
          // Rozpocznij złożoną operację edycji
          val compoundEdit = new undo.CompoundEdit()

          val rtfFlavor = new DataFlavor("text/rtf", "RTF")

          if (contents.isDataFlavorSupported(rtfFlavor)) {
            // Wklej sformatowany tekst RTF
            val rtfData = contents.getTransferData(rtfFlavor).asInstanceOf[String]
            val rtfKit = new RTFEditorKit()

            // Stwórz tymczasowy dokument do przechowania wklejanej zawartości
            val tempDoc = new javax.swing.text.DefaultStyledDocument()
            rtfKit.read(new ByteArrayInputStream(rtfData.getBytes("UTF-8")), tempDoc, 0)

            // Pobierz pozycję kursora
            val caretPosition = peer.getCaretPosition
            val targetDoc = peer.getStyledDocument

            // Skopiuj zawartość z tempDoc do właściwego dokumentu
            val docLength = tempDoc.getLength
            for (i <- 0 until docLength) {
              val char = tempDoc.getText(i, 1)
              val attrs = tempDoc.getCharacterElement(i).getAttributes

              // Stwórz nowy zestaw atrybutów bez czarnego tła
              val newAttrs = new SimpleAttributeSet(attrs)

              // Sprawdź, czy tło nie jest zdefiniowane w źródłowym tekście
              if (StyleConstants.getBackground(attrs) == null ||
                StyleConstants.getBackground(attrs) == Color.BLACK) {
                // Ustaw przezroczyste (lub białe) tło
                StyleConstants.setBackground(newAttrs, Color.WHITE)
              }

              // Wstaw tekst z nowymi atrybutami jako część złożonej operacji
              targetDoc.insertString(caretPosition + i, char, newAttrs)
            }

          } else if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            // Dla zwykłego tekstu, wklej bez czarnego tła
            val text = contents.getTransferData(DataFlavor.stringFlavor).asInstanceOf[String]
            val style = new SimpleAttributeSet()
            StyleConstants.setBackground(style, Color.WHITE)
            val caretPosition = peer.getCaretPosition
            peer.getStyledDocument.insertString(caretPosition, text, style)
          }

          // Zakończ złożoną operację edycji i dodaj ją do historii zmian
          compoundEdit.end()
          undoManager.addEdit(compoundEdit)
        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
          // Fallback do standardowego wklejania
          peer.paste()
      }
    }
  }

  // Dodanie skrótów klawiszowych
  peer.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
    .put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "CustomCopy")
  peer.getActionMap().put("CustomCopy", copyAction)

  peer.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
    .put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "CustomPaste")
  peer.getActionMap().put("CustomPaste", pasteAction)

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  // DO ASPELLA            !!!!!!!!!!!!!!!!!!!!!!!!

  // Tu ustawiamy kolor podkreślenia błedu w tekscie. W tym przypadku to przezroczysty czerwony
  private val spellingHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 0, 0, 50))

  peer.addMouseListener(new java.awt.event.MouseAdapter {
    override def mouseClicked(e: java.awt.event.MouseEvent): Unit = {
      if (e.getButton == java.awt.event.MouseEvent.BUTTON3) { // Prawy przycisk myszy
        val pos = peer.viewToModel2D(e.getPoint)
        if (pos >= 0) {
          // Sprawdź, czy kliknięcie jest na podświetlonym słowie
          val highlights = peer.getHighlighter.getHighlights
          // Użyj exists zamiast for z break
          if (highlights.exists(highlight =>
            pos >= highlight.getStartOffset && pos <= highlight.getEndOffset
          )) {
            showSpellingSuggestionsAt(pos)
          }
        }
      }
    }
  })

  def checkSpelling(): Unit = {
    println("Rozpoczynam sprawdzanie pisowni...")
    val text = peer.getText
    peer.getHighlighter.removeAllHighlights()

    var incorrectWords = List[(String, Int, Int)]()
    val wordPattern = "\\b[\\p{L}]+\\b".r

    println(s"Tekst do sprawdzenia: $text")

    for (matchResult <- wordPattern.findAllMatchIn(text)) {
      val word = matchResult.group(0)
      println(s"Sprawdzam słowo: $word")
      val isCorrect = AspellChecker.checkWord(word)
      println(s"Wynik sprawdzania słowa '$word': ${if (isCorrect) "poprawne" else "niepoprawne"}")

      if (!isCorrect) {
        println(s"Znaleziono błędne słowo: $word na pozycji ${matchResult.start}")
        try {
          peer.getHighlighter.addHighlight(
            matchResult.start,
            matchResult.end,
            spellingHighlightPainter
          )
          incorrectWords = (word, matchResult.start, matchResult.end) :: incorrectWords
        } catch {
          case e: Exception =>
            println(s"Błąd podczas podświetlania: ${e.getMessage}")
        }
      }
    }

    println(s"Znalezione błędne słowa: ${incorrectWords.map(_._1).mkString(", ")}")

    if (incorrectWords.nonEmpty) {
      Dialog.showMessage(
        null,
        s"Znalezione błędy:\n${incorrectWords.map(_._1).mkString("\n")}",
        "Błędy pisowni",
        Dialog.Message.Warning
      )
    } else {
      Dialog.showMessage(
        null,
        "Nie znaleziono błędów pisowni",
        "Sprawdzanie pisowni",
        Dialog.Message.Info
      )
    }
  }

  // dla kazdego blednego slowa mozna znalezc dzieki temu sugestie
  def showSpellingSuggestionsAt(position: Int): Unit = {
    val text = peer.getText
    val wordStart = findWordStart(text, position)
    val wordEnd = findWordEnd(text, position)
    val textPane = this

    if (wordStart >= 0 && wordEnd > wordStart) {
      val word = text.substring(wordStart, wordEnd)
      println(s"Sprawdzanie sugestii dla słowa: $word") // Dodaj dla debugowania
      val suggestions = AspellChecker.getSuggestions(word)
      println(s"Otrzymane sugestie: ${suggestions.mkString(", ")}") // Dodaj dla debugowania

      if (suggestions.nonEmpty) {
        Dialog.showMessage(
          null,
          s"Sugestie dla słowa '$word':\n${suggestions.mkString("\n")}",
          "Sugestie",
          Dialog.Message.Info
        )
      } else {
        Dialog.showMessage(
          null,
          s"Brak sugestii dla słowa '$word'",
          "Sugestie",
          Dialog.Message.Info
        )
      }
    }
  }

  // Znalezienie początku słowa
  private def findWordStart(text: String, position: Int): Int = {
    var pos = position
    while (pos > 0 && Character.isLetterOrDigit(text.charAt(pos - 1))) {
      pos -= 1
    }
    pos
  }

  // znalezienie konca slowa
  private def findWordEnd(text: String, position: Int): Int = {
    var pos = position
    while (pos < text.length && Character.isLetterOrDigit(text.charAt(pos))) {
      pos += 1
    }
    pos
  }

}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

// Obserwuje zdarzenia w aplikacji //Wyłapuje moment, gdy użytkownik zmienia zakładkę //Pozwala bezpiecznie reagować na taką zmianę
object TabChanged {
  // Ta metoda jest używana w dopasowaniu wzorców (pattern matching) // Sprawdza czy zdarzenie (Event) jest zmianą zakładki i jeśli tak,
  // zwraca informację o komponencie zakładek (TabbedPane)
  def unapply(e: Event): Option[TabbedPane] = e match {
    // Sprawdza czy zdarzenie jest typu SelectionChanged (zmiana wyboru)
    case event: SelectionChanged =>
      event.source match {
        // Sprawdza czy źródło zdarzenia to TabbedPane (panel z zakładkami)
        case tabbedPane: TabbedPane => Some(tabbedPane)
        case _ => None  // Jeśli nie, zwraca None
      }
    case _ => None  // Jeśli to nie jest SelectionChanged, zwraca None
  }
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

// kluczowy element edytora pozwalający na pracę z wieloma dokumentami jednocześnie
class EditorTabbedPane extends TabbedPane {
  // Lista przechowująca wszystkie zakładki edytora
  private var tabs = List[EditorTab]()
  // Indeks aktualnie wybranej zakładki
  private var currentTabIndex = 0

  // Metoda dodająca nową zakładkę
  def addNewTab(tab: EditorTab): Unit = {
    pages += new TabbedPane.Page(tab.title, new ScrollPane(tab.textPane))
    tabs = tabs :+ tab
    selection.index = tabs.length - 1
    currentTabIndex = tabs.length - 1
  }

  // Metoda usuwająca zakładkę o podanym indeksie
  def removeTab(index: Int): Unit = {
    if (index >= 0 && index < tabs.length) {
      pages.remove(index)
      tabs = tabs.patch(index, Nil, 1)
      if (tabs.isEmpty) {
        addNewTab(new EditorTab())
      }
      currentTabIndex = selection.index
    }
  }

  // Metoda zwracająca aktualnie wybraną zakładkę
  def getCurrentTab: Option[EditorTab] = {
    if (tabs.nonEmpty && currentTabIndex >= 0 && currentTabIndex < tabs.length) {
      Some(tabs(currentTabIndex))
    } else None
  }

  // Aktualizacja indeksu aktualnej zakładki
  def updateCurrentTabIndex(): Unit = {
    currentTabIndex = selection.index
  }

  // Zwraca liczbę wszystkich zakładek
  def getTabCount: Int = tabs.length
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

// główna klasa aplikacji edytora tekstu. Jest to punkt startowy całego programu.
// Nadpisujemy SimpleSwingApplication dlatego nie musimy mieć klasy "main" oraz w niej wywoływac odpiednie obiekty i funkje
object TextEditor extends SimpleSwingApplication {
  def top: MainFrame = new MainFrame {
    title = "Edytor tekstowy"

    // Lista zakładek
    private var tabs = List[EditorTab]()
    private var currentTabIndex = 0

    // Zmiana na własną klasę EditorTabbedPane
    val tabbedPane = new EditorTabbedPane()

    // Aktualizacja tytułu okna
    def updateTitle(): Unit = {
      tabbedPane.getCurrentTab foreach { tab =>
        val modifiedMark = if (tab.isModified) "*" else ""
        val fileName = tab.filePath.map(path => new File(path).getName).getOrElse(tab.title)
        title = s"$fileName$modifiedMark - Edytor tekstowy"
      }
    }

    // Nasłuchiwanie zmiany zakładek
    listenTo(tabbedPane.selection)
    reactions += {
      case SelectionChanged(`tabbedPane`) =>
        tabbedPane.updateCurrentTabIndex()
        updateTitle()
    }


    // Definicja dostępnych kolorów czcionek
    val availableColors = List(
      ("Czarny", Color.BLACK),
      ("Czerwony", Color.RED),
      ("Niebieski", Color.BLUE),
      ("Zielony", Color.GREEN),
      ("Żółty", Color.YELLOW),
      ("Różowy", Color.PINK),
      ("Pomarańczowy", Color.ORANGE),
      ("Fioletowy", new Color(128, 0, 128)),
      ("Brązowy", new Color(139, 69, 19)),
      ("Szary", Color.GRAY)
    )

    // Definicja dostępnych stylów czcionek
    val availableFonts = List(
      "Arial",
      "Times New Roman",
      "Courier New",
      "Verdana",
      "Georgia",
      "Tahoma",
      "Impact"
    )

    // Definicja dostępnych kolorów tła danego fragmentu tekstu
    val availableBackgroundColors = List(
      ("Żółty", new Color(255, 255, 0, 128)),     // Półprzezroczysty żółty
      ("Zielony", new Color(144, 238, 144, 128)), // Jasny zielony
      ("Różowy", new Color(255, 182, 193, 128)),  // Jasny różowy
      ("Niebieski", new Color(173, 216, 230, 128)), // Jasny niebieski
      ("Pomarańczowy", new Color(255, 165, 0, 128)), // Pomarańczowy
      ("Brak koloru", null)  // Opcja usunięcia koloru tła
    )


    // Definicja menu
    menuBar = new MenuBar {
      contents += new Menu("Plik") {
        contents += new MenuItem(Action("Nowy") {
          tabbedPane.addNewTab(new EditorTab())
        })

        contents += new MenuItem(Action("Otwórz") {
          openDocument()
        })

        contents += new MenuItem(Action("Zapisz") {
          tabbedPane.getCurrentTab.foreach(saveDocument)
        })

        contents += new MenuItem(Action("Zamknij zakładkę") {
          if (tabbedPane.getTabCount > 1) {
            tabbedPane.removeTab(tabbedPane.selection.index)
          }
        })

        contents += new Separator
        contents += new MenuItem(Action("Zakończ") {
          sys.exit(0)
        })
      }


      contents += new Menu("Format") {
        contents += new Menu("Kolor czcionki") {
          for ((name, color) <- availableColors) {
            contents += new MenuItem(Action(name) {
              tabbedPane.getCurrentTab.foreach(tab =>
                applyStyleToSelectedText(tab.textPane, color = Some(color))
              )
            }) {
              icon = new ColorIcon(color)
            }
          }
        }

        contents += new Menu("Czcionka") {
          for (font <- availableFonts) {
            contents += new MenuItem(Action(font) {
              tabbedPane.getCurrentTab.foreach(tab =>
                applyStyleToSelectedText(tab.textPane, fontFamily = Some(font))
              )
            })
          }
        }

        contents += new MenuItem(Action("Pogrubienie") {
          tabbedPane.getCurrentTab.foreach(tab => toggleBold(tab.textPane))
        })

        contents += new MenuItem(Action("Kursywa") {
          tabbedPane.getCurrentTab.foreach(tab => toggleItalic(tab.textPane))
        })

        contents += new Menu("Kolor tła") {
          for ((name, color) <- availableBackgroundColors) {
            contents += new MenuItem(Action(name) {
              tabbedPane.getCurrentTab.foreach(tab =>
                applyStyleToSelectedText(tab.textPane, backgroundColor = Some(color))
              )
            }) {
              icon = if (color != null) new ColorIcon(color) else null
            }
          }
        }

        contents += new Menu("Rozmiar czcionki") {
          val sizes = List(8, 10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48, 72)
          for (size <- sizes) {
            contents += new MenuItem(Action(s"$size pt") {
              tabbedPane.getCurrentTab.foreach(tab =>
                applyStyleToSelectedText(tab.textPane, fontSize = Some(size))
              )
            })
          }
        }

        contents += new Menu("Wyrównanie") {
          contents += new MenuItem(Action("Do lewej") {
            tabbedPane.getCurrentTab.foreach(tab =>
              applyStyleToSelectedText(tab.textPane, alignment = Some(StyleConstants.ALIGN_LEFT))
            )
          })
          contents += new MenuItem(Action("Do środka") {
            tabbedPane.getCurrentTab.foreach(tab =>
              applyStyleToSelectedText(tab.textPane, alignment = Some(StyleConstants.ALIGN_CENTER))
            )
          })
          contents += new MenuItem(Action("Do prawej") {
            tabbedPane.getCurrentTab.foreach(tab =>
              applyStyleToSelectedText(tab.textPane, alignment = Some(StyleConstants.ALIGN_RIGHT))
            )
          })
          contents += new MenuItem(Action("Wyjustowane") {
            tabbedPane.getCurrentTab.foreach(tab =>
              applyStyleToSelectedText(tab.textPane, alignment = Some(StyleConstants.ALIGN_JUSTIFIED))
            )
          })
        }

        contents += new MenuItem(Action("Podkreślenie") {
          tabbedPane.getCurrentTab.foreach(tab => toggleUnderline(tab.textPane))
        })
      }

      contents += new MenuItem(Action("Kopiuj") {
        tabbedPane.getCurrentTab.foreach { tab =>
          tab.textPane.copyAction.actionPerformed(
            new ActionEvent(tab.textPane.peer, ActionEvent.ACTION_PERFORMED, "Copy")
          )
        }
      })

      contents += new MenuItem(Action("Wklej") {
        tabbedPane.getCurrentTab.foreach { tab =>
          tab.textPane.pasteAction.actionPerformed(
            new ActionEvent(tab.textPane.peer, ActionEvent.ACTION_PERFORMED, "Paste")
          )
        }
      })

      contents += new MenuItem(Action("Zamień słowo") {
        tabbedPane.getCurrentTab.foreach { tab =>
          val selectedText = tab.textPane.peer.getSelectedText
          if (selectedText != null && selectedText.nonEmpty) {
            // Okno dialogowe do wprowadzenia nowego słowa
            val newWord = Dialog.showInput(
              null,
              "Wprowadź nowe słowo:",
              "Zamiana słowa",
              Dialog.Message.Question,
              initial = ""
            )

            newWord.foreach { word =>
              if (word.nonEmpty) {
                replaceWordInText(tab.textPane, selectedText, word)
              }
            }
          } else {
            Dialog.showMessage(
              null,
              "Zaznacz słowo, które chcesz zamienić.",
              "Uwaga",
              Dialog.Message.Warning
            )
          }
        }
      })


      contents += new Menu("Analiza tekstu") {

        contents += new MenuItem(Action("Znajdź podobne słowa") {
          tabbedPane.getCurrentTab.foreach { tab =>
            val selectedText = tab.textPane.peer.getSelectedText
            if (selectedText != null && selectedText.nonEmpty) {
              val plainText = tab.textPane.peer.getText // Pobieramy zwykły tekst
              val similarWords = LevenshteinExample.findSimilarWords(selectedText, plainText, 10)

              val resultText = if (similarWords.isEmpty) {
                "Nie znaleziono podobnych słów."
              } else {
                "Podobne słowa:\n" + similarWords.mkString("\n")
              }

              Dialog.showMessage(null, resultText, "Podobne słowa", Dialog.Message.Info)
            } else {
              Dialog.showMessage(null, "Zaznacz słowo, aby znaleźć podobne.", "Uwaga", Dialog.Message.Warning)
            }
          }
        })

        contents += new MenuItem(Action("Sprawdź czytelność (FOG i Flesch)") {
          tabbedPane.getCurrentTab.foreach { tab =>
            val selectedText = tab.textPane.peer.getSelectedText
            if (selectedText != null && selectedText.nonEmpty) {
              val plainText = selectedText.replaceAll("\\r\\n|\\r|\\n", " ").trim()

              val fogIndex = FOGIndex.calculateFOGIndex(plainText)
              val fogCategory = FOGIndex.getDifficultyCategory(fogIndex)
              val fogColor = FOGIndex.getDifficultyColor(fogIndex)

              val fleschIndex = FleschIndex.calculateFleschIndex(plainText)
              val fleschCategory = FleschIndex.getDifficultyCategory(fleschIndex)
              val fleschColor = FleschIndex.getDifficultyColor(fleschIndex)

              val dialog = new Dialog(null) {
                title = "Analiza czytelności tekstu"
                modal = true

                contents = new GridPanel(4, 1) {
                  contents += new Label("Indeks FOG:")
                  contents += new BorderPanel {
                    layout(new Label(f"$fogIndex%.2f - $fogCategory")) = BorderPanel.Position.North
                    layout(new Component {
                      override lazy val peer = new javax.swing.JPanel {
                        setPreferredSize(new Dimension(100, 20))
                        setBackground(fogColor)
                        setBorder(javax.swing.BorderFactory.createLineBorder(Color.BLACK))
                      }
                    }) = BorderPanel.Position.Center
                  }

                  contents += new Label("Indeks Flesch:")
                  contents += new BorderPanel {
                    layout(new Label(f"$fleschIndex%.2f - $fleschCategory")) = BorderPanel.Position.North
                    layout(new Component {
                      override lazy val peer = new javax.swing.JPanel {
                        setPreferredSize(new Dimension(100, 20))
                        setBackground(fleschColor)
                        setBorder(javax.swing.BorderFactory.createLineBorder(Color.BLACK))
                      }
                    }) = BorderPanel.Position.Center
                  }
                }

                peer.setLocationRelativeTo(null)
              }

              dialog.pack()
              dialog.open()
            } else {
              Dialog.showMessage(null,
                "Zaznacz tekst do analizy.",
                "Uwaga",
                Dialog.Message.Warning)
            }
          }
        })
      }

    }

    menuBar.contents += new Menu("Sprawdzanie pisowni") {
      contents += new MenuItem(Action("Sprawdź pisownię") {
        if (AspellChecker.initialize()) {
          tabbedPane.getCurrentTab.foreach(_.textPane.checkSpelling())
        } else {
          Dialog.showMessage(
            null,
            "Nie znaleziono programu Aspell. Upewnij się, że jest zainstalowany w systemie.",
            "Błąd",
            Dialog.Message.Error
          )
        }
      })

      contents += new MenuItem(Action("Sugestie dla bieżącego słowa") {
        if (AspellChecker.initialize()) {
          tabbedPane.getCurrentTab.foreach { tab =>
            val pos = tab.textPane.peer.getCaretPosition
            tab.textPane.showSpellingSuggestionsAt(pos)
          }
        }
      })

      contents += new MenuItem(Action("Sprawdź instalację Aspell") {
        Dialog.showMessage(
          null,
          s"""Status Aspell:
             |Zainstalowany: ${AspellChecker.initialize()}
             |Polski słownik: ${AspellChecker.isPolishDictionaryInstalled}""".stripMargin,
          "Informacje o Aspell",
          Dialog.Message.Info
        )
      })

      contents += new MenuItem(Action("Test Aspell") {
        Dialog.showMessage(
          null,
          AspellChecker.testAspell(),
          "Test Aspell",
          Dialog.Message.Info
        )
      })

    }


    val textPane = new StyledTextPane()

    // Główny układ
    contents = new BorderPanel {
      layout(new ScrollPane(textPane)) = BorderPanel.Position.Center
    }

    // Obsługa zdarzeń
    listenTo()

    reactions += {
      case _ => // na razie puste, bo wszystkie akcje są obsługiwane przez menu
    }

    // odpowiada za włączanie/wyłączanie pogrubienia zaznaczonego tekstu
    def toggleBold(textPane: StyledTextPane): Unit = {
      val start = textPane.peer.getSelectionStart
      val end = textPane.peer.getSelectionEnd
      if (start != end) {
        val hasBold = isBold(textPane, start, end)
        if (hasBold) {
          applyStyleToSelectedText(textPane, removeBold = true)
        } else {
          applyStyleToSelectedText(textPane, bold = true)
        }
      }
    }

    // odpowiada za włączanie/wyłączanie kursywy zaznaczonego tekstu ( nachylenie tekstu )
    def toggleItalic(textPane: StyledTextPane): Unit = {
      val start = textPane.peer.getSelectionStart
      val end = textPane.peer.getSelectionEnd
      if (start != end) {
        val hasItalic = isItalic(textPane, start, end)
        if (hasItalic) {
          applyStyleToSelectedText(textPane, removeItalic = true)
        } else {
          applyStyleToSelectedText(textPane, italic = true)
        }
      }
    }

    // odpowiada za włączanie/wyłączanie podkreslenia zaznaczonego tekstu
    def toggleUnderline(textPane: StyledTextPane): Unit = {
      val start = textPane.peer.getSelectionStart
      val end = textPane.peer.getSelectionEnd
      if (start != end) {
        val hasUnderline = isUnderlined(textPane, start, end)
        if (hasUnderline) {
          applyStyleToSelectedText(textPane, removeUnderline = true)
        } else {
          applyStyleToSelectedText(textPane, underline = true)
        }
      }
    }

    // sprawdza czy tekst jest pogrubiony
    def isBold(textPane: StyledTextPane, start: Int, end: Int): Boolean = {
      val doc = textPane.getStyledDoc
      var isAnyBold = false
      for (i <- start until end) {
        val attrs = doc.getCharacterElement(i).getAttributes
        if (StyleConstants.isBold(attrs)) {
          isAnyBold = true
        }
      }
      isAnyBold
    }

    // sprawdza czy tekst jest pochylony
    def isItalic(textPane: StyledTextPane, start: Int, end: Int): Boolean = {
      val doc = textPane.getStyledDoc
      var isAnyItalic = false
      for (i <- start until end) {
        val attrs = doc.getCharacterElement(i).getAttributes
        if (StyleConstants.isItalic(attrs)) {
          isAnyItalic = true
        }
      }
      isAnyItalic
    }

    // sprawdza czy tekst jest podkreślony
    def isUnderlined(textPane: StyledTextPane, start: Int, end: Int): Boolean = {
      val doc = textPane.getStyledDoc
      var isAnyUnderlined = false
      for (i <- start until end) {
        val attrs = doc.getCharacterElement(i).getAttributes
        if (StyleConstants.isUnderline(attrs)) {
          isAnyUnderlined = true
        }
      }
      isAnyUnderlined
    }

    // zapis pliku
    def saveDocument(tab: EditorTab): Unit = {
      val fileChooser = new JFileChooser()
      tab.filePath.foreach { path =>
        fileChooser.setSelectedFile(new File(path))
      }

      if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        val file = fileChooser.getSelectedFile
        val doc = tab.textPane.peer.getStyledDocument

        val fos = new FileOutputStream(file)
        try {
          val rtfKit = new RTFEditorKit()
          rtfKit.write(fos, doc, 0, doc.getLength)
          tab.filePath = Some(file.getAbsolutePath)
          tab.isModified = false

          // Aktualizuj nazwę zakładki
          val index = tabbedPane.selection.index
          val fileName = file.getName
          tabbedPane.pages(index).title = fileName
          updateTitle()
        } catch {
          case e: Exception =>
            Dialog.showMessage(null, s"Błąd podczas zapisywania pliku: ${e.getMessage}", "Błąd", Dialog.Message.Error)
        } finally {
          fos.close()
        }
      }
    }

    // otwieranie pliku
    def openDocument(): Unit = {
      val fileChooser = new JFileChooser()
      if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        val file = fileChooser.getSelectedFile
        val newTab = new EditorTab(file.getName)
        tabbedPane.addNewTab(newTab)

        val fis = new FileInputStream(file)
        try {
          val rtfKit = new RTFEditorKit()
          val doc = newTab.textPane.peer.getStyledDocument
          doc.remove(0, doc.getLength)

          // Dodajemy obsługę błędów RTF
          try {
            rtfKit.read(fis, doc, 0)
          } catch {
            case e: RuntimeException if e.getMessage != null && e.getMessage.contains("Unescaped trailing backslash") =>
              // Ignorujemy ten konkretny błąd, gdyż nie wpływa na poprawność wyświetlania
              println("Ostrzeżenie: Wykryto problem z formatowaniem RTF, ale plik został wczytany poprawnie")
          }

          newTab.filePath = Some(file.getAbsolutePath)
          newTab.isModified = false
          updateTitle()
        } catch {
          case e: Exception =>
            // Wyświetlamy błąd tylko jeśli jest to poważny problem uniemożliwiający otwarcie pliku
            if (!e.getMessage.contains("Unescaped trailing backslash")) {
              Dialog.showMessage(null, s"Błąd podczas otwierania pliku: ${e.getMessage}", "Błąd", Dialog.Message.Error)
            }
        } finally {
          fis.close()
        }
      }
    }

    // zastap zaznaczone slowo w tekscie
    def replaceWordInText(textPane: StyledTextPane, oldWord: String, newWord: String): Unit = {
      val doc = textPane.getStyledDoc
      val fullText = doc.getText(0, doc.getLength)

      // Pattern dopasowujący całe słowa
      val pattern = s"\\b${Pattern.quote(oldWord)}\\b"
      val wordBoundaryPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
      val matcher = wordBoundaryPattern.matcher(fullText)

      // Lista znalezionych pozycji (od końca, żeby zamiana nie wpływała na kolejne indeksy)
      var positions = List[(Int, Int)]()
      while (matcher.find()) {
        positions = (matcher.start(), matcher.end()) :: positions
      }

      // Styl dla nowego słowa
      val style = new SimpleAttributeSet()
      StyleConstants.setBackground(style, Color.RED)
      StyleConstants.setForeground(style, Color.BLACK)
      StyleConstants.setFontFamily(style, "Arial") // lub inna domyślna czcionka
      StyleConstants.setFontSize(style, 12) // domyślny rozmiar
      StyleConstants.setBold(style, false)
      StyleConstants.setItalic(style, false)
      StyleConstants.setUnderline(style, false)

      // Zamiana słów od końca tekstu
      positions.foreach { case (start, end) =>
        doc.remove(start, end - start)
        doc.insertString(start, newWord, style)
      }
    }

    // Inicjalizacja pierwszej zakładki
    tabbedPane.addNewTab(new EditorTab())

    // Główny układ
    contents = new BorderPanel {
      layout(tabbedPane) = BorderPanel.Position.Center
    }

    // Dostosowanie rozmiaru okna
    size = new Dimension(800, 600)


    // Metody pomocnicze - serce w ktorym wykorzystujesz fuckje zamiany stylow itd
    def applyStyleToSelectedText(
                                  textPane: StyledTextPane,
                                  color: Option[Color] = None,
                                  fontSize: Option[Int] = None,
                                  underline: Boolean = false,
                                  removeUnderline: Boolean = false,
                                  backgroundColor: Option[Color] = None,
                                  fontFamily: Option[String] = None,
                                  bold: Boolean = false,
                                  italic: Boolean = false,
                                  removeBold: Boolean = false,
                                  removeItalic: Boolean = false,
                                  alignment: Option[Int] = None
                                ): Unit = {
      val doc = textPane.getStyledDoc
      val start = textPane.peer.getSelectionStart
      val end = textPane.peer.getSelectionEnd
      val length = end - start

      if (length > 0) {
        // Dla każdego znaku osobno zachowujemy jego atrybuty
        for (i <- start until end) {
          val style = new SimpleAttributeSet()
          val element = doc.getCharacterElement(i)
          val attrs = element.getAttributes
          val paragraph = doc.getParagraphElement(i)
          val paragraphAttrs = paragraph.getAttributes

          // Zachowaj obecne atrybuty znaku
          StyleConstants.setForeground(style, StyleConstants.getForeground(attrs))
          if (StyleConstants.getBackground(attrs) != null) {
            backgroundColor.foreach { c =>
              if (c != null) {
                StyleConstants.setBackground(style, c) // Ustaw nowe tło, jeśli podano
              } else if (StyleConstants.getBackground(attrs) != null) {
                StyleConstants.setBackground(style, StyleConstants.getBackground(attrs)) // Zachowaj istniejące tło
              } else {
                StyleConstants.setBackground(style, Color.WHITE) // Domyślne tło
              }
            }
          }
          StyleConstants.setFontSize(style, StyleConstants.getFontSize(attrs))
          StyleConstants.setFontFamily(style, StyleConstants.getFontFamily(attrs))

          // Zachowaj pogrubienie/kursywę tylko jeśli nie usuwamy
          if (!removeBold) {
            StyleConstants.setBold(style, if (bold) true else StyleConstants.isBold(attrs))
          } else {
            StyleConstants.setBold(style, false)
          }

          if (!removeItalic) {
            StyleConstants.setItalic(style, if (italic) true else StyleConstants.isItalic(attrs))
          } else {
            StyleConstants.setItalic(style, false)
          }

          StyleConstants.setUnderline(style, StyleConstants.isUnderline(attrs))

          // Zachowaj wyrównanie paragrafu
          if (alignment.isEmpty) {
            StyleConstants.setAlignment(style, StyleConstants.getAlignment(paragraphAttrs))
          }

          // Zastosuj nowe style tylko jeśli są podane
          color.foreach(c => StyleConstants.setForeground(style, c))
          fontSize.foreach(s => StyleConstants.setFontSize(style, s))
          fontFamily.foreach(f => StyleConstants.setFontFamily(style, f))
          backgroundColor.foreach(c =>
            if (c != null) StyleConstants.setBackground(style, c)
            else StyleConstants.setBackground(style, Color.WHITE)
          )
          if (bold) StyleConstants.setBold(style, true)
          if (italic) StyleConstants.setItalic(style, true)
          if (underline) StyleConstants.setUnderline(style, true)
          if (removeUnderline) StyleConstants.setUnderline(style, false)
          alignment.foreach(a => StyleConstants.setAlignment(style, a))

          // Aplikuj zmiany dla pojedynczego znaku
          doc.setCharacterAttributes(i, 1, style, false)

          // Jeśli zmieniamy wyrównanie, zastosuj je do całego paragrafu
          if (alignment.isDefined) {
            doc.setParagraphAttributes(
              paragraph.getStartOffset,
              paragraph.getEndOffset - paragraph.getStartOffset,
              style,
              false
            )
          }
        }
      }
    }

  }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

object LevenshteinExample {
  // Funkcja do obliczania odległości Levenshteina
  def levenshtein(str1: String, str2: String): Int = {
    val lenStr1 = str1.length
    val lenStr2 = str2.length
    val dp = Array.ofDim[Int](lenStr1 + 1, lenStr2 + 1)

    for (i <- 0 to lenStr1) dp(i)(0) = i
    for (j <- 0 to lenStr2) dp(0)(j) = j

    for (i <- 1 to lenStr1) {
      for (j <- 1 to lenStr2) {
        dp(i)(j) = math.min(
          dp(i - 1)(j) + 1, // usunięcie
          math.min(
            dp(i)(j - 1) + 1, // dodanie
            dp(i - 1)(j - 1) + (if (str1(i - 1).toLower == str2(j - 1).toLower) 0 else 1) // zamiana
          )
        )
      }
    }
    dp(lenStr1)(lenStr2)
  }

  // Funkcja do znalezienia słów pokrewnych w tekście
  def findSimilarWords(word: String, plainText: String, maxDistance: Int): List[String] = {
    // Konwertujemy cały tekst na listę słów i od razu wszystko na małe litery
    val wordList = plainText
      .toLowerCase()
      .split("\\s+|\\p{Punct}")
      .filter(_.nonEmpty)
      .distinct
      .toList

    // Szukane słowo też konwertujemy na małe litery
    val searchWord = word.toLowerCase()

    wordList
      .filter(w => w != searchWord && levenshtein(w, searchWord) <= maxDistance)
      .sortBy(w => levenshtein(w, searchWord))
  }

}

object FleschIndex {
  // Funkcja licząca sylaby w polskim słowie
  def countSyllables(word: String): Int = {
    val vowels = Set('a', 'ą', 'e', 'ę', 'i', 'o', 'ó', 'u', 'y')
    var count = 0
    var prevIsVowel = false

    for (c <- word.toLowerCase) {
      if (vowels.contains(c)) {
        if (!prevIsVowel) {
          count += 1
        }
        prevIsVowel = true
      } else {
        prevIsVowel = false
      }
    }

    math.max(1, count)
  }

  // Funkcja obliczająca indeks Flescha według nowego wzoru
  def calculateFleschIndex(plainText: String): Double = {
    // Dzielimy tekst na zdania
    val sentences = plainText.split("[.!?]+").filter(_.trim.nonEmpty)

    // Dzielimy na słowa
    val words = plainText.split("\\s+|\\p{Punct}+").filter(_.nonEmpty)

    if (words.isEmpty || sentences.isEmpty) return 0.0

    val totalSyllables = words.map(countSyllables).sum

    // Nowy wzór:
    // 206.835 - 1.015(liczba słów/liczba zdań) - 84.6(liczba sylab/liczba słów)
    val wordsPerSentence = words.length.toDouble / sentences.length
    val syllablesPerWord = totalSyllables.toDouble / words.length

    206.835 - (1.015 * wordsPerSentence) - (84.6 * syllablesPerWord)
  }

  // funkcja zwracająca kategorię trudności tekstu
  def getDifficultyCategory(index: Double): String = {
    if (index >= 90) "Tekst łatwy (poziom 11-latka)"
    else if (index >= 60) "Tekst łatwy (poziom 13-15 lat)"
    else if (index >= 30) "Tekst akademicki"
    else "Tekst naukowy (poziom absolwenta)"
  }

  def getDifficultyColor(index: Double): Color = {
    if (index >= 90) new Color(0, 255, 0)       // Zielony
    else if (index >= 60) new Color(128, 255, 0) // Jasnozielony
    else if (index >= 30) new Color(255, 128, 0) // Pomarańczowy
    else new Color(255, 0, 0)                    // Czerwony
  }
}

object FOGIndex {
  // Funkcja licząca sylaby w polskim słowie
  def countSyllables(word: String): Int = {
    val vowels = Set('a', 'ą', 'e', 'ę', 'i', 'o', 'ó', 'u', 'y')
    var count = 0
    var prevIsVowel = false

    for (c <- word.toLowerCase) {
      if (vowels.contains(c)) {
        if (!prevIsVowel) {
          count += 1
        }
        prevIsVowel = true
      } else {
        prevIsVowel = false
      }
    }

    math.max(1, count)
  }

  // Funkcja sprawdzająca czy słowo jest "długie" (4 lub więcej sylab)
  def isLongWord(word: String): Boolean = {
    countSyllables(word) >= 4
  }

  // Funkcja obliczająca indeks FOG
  def calculateFOGIndex(plainText: String): Double = {
    // Dzielimy tekst na zdania
    val sentences = plainText.split("[.!?]+").filter(_.trim.nonEmpty)

    // Dzielimy na słowa
    val words = plainText.split("\\s+|\\p{Punct}+").filter(_.nonEmpty)

    if (words.isEmpty || sentences.isEmpty) return 0.0

    val numberOfWords = words.length.toDouble
    val numberOfSentences = sentences.length.toDouble
    val numberOfLongWords = words.count(isLongWord).toDouble

    // Wzór FOG = 0.4 * (liczba_słów/liczba_zdań + 100 * liczba_trudnych_słów/liczba_słów)
    0.4 * ((numberOfWords / numberOfSentences) + 100 * (numberOfLongWords / numberOfWords))
  }

  // Funkcja zwracająca kategorię trudności tekstu
  def getDifficultyCategory(index: Double): String = {
    if (index <= 6) "Język bardzo prosty (poziom szkoły podstawowej)"
    else if (index <= 9) "Język prosty (poziom gimnazjum)"
    else if (index <= 12) "Język dość prosty (poziom liceum)"
    else if (index <= 15) "Język dość trudny (poziom studiów licencjackich)"
    else if (index <= 17) "Język trudny (poziom studiów magisterskich)"
    else "Język bardzo trudny (poziom magistra i wyżej)"
  }

  def getDifficultyColor(index: Double): Color = {
    if (index <= 6) new Color(0, 255, 0)       // Zielony
    else if (index <= 9) new Color(128, 255, 0) // Jasnozielony
    else if (index <= 12) new Color(255, 255, 0) // Żółty
    else if (index <= 15) new Color(255, 165, 0) // Pomarańczowy
    else if (index <= 17) new Color(255, 69, 0)  // Czerwonopomarańczowy
    else new Color(255, 0, 0)                    // Czerwony
  }
}


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

// TO DO ASPELLA      !!!!!!!!!!!!!!!!!!!!!!!!!!!!!

import com.sun.jna.{Library, Native, Platform}
import scala.sys.process._
import java.io._
import scala.io.Source
import java.lang.ProcessBuilder

// obiekt odpowiedzialny za sprawdzanie pisowni w edytorze
object AspellChecker {
  private var aspellPath: Option[String] = None
  private var debug = true

  def initialize(): Boolean = {
    if(debug) println("Inicjalizacja AspellChecker...")
    aspellPath = findAspell()
    if(debug) println(s"Znaleziona ścieżka Aspell: ${aspellPath.getOrElse("Nie znaleziono")}")
    aspellPath.isDefined
  }

  private def findAspell(): Option[String] = {
    if (Platform.isWindows) {
      if(debug) println("Wykryto system Windows")
      val programFiles = System.getenv("ProgramFiles")
      val programFilesX86 = System.getenv("ProgramFiles(x86)")
      val possiblePaths = List(
        s"$programFiles\\Aspell\\bin\\aspell.exe",
        s"$programFilesX86\\Aspell\\bin\\aspell.exe",
        "C:\\Program Files\\Aspell\\bin\\aspell.exe",
        "C:\\Program Files (x86)\\Aspell\\bin\\aspell.exe"
      )
      possiblePaths.find(path => new File(path).exists())
    } else {
      try {
        val result = "which aspell".!!.trim
        if (result.nonEmpty) Some(result) else None
      } catch {
        case _: Exception => None
      }
    }
  }

  def checkWord(word: String): Boolean = {
    if (debug) println(s"Sprawdzanie słowa: $word")
    aspellPath.exists { path =>
      var process: java.lang.Process = null
      var writer: PrintWriter = null
      var reader: BufferedReader = null

      try {
        val processBuilder = new java.lang.ProcessBuilder(
          path,
          "-a",
          "--encoding=utf-8",
          "--lang=pl"
        )
        process = processBuilder.start()

        writer = new PrintWriter(new OutputStreamWriter(process.getOutputStream, "UTF-8"), true)
        reader = new BufferedReader(new InputStreamReader(process.getInputStream, "UTF-8"))

        // Pomijamy pierwszą linię (banner)
        reader.readLine()

        writer.println(word)
        writer.flush()

        val response = reader.readLine()
        if (debug) println(s"Odpowiedź Aspell: $response")

        response != null && (response.startsWith("*") || response.startsWith("+"))
      } catch {
        case e: Exception =>
          if (debug) println(s"Błąd podczas sprawdzania słowa: ${e.getMessage}")
          false
      } finally {
        if (writer != null) writer.close()
        if (reader != null) reader.close()
        if (process != null) {
          process.getOutputStream.close()
          process.getInputStream.close()
          process.getErrorStream.close()
          process.destroy()
        }
      }
    }
  }

  def getSuggestions(word: String): List[String] = {
    if (debug) println(s"Pobieranie sugestii dla słowa: $word")
    aspellPath.map { path =>
      var process: java.lang.Process = null
      var writer: PrintWriter = null
      var reader: BufferedReader = null

      try {
        val processBuilder = new java.lang.ProcessBuilder(
          path,
          "-a",
          "--encoding=utf-8",
          "--lang=pl"
        )
        process = processBuilder.start()

        writer = new PrintWriter(new OutputStreamWriter(process.getOutputStream, "UTF-8"), true)
        reader = new BufferedReader(new InputStreamReader(process.getInputStream, "UTF-8"))

        // Pomijamy pierwszą linię (banner)
        reader.readLine()

        writer.println(word)
        writer.flush()

        val response = reader.readLine()
        if (debug) println(s"Odpowiedź Aspell dla sugestii: $response")

        if (response != null && response.startsWith("&")) {
          response.split(":")(1).split(",").map(_.trim).toList
        } else {
          List.empty[String]
        }
      } catch {
        case e: Exception =>
          if (debug) println(s"Błąd podczas pobierania sugestii: ${e.getMessage}")
          List.empty[String]
      } finally {
        if (writer != null) writer.close()
        if (reader != null) reader.close()
        if (process != null) {
          process.getOutputStream.close()
          process.getInputStream.close()
          process.getErrorStream.close()
          process.destroy()
        }
      }
    }.getOrElse(List.empty[String])
  }

  def testAspell(): String = {
    if (!initialize()) {
      return "Nie można zainicjalizować Aspell"
    }

    try {
      val testWord = "pies"
      val startTime = System.currentTimeMillis()
      val isCorrect = checkWord(testWord)
      val suggestions = getSuggestions("piesl")
      val endTime = System.currentTimeMillis()

      s"""Test Aspell:
         |Słowo testowe: $testWord
         |Poprawne: $isCorrect
         |Sugestie dla 'piesl': ${suggestions.mkString(", ")}
         |Czas wykonania: ${endTime - startTime}ms
         |Ścieżka Aspell: ${aspellPath.getOrElse("Nie znaleziono")}""".stripMargin
    } catch {
      case e: Exception => s"Błąd podczas testu: ${e.getMessage}"
    }
  }

  def isPolishDictionaryInstalled: Boolean = {
    aspellPath.exists { path =>
      var process: java.lang.Process = null
      var reader: BufferedReader = null

      try {
        val processBuilder = new java.lang.ProcessBuilder(path, "dump", "dicts")
        process = processBuilder.start()
        reader = new BufferedReader(new InputStreamReader(process.getInputStream))

        var line: String = null
        var found = false
        while ({line = reader.readLine(); line != null} && !found) {
          found = line.contains("pl")
        }
        found
      } catch {
        case e: Exception =>
          if(debug) println(s"Błąd podczas sprawdzania słowników: ${e.getMessage}")
          false
      } finally {
        if (reader != null) reader.close()
        if (process != null) {
          process.getInputStream.close()
          process.getErrorStream.close()
          process.destroy()
        }
      }
    }
  }
}