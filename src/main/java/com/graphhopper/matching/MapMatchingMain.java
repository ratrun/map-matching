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
import com.graphhopper.routing.util.EncodingManager;
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

/**
 *
 * @author Peter Karich
 */
public class MapMatchingMain {

    public static void main(String[] args) {
        new MapMatchingMain().start(CmdArgs.read(args));
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ArrayList <ArrayList<String>> htmlresultlist = new ArrayList<ArrayList<String>>();
    private final List <String> gpxfiles = new ArrayList <String> ();

    private void start(CmdArgs args) {
        String action = args.get("action", "").toLowerCase();
        args.put("graph.location", "./graph-cache");
        if (action.equals("import")) {
            String vehicle = args.get("vehicle", "car").toLowerCase();
            args.put("graph.flagEncoders", vehicle);
            args.put("osmreader.osm", args.get("datasource", ""));
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
            mapMatching.setSeparatedSearchDistance(args.getInt("separatedSearchDistance", 500));
            mapMatching.setMaxSearchMultiplier(args.getInt("maxSearchMultiplier", 100));
            mapMatching.setForceRepair(args.getBool("forceRepair", false));

            // do the actual matching, get the GPX entries from a file or via stream
            String gpxLocation = args.get("gpx", "");
            File[] files;
            if (gpxLocation.contains("*")) {
                int lastIndex;
                File dir = new File(".");
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
            } else {
                files = new File[]{
                    new File(gpxLocation)
                };
            }

            logger.info("Now processing " + files.length + " files");
            StopWatch importSW = new StopWatch();
            StopWatch matchSW = new StopWatch();
            for (File gpxFile : files) {
                ArrayList <String>  htmlresult= new ArrayList <String> ();
                try {
                    importSW.start();
                    List<GPXEntry> inputGPXEntries = new GPXFile().doImport(gpxFile.getAbsolutePath()).getEntries();
                    importSW.stop();
                    matchSW.start();
                    String fileName = gpxFile.getAbsolutePath().substring(gpxFile.getAbsolutePath().lastIndexOf("\\")+1);
                    gpxfiles.add("<a href=" + "http://127.0.0.1:8111/open_file?filename=" + gpxFile.toString() + " target=\"hiddenIframe\" >" + fileName + "</a>");
                    MatchResult mr = mapMatching.doWork(inputGPXEntries,htmlresult);
                    matchSW.stop();
                    System.out.println(gpxFile);
                    System.out.println("\tmatches:\t" + mr.getEdgeMatches().size() + ", gps entries:" + inputGPXEntries.size());
                    System.out.println("\tgpx length:\t" + (float) mr.getGpxEntriesLength() + " vs " + (float) mr.getMatchLength());
                    System.out.println("\tgpx time:\t" + mr.getGpxEntriesMillis() / 1000f + " vs " + mr.getMatchMillis() / 1000f);
                    
                    htmlresult.add("<small>");
                    htmlresult.add("matches: &ensp; " + mr.getEdgeMatches().size() + ", gps entries:" + inputGPXEntries.size() + "<br>");
                    htmlresult.add("gpx length: &ensp; " + (float) mr.getGpxEntriesLength() + " vs " + (float) mr.getMatchLength() + "<br>");
                    htmlresult.add("gpx time: &ensp; " + mr.getGpxEntriesMillis() / 1000f + " vs " + mr.getMatchMillis() / 1000f  + "<br>");
                    htmlresult.add("</small>");
                    
                    String outFile = gpxFile.getAbsolutePath() + ".res.gpx";
                    new GPXFile(mr).doExport(outFile);
                    fileName = fileName + ".res.gpx";
                    htmlresult.add("<a href=" + "http://127.0.0.1:8111/open_file?filename=" + outFile + " target=\"hiddenIframe\" >" + fileName + "</a>");

                } catch (Exception ex) {
                    importSW.stop();
                    matchSW.stop();
                    //htmlresult.add("Problem with file " + gpxFile + " Error: " + ex.getMessage() + "<br>");
                    logger.error("Problem with file " + gpxFile + " Error: " + ex.getMessage());
                }
                htmlresultlist.add(htmlresult);
            }
            System.out.println("gps import took:" + importSW.getSeconds() + "s, match took: " + matchSW.getSeconds());

            generateHtmlReport(gpxfiles,htmlresultlist,args.get("report", ""), hopper.getEncodingManager().getSingle().toString());

        } else {
            System.out.println("Usage: Do an import once, then do the matching\n"
                    + "./map-matching action=import datasource=your.pbf\n"
                    + "./map-matching action=match gpx=your.gpx\n"
                    + "./map-matching action=match gpx=.*gpx\n\n");
        }
    }
    
    private void generateHtmlReport (List <String> gpxfiles, ArrayList <ArrayList<String>> findinglistoflists, String reportFileName, String vehicle)
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
        
        html.append("<h1>Result of map-match for vehicle " + vehicle + " </h1>");
        html.append("<table border=\"1\" cellpadding=\"2\" frame=\"box\">");
        html.append("<colgroup width=\"200\" span=\"3\"> <col width=\"70\"><col width=\"150\"><col width=\"700\"></colgroup>");
        int i = 0;
        for ( String josmlink : gpxfiles ) {
              html.append("<tr>");
              html.append("<td>" + "File " + i  +"</td>");
              html.append("<td>" + josmlink +"</td>");
              
              html.append("<td>");
              List <String> findings=findinglistoflists.get(i);
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
            PrintWriter out = new PrintWriter( reportFile );
            out.println(html);
            out.close();
         } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            System.out.println("Could not create file" + reportFileName );
        }
    }
}
