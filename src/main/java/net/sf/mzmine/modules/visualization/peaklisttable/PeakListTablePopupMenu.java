/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.visualization.peaklisttable;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.FormulaPredictionModule;
import net.sf.mzmine.modules.peaklistmethods.identification.nist.NistMsSearchModule;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.OnlineDBSearchModule;
import net.sf.mzmine.modules.peaklistmethods.identification.sirius.SiriusProcessingModule;
import net.sf.mzmine.modules.peaklistmethods.io.siriusexport.SiriusExportModule;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.view.MSMSLibrarySubmissionWindow;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.manual.ManualPeakPickerModule;
import net.sf.mzmine.modules.visualization.intensityplot.IntensityPlotModule;
import net.sf.mzmine.modules.visualization.peaklisttable.export.IsotopePatternExportModule;
import net.sf.mzmine.modules.visualization.peaklisttable.export.MSMSExportModule;
import net.sf.mzmine.modules.visualization.peaklisttable.table.CommonColumnType;
import net.sf.mzmine.modules.visualization.peaklisttable.table.DataFileColumnType;
import net.sf.mzmine.modules.visualization.peaklisttable.table.PeakListTable;
import net.sf.mzmine.modules.visualization.peaklisttable.table.PeakListTableColumnModel;
import net.sf.mzmine.modules.visualization.peaksummary.PeakSummaryVisualizerModule;
import net.sf.mzmine.modules.visualization.spectra.mirrorspectra.MirrorScanWindow;
import net.sf.mzmine.modules.visualization.spectra.multimsms.MultiMSMSWindow;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.MultiSpectraVisualizerWindow;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import net.sf.mzmine.modules.visualization.threed.ThreeDVisualizerModule;
import net.sf.mzmine.modules.visualization.tic.TICPlotType;
import net.sf.mzmine.modules.visualization.tic.TICVisualizerModule;
import net.sf.mzmine.modules.visualization.twod.TwoDVisualizerModule;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;

/**
 * Peak-list table pop-up menu.
 * 
 */
public class PeakListTablePopupMenu extends JPopupMenu implements ActionListener {

  private static final long serialVersionUID = 1L;

  private final PeakListTable table;
  private final PeakList peakList;
  private final PeakListTableColumnModel columnModel;

  private final JMenu showMenu;
  private final JMenu searchMenu;
  private final JMenu idsMenu;
  private final JMenu exportMenu;
  private final JMenuItem deleteRowsItem;
  private final JMenuItem addNewRowItem;
  private final JMenuItem plotRowsItem;
  private final JMenuItem showSpectrumItem;
  private final JMenuItem showXICItem;
  private final JMenuItem showXICSetupItem;
  private final JMenuItem showMSMSItem;
  private final JMenuItem showMSMSMirrorItem;
  private final JMenuItem showMSMSMirrorMatchDBItem;
  private final JMenuItem showAllMSMSItem;
  private final JMenuItem showIsotopePatternItem;
  private final JMenuItem show2DItem;
  private final JMenuItem show3DItem;
  private final JMenuItem exportIsotopesItem;
  private final JMenuItem exportMSMSItem;

  private final JMenuItem openCompoundIdUrl;
  ///// kaidu edit
  private final JMenuItem exportToSirius;
  // for building MS/MS library and submission to online libraries
  private final JMenuItem exportMSMSLibrary;
  ////
  private final JMenuItem manuallyDefineItem;
  private final JMenuItem showPeakRowSummaryItem;
  private final JMenuItem clearIdsItem;
  private final JMenuItem dbSearchItem;
  private final JMenuItem formulaItem;
  private final JMenuItem siriusItem;
  private final JMenuItem nistSearchItem;
  private final JMenuItem copyIdsItem;
  private final JMenuItem pasteIdsItem;

  private final PeakListTableWindow window;
  private RawDataFile clickedDataFile;
  private PeakListRow clickedPeakListRow;
  private PeakListRow[] allClickedPeakListRows;

  // For copying and pasting IDs - shared by all peak-list table instances.
  // Currently only accessed from this
  // class.
  private static PeakIdentity copiedId = null;

  public PeakListTablePopupMenu(final PeakListTableWindow window, PeakListTable listTable,
      final PeakListTableColumnModel model, final PeakList list) {

    this.window = window;
    table = listTable;
    peakList = list;
    columnModel = model;

    clickedDataFile = null;
    clickedPeakListRow = null;
    allClickedPeakListRows = null;

    showMenu = new JMenu("Show");
    add(showMenu);

    showXICItem = GUIUtils.addMenuItem(showMenu, "XIC (base peak) (quick)", this);
    showXICSetupItem = GUIUtils.addMenuItem(showMenu, "XIC (dialog)", this);
    showSpectrumItem = GUIUtils.addMenuItem(showMenu, "Mass spectrum", this);
    show2DItem = GUIUtils.addMenuItem(showMenu, "Peak in 2D", this);
    show3DItem = GUIUtils.addMenuItem(showMenu, "Peak in 3D", this);
    showMSMSItem = GUIUtils.addMenuItem(showMenu, "Most intense MS/MS ", this);
    showMSMSItem.setToolTipText("MS/MS of a single or multiple rows");
    showAllMSMSItem = GUIUtils.addMenuItem(showMenu, "All MS/MS", this);
    showMSMSMirrorItem = GUIUtils.addMenuItem(showMenu, "MS/MS mirror (select 2 rows)", this);
    showMSMSMirrorMatchDBItem =
        GUIUtils.addMenuItem(showMenu, "MS/MS mirror (DB matched identity)", this);
    showIsotopePatternItem = GUIUtils.addMenuItem(showMenu, "Isotope pattern", this);
    showPeakRowSummaryItem = GUIUtils.addMenuItem(showMenu, "Peak row summary", this);

    searchMenu = new JMenu("Search");
    add(searchMenu);
    dbSearchItem = GUIUtils.addMenuItem(searchMenu, "Search online database", this);
    nistSearchItem = GUIUtils.addMenuItem(searchMenu, "NIST MS Search", this);
    formulaItem = GUIUtils.addMenuItem(searchMenu, "Predict molecular formula", this);
    siriusItem = GUIUtils.addMenuItem(searchMenu, "SIRIUS structure prediction", this);

    exportMenu = new JMenu("Export");
    add(exportMenu);
    exportIsotopesItem = GUIUtils.addMenuItem(exportMenu, "Isotope pattern", this);
    // kaidu edit
    exportToSirius = GUIUtils.addMenuItem(exportMenu, "Export to SIRIUS", this);
    exportMSMSLibrary = GUIUtils.addMenuItem(exportMenu, "Export MS/MS library", this);
    //
    exportMSMSItem = GUIUtils.addMenuItem(exportMenu, "MS/MS pattern", this);

    // Identities menu.
    idsMenu = new JMenu("Identities");
    add(idsMenu);
    openCompoundIdUrl = GUIUtils.addMenuItem(idsMenu, "Open compound ID URL", this);
    clearIdsItem = GUIUtils.addMenuItem(idsMenu, "Clear", this);
    copyIdsItem = GUIUtils.addMenuItem(idsMenu, "Copy", this);
    pasteIdsItem = GUIUtils.addMenuItem(idsMenu, "Paste", this);

    plotRowsItem = GUIUtils.addMenuItem(this, "Plot using Intensity Plot module", this);
    manuallyDefineItem = GUIUtils.addMenuItem(this, "Manually define peak", this);
    deleteRowsItem = GUIUtils.addMenuItem(this, "Delete selected row(s)", this);
    addNewRowItem = GUIUtils.addMenuItem(this, "Add new row", this);
  }

  @Override
  public void show(final Component invoker, final int x, final int y) {

    // Select the row where clicked?
    final Point clickedPoint = new Point(x, y);
    final int clickedRow = table.rowAtPoint(clickedPoint);
    if (table.getSelectedRowCount() < 2) {
      ListSelectionModel selectionModel = table.getSelectionModel();
      selectionModel.setSelectionInterval(clickedRow, clickedRow);
    }

    // First, disable all the Show... items
    show2DItem.setEnabled(false);
    show3DItem.setEnabled(false);
    manuallyDefineItem.setEnabled(false);
    showMSMSItem.setEnabled(false);
    showMSMSMirrorItem.setEnabled(false);
    showMSMSMirrorMatchDBItem.setEnabled(false);
    showAllMSMSItem.setEnabled(false);
    showIsotopePatternItem.setEnabled(false);
    showPeakRowSummaryItem.setEnabled(false);
    exportIsotopesItem.setEnabled(false);
    exportMSMSItem.setEnabled(false);
    exportMenu.setEnabled(false);

    // Enable row items if applicable
    final int[] selectedRows = table.getSelectedRows();
    final boolean rowsSelected = selectedRows.length > 0;
    deleteRowsItem.setEnabled(rowsSelected);
    clearIdsItem.setEnabled(rowsSelected);
    pasteIdsItem.setEnabled(rowsSelected && copiedId != null);
    plotRowsItem.setEnabled(rowsSelected);
    showMenu.setEnabled(rowsSelected);
    idsMenu.setEnabled(rowsSelected);
    exportIsotopesItem.setEnabled(rowsSelected);
    exportToSirius.setEnabled(rowsSelected);
    exportMSMSLibrary.setEnabled(rowsSelected);
    exportMenu.setEnabled(rowsSelected);

    final boolean oneRowSelected = selectedRows.length == 1;
    searchMenu.setEnabled(oneRowSelected);

    // Find the row and column where the user clicked
    clickedDataFile = null;
    final int clickedColumn =
        columnModel.getColumn(table.columnAtPoint(clickedPoint)).getModelIndex();
    if (clickedRow >= 0 && clickedColumn >= 0) {

      final int rowIndex = table.convertRowIndexToModel(clickedRow);
      clickedPeakListRow = getPeakListRow(rowIndex);
      allClickedPeakListRows = new PeakListRow[selectedRows.length];
      for (int i = 0; i < selectedRows.length; i++) {
        allClickedPeakListRows[i] = getPeakListRow(table.convertRowIndexToModel(selectedRows[i]));
      }

      // Enable items.
      show2DItem.setEnabled(oneRowSelected);
      show3DItem.setEnabled(oneRowSelected);
      showPeakRowSummaryItem.setEnabled(oneRowSelected);

      if (clickedPeakListRow.getBestPeak() != null) {
        exportMSMSItem.setEnabled(oneRowSelected
            && clickedPeakListRow.getBestPeak().getMostIntenseFragmentScanNumber() > 0);
      }

      // If we clicked on data file columns, check the peak
      if (clickedColumn >= CommonColumnType.values().length) {

        // Enable manual peak picking
        manuallyDefineItem.setEnabled(oneRowSelected);

        // Find the actual peak, if we have it.
        clickedDataFile = peakList.getRawDataFile((clickedColumn - CommonColumnType.values().length)
            / DataFileColumnType.values().length);

        final Feature clickedPeak =
            getPeakListRow(table.convertRowIndexToModel(clickedRow)).getPeak(clickedDataFile);

        // If we have the peak, enable Show... items
        if (clickedPeak != null && oneRowSelected) {
          showIsotopePatternItem.setEnabled(clickedPeak.getIsotopePattern() != null);
        }
      } else {
        showIsotopePatternItem
            .setEnabled(clickedPeakListRow.getBestIsotopePattern() != null && oneRowSelected);
      }

      long nRowsWithFragmentation = Arrays.stream(allClickedPeakListRows)
          .filter(r -> r.getBestFragmentation() != null).count();
      // always show for multi MSMS window
      showMSMSItem.setEnabled((oneRowSelected && getSelectedPeakForMSMS() != null
          && getSelectedPeakForMSMS().getMostIntenseFragmentScanNumber() > 0)
          || (selectedRows.length > 1 && nRowsWithFragmentation > 1));

      // always show if at least one fragmentation is available
      showAllMSMSItem
          .setEnabled(clickedPeakListRow.getBestFragmentation() != null && oneRowSelected);

      // only show if selected rows == 2 and both have MS2
      boolean bothMS2 = selectedRows.length == 2 && nRowsWithFragmentation == 2;
      showMSMSMirrorItem.setEnabled(bothMS2);

      // has identity of MS/MS database match
      PeakIdentity pi = clickedPeakListRow.getPreferredPeakIdentity();
      showMSMSMirrorMatchDBItem.setEnabled(pi != null && pi instanceof SpectralDBPeakIdentity);

      // open id url if available
      String url = null;
      if (pi != null)
        url = pi.getPropertyValue(PeakIdentity.PROPERTY_URL);
      openCompoundIdUrl.setEnabled(url != null && !url.isEmpty());
    }

    copyIdsItem
        .setEnabled(oneRowSelected && allClickedPeakListRows[0].getPreferredPeakIdentity() != null);

    super.show(invoker, x, y);
  }

  /**
   * 
   * @param modelIndex the row index in the table model
   * @return
   */
  protected PeakListRow getPeakListRow(int modelIndex) {
    return peakList.getRow(modelIndex);
  }

  @Override
  public void actionPerformed(final ActionEvent e) {

    final Object src = e.getSource();

    if (deleteRowsItem.equals(src)) {

      final int[] rowsToDelete = table.getSelectedRows();

      final int[] unsortedIndexes = new int[rowsToDelete.length];
      for (int i = rowsToDelete.length - 1; i >= 0; i--) {

        unsortedIndexes[i] = table.convertRowIndexToModel(rowsToDelete[i]);
      }

      // sort row indexes and start removing from the last
      Arrays.sort(unsortedIndexes);

      // delete the rows starting from last
      for (int i = unsortedIndexes.length - 1; i >= 0; i--) {
        peakList.removeRow(unsortedIndexes[i]);
      }

      // Notify the GUI that peaklist contents have changed
      updateTableGUI();
    }

    if (plotRowsItem.equals(src)) {

      final int[] selectedTableRows = table.getSelectedRows();

      final PeakListRow[] selectedRows = new PeakListRow[selectedTableRows.length];
      for (int i = 0; i < selectedTableRows.length; i++) {
        selectedRows[i] = getPeakListRow(table.convertRowIndexToModel(selectedTableRows[i]));
      }

      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          IntensityPlotModule.showIntensityPlot(MZmineCore.getProjectManager().getCurrentProject(),
              peakList, selectedRows);
        }
      });
    }

    if (showXICItem.equals(src) && allClickedPeakListRows.length != 0) {

      // Map peaks to their identity labels.
      final Map<Feature, String> labelsMap =
          new HashMap<Feature, String>(allClickedPeakListRows.length);

      final RawDataFile selectedDataFile =
          clickedDataFile == null ? allClickedPeakListRows[0].getBestPeak().getDataFile()
              : clickedDataFile;

      Range<Double> mzRange = null;
      final List<Feature> selectedPeaks = new ArrayList<Feature>(allClickedPeakListRows.length);
      for (final PeakListRow row : allClickedPeakListRows) {

        for (final Feature peak : row.getPeaks()) {
          if (mzRange == null) {
            mzRange = peak.getRawDataPointsMZRange();
            double upper = mzRange.upperEndpoint();
            double lower = mzRange.lowerEndpoint();
            if ((upper - lower) < 0.000001) {
              // Workaround to make ultra narrow mzRanges (e.g. from imported mzTab peaklist),
              // a more reasonable default for a HRAM instrument (~5ppm)
              double fiveppm = (upper * 5E-6);
              mzRange = Range.closed(lower - fiveppm, upper + fiveppm);
            }
          } else {
            mzRange = mzRange.span(peak.getRawDataPointsMZRange());
          }
        }

        final Feature filePeak = row.getPeak(selectedDataFile);
        if (filePeak != null) {

          selectedPeaks.add(filePeak);

          // Label the peak with the row's preferred identity.
          final PeakIdentity identity = row.getPreferredPeakIdentity();
          if (identity != null) {
            labelsMap.put(filePeak, identity.getName());
          }
        }
      }

      ScanSelection scanSelection = new ScanSelection(selectedDataFile.getDataRTRange(1), 1);

      TICVisualizerModule.showNewTICVisualizerWindow(new RawDataFile[] {selectedDataFile},
          selectedPeaks.toArray(new Feature[selectedPeaks.size()]), labelsMap, scanSelection,
          TICPlotType.BASEPEAK, mzRange);
    }

    if (showXICSetupItem.equals(src) && allClickedPeakListRows.length != 0) {

      // Map peaks to their identity labels.
      final Map<Feature, String> labelsMap =
          new HashMap<Feature, String>(allClickedPeakListRows.length);

      final RawDataFile[] selectedDataFiles = clickedDataFile == null ? peakList.getRawDataFiles()
          : new RawDataFile[] {clickedDataFile};

      Range<Double> mzRange = null;
      final ArrayList<Feature> allClickedPeaks =
          new ArrayList<Feature>(allClickedPeakListRows.length);
      final ArrayList<Feature> selectedClickedPeaks =
          new ArrayList<Feature>(allClickedPeakListRows.length);
      for (final PeakListRow row : allClickedPeakListRows) {

        // Label the peak with the row's preferred identity.
        final PeakIdentity identity = row.getPreferredPeakIdentity();

        for (final Feature peak : row.getPeaks()) {

          allClickedPeaks.add(peak);
          if (peak.getDataFile() == clickedDataFile) {
            selectedClickedPeaks.add(peak);
          }

          if (mzRange == null) {
            mzRange = peak.getRawDataPointsMZRange();
          } else {
            mzRange = mzRange.span(peak.getRawDataPointsMZRange());
          }

          if (identity != null) {
            labelsMap.put(peak, identity.getName());
          }
        }
      }

      ScanSelection scanSelection = new ScanSelection(selectedDataFiles[0].getDataRTRange(1), 1);

      TICVisualizerModule.setupNewTICVisualizer(
          MZmineCore.getProjectManager().getCurrentProject().getDataFiles(), selectedDataFiles,
          allClickedPeaks.toArray(new Feature[allClickedPeaks.size()]),
          selectedClickedPeaks.toArray(new Feature[selectedClickedPeaks.size()]), labelsMap,
          scanSelection, mzRange);
    }

    if (show2DItem.equals(src)) {

      final Feature showPeak = getSelectedPeak();
      if (showPeak != null) {

        TwoDVisualizerModule.show2DVisualizerSetupDialog(showPeak.getDataFile(),
            getPeakMZRange(showPeak), getPeakRTRange(showPeak));
      }
    }

    if (show3DItem.equals(src)) {

      final Feature showPeak = getSelectedPeak();
      if (showPeak != null) {

        ThreeDVisualizerModule.setupNew3DVisualizer(showPeak.getDataFile(),
            getPeakMZRange(showPeak), getPeakRTRange(showPeak));
      }
    }

    if (manuallyDefineItem.equals(src)) {

      ManualPeakPickerModule.runManualDetection(clickedDataFile, clickedPeakListRow, peakList,
          table);
    }

    if (showSpectrumItem.equals(src)) {

      final Feature showPeak = getSelectedPeak();
      if (showPeak != null) {

        SpectraVisualizerModule.showNewSpectrumWindow(showPeak.getDataFile(),
            showPeak.getRepresentativeScanNumber(), showPeak);
      }
    }
    if (openCompoundIdUrl.equals(src)) {
      if (clickedPeakListRow != null && clickedPeakListRow.getPreferredPeakIdentity() != null) {
        String url = clickedPeakListRow.getPreferredPeakIdentity()
            .getPropertyValue(PeakIdentity.PROPERTY_URL);
        if (url != null && !url.isEmpty() && Desktop.isDesktopSupported()) {
          try {
            Desktop.getDesktop().browse(new URI(url));
          } catch (IOException | URISyntaxException e1) {
          }
        }
      }
    }

    if (showMSMSItem.equals(src)) {
      if (allClickedPeakListRows != null && allClickedPeakListRows.length > 1) {
        // show multi msms window of multiple rows
        MultiMSMSWindow multi = new MultiMSMSWindow();
        multi.setData(allClickedPeakListRows, peakList.getRawDataFiles(), clickedDataFile, true,
            SortingProperty.MZ, SortingDirection.Ascending);
        multi.setVisible(true);
      } else {
        Feature showPeak = getSelectedPeakForMSMS();
        if (showPeak != null) {
          final int scanNumber = showPeak.getMostIntenseFragmentScanNumber();
          if (scanNumber > 0) {
            SpectraVisualizerModule.showNewSpectrumWindow(showPeak.getDataFile(), scanNumber);
          } else {
            MZmineCore.getDesktop().displayMessage(window,
                "There is no fragment for "
                    + MZmineCore.getConfiguration().getMZFormat().format(showPeak.getMZ())
                    + " m/z in the current raw data.");
          }
        }
      }
    }

    // mirror of the two best fragment scans
    if (showMSMSMirrorItem.equals(src)) {
      if (allClickedPeakListRows != null && allClickedPeakListRows.length == 2) {
        PeakListRow a = allClickedPeakListRows[0];
        PeakListRow b = allClickedPeakListRows[1];
        Scan scan = a.getBestFragmentation();
        Scan mirror = b.getBestFragmentation();
        if (scan != null && mirror != null) {
          // show mirror msms window of two rows
          MirrorScanWindow mirrorWindow = new MirrorScanWindow();
          mirrorWindow.setScans(scan, mirror);
          mirrorWindow.setVisible(true);
        }
      }
    }
    // mirror of MS/MS and MS/MS library match identity
    if (showMSMSMirrorMatchDBItem.equals(src)) {
      PeakIdentity pi = clickedPeakListRow.getPreferredPeakIdentity();
      if (pi != null && pi instanceof SpectralDBPeakIdentity) {
        SpectralDBPeakIdentity db = (SpectralDBPeakIdentity) pi;
        // show mirror msms window of two rows
        MirrorScanWindow mirrorWindow = new MirrorScanWindow();
        mirrorWindow.setScans(clickedPeakListRow, db);
        mirrorWindow.setVisible(true);
      }
    }

    if (showAllMSMSItem.equals(src)) {
      final Feature showPeak = getSelectedPeakForMSMS();
      RawDataFile raw = clickedPeakListRow.getBestFragmentation().getDataFile();
      if (showPeak != null && showPeak.getMostIntenseFragmentScanNumber() != 0)
        raw = showPeak.getDataFile();

      if (clickedPeakListRow.getBestFragmentation() != null) {
        MultiSpectraVisualizerWindow multiSpectraWindow =
            new MultiSpectraVisualizerWindow(clickedPeakListRow, raw);
        multiSpectraWindow.setVisible(true);
      } else {
        MZmineCore.getDesktop().displayMessage(window,
            "There is no fragment for "
                + MZmineCore.getConfiguration().getMZFormat().format(showPeak.getMZ())
                + " m/z in the current raw data.");

      }
    }

    if (showIsotopePatternItem.equals(src)) {

      final Feature showPeak = getSelectedPeak();
      if (showPeak != null && showPeak.getIsotopePattern() != null) {

        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            SpectraVisualizerModule.showNewSpectrumWindow(showPeak.getDataFile(),
                showPeak.getRepresentativeScanNumber(), showPeak.getIsotopePattern());
          }
        });
      }
    }

    if (formulaItem != null && formulaItem.equals(src)) {

      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          FormulaPredictionModule.showSingleRowIdentificationDialog(clickedPeakListRow);
        }
      });

    }

    // //TODO: what is going on here?
    // TODO: probably remove singlerowidentificationDialog as Sirius works with spectrum, not 1
    // peak.
    if (siriusItem != null && siriusItem.equals(src)) {

      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          SiriusProcessingModule.showSingleRowIdentificationDialog(clickedPeakListRow);
        }
      });

    }

    if (dbSearchItem != null && dbSearchItem.equals(src)) {

      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          OnlineDBSearchModule.showSingleRowIdentificationDialog(clickedPeakListRow);
        }
      });

    }

    if (nistSearchItem != null && nistSearchItem.equals(src)) {

      NistMsSearchModule.singleRowSearch(peakList, clickedPeakListRow);
    }

    if (addNewRowItem.equals(src)) {

      // find maximum ID and add 1
      int newID = 1;
      for (final PeakListRow row : peakList.getRows()) {
        if (row.getID() >= newID) {
          newID = row.getID() + 1;
        }
      }

      // create a new row
      final PeakListRow newRow = new SimplePeakListRow(newID);
      ManualPeakPickerModule.runManualDetection(peakList.getRawDataFiles(), newRow, peakList,
          table);

    }

    if (showPeakRowSummaryItem.equals(src)) {

      PeakSummaryVisualizerModule.showNewPeakSummaryWindow(clickedPeakListRow);
    }

    if (exportIsotopesItem.equals(src)) {
      IsotopePatternExportModule.exportIsotopePattern(clickedPeakListRow);
    }
    if (exportToSirius.equals(src)) {
      // export all selected rows
      SiriusExportModule.exportSingleRows(allClickedPeakListRows);
    }
    if (exportMSMSLibrary.equals(src)) {
      // open window with all selected rows
      MSMSLibrarySubmissionWindow libraryWindow = new MSMSLibrarySubmissionWindow();
      libraryWindow.setData(allClickedPeakListRows, SortingProperty.MZ, SortingDirection.Ascending);
      libraryWindow.setVisible(true);
    }

    if (exportMSMSItem.equals(src)) {
      MSMSExportModule.exportMSMS(clickedPeakListRow);
    }

    if (clearIdsItem.equals(src)) {

      // Delete identities of selected rows.
      for (final PeakListRow row : allClickedPeakListRows) {

        // Selected row index.
        for (final PeakIdentity id : row.getPeakIdentities()) {

          // Remove id.
          row.removePeakIdentity(id);
        }
      }

      // Update table GUI.
      updateTableGUI();
    }

    if (copyIdsItem.equals(src) && allClickedPeakListRows.length > 0) {

      final PeakIdentity id = allClickedPeakListRows[0].getPreferredPeakIdentity();
      if (id != null) {

        copiedId = (PeakIdentity) id.clone();
      }
    }

    if (pasteIdsItem.equals(src) && copiedId != null) {

      // Paste identity into selected rows.
      for (final PeakListRow row : allClickedPeakListRows) {

        row.setPreferredPeakIdentity((PeakIdentity) copiedId.clone());
      }

      // Update table GUI.
      updateTableGUI();
    }
  }

  /**
   * Update the table.
   */
  private void updateTableGUI() {
    ((AbstractTableModel) table.getModel()).fireTableDataChanged();
    MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(peakList, true);
  }

  /**
   * Get a peak's m/z range.
   * 
   * @param peak the peak.
   * @return The peak's m/z range.
   */
  private static Range<Double> getPeakMZRange(final Feature peak) {

    final Range<Double> peakMZRange = peak.getRawDataPointsMZRange();

    // By default, open the visualizer with the m/z range of
    // "peak_width x 2", but no smaller than 0.1 m/z, because with smaller
    // ranges VisAD tends to show nasty anti-aliasing artifacts.
    // For example of such artifacts, set mzMin = 440.27, mzMax = 440.28 and
    // mzResolution = 500
    final double minRangeCenter = (peakMZRange.upperEndpoint() + peakMZRange.lowerEndpoint()) / 2.0;
    final double minRangeWidth =
        Math.max(0.1, (peakMZRange.upperEndpoint() - peakMZRange.lowerEndpoint()) * 2);
    double mzMin = minRangeCenter - (minRangeWidth / 2);
    if (mzMin < 0)
      mzMin = 0;
    double mzMax = minRangeCenter + (minRangeWidth / 2);
    return Range.closed(mzMin, mzMax);
  }

  /**
   * Get a peak's RT range.
   * 
   * @param peak the peak.
   * @return The peak's RT range.
   */
  private static Range<Double> getPeakRTRange(final Feature peak) {

    final Range<Double> range = peak.getRawDataPointsRTRange();
    final double rtLen = range.upperEndpoint() - range.lowerEndpoint();
    return Range.closed(Math.max(0.0, range.lowerEndpoint() - rtLen),
        range.upperEndpoint() + rtLen);
  }

  /**
   * Get the selected peak.
   * 
   * @return the peak.
   */
  private Feature getSelectedPeak() {
    return clickedDataFile != null ? clickedPeakListRow.getPeak(clickedDataFile)
        : clickedPeakListRow.getBestPeak();
  }

  /**
   * Get the selected peak. If no specific raw data file was clicked return highest peak with MS/MS
   * 
   * @return the peak.
   */
  private Feature getSelectedPeakForMSMS() {
    Feature peak = getSelectedPeak();
    // always return if a raw data file was clicked
    if (clickedDataFile != null || peak != null)
      return peak;
    else {
      // no specific raw data file was chosen and bestPeak has no MSMS
      // try to find highest peak with MSMS
      Scan scan = clickedPeakListRow.getBestFragmentation();
      return scan == null ? null : clickedPeakListRow.getPeak(scan.getDataFile());
    }
  }
}


