# Die Idee

Ein Space-Invaders-ähnliches Spiel.  
Doch was soll mein einzigartiger Twist sein? Mein *USP* sozusagen.  
Ich habe lange darüber nachgedacht – *schlaflose Nächte*.  
Und schlussendlich kam mir die Idee: **Ich werde ein einzigartiges Sounddesign umsetzen.**

Grund dafür war die Demo des Spiels *Metro Gravity*, ein Rhythmusspiel.  
Anfangs hatte ich vor, die Angriffe der einzelnen Gegner sowie des Bosses nach einem Song zu timen – quasi zu choreografieren.  
Doch mir wurde relativ früh klar, dass das ein wenig zu ambitioniert für ein gerade mal **vierwöchiges Solo-Dev-Projekt** ist.

Schlussendlich habe ich mich für ein simpleres **SFX** (Sound Effects)-System entschieden – eines, das ich bzw. meine Freunde und Kollegen **selbst aufnehmen**.  
Und so war die Grundidee geboren:

## Features

- Der Spieler startet im **Hauptmenü**, in dem er eine von vielen **Voice-Actor-Stimmen** auswählen kann.  
  Diese wird dann als *SFX* für den gesamten Playthrough verwendet.  
  Um eine fundierte Entscheidung treffen zu können, gibt es ein kurzes **Intro** jeder Stimme, das sich der Spieler anhören kann, um den **Vibe** der Stimme kennenzulernen.

- Nachdem der Spieler seine Lieblingsstimme gewählt und die **Gameloop** gestartet hat, wechseln wir die **Szene**  
  (eine von fünf, definiert in einem *Enum* (Aufzählungstyp)).  
  Nun beginnt der eigentliche Spaß:

  1. Der Spieler kämpft sich durch eine Gruppe von Gegnern, die sich **smooth** nach links und rechts bewegen.
  2. Danach folgt eine Welle von Gegnern, die sich in **klassischer Space-Invaders-Manier** bewegen.
  3. In der dritten Welle bewegen sich die Gegner in einer **zufälligen Sinusbewegung** nach unten.

- Nach den drei Wellen erscheint der **Boss**:

  ### Boss Phase 1
  - Der Boss feuert **ein Projektil** in Richtung des Spielers.
  - Nach dem ersten Rückzug spawnen **X Gegner**, die sich in einem **diagonalen Muster** nach unten bewegen.

  ### Boss Phase 2
  - Der Boss kehrt **etwas stärker** zurück.
  - Er feuert nun **zwei Projektile**, die den Spieler **verfolgen**.
  - Danach erscheinen erneut **X Gegner**, diesmal in einem **zufälligen Pattern**, denen der Spieler **ausweichen oder sie zerstören** muss.

  ### Boss Phase 3 (Finale)
  - Der Boss ist jetzt **deutlich gefährlicher**.
  - Er nutzt drei verschiedene **Angriffsarten**:
    - Drei **verfolgende Projektile**
    - Drei **schnelle, parallele Schüsse** nach unten
    - Eine **Spray Attack**, der schwerer auszuweichen ist

- Sobald der Spieler den Boss besiegt hat:
  - **Wechsel zur Credits-Szene**
  - Anzeige aller **Mitwirkenden**
  - Danach Rückkehr ins **Hauptmenü**

- Falls der Spieler **stirbt**:
  - Rückkehr ins **Hauptmenü**

- Wenn der Spieler `ESC` drückt:
  - Öffnet sich das **Pausenmenü** mit drei Optionen:
    - `Continue`
    - `Reset`
    - `Back to Main Menu`



git fetch origin
git reset --hard origin/main

if I want to reset my current local changes and start from the last remote check-point
