/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.GNPSSpectralLibMatchSummaryType;
import io.github.mzmine.datamodel.features.types.GNPSSpectralLibraryMatchType;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch.ATT;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceGraphML;

/**
 * Import GNPS library matches form a graphml network file from FBMN or IIMN
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class GNPSResultsImportTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final ModularFeatureList featureList;
  private final File file;
  private final AtomicDouble progress = new AtomicDouble(0);
  private final ParameterSet parameters;

  public GNPSResultsImportTask(ParameterSet parameters, ModularFeatureList featureList) {
    super(null); // no new data stored -> null
    this.parameters = parameters;
    this.featureList = featureList;
    file = parameters.getParameter(GNPSResultsImportParameters.FILE).getValue();
  }

  /**
   * Adds a GNPS library match to the row
   */
  public static void addGNPSLibraryMatchToRow(ModularFeatureListRow row,
      GNPSLibraryMatch identity) {
    // add column first if needed
    row.get(GNPSSpectralLibraryMatchType.class).get(GNPSSpectralLibMatchSummaryType.class)
        .add(identity);
  }

  @Override
  public double getFinishedPercentage() {
    return progress.get();
  }

  @Override
  public String getTaskDescription() {
    return "Importing GNPS results for " + featureList + " in file " + file;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Importing GNPS results for " + featureList.getName());

    // remove zero ids from edges to prevent exception
    removeZeroIDFromEdge(file);

    Graph graph = new DefaultGraph("GNPS");
    if (importGraphData(graph, file)) {
      // import library matches from nodes
      featureList.addRowType(new GNPSSpectralLibraryMatchType());
      importLibraryMatches(graph);

      // Add task description to peakList
      featureList.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod("GNPS FBMN/IIMN results import",
              GNPSResultsImportModule.class, parameters));

      setStatus(TaskStatus.FINISHED);
      logger.info("Finished import of GNPS results for " + featureList.getName());
    }
  }

  /**
   * All edges have id=0 - this causes an exception. Replace all zero ids and save the file
   *
   * @param file a graphml file
   */
  private void removeZeroIDFromEdge(File file) {
    try {
      logger.info("replacing zero ids in graphml");
      Path path = Paths.get(file.getAbsolutePath());
      Stream<String> lines = Files.lines(path);
      List<String> replaced =
          lines.map(line -> line.replaceAll("edge id=\"0\"", "edge")).collect(Collectors.toList());
      Files.write(path, replaced);
      lines.close();
      logger.info("zero ids in graphml replaces");
    } catch (IOException e) {
      logger.log(Level.SEVERE, "graphml NOT LOADED: " + file.getAbsolutePath(), e);
      setErrorMessage("Cannot load graphml file: " + file.getAbsolutePath());
      setStatus(TaskStatus.ERROR);
      cancel();
    }
  }

  /**
   * Import all library matches from the nodes
   *
   * @param graph the loaded graph from graphml (FBMN or IIMN network)
   */
  private void importLibraryMatches(Graph graph) {
    AtomicInteger missingRows = new AtomicInteger(0);
    AtomicInteger libraryMatches = new AtomicInteger(0);
    // go through all nodes and add info
    graph.nodes().forEach(node -> {
      int id = Integer.parseInt(node.getId());
      // has library match?
      String compoundName = (String) node.getAttribute(ATT.COMPOUND_NAME.getKey());
      FeatureListRow rowtemp = featureList.findRowByID(id);
      if (rowtemp instanceof ModularFeatureListRow row) {
        if (compoundName != null && !compoundName.isEmpty()) {
          libraryMatches.getAndIncrement();
          // add identity
          // find all results
          HashMap<String, Object> results = new HashMap<>();
          for (ATT att : ATT.values()) {
            Object result = node.getAttribute(att.getKey());
            if (result != null) {
              results.put(att.getKey(), result);
            }
          }

          // add identity
          GNPSLibraryMatch identity = new GNPSLibraryMatch(results, compoundName);
          addGNPSLibraryMatchToRow(row, identity);
        }
      } else {
        missingRows.getAndIncrement();
      }
    });

    if (missingRows.get() > 0) {
      logger.info(missingRows.get()
                  + " rows (features) that were present in the GNPS results were not found in the peakList. Check if you selected the correct feature list, did some filtering or applied renumbering (IDs have to match).");
    }

    logger.info(libraryMatches.get() + " rows found with library matches");
  }

  private boolean importGraphData(Graph graph, File file) {
    boolean result = true;
    FileSource fs = null;
    logger.info("Importing graphml data");
    try {
      fs = new FileSourceGraphML();
      fs.addSink(graph);
      fs.readAll(file.getAbsolutePath());
      logger.info(() -> MessageFormat.format("GNPS results: nodes={0} edges={1}",
          graph.getNodeCount(), graph.getEdgeCount()));
    } catch (IOException e) {
      logger.log(Level.SEVERE, "graphml NOT LOADED: " + file.getAbsolutePath(), e);
      setErrorMessage("Cannot load graphml file: " + file.getAbsolutePath());
      setStatus(TaskStatus.ERROR);
      result = false;
    } finally {
      if (fs != null) {
        fs.removeSink(graph);
      }
    }
    return result;
  }

  /**
   * different edge types
   */
  public enum EdgeAtt {
    EDGE_TYPE("EdgeType", String.class), // edgetype
    EDGE_SCORE("EdgeScore", Double.class), EDGE_ANNOTATION("EdgeAnnotation", String.class);

    private final String key;
    private final Class c;

    EdgeAtt(String key, Class c) {
      this.c = c;
      this.key = key;
    }

    public Class getValueClass() {
      return c;
    }

    public String getKey() {
      return key;
    }
  }

  public enum EdgeType {
    MS1_ANNOTATION("MS1 annotation"), COSINE("Cosine");

    private final String key;

    EdgeType(String key) {
      this.key = key;
    }
  }

}
