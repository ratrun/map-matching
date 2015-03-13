#OSM Daten Qualitätsverbesserung durch Analyse von GraphHopper "Map Matching" Ergebnissen

Bei der Evaluierung der GraphHopper "Map Matching" Implementierung mit 
vorhandenen GPX Daten aus der Vergangenheit hat sich herausgestellt, dass der im 
GraphHopper "Map Matching" verwendete 
[Algorithmus](https://karussell.wordpress.com/2014/07/28/digitalizing-gpx-points-or-how-to-track-vehicles-with-GraphHopper/ "Algorithmus") 
gut dafür genutzt werden kann, Fehler oder Unvollständigkeiten im vorhandenen 
OSM Datenmaterial zu erkennen. Wenn der Algorithmus keinen Weg findet ist es 
nämlich oft so, dass in den aufgezeichneten GPX Daten Wissen über mögliche 
und passierbare Verbindungen steckt, das bisher nicht in den OSM Daten 
abgebildet worden ist. Es wird dabei ein HTML Report erzeugt,
über den die OSM Daten mit Hilfe von JOSM bearbeitet werden können.

Im folgenden werden die dabei gemachten Erfahrungen beschrieben, die es 
leichter machen sollen eine Analyse mit eignen GPX Dateien auszuprobieren.

Der Algorithmus versucht basierend auf der in GPX Daten enthaltenen Koordinaten 
anhand der OSM Daten einen der Aktivität angepassten möglichst deckungsgleichen 
Weg zu finden. Es ist also wichtig, dass die Art der Aktivität bekannt ist, bei 
der die GPS Daten entstanden ist. 

Die erwähnten Beispiele gehen oft von einer Fahrrad Aktivität aus, die Beispiele 
sollten sich aber auch auf andere Aktivitäten übertragen lassen.

Momentan muss man für einen Test GraphHopper "Map Matching" leider noch lokal 
installieren. Vielleicht findet sich in Zukunft jemand, der diese 
Analysemöglichkeit als Web-Service anbietet, bei dem die Analyse optional 
durchgeführt wird, wenn man GPX Daten auf eines der GPX Sammelportale 
oder noch besser auf OSM hochlädt. Bis sich wer findet, der das umsetzt, muss 
man GraphHopper "Map Matching" durch Verwendung einer lokale Instanz aus einem OSM 
Datenextrakt für einen entsprechenden Bereich (z.B. von der geofabrik) die 
GraphHopper graph Daten für die gewünschte Aktivität erstellen, um danach das 
GraphHopper "Map Matching" auf die vorhandenen GPX Dateien anzuwerfen. Siehe die 
GraphHopper "Map Matching" 
[Beschreibung](https://github.com/ratrun/map-matching/tree/htmlresultoutput "Beschreibung"). 
Als Ergebnis erhält man eine 
HTML Tabelle der erfolgreichen und der nicht erfolgreich zuordenbaren GPX 
Dateien:

![Result](GraphHopperMapMatchingResultTable.PNG "Result")

Bei Erfolg werden die originalen und die gefundenen GPX Daten in der Tabelle in 
der zweiten und dritten Spalte mit dem 
[Remote Control Mechanismus von JOSM](http://josm.openstreetmap.de/wiki/Help/Preferences/RemoteControl "JOSM Remote Control") 
verknüpft, damit man die originale GPX Datei sowie die resultierende GPX Datei 
auf Knopfdruck in JOSM laden kann. Bei gröberen Unterschieden in der Distanz der 
beiden GPS Dateien können die beiden Dateien in JOSM graphisch 
übereinandergelegt werden, um die Abweichungen zu vergleichen. Bei den Stellen 
mit größeren Abweichungen im Verlauf kann man in JOSM die OSM Datenlage 
überprüfen indem man den entsprechenden OSM Datenausschnitte nachlädt. Bei einem 
Abbruch des Algorithmus durch einen Fehler wird in der "Result links" Spalte 
stattdessen ein Verweis angelegt, der den fragwürdigen Ausschnitt mit Hilfe 
einer lokal laufenden GraphHopper-web Instanz anzeigt, sodass man graphisch 
erkennt wie und wo das Routing basierend auf den OSM Daten in die Irre leiten 
würde.

Eine lokal laufende GraphHopperWeb Instanz nur für die Anzeige mag auf den 
ersten Blick etwas aufwendig erscheinen, allerdings muss der graph sowieso 
berechnet werden, sodass dieser von grapphhopper-web verwendet werden kann und 
nicht neu berechnet werden muss. Dadurch wird auch automatisch sichergestellt, dass 
die entsprechende Aktivität ausgewählt werden kann und dass die 
zugrundeliegenden OSM Daten den exakt gleichen Datenstand haben.

Ein weiterer JOSM Verweis in der Tabelle ganz recht öffnet JOSM an genau der 
problematischen Stelle, sodass man auch in dem Fall die Datenlage durch das Laden der 
OSM Daten manuell genauer überprüfen kann. Für die Analyse ist es meist 
hilfreich zuerst die entsprechende Ursprungs GPX Datei über die Spalte ganz 
links in JOSM zu laden.
 
Für das Einordnen und das Beurteilen der Ergebnisse und Fehler ist das Wissen und 
die Erinnerung des GPX track Autors sehr wichtig. Mit fremden GPX Daten dürfte 
es nur in seltenen Fällen möglich sein eine sinnvolle OSM Datenverbesserung 
durchzuführen.

Hier ein paar Gründe für ein Scheitern des "Map Matchings":

* Noch nicht vorhandene Wegsegmente in den OSM Daten. Das können längere Abschnitte, aber auch ganz
  kurze Segmente sein. Zum Beispiel, wenn ein Weg bei einer Kreuzung laut OSM Daten nur für 
  Radfahrer erlaubt ist, aber nicht für Fußgänger. Für die Erkennung von Datenproblemen erweist es 
  sich dabei als nützlich, dass GraphHopper momentan nur auf korrekt miteinander verbundenen 
  Wegsegmenten routen kann und auch kurze nicht erlaubte Verbindungen nicht verwendet.
* Fehler im Tagging der OSM Wege, die eine Benützung verhindern. 
  (z.B. fehlendes bicycle=yes, designated auf einem highway=footway, motorway ohne bicyle=yes ..., oneway=yes, ...)
* Routenverlauf über Plätze – dieses Routing wird von grapphopper momentan noch nicht unterstützt
* Multi-modale GPX Aufzeichnungen: z.B. längerer Abstecher während einer Radtour um zu Fuß zu einem Aussichtspunkt hochzusteigen.
* Kleinere Missachtungen der Gesetzeslage, z.B. durch das Begehen eines Privatweges, Radfahren gegen eine Einbahn, 
   Benutzung eines für Radfahrer nicht erlaubten Gehweges, Radfahren auf einer nicht für Radfahrer 
   freigegebenen Straße (z.B. Autobahn, Schnellstraße, Fußgängerzone, ..)
   In Zukunft kann auch die Missachtung von Abbiegeverboten dazu führen.
   Dazu ist jedoch erst die Unterstützung von Abbiegeverboten durch GraphHopper Voraussetzung.
* Zu große GPS Ungenauigkeiten: Es ist allerdings relativ unwahrscheinlich, dass aufgrund von GPS Ungenauigkeiten bei einer entsprechend freizügigen 
   map-maptching Parametrisierung gar keine Verbindung gefunden werden kann. Hier ist es eher wahrscheinlich, dass GraphHopper ein Segment 
   auswählt, das in Wirklichkeit nicht verwendet worden ist. Solch einen Fehler kann jedoch vom Algorithmus nicht erkannt werden und führt deshalb auch gar 
   nicht dazu, dass gar keine passende Verbindung gefunden werden kann.
* Zu restriktive Parametrisierung, z.B. hilft oft das Erhöhen des maxSearchMultiplier Wertes auf z.B. 500
* Temporäre Umleitungen über provisorische normalerweise nicht verwendbaren Wege bei Baustellen, Murenabgängen oder anderen Naturereignissen
* Probleme bei der Wege-Verfolgung in Tunneln aufgrund von GPS Empfangsverlust
* Momentan gibt es auch noch Fehler im "Map Matching" Algorithmus. Es ist z.B. relativ unwahrscheinlich, dass die Benützung von Fähren korrekt erkannt wird.
  Es scheint auch noch ein Problem beim Erkennen des allerletzten Wegsegments zu geben. Hier hängt es oft davon ab, zu welchem Zeitpunkt man die GPX  Aufzeichnung beendet hat. Wenn man dies erst in den eignen vier Wänden gemacht hat und man eine vergleichsweise lange Strecke zum letzten in OSM vorhandenen Wegsegment liegt, dann tritt dadurch ganz knapp vor dem Ziel noch ein Fehler auf.
* Veraltetes GPX Datenmaterial durch Baumaßnahmen, Straßenverlegungen u.s.w, die danach fertiggestellt wurden und mittlerweil in OSM eingetragen wurden.
* Es gibt sicher noch ein Menge anderer Gründe, die man jeweils von Fall spezifisch beurteilen muss.

Die Verbesserung von OSM Daten basierend auf der manueller Analyse der GPX Daten 
ist relativ aufwändig. Der GraphHopper "Map Matching" Algorithmus erlaubt eine Überprüfung 
auf relativ simple, gründliche, schnelle und auch automatisierbare Weise, sodass 
das Verbessern der OSM Daten damit recht flott erfolgen kann.

__Viel Spass beim Analysieren und Verbessern der OSM Daten anhand von GPX Dateien!__