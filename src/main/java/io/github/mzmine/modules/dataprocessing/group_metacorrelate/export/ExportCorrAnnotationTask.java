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

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.export;


import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.RowGroup;
import io.github.mzmine.datamodel.features.RowGroupList;
import io.github.mzmine.datamodel.features.correlation.MS2SimilarityProviderGroup;
import io.github.mzmine.datamodel.features.correlation.R2RCorrelationData;
import io.github.mzmine.datamodel.features.correlation.R2RMS2Similarity;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.datamodel.identities.iontype.networks.IonNetworkRelationInterf;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.rowfilter.RowFilter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.TxtWriter;
import javafx.collections.ObservableList;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ExportCorrAnnotationTask extends AbstractTask {
  // Logger.
  private static final Logger LOG = Logger.getLogger(ExportCorrAnnotationTask.class.getName());

  public enum EDGES {
    ID1, ID2, EdgeType, Score, Annotation;
  }

  private Double progress = 0d;

  private boolean exportAnnotationEdges = true, exportCorrelationEdges = false;
  private boolean exportIinRelationships = false;
  private boolean exportMS2SimilarityEdges = false;
  private boolean exportMS2DiffSimilarityEdges = false;
  private double minR;
  private final ModularFeatureList[] featureLists;
  private File filename;

  private RowFilter filter;
  private boolean mergeLists = false;

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   */
  public ExportCorrAnnotationTask(final ParameterSet parameterSet, final ModularFeatureList[] featureLists) {
    super(null);
    this.featureLists = featureLists;

    // tolerances
    filename = parameterSet.getParameter(ExportCorrAnnotationParameters.FILENAME).getValue();
    minR = parameterSet.getParameter(ExportCorrAnnotationParameters.MIN_R).getValue();
    exportAnnotationEdges =
        parameterSet.getParameter(ExportCorrAnnotationParameters.EX_ANNOT).getValue();
    exportIinRelationships =
        parameterSet.getParameter(ExportCorrAnnotationParameters.EX_IIN_RELATIONSHIP).getValue();
    exportCorrelationEdges =
        parameterSet.getParameter(ExportCorrAnnotationParameters.EX_CORR).getValue();
    exportMS2DiffSimilarityEdges =
        parameterSet.getParameter(ExportCorrAnnotationParameters.EX_MS2_DIFF_SIMILARITY).getValue();
    exportMS2SimilarityEdges =
        parameterSet.getParameter(ExportCorrAnnotationParameters.EX_MS2_SIMILARITY).getValue();
    filter = parameterSet.getParameter(ExportCorrAnnotationParameters.FILTER).getValue();
  }

  /**
   * Create the task.
   *
   */
  public ExportCorrAnnotationTask(ModularFeatureList[] featureLists, File filename, double minR,
      RowFilter filter, boolean exportAnnotationEdges, boolean exportCorrelationEdges,
      boolean exportIinRelationships, boolean mergeLists) {
    super(null);
    this.featureLists = featureLists;
    this.filename = filename;
    this.minR = minR;
    this.filter = filter;
    this.exportAnnotationEdges = exportAnnotationEdges;
    this.exportCorrelationEdges = exportCorrelationEdges;
    this.exportIinRelationships = exportIinRelationships;
    this.mergeLists = mergeLists;
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public String getTaskDescription() {
    if (featureLists != null && featureLists.length > 0)
      return "Export adducts and correlation networks " + featureLists[0].getName() + " ";
    else
      return "";
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
    }

    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);
  }


  private void exportLists() {
    for (FeatureList featureList : featureLists) {
      LOG.info("Starting export of adduct and correlation networks" + featureList.getName());
      // export edges of annotations
      if (exportAnnotationEdges)
        exportAnnotationEdges(featureList, filename, filter.equals(RowFilter.ONLY_WITH_MS2), progress,
            this);

      // relationships between ion identity networks (+O) ...
      if (exportIinRelationships)
        exportIINRelationships(featureList, filename, filter.equals(RowFilter.ONLY_WITH_MS2), progress,
            this);

      // export MS2Similarity edges
      if (exportMS2DiffSimilarityEdges)
        exportMS2DiffSimilarityEdges(featureList, filename, filter, progress, this);
      if (exportMS2SimilarityEdges)
        exportMS2SimilarityEdges(featureList, filename, filter, progress, this);

      // export edges of corr
      if (exportCorrelationEdges)
        exportCorrelationEdges(featureList, filename, progress, this, minR, filter);
    }
  }

  private void exportMergedLists() {
    LOG.info("Starting export of adduct and correlation networks (merged) for n(peaklists)="
        + featureLists.length);
    // export edges of annotations
    if (exportAnnotationEdges)
      exportAnnotationEdgesMerged(featureLists, filename, filter.equals(RowFilter.ONLY_WITH_MS2),
          progress, this);
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
        if (!filter.filter(r))
          continue;
        renumbered.put(getRowMapKey(r), lastID);
        lastID++;
      }
    }

    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat corrForm = new DecimalFormat("0.000");
    try {
      StringBuilder ann = new StringBuilder();
      // add header
      ann.append(StringUtils.join(EDGES.values(), ','));
      ann.append("\n");

      AtomicInteger added = new AtomicInteger(0);

      for (FeatureList pkl : featureLists) {
        ObservableList<FeatureListRow> rows = pkl.getRows();
        Collections.sort(rows, new FeatureListRowSorter(SortingProperty.ID, SortingDirection.Ascending));


        // for all rows
        for (FeatureListRow r : rows) {
          if (!filter.filter(r))
            continue;

          if (task != null && task.isCanceled()) {
            return false;
          }
          // row1
          int rowID = r.getID();

          //
          if (r.hasIonIdentity()) {
            r.getIonIdentities().forEach(adduct -> {
              ConcurrentHashMap<FeatureListRow, IonIdentity> links = adduct.getPartner();

              // add all connection for ids>rowID
              links.entrySet().stream().filter(Objects::nonNull)
                  .filter(e -> e.getKey().getID() > rowID).forEach(e -> {
                    FeatureListRow link = e.getKey();
                    if (!limitToMSMS || link.getBestFragmentation() != null) {
                      IonIdentity id = e.getValue();
                      double dmz = Math.abs(r.getAverageMZ() - link.getAverageMZ());

                      // convert ids for merging
                      Integer id1 = renumbered.get(getRowMapKey(r));
                      Integer id2 = renumbered.get(getRowMapKey(e.getKey()));

                      // the data
                      exportEdge(ann, "MS1 annotation", id1, id2,
                          corrForm.format((id.getScore() + adduct.getScore()) / 2d), //
                          id.getAdduct() + " " + adduct.getAdduct() + " dm/z="
                              + mzForm.format(dmz));
                      added.incrementAndGet();
                    }
                  });
            });
          }
        }

        LOG.info("Annotation edges exported " + added.get() + "");
      }

      // export ann edges
      // Filename
      if (added.get() > 0) {
        writeToFile(ann.toString(), filename, "_edges_msannotation", ".csv");
        return true;
      } else
        return false;
    } catch (Exception e) {
      throw new MSDKRuntimeException(e);
    }
  }


  private String getRowMapKey(FeatureListRow r) {
    String rawnames = r.getRawDataFiles().stream().map(RawDataFile::getName)
        .collect(Collectors.joining(","));
    return rawnames + r.getID();
  }

  public static boolean exportAnnotationEdges(FeatureList pkl, File filename, boolean limitToMSMS,
      Double progress, AbstractTask task) {
    LOG.info("Export annotation edge file");
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat corrForm = new DecimalFormat("0.000");
    try {
      List<FeatureListRow> rows = pkl.getRows();
      Collections.sort(rows, new FeatureListRowSorter(SortingProperty.ID, SortingDirection.Ascending));
      StringBuilder ann = new StringBuilder();

      // add header
      ann.append(StringUtils.join(EDGES.values(), ','));
      ann.append("\n");

      AtomicInteger added = new AtomicInteger(0);
      // for all rows
      for (FeatureListRow r : rows) {

        if (limitToMSMS && r.getBestFragmentation() == null)
          continue;

        if (task != null && task.isCanceled()) {
          return false;
        }
        // row1
        int rowID = r.getID();

        //
        if (r.hasIonIdentity()) {
          r.getIonIdentities().forEach(adduct -> {
            ConcurrentHashMap<FeatureListRow, IonIdentity> links = adduct.getPartner();

            // add all connection for ids>rowID
            links.entrySet().stream().filter(Objects::nonNull)
                .filter(e -> e.getKey().getID() > rowID).forEach(e -> {
                  FeatureListRow link = e.getKey();
                  if (!limitToMSMS || link.getBestFragmentation() != null) {
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
      }

      LOG.info("Annotation edges exported " + added.get() + "");

      // export ann edges
      // Filename
      if (added.get() > 0) {
        writeToFile(ann.toString(), filename, "_edges_msannotation", ".csv");
        return true;
      } else
        return false;
    } catch (Exception e) {
      throw new MSDKRuntimeException(e);
    }
  }


  public static boolean exportIINRelationships(FeatureList pkl, File filename, boolean limitToMSMS,
      Double progress, AbstractTask task) {
    LOG.info("Export IIN relationships edge file");
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat corrForm = new DecimalFormat("0.000");

    try {
      StringBuilder ann = new StringBuilder();

      // add header
      ann.append(StringUtils.join(EDGES.values(), ','));
      ann.append("\n");

      AtomicInteger added = new AtomicInteger(0);

      IonNetwork[] nets = IonNetworkLogic.getAllNetworks(pkl, true);
      for (IonNetwork n : nets) {
        Map<IonNetwork, IonNetworkRelationInterf> relations = n.getRelations();
        if (relations != null && !relations.isEmpty()) {
          for (Map.Entry<IonNetwork, IonNetworkRelationInterf> rel : relations.entrySet()) {
            // export all relations where n.id is smaller than the related network
            if (rel.getValue().isLowestIDNetwork(n)) {
              // relationship can be between multiple nets
              for (IonNetwork net2 : rel.getValue().getAllNetworks()) {
                if (net2.equals(n))
                  continue;

                // find best two nodes
                FeatureListRow[] rows = getBestRelatedRows(n, net2, limitToMSMS);
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
      LOG.info("IIN relationship edges exported " + added.get() + "");

      // export ann edges
      // Filename
      if (added.get() > 0) {
        writeToFile(ann.toString(), filename, "_edges_iin_relations", ".csv");
        return true;
      } else
        return false;
    } catch (Exception e) {
      throw new MSDKRuntimeException(e);
    }
  }

  private static FeatureListRow[] getBestRelatedRows(IonNetwork a, IonNetwork b, boolean limitToMSMS) {
    FeatureListRow[] rows = new FeatureListRow[2];
    double sumIntensity = 0;
    for (Map.Entry<FeatureListRow, IonIdentity> entryA : a.entrySet()) {
      if (!limitToMSMS || entryA.getKey().getBestFragmentation() != null) {
        IonIdentity iinA = entryA.getValue();
        for (Map.Entry<FeatureListRow, IonIdentity> entryB : b.entrySet()) {
          if (!limitToMSMS || entryB.getKey().getBestFragmentation() != null) {
            IonIdentity iinB = entryB.getValue();
            if (iinA.getAdduct().equals(iinB.getAdduct())) {
              double sum = entryA.getKey().getAverageHeight() + entryB.getKey().getAverageHeight();
              if (sum >= sumIntensity) {
                sumIntensity = sum;
                rows[0] = entryA.getKey();
                rows[1] = entryB.getKey();
              }
            }
          }
        }
      }
    }
    if (rows[0] == null) {
      try {
        rows[0] = a.keySet().iterator().next();
        rows[1] = b.keySet().iterator().next();
      } catch (Exception ex) {
      }
    }
    return rows;
  }

  public static boolean exportMS2SimilarityEdges(FeatureList pkl, File filename, RowFilter filter,
      Double progress, AbstractTask task) {
    try {
      RowGroupList groups = pkl.getGroups();
      if (groups != null && !groups.isEmpty()) {
        LOG.info("Export MS2 similarities edge file");
        NumberFormat corrForm = new DecimalFormat("0.000");
        NumberFormat overlapForm = new DecimalFormat("0.0");

        StringBuilder ann = new StringBuilder();
        // add header
        ann.append(StringUtils.join(EDGES.values(), ','));
        ann.append("\n");
        AtomicInteger added = new AtomicInteger(0);

        for (RowGroup g : groups) {
          if (task != null && task.isCanceled()) {
            return false;
          }

          if (g instanceof MS2SimilarityProviderGroup) {
            R2RMap<R2RMS2Similarity> map = ((MS2SimilarityProviderGroup) g).getMS2SimilarityMap();
            for (Map.Entry<String, R2RMS2Similarity> e : map.entrySet()) {
              R2RMS2Similarity r2r = e.getValue();
              if (r2r.getDiffAvgCosine() == 0 && r2r.getDiffMaxOverlap() == 0)
                continue;
              FeatureListRow a = r2r.getA();
              FeatureListRow b = r2r.getB();
              // no self-loops
              if (a.getID() != b.getID() && filter.filter(a) && filter.filter(b)) {
                // the data
                exportEdge(ann, "MS2 sim", a.getID(), b.getID(),
                    corrForm.format(r2r.getDiffAvgCosine()), //
                    MessageFormat.format("cos={0} ({1})", corrForm.format(r2r.getDiffAvgCosine()),
                        overlapForm.format(r2r.getDiffMaxOverlap())));
                added.incrementAndGet();
              }
            }
          }
        }

        LOG.info("MS2 similarity edges exported " + added.get() + "");

        // export ann edges
        // Filename
        if (added.get() > 0) {
          writeToFile(ann.toString(), filename, "_edges_ms2similarity", ".csv");
          return true;
        } else
          return false;
      }
    } catch (Exception e) {
      throw new MSDKRuntimeException(e);
    }
    return false;
  }

  public static boolean exportMS2DiffSimilarityEdges(FeatureList pkl, File filename, RowFilter filter,
      Double progress, AbstractTask task) {
    try {
      RowGroupList groups = pkl.getGroups();
      if (groups != null && !groups.isEmpty()) {
        LOG.info("Export MS2 diff similarities edge file");
        NumberFormat corrForm = new DecimalFormat("0.000");
        NumberFormat overlapForm = new DecimalFormat("0.0");

        StringBuilder ann = new StringBuilder();
        // add header
        ann.append(StringUtils.join(EDGES.values(), ','));
        ann.append("\n");
        AtomicInteger added = new AtomicInteger(0);

        for (RowGroup g : groups) {
          if (task != null && task.isCanceled()) {
            return false;
          }

          if (g instanceof MS2SimilarityProviderGroup) {
            R2RMap<R2RMS2Similarity> map = ((MS2SimilarityProviderGroup) g).getMS2SimilarityMap();
            for (Map.Entry<String, R2RMS2Similarity> e : map.entrySet()) {
              R2RMS2Similarity r2r = e.getValue();
              if (r2r.getSpectralAvgCosine() == 0 && r2r.getSpectralMaxOverlap() == 0)
                continue;
              FeatureListRow a = r2r.getA();
              FeatureListRow b = r2r.getB();
              // no self-loops
              if (a.getID() != b.getID() && filter.filter(a) && filter.filter(b)) {
                // the data
                exportEdge(ann, "MS2 diff sim", a.getID(), b.getID(),
                    corrForm.format(r2r.getSpectralAvgCosine()), //
                    MessageFormat.format("diff cos={0} ({1})",
                        corrForm.format(r2r.getSpectralAvgCosine()),
                        overlapForm.format(r2r.getSpectralMaxOverlap())));
                added.incrementAndGet();
              }
            }
          }
        }

        LOG.info("MS2 diff similarity edges exported " + added.get() + "");

        // export ann edges
        // Filename
        if (added.get() > 0) {
          writeToFile(ann.toString(), filename, "_edges_ms2diffsimilarity", ".csv");
          return true;
        } else
          return false;
      }
    } catch (Exception e) {
      throw new MSDKRuntimeException(e);
    }
    return false;
  }

  public static boolean exportCorrelationEdges(FeatureList pkl, File filename, Double progress,
      AbstractTask task, double minCorr, RowFilter filter) {

    NumberFormat corrForm = new DecimalFormat("0.000");
    try {
      StringBuilder ann = new StringBuilder();
      // add header
      ann.append(StringUtils.join(EDGES.values(), ','));
      ann.append("\n");

      AtomicInteger added = new AtomicInteger(0);
      // for all rows
      R2RCorrelationData.streamFrom(pkl).filter(r2r -> r2r.getAvgShapeR() >= minCorr)
          .forEach(r2r -> {
            FeatureListRow a = r2r.getRowA();
            FeatureListRow b = r2r.getRowB();
            //
            boolean export = true;
            if (!filter.equals(RowFilter.ALL)) {
              // only export rows with MSMS
              export = filter.filter(a) && filter.filter(b);
            }

            //
            if (export) {
              exportEdge(ann, "MS1 shape correlation", a.getID(), b.getID(),
                  corrForm.format(r2r.getAvgShapeR()), "r=" + corrForm.format(r2r.getAvgShapeR()));
              added.incrementAndGet();
            }
          });

      LOG.info("Correlation edges exported " + added.get() + "");

      // export ann edges
      // Filename
      if (added.get() > 0) {
        writeToFile(ann.toString(), filename, "_edges_ms1correlation", ".csv");
        return true;
      } else
        return false;
    } catch (Exception e) {
      throw new MSDKRuntimeException(e);
    }
  }

  private static void writeToFile(String data, File filename, String suffix, String format) {
    TxtWriter writer = new TxtWriter();
    File realFile = FileAndPathUtil.eraseFormat(filename);
    realFile = FileAndPathUtil.getRealFilePath(filename.getParentFile(),
        realFile.getName() + suffix, format);
    writer.write(data, realFile);
    LOG.info("File created: " + realFile);
  }

  private static void exportEdge(StringBuilder ann, String type, int id1, int id2, String score,
      String annotation) {
    // the data
    Object[] data = new Object[EDGES.values().length];

    // add all data
    for (int d = 0; d < EDGES.values().length; d++) {
      switch (EDGES.values()[d]) {
        case ID1:
          data[d] = id1 + "";
          break;
        case ID2:
          data[d] = id2 + "";
          break;
        case EdgeType:
          data[d] = type;
          break;
        case Annotation:
          data[d] = annotation;
          break;
        case Score:
          data[d] = score;
          break;
      }
    }
    // replace null
    for (int j = 0; j < data.length; j++) {
      if (data[j] == null)
        data[j] = "";
    }
    // add data
    ann.append(StringUtils.join(data, ','));
    ann.append("\n");
  }

}
