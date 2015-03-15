#OSM Daten Qualit�tsverbesserung durch Analyse von GraphHopper "Map Matching" Ergebnissen

Bei der Evaluierung der GraphHopper "Map Matching" Implementierung mit 
vorhandenen GPX Daten aus der Vergangenheit hat sich herausgestellt, dass der im 
GraphHopper "Map Matching" verwendete 
[Algorithmus](https://karussell.wordpress.com/2014/07/28/digitalizing-gpx-points-or-how-to-track-vehicles-with-GraphHopper/ "Algorithmus") 
gut daf�r genutzt werden kann, Fehler oder Unvollst�ndigkeiten im vorhandenen 
OSM Datenmaterial zu erkennen. Wenn der Algorithmus keinen Weg findet ist es 
n�mlich oft so, dass in den aufgezeichneten GPX Daten Wissen �ber m�gliche 
und passierbare Verbindungen steckt, das bisher nicht in den OSM Daten 
abgebildet worden ist. F�r die Analyse wird dabei ein HTML Report erzeugt,
�ber den die OSM Daten mit Hilfe von JOSM angesehen und bearbeitet werden k�nnen.

Im folgenden werden die dabei gemachten Erfahrungen und beschrieben, die es 
m�glich machen ein Beispiel aus Leipzig nachzuvollziehen und es erm�glichen 
die Methode anhand eigener GPX Dateien zu testen.

Der Algorithmus versucht basierend auf der in GPX Daten enthaltenen Koordinaten 
anhand der OSM Daten einen der Aktivit�t angepassten m�glichst deckungsgleichen 
Weg zu finden. Es ist also wichtig, dass die Art der Aktivit�t bekannt ist, bei 
der die GPS Daten entstanden ist. 

Die erw�hnten Beispiele gehen oft von einer Fahrrad Aktivit�t aus, sie 
sollten sich aber auch auf andere Aktivit�ten �bertragen lassen.

Momentan muss man f�r einen Test GraphHopper "Map Matching" leider noch lokal 
installieren. Vielleicht findet sich in Zukunft jemand, der diese 
Analysem�glichkeit als Web-Service anbietet, bei dem die Analyse optional 
durchgef�hrt wird, wenn man GPX Daten auf eines der GPX Sammelportale 
oder noch besser auf OSM hochl�dt. Bis sich wer findet, der so etwas umsetzt, muss 
man GraphHopper "Map Matching" durch Verwendung einer lokalen Instanz aus einem OSM 
Datenextrakt f�r einen entsprechenden Bereich (z.B. von der geofabrik) die 
GraphHopper graph Daten f�r die gew�nschte Aktivit�t erstellen, um danach das 
GraphHopper "Map Matching" auf die vorhandenen GPX Dateien anzuwerfen. 
Als Ergebnis erh�lt man eine 
HTML Tabelle der erfolgreichen und der nicht erfolgreich zuordenbaren GPX 
Dateien:

![Result](GraphHopperMapMatchingResultTable.PNG "Result")

Bei Erfolg werden die originalen in der zweiten Spalte und die gefundenen GPX Datei in der Tabelle in 
der dritten Spalte mit dem 
[Remote Control Mechanismus von JOSM](http://josm.openstreetmap.de/wiki/Help/Preferences/RemoteControl "JOSM Remote Control") 
verkn�pft, damit man beide auf Knopfdruck in JOSM laden kann. 
Bei gr�beren Unterschieden in der Distanz der beiden GPS Dateien k�nnen die 
beiden GPX Spuren in JOSM graphisch �bereinandergelegt werden, um die Abweichungen zu vergleichen. 
Bei den Stellen mit gr��eren Abweichungen im Verlauf kann man in JOSM die OSM Datenlage 
�berpr�fen indem man den entsprechenden OSM Datenausschnitt nachl�dt. Bei einem 
Abbruch des Algorithmus durch einen Fehler wird in der "Result links" Spalte 
stattdessen ein Verweis angelegt, der den fragw�rdigen Ausschnitt mit Hilfe 
einer lokal laufenden GraphHopper-web Instanz anzeigt, sodass man graphisch 
erkennt wie und wo das Routing basierend auf den OSM Daten in die Irre leiten 
w�rde.

Das Ausf�hren einer lokal laufende GraphHopperWeb Instanz ist sinnvoll, weil nur 
dadurch sichergestellt werden kann, dass die entsprechende Aktivit�t ausgew�hlt 
werden kann und dass die zugrundeliegenden OSM Daten den exakt gleichen 
Datenstand haben. Da der graph sowieso berechnet werden muss, wird die zur 
Erstellung der graph Daten verwendete grapphhopper-web Instanz daf�r benutzt.

Ein weiterer JOSM Verweis in der Tabelle ganz rechts �ffnet JOSM an genau der 
problematischen Stelle, sodass man auch in diesem Fall die Datenlage durch das Laden der 
OSM Daten manuell genauer �berpr�fen kann. F�r die Analyse ist es meist 
hilfreich zuerst die entsprechende Ursprungs GPX Datei �ber die Spalte ganz 
links in JOSM zu laden.
 
Um die Ergebnisse und erkannten Fehler zu beurteilen ist das Wissen und 
die Erinnerung des GPX track Autors sehr wichtig. Mit fremden GPX Daten d�rfte 
es nur in seltenen F�llen m�glich sein eine sinnvolle OSM Datenverbesserung 
durchzuf�hren.

Hier ein paar Gr�nde f�r das Scheitern des "Map Matchings":

* Noch nicht vorhandene Wegsegmente in den OSM Daten. Das k�nnen l�ngere Abschnitte, aber auch ganz
  kurze Segmente sein. Zum Beispiel, wenn ein Weg bei einer Kreuzung laut OSM Daten nur f�r 
  Radfahrer erlaubt ist, aber nicht f�r Fu�g�nger. F�r die Erkennung von Datenproblemen erweist es 
  sich dabei als n�tzlich, dass GraphHopper momentan nur auf korrekt miteinander verbundenen 
  Wegsegmenten routen kann und auch kurze nicht erlaubte Verbindungen nicht verwendet.
* Fehler im Tagging der OSM Wege, die eine Ben�tzung verhindern. 
  (z.B. fehlendes bicycle=yes, designated auf einem highway=footway, motorway ohne bicyle=yes ..., oneway=yes, ...)
* Routenverlauf �ber Pl�tze � dieses Routing wird von grapphopper momentan noch nicht unterst�tzt
* Multi-modale GPX Aufzeichnungen: z.B. l�ngerer Abstecher w�hrend einer Radtour um zu Fu� zu einem Aussichtspunkt hochzusteigen.
* Kleinere Missachtungen der Gesetzeslage, z.B. durch das Begehen eines Privatweges, Radfahren gegen eine Einbahn, 
   Benutzung eines f�r Radfahrer nicht erlaubten Gehweges, Radfahren auf einer nicht f�r Radfahrer 
   freigegebenen Stra�e (z.B. Schnellstra�e, Fu�g�ngerzone, ..)
* Missachtung von Abbiegeverboten.
* Zu gro�e GPS Ungenauigkeiten: Es ist allerdings relativ unwahrscheinlich, dass aufgrund von GPS Ungenauigkeiten bei einer entsprechend freiz�gigen 
   map-maptching Parametrisierung gar keine Verbindung gefunden werden kann. Hier ist es eher wahrscheinlich, dass GraphHopper ein Segment 
   ausw�hlt, das in Wirklichkeit nicht verwendet worden ist. Solch einen Fehler kann jedoch vom Algorithmus nicht erkannt werden und f�hrt deshalb auch 
   nicht dazu, dass �berhaupt keine passende Verbindung gefunden werden kann.
* Zu restriktive Parametrisierung, z.B. hilft oft momentan das Erh�hen des maxSearchMultiplier Wertes auf z.B. 500. In zuk�nftigen GraphHopper "Map Matching"
  Versionen sollte so ein Tuning nicht mehr n�tig sein.
* Tempor�re Umleitungen �ber provisorische normalerweise nicht verwendbaren Wege bei Baustellen, Murenabg�ngen oder anderen Naturereignissen
* Momentan gibt es auch noch Fehler im "Map Matching" Algorithmus. Es ist z.B. relativ unwahrscheinlich, dass die Ben�tzung von F�hren korrekt erkannt wird.
  Es scheint z.B. noch ein Problem beim Abbiegen aus Einbahnen zu geben. Weiters h�ngt es beim allerletzten Wegsegment oft davon ab, zu welchem Zeitpunkt man 
  die GPX  Aufzeichnung beendet. Wenn man dies erst nach Ankunft in den eignen vier W�nden gemacht hat und eine l�ngere Strecke zum letzten in OSM vorhandenen Wegsegment 
  liegt, dann tritt dadurch ganz knapp vor dem Ziel noch ein Fehler auf.
* Veraltetes GPX Datenmaterial durch Bauma�nahmen, Stra�enverlegungen u.s.w, die danach fertiggestellt wurden und mittlerweile in OSM eingetragen wurden.
* Es gibt sicher noch ein Menge anderer Gr�nde, die man jeweils einzeln von Fall zu Fall beurteilen muss.

Auf [https://github.com/ratrun/map-matching/releases](https://github.com/ratrun/map-matching/releases) liegt eine zip Datei
bereit, die alle notwendigen Komponenten enth�lt um das oben gezeigte Beispiel aus Leipzig nachzuvollziehen.

Nach dem Entpacken m�ssen die Pfade zur Java Runtime in den beiden "cmd" b.z.w. "sh" scripten h�ndisch angepasst werden.

Die HTML Datei mit dem Namen "mapmatchresult.html" sollte nach dem Aufruf von 

```Windows cmd
rungraphhopper.cmd
runmatch.cmd
```

b.z.w. 

```Linux bash
rungraphhopper.sh
runmatch.sh
```

im Verzeichnis track-data neu erstellt werden.

F�r das Ausprobieren mit eigenen Dateien in einer anderen Region sollte es 
reichen im Verzeichnis "map-data" das OSM Extrakt abzulegen, den Dateinamen im 
"rungraphhopper" script und die Aktivit�t in der Datei "config.properties" anzupassen und die GPX Dateien im Verzeichnis "track-data" 
durch eigenen Dateien zu ersetzen.

Es w�re sch�n, wenn die aufgezeigte Methode andere Benutzer anregen k�nnte mit 
ihren GPX Dateien zu experimentieren um OSM Daten zu verbessern. 

__Viel Spass!__