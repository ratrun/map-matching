/*
 *  Licensed to GraphHopper and Peter Karich under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  GraphHopper licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.matching;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.GraphStorage;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author Peter Karich
 */
public class MapMatchingMain {

    public static void main(String[] args) {
        new MapMatchingMain().start(CmdArgs.read(args));
    }
    
    private String formatTextUml (String text)
    {
        text = text.replaceAll("ä", "&auml;");
        text = text.replaceAll("Ä", "&Auml;");
        text = text.replaceAll("ö", "&ouml;");
        text = text.replaceAll("Ö", "&Ouml;");
        text = text.replaceAll("ü", "&uuml;");
        text = text.replaceAll("Ü", "&Uuml;");
        return text;
    }    

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ArrayList <ArrayList<String>> htmlresultlist = new ArrayList<ArrayList<String>>();
    private final List <String> gpxfiles = new ArrayList <String> ();

    private void start(CmdArgs args) {
        String htmlReportFileName;
        String action = args.get("action", "").toLowerCase();
        int separatedSearchDistance = args.getInt("separatedSearchDistance", 500);
        int maxSearchMultiplier = args.getInt("maxSearchMultiplier", 100);
        boolean forceRepair = args.getBool("forceRepair", false);
        args.put("graph.location", "./graph-cache");
        if (action.equals("import")) {
            String vehicle = args.get("vehicle", "car").toLowerCase();
            args.put("graph.flagEncoders", vehicle);
            args.put("osmreader.osm", args.get("datasource", ""));
            
            // standard should be to remove disconnected islands
            args.put("prepare.minNetworkSize", 200);
            args.put("prepare.minOneWayNetworkSize", 200);            
            GraphHopper hopper = new GraphHopper().init(args);
            hopper.setCHEnable(false);
            hopper.importOrLoad();

        } else if (action.equals("match")) {
            GraphHopper hopper = new GraphHopper().init(args);
            hopper.setCHEnable(false);
            logger.info("loading graph from cache");
            hopper.load("./graph-cache");
            FlagEncoder firstEncoder = hopper.getEncodingManager().fetchEdgeEncoders().get(0);
            GraphStorage graph = hopper.getGraph();

            int gpxAccuracy = args.getInt("gpxAccuracy", 15);
            logger.info("Setup lookup index. Accuracy filter is at " + gpxAccuracy + "m");
            LocationIndexMatch locationIndex = new LocationIndexMatch(graph,
                    (LocationIndexTree) hopper.getLocationIndex(), gpxAccuracy);
            MapMatching mapMatching = new MapMatching(graph, locationIndex, firstEncoder);
            mapMatching.setSeparatedSearchDistance(separatedSearchDistance);
            mapMatching.setMaxSearchMultiplier(maxSearchMultiplier);
            mapMatching.setForceRepair(forceRepair);

            // do the actual matching, get the GPX entries from a file or via stream
            String gpxLocation = args.get("gpx", "");
            File[] files;
            File dir = new File(".");
            int lastIndex;
            if (gpxLocation.contains("*")) {
                final String pattern;
                if ( (gpxLocation.contains("{")) && (gpxLocation.contains("}")) ) {
                   // Treat everything within {} as regular expression for the filename. E.g. {[\w-_]*\.gpx} can be used to exclude prior generated "res" files
                   int bracketIndex=gpxLocation.lastIndexOf("{");
                   lastIndex = gpxLocation.substring(0, bracketIndex).lastIndexOf(File.separator);
                   dir = new File(gpxLocation.substring(0, lastIndex));
                   pattern = gpxLocation.substring(bracketIndex+1, gpxLocation.length()-1);
                }
                else
                {
                    lastIndex = gpxLocation.lastIndexOf(File.separator);
                    if (lastIndex >= 0) {
                        dir = new File(gpxLocation.substring(0, lastIndex));
                        pattern = gpxLocation.substring(lastIndex + 1);
                    } else {
                        pattern = gpxLocation;
                    }
                }

                files = dir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.matches(pattern);
                    }
                });
                htmlReportFileName = dir.toString() + File.separator + "mapmatchresult.html";
            } else {
                lastIndex = gpxLocation.lastIndexOf(File.separator);
                dir = new File(gpxLocation.substring(0, lastIndex));
                files = new File[]{
                    new File(gpxLocation)
                };
                String filename = gpxLocation.substring(lastIndex + 1);
                htmlReportFileName = dir.toString() + File.separator + "mapmatchresult" + filename + ".html";
            }

            logger.info("Now processing " + files.length + " files");
            StopWatch importSW = new StopWatch();
            StopWatch matchSW = new StopWatch();
            int successcounter = 0;
            int failcounter = 0;
            for (File gpxFile : files) {
                ArrayList <String>  htmlresult= new ArrayList <String> ();
                try {
                    importSW.start();
                    List<GPXEntry> inputGPXEntries = new GPXFile().doImport(gpxFile.getAbsolutePath()).getEntries();
                    importSW.stop();
                    matchSW.start();
                    String fileName = gpxFile.getAbsolutePath().substring(gpxFile.getAbsolutePath().lastIndexOf("\\")+1);
                    gpxfiles.add("<a href=" + "http://127.0.0.1:8111/open_file?filename=" + gpxFile.toString() + " target=\"hiddenIframe\" >" + formatTextUml(fileName) + "</a>");
                    MatchResult mr = mapMatching.doWork(inputGPXEntries,htmlresult);
                    matchSW.stop();
                    System.out.println(gpxFile);
                    System.out.println("\tmatches:\t" + mr.getEdgeMatches().size() + ", gps entries:" + inputGPXEntries.size());
                    System.out.println("\tgpx length:\t" + (float) mr.getGpxEntriesLength() + " vs " + (float) mr.getMatchLength());
                    System.out.println("\tgpx time:\t" + mr.getGpxEntriesMillis() / 1000f + " vs " + mr.getMatchMillis() / 1000f);
                    
                    htmlresult.add("<small>");
                    htmlresult.add("matches: &ensp; " + mr.getEdgeMatches().size() + ", gps entries:" + inputGPXEntries.size() + "<br>");
                    htmlresult.add("gpx length: &ensp; " + (float) mr.getGpxEntriesLength() + " vs " + (float) mr.getMatchLength() + " meters <br>");
                    htmlresult.add("gpx time: &ensp; " + mr.getGpxEntriesMillis() / 1000f + " vs " + mr.getMatchMillis() / 1000f  + " seconds <br>");
                    htmlresult.add("</small>");
                    successcounter++;
                    
                    String outFile = gpxFile.getAbsolutePath() + ".res.gpx";
                    new GPXFile(mr).doExport(outFile);
                    fileName = fileName + ".res.gpx";
                    htmlresult.add("<a href=" + "http://127.0.0.1:8111/open_file?filename=" + outFile + " target=\"hiddenIframe\" >" + formatTextUml(fileName) + "</a>");

                } catch (Exception ex) {
                    importSW.stop();
                    matchSW.stop();
                    failcounter++;
                    //htmlresult.add("Problem with file " + gpxFile + " Error: " + ex.getMessage() + "<br>");
                    logger.error("Problem with file " + gpxFile + " Error: " + ex.getMessage());
                }
                
                if (!mapMatching.getFixLinkAdded() ) {
                     htmlresult.add("<td></td>");
                }

                htmlresultlist.add(htmlresult);
            }
            System.out.println("gps import took:" + importSW.getSeconds() + "s, match took: " + matchSW.getSeconds());

            if ("true".equals(args.get("htmlReport", "false")))
            {
                generateHtmlReport(gpxfiles,htmlresultlist, htmlReportFileName , firstEncoder.toString(), 
                                     successcounter, failcounter, gpxAccuracy, separatedSearchDistance, maxSearchMultiplier, forceRepair );
            }

        } else {
            System.out.println("Usage: Do an import once, then do the matching\n"
                    + "./map-matching action=import datasource=your.pbf\n"
                    + "./map-matching action=match gpx=your.gpx\n"
                    + "./map-matching action=match gpx=.*gpx\n\n");
        }
    }
    
    private void generateHtmlReport (List <String> gpxfiles, ArrayList <ArrayList<String>> findinglistoflists, String reportFileName, String vehicle, 
                                     int successcounter, 
                                     int failcounter,
                                     int gpxAccuracy,
                                     int separatedSearchDistance,
                                     int maxSearchMultiplier,
                                     boolean forceRepair
                                     )
    {
        StringBuilder html = new StringBuilder();
        html.append( "<!doctype html>\n" );
        html.append( "<html lang='en'>\n" );

        html.append( "<head>\n" );
        html.append( "<meta charset='utf-8' >\n" );
        html.append( "<title>Page for improving OSM data based on map-matched GPX files</title>\n" );
        html.append( "<style>");
        html.append( "body {");
          html.append( "color: #000;");
          html.append( "font-family: \"Helvetica Neue\", Helvetica, Arial, sans-serif;");
          html.append( "line-height: 1.0;");
          html.append( "color: #111111;");
          html.append( "font-size:50%");
          html.append( "background-color: white;");
          html.append( "margin: 0;");
          html.append( "td { border:thin solid black; }");
          html.append( "min-width: 800px;");
        html.append( "}");

        html.append( "/* iframe for josm and rawedit links */");
        html.append( "iframe#hiddenIframe {");
          html.append( "display: none;");
          html.append( "position: absolute;");
        html.append( "}");
        html.append( "</style>");
        
        html.append( "</head>\n\n" );

        html.append( "<body>\n" );
        
        html.append("<h2>Result of GraphHopper map matching for vehicle " + vehicle + " " + successcounter + " OK, " + failcounter + " failed.</h2>");
        html.append("Algorithm parameters: gpxAccuracy= " + gpxAccuracy + ", separatedSearchDistance=" + separatedSearchDistance + ", maxSearchMultiplier=" + maxSearchMultiplier + ", forceRepair=" + forceRepair + "<br><br>");
        
        html.append("<table border=\"1\" cellpadding=\"2\" frame=\"box\">");
        html.append("<colgroup width=\"200\" span=\"3\"> <col width=\"70\"><col width=\"250\"><col width=\"750\"></colgroup>");
        
        html.append("<tr>");
        html.append("<td>" + "File #" +"</td>");
        html.append("<td>" + "GPX input" +"</td>");
        html.append("<td>" + "Result links" + "</td>");
        html.append("<td>" + "Fix links" + "</td>");
        html.append("</tr>");
        
        int i = 0;
        for ( String josmlink : gpxfiles ) {
              html.append("<tr>");
              html.append("<td>" + "File " + (i+1)  +"</td>");
              html.append("<td>" + josmlink +"</td>");
              
              html.append("<td>");
              List <String> findings = findinglistoflists.get(i);
              for ( String reportline : findings ) {
                  html.append(reportline);
              }
              html.append("</td>");
              
              html.append("</tr>");
              i++;
        }
        html.append("</table>");
        
        html.append("<iframe id=\"hiddenIframe\" name=\"hiddenIframe\"></iframe>");

        html.append( "</body>\n\n" );

        html.append( "</html>" );
        try {
            File reportFile = new File (reportFileName);
            
            PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(reportFile),
                StandardCharsets.UTF_8), true);
            
            out.println(html);
            out.close();
         } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            System.out.println("Could not create file" + reportFileName );
        }
    }
}
