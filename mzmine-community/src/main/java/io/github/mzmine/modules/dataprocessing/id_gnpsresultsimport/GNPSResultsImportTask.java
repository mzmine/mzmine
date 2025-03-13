/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.correlation.R2RMS2CosineSimilarityGNPS;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.types.annotations.GNPSSpectralLibraryMatchesType;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch.ATT;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowIdCache;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceGraphML;
import org.jetbrains.annotations.NotNull;

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
  private final FeatureListRowIdCache rowIdCache;

  public GNPSResultsImportTask(ParameterSet parameters, ModularFeatureList featureList,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.parameters = parameters;
    this.featureList = featureList;
    this.rowIdCache = new FeatureListRowIdCache(featureList);
    file = parameters.getParameter(GNPSResultsImportParameters.FILE).getValue();
  }

  /**
   * Adds a GNPS library match to the row
   */
  public static void addGNPSLibraryMatchToRow(ModularFeatureListRow row,
      GNPSLibraryMatch identity) {
    // add column first if needed
    List<GNPSLibraryMatch> list = Objects.requireNonNullElse(
        row.get(GNPSSpectralLibraryMatchesType.class), new ArrayList<>());
    list.add(identity);
    row.set(GNPSSpectralLibraryMatchesType.class, list);
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

    // initialize row cache to reduce index of calls
    rowIdCache.preCacheRowIds();

    // remove zero ids from edges to prevent exception
    removeZeroIDFromEdge(file);

    Graph graph = new DefaultGraph("GNPS");
    if (importGraphData(graph, file)) {
      // import library matches from nodes
      featureList.addRowType(new GNPSSpectralLibraryMatchesType());
      importLibraryMatches(graph);

      // import all edges between two feature list rows
      importNetworkEdges(graph);

      // Add task description to peakList
      featureList.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod("GNPS FBMN/IIMN results import",
              GNPSResultsImportModule.class, parameters, getModuleCallDate()));

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
      try (Stream<String> lines = Files.lines(path)) {
        List<String> replaced = lines.map(line -> line.replaceAll("edge id=\"0\"", "edge")
            .replaceAll("edge id=\"Cosine\"", "edge")).collect(Collectors.toList());
        Files.write(path, replaced);
        lines.close();
        logger.info("zero ids in graphml replaces");
      }
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
      FeatureListRow rowtemp = getRow(node);
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

  /**
   * Import all edges between two feature list rows and add them to the feature list relationship
   * map
   *
   * @param graph the FBMN or IIMN network from GNPS
   */
  private void importNetworkEdges(Graph graph) {
    final R2RMap<R2RMS2CosineSimilarityGNPS> gnpsEdges = new R2RMap<>();
    graph.edges().forEach(edge -> {
      Node nodeA = edge.getNode0();
      Node nodeB = edge.getNode1();
      FeatureListRow rowA = getRow(nodeA);
      FeatureListRow rowB = getRow(nodeB);
      if (rowA != null && rowB != null) {
        double cosineScore = edge.getAttribute(EdgeAtt.EDGE_SCORE.getKey(), Double.class);
        String annotation = edge.getAttribute(EdgeAtt.EDGE_ANNOTATION.getKey(), String.class);
        String edgeType = edge.getAttribute(EdgeAtt.EDGE_TYPE.getKey(), String.class);
        gnpsEdges.add(rowA, rowB,
            new R2RMS2CosineSimilarityGNPS(rowA, rowB, cosineScore, annotation, edgeType));

        // TODO add all other edge attributes
      }
    });
    // add all edges to feature list
    featureList.getRowMaps().addAllRowsRelationships(gnpsEdges, Type.MS2_GNPS_COSINE_SIM);
  }

  /**
   * Caches the row.getID() to row for nodes
   *
   * @param node a node in an FBMN / IIMN network representing a feature list row (aligned by
   *             getID)
   * @return the row or null if a row with this ID is missing
   */
  private FeatureListRow getRow(Node node) {
    int id = Integer.parseInt(node.getId());
    return rowIdCache.get(id);
  }

  /**
   * Import the network data where nodes are {@link FeatureListRow}s
   *
   * @param graph the loaded FBMN or IIMN network
   * @param file  the graphml file
   * @return true if success, false otherwise
   */
  private boolean importGraphData(Graph graph, File file) {
    boolean result = true;
    FileSource fs = null;
    logger.info("Importing graphml data");
    try {
      fs = new FileSourceGraphML();
      fs.addSink(graph);
      fs.readAll(file.getAbsolutePath());
      logger.info(
          () -> MessageFormat.format("GNPS results: nodes={0} edges={1}", graph.getNodeCount(),
              graph.getEdgeCount()));
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
