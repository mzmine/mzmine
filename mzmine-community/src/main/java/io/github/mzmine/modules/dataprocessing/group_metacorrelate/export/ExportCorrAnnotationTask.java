/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.export;

import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.datamodel.identities.iontype.networks.IonNetworkRelation;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.TxtWriter;
import java.io.File;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExportCorrAnnotationTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(ExportCorrAnnotationTask.class.getName());
  private final Double progress = 0d;
  private final FeatureList[] featureLists;
  private final File filename;
  private final FeatureListRowsFilter filter;
  private final Type[] exportTypes;
  private final boolean allInOneFile;
  private final double minR;
  private boolean exportAnnotationEdges = true;
  private boolean exportIinRelationships = false;
  private boolean mergeLists = false;

  private final List<File> exportedFiles = new ArrayList<>();

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   */
  public ExportCorrAnnotationTask(final ParameterSet parameterSet,
      final ModularFeatureList[] featureLists, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.featureLists = featureLists;

    // tolerances
    filename = parameterSet.getParameter(ExportCorrAnnotationParameters.filename).getValue();
    exportAnnotationEdges = parameterSet.getParameter(ExportCorrAnnotationParameters.exportIIN)
        .getValue();
    exportIinRelationships = parameterSet.getParameter(
        ExportCorrAnnotationParameters.exportIINRelationship).getValue();
    filter = parameterSet.getParameter(ExportCorrAnnotationParameters.filter).getValue();
    exportTypes = parameterSet.getParameter(ExportCorrAnnotationParameters.exportTypes).getValue();
    allInOneFile = parameterSet.getParameter(ExportCorrAnnotationParameters.allInOneFile)
        .getValue();
    minR = 0;
  }

  /**
   * Create the task.
   */
  public ExportCorrAnnotationTask(FeatureList[] featureLists, File filename, double minR,
      FeatureListRowsFilter filter, boolean exportAnnotationEdges, boolean exportIinRelationships,
      boolean mergeLists, boolean allInOneFile, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.featureLists = featureLists;
    this.filename = filename;
    this.allInOneFile = allInOneFile;
    this.minR = minR;
    this.filter = filter;
    this.exportAnnotationEdges = exportAnnotationEdges;
    this.exportIinRelationships = exportIinRelationships;
    this.mergeLists = mergeLists;
    exportTypes = new Type[0];
  }

  public boolean exportIonIdentityEdges(FeatureList featureList, File filename, Double progress,
      AbstractTask task) {
    LOG.info("Export ion identity networking edges file");
    NumberFormats formats = MZmineCore.getConfiguration().getExportFormats();
    NumberFormat mzForm = formats.mzFormat();
    NumberFormat corrForm = formats.scoreFormat();
    try {
      // copy of list to not sort the original
      List<FeatureListRow> rows = new ArrayList<>(featureList.getRows());
      rows.sort(new FeatureListRowSorter(SortingProperty.ID, SortingDirection.Ascending));
      StringBuilder ann = createHeader();

      AtomicInteger added = new AtomicInteger(0);
      // for all rows
      for (FeatureListRow r : rows) {
        if (!filter.accept(r)) {
          continue;
        }

        if (task != null && task.isCanceled()) {
          return false;
        }
        // row1
        int rowID = r.getID();

        //
        final List<IonIdentity> ions = r.getIonIdentities();
        if (ions == null) {
          continue;
        }
        ions.forEach(adduct -> {
          final IonNetwork network = adduct.getNetwork();

          // add all connection for ids>rowID to avoid duplicates
          network.entrySet().stream().filter(Objects::nonNull)
              .filter(e -> e.getKey().getID() > rowID).forEach(e -> {
                FeatureListRow link = e.getKey();
                if (filter.accept(link)) {
                  IonIdentity id = e.getValue();
                  double dmz = Math.abs(r.getAverageMZ() - link.getAverageMZ());
                  // the data
                  exportEdge(ann, "MS1 annotation", rowID, e.getKey().getID(),
                      corrForm.format((id.getScore() + adduct.getScore()) / 2d), //
                      id.getAdduct() + " " + adduct.getAdduct() + " dm/z=" + mzForm.format(dmz));
                  added.incrementAndGet();
                }
              });
        });
      }

      LOG.log(Level.INFO, "Annotation edges exported {0}", added.get());

      // export ann edges
      // Filename

      if (added.get() > 0) {
        String CString = "{}";
        boolean check = filename.getPath().contains(CString);
        if (check) {
          File curFile = filename;
          String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
          String newFilename = filename.getPath().replaceAll(Pattern.quote(CString), cleanPlName);
          curFile = new File(newFilename);
          writeToFile(ann.toString(), curFile, "_edges_msannotation");
        } else {
          writeToFile(ann.toString(), filename, "_edges_msannotation");
        }
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      throw new MSDKRuntimeException(e);
    }
  }

  public boolean exportIINRelationships(FeatureList pkl, File filename, Double progress,
      AbstractTask task) {
    LOG.fine("Export IIN relationships edge file");

    try {
      StringBuilder ann = createHeader();

      AtomicInteger added = new AtomicInteger(0);

      IonNetwork[] nets = IonNetworkLogic.getAllNetworks(pkl, true);
      for (IonNetwork n : nets) {
        Map<IonNetwork, IonNetworkRelation> relations = n.getRelations();
        if (relations != null && !relations.isEmpty()) {
          for (Map.Entry<IonNetwork, IonNetworkRelation> rel : relations.entrySet()) {
            // export all relations where n.id is smaller than the related network
            if (rel.getValue().isLowestIDNetwork(n)) {
              // relationship can be between multiple nets
              for (IonNetwork net2 : rel.getValue().getAllNetworks()) {
                if (net2.equals(n)) {
                  continue;
                }

                // find best two nodes
                FeatureListRow[] rows = getBestRelatedRows(n, net2);
                // export lowest mz -> highest mz
                if (rows[0].getAverageMZ() > rows[1].getAverageMZ()) {
                  exportEdge(ann, "IIN M relationship", rows[1].getID(), rows[0].getID(), "0", //
                      rel.getValue().getName(net2));
                } else {
                  exportEdge(ann, "IIN M relationship", rows[0].getID(), rows[1].getID(), "0", //
                      rel.getValue().getName(n));
                }

                added.incrementAndGet();
              }
            }
          }
        }
      }
      LOG.info("IIN relationship edges exported " + added.get());

      // export ann edges
      // Filename
      if (added.get() > 0) {
        String CString = "{}";
        boolean check = filename.getPath().contains(CString);
        if (check) {
          File curFile = filename;
          String cleanPlName = pkl.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
          String newFilename = filename.getPath().replaceAll(Pattern.quote(CString), cleanPlName);
          curFile = new File(newFilename);
          writeToFile(ann.toString(), curFile, "_edges_iin_relations");
        } else {
          writeToFile(ann.toString(), filename, "_edges_iin_relations");
        }
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      throw new MSDKRuntimeException(e);
    }
  }

  /**
   * Filters rows by row filter (MS/MS, IIN, ...) and finds the pair with the highest intensity sum
   * to represent the relationship between the two {@link IonNetwork}
   *
   * @param netA network a
   * @param netB network netB
   * @return an array[2] of the representative rows for netA and netB or null if there was no
   * relationship or no pair of rows matching the filter
   */
  @Nullable
  private FeatureListRow[] getBestRelatedRows(IonNetwork netA, IonNetwork netB) {
    FeatureListRow[] rows = new FeatureListRow[2];
    double sumIntensity = 0;
    for (Map.Entry<FeatureListRow, IonIdentity> entryA : netA.entrySet()) {
      FeatureListRow rowA = entryA.getKey();
      if (filter.accept(rowA)) {
        IonIdentity iinA = entryA.getValue();
        for (Map.Entry<FeatureListRow, IonIdentity> entryB : netB.entrySet()) {
          FeatureListRow rowB = entryB.getKey();
          if (filter.accept(rowB)) {
            IonIdentity iinB = entryB.getValue();
            if (iinA.getAdduct().equals(iinB.getAdduct())) {
              // find pair with the highest sum intensity (that match the row filter)
              double sum = rowA.getMaxHeight() + rowB.getMaxHeight();
              if (sum >= sumIntensity) {
                sumIntensity = sum;
                rows[0] = rowA;
                rows[1] = rowB;
              }
            }
          }
        }
      }
    }
    if (rows[0] == null) {
      return null;
    } else {
      return rows;
    }
  }

  /**
   * Exports fields in the correct order
   *
   * @param builder    creates the whole data string
   * @param type       edge type
   * @param id1        id of row a
   * @param id2        id of row b
   * @param score      edge score
   * @param annotation edge annotation
   */
  private void exportEdge(StringBuilder builder, String type, int id1, int id2, String score,
      String annotation) {
    // the data
    Object[] data = new Object[EDGES.values().length];

    // add all data
    for (int d = 0; d < EDGES.values().length; d++) {
      switch (EDGES.values()[d]) {
        case ID1 -> data[d] = String.valueOf(id1);
        case ID2 -> data[d] = String.valueOf(id2);
        case EdgeType -> data[d] = type;
        case Annotation -> data[d] = annotation;
        case Score -> data[d] = score;
      }
    }
    // replace null
    for (int j = 0; j < data.length; j++) {
      if (data[j] == null) {
        data[j] = "";
      }
    }
    // add data
    builder.append(StringUtils.join(data, ','));
    builder.append("\n");
  }

  /**
   * Create the standard header
   *
   * @return adds the header and new line to a new String Builder
   */
  @NotNull
  private StringBuilder createHeader() {
    StringBuilder ann = new StringBuilder();
    // only write header if not all in one file
    if (!allInOneFile) {
      writeHeader(ann);
    }
    return ann;
  }

  private void writeHeader(StringBuilder ann) {
    ann.append(StringUtils.join(EDGES.values(), ','));
    ann.append("\n");
  }

  /**
   * Write to a file. Checks allInOneFile to combine all into one file.
   *
   * @param data     the data to be exported
   * @param filename the filename
   * @param suffix   the suffix that is added to the filename (only used if no allInOneFile is
   *                 false)
   */
  private void writeToFile(String data, File filename, String suffix) {
    File realFile = FileAndPathUtil.eraseFormat(filename);
    String name = allInOneFile ? realFile.getName() : realFile.getName() + suffix;
    realFile = FileAndPathUtil.getRealFilePath(filename.getParentFile(),
        realFile.getName() + suffix, ".csv");

    if (allInOneFile) {
      realFile = FileAndPathUtil.getRealFilePath(filename.getParentFile(), realFile.getName(),
          ".csv");
      boolean append = exportedFiles.size() > 0;
      TxtWriter.write(data, realFile, append);
      LOG.log(Level.INFO, "File {1}: {0}",
          new Object[]{realFile.getAbsolutePath(), append ? "created" : "appended"});
    } else {
      realFile = FileAndPathUtil.getRealFilePath(filename.getParentFile(),
          realFile.getName() + suffix, ".csv");
      TxtWriter.write(data, realFile);
      LOG.log(Level.INFO, "File created: {0}", realFile.getAbsolutePath());
    }

    if (!exportedFiles.contains(realFile)) {
      exportedFiles.add(realFile);
    }
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public String getTaskDescription() {
    if (featureLists != null && featureLists.length > 0) {
      return "Export adducts and correlation networks " + featureLists[0].getName() + " ";
    } else {
      return "";
    }
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    try {

      if (mergeLists) {
        exportMergedLists();
      } else {
        exportLists();
      }
    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Export of correlation and MS annotation results error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
      return;
    }

    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  private void exportLists() {
    for (FeatureList featureList : featureLists) {
      LOG.log(Level.FINE, "Starting export of adduct and correlation networks {0}",
          featureList.getName());

      // exports all row-2-row relationship maps
      var rowMaps = featureList.getRowMaps();
      if (exportTypes != null) {
        for (Type type : exportTypes) {
          rowMaps.getRowsMap(type.toString())
              .ifPresent(map -> exportMap(type, map.values(), featureList.getName()));
        }
      }

      // export edges of annotations
      if (exportAnnotationEdges) {
        exportIonIdentityEdges(featureList, filename, progress, this);
      }

      // relationships between ion identity networks (+O) ...
      if (exportIinRelationships) {
        exportIINRelationships(featureList, filename, progress, this);
      }
    }
  }

  /**
   * Export all relationships to one csv file
   *
   * @param type          type.toString is used as a file suffix
   * @param relationships list of relationships to export to one file
   */
  private void exportMap(Type type, Collection<RowsRelationship> relationships, String fname) {
    // creates the standard header
    StringBuilder ann = createHeader();
    for (RowsRelationship rel : relationships) {
      // only export if both rows match
      if (filter.accept(rel.getRowA()) && filter.accept(rel.getRowB())) {
        exportEdge(ann, rel.getType(), rel.getRowA().getID(), rel.getRowB().getID(),
            rel.getScoreFormatted(), rel.getAnnotation());
      }
    }
    String suffix = "_" + type.toString().toLowerCase().replaceAll(" ", "_");
    String CString = "{}";
    boolean check = filename.getPath().contains(CString);
    if (check) {
      File curFile = filename;
      String cleanPlName = fname.replaceAll("[^a-zA-Z0-9.-]", "_");
      String newFilename = filename.getPath().replaceAll(Pattern.quote(CString), cleanPlName);
      curFile = new File(newFilename);
      writeToFile(ann.toString(), filename, suffix);
    } else {
      writeToFile(ann.toString(), filename, suffix);
    }
  }

  private void exportMergedLists() {
    LOG.info("Starting export of adduct and correlation networks (merged) for n(peaklists)="
             + featureLists.length);
    // export edges of annotations
    if (exportAnnotationEdges) {
      exportAnnotationEdgesMerged(featureLists, filename,
          filter.equals(FeatureListRowsFilter.ONLY_WITH_MS2), progress, this);
    }
  }

  /**
   * Merged peak lists
   *
   * @param featureLists
   * @param filename
   * @param limitToMSMS
   * @param progress
   * @param task
   * @return
   */
  public boolean exportAnnotationEdgesMerged(FeatureList[] featureLists, File filename,
      boolean limitToMSMS, Double progress, AbstractTask task) {
    LOG.info("Export annotation edge file");

    HashMap<String, Integer> renumbered = new HashMap<>();
    int lastID = 0;
    for (FeatureList pkl : featureLists) {
      for (FeatureListRow r : pkl.getRows()) {
        if (!filter.accept(r)) {
          continue;
        }
        renumbered.put(getRowMapKey(r), lastID);
        lastID++;
      }
    }

    NumberFormats formats = MZmineCore.getConfiguration().getExportFormats();
    NumberFormat mzForm = formats.mzFormat();
    NumberFormat corrForm = formats.scoreFormat();
    try {
      StringBuilder ann = createHeader();

      AtomicInteger added = new AtomicInteger(0);

      for (FeatureList pkl : featureLists) {
        List<FeatureListRow> rows = new ArrayList<>(pkl.getRows());
        rows.sort(new FeatureListRowSorter(SortingProperty.ID, SortingDirection.Ascending));

        // for all rows
        for (FeatureListRow r : rows) {
          if (!filter.accept(r)) {
            continue;
          }

          if (task != null && task.isCanceled()) {
            return false;
          }
          // row1
          int rowID = r.getID();

          //
          final List<IonIdentity> ions = r.getIonIdentities();
          if (ions == null) {
            continue;
          }
          ions.forEach(adduct -> {
            final IonNetwork network = adduct.getNetwork();

            // add all connection for ids>rowID
            network.entrySet().stream().filter(Objects::nonNull)
                .filter(e -> e.getKey().getID() > rowID).forEach(e -> {
                  FeatureListRow link = e.getKey();
                  if (!limitToMSMS || link.getMostIntenseFragmentScan() != null) {
                    IonIdentity id = e.getValue();
                    double dmz = Math.abs(r.getAverageMZ() - link.getAverageMZ());

                    // convert ids for merging
                    Integer id1 = renumbered.get(getRowMapKey(r));
                    Integer id2 = renumbered.get(getRowMapKey(e.getKey()));

                    // the data
                    exportEdge(ann, "MS1 annotation", id1, id2,
                        corrForm.format((id.getScore() + adduct.getScore()) / 2d), //
                        id.getAdduct() + " " + adduct.getAdduct() + " dm/z=" + mzForm.format(dmz));
                    added.incrementAndGet();
                  }
                });
          });
        }

        LOG.info("Annotation edges exported " + added.get());
      }

      // export ann edges
      // Filename
      if (added.get() > 0) {
        writeToFile(ann.toString(), filename, "_edges_msannotation");
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      throw new MSDKRuntimeException(e);
    }
  }

  private String getRowMapKey(FeatureListRow r) {
    String rawnames = r.getRawDataFiles().stream().map(RawDataFile::getName)
        .collect(Collectors.joining(","));
    return rawnames + r.getID();
  }

  public enum EDGES {
    ID1, ID2, EdgeType, Score, Annotation
  }
}