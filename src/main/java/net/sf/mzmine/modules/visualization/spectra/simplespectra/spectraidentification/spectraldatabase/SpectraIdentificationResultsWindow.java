/*
 * Copyright 2006-2019 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;
import net.sf.mzmine.desktop.impl.WindowsMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.molstructure.Structure2DComponent;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra.MirrorScanWindow;
import net.sf.mzmine.taskcontrol.impl.TaskControllerImpl;
import net.sf.mzmine.util.components.ComponentCellRenderer;
import net.sf.mzmine.util.components.MultiLineLabel;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;

public class SpectraIdentificationResultsWindow extends JFrame {
  /**
   * Window to show all spectral database matches from selected scan
   * 
   * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
   */

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final long serialVersionUID = 1L;
  private JPanel pnGrid;
  private Font titleFont = new Font("Dialog", Font.BOLD, 18);
  private Font scoreFont = new Font("Dialog", Font.BOLD, 36);
  private Font headerFont = new Font("Dialog", Font.BOLD, 16);
  private static final DecimalFormat COS_FORM = new DecimalFormat("0.000");
  private JScrollPane scrollPane;
  private List<SpectralDBPeakIdentity> totalMatches;
  private Map<SpectralDBPeakIdentity, JPanel> matchPanels;

  public SpectraIdentificationResultsWindow() {
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setSize(new Dimension(1000, 800));
    setExtendedState(MAXIMIZED_HORIZ);
    getContentPane().setLayout(new BorderLayout());
    setTitle("Processing...");

    pnGrid = new JPanel();
    // any number of rows
    pnGrid.setLayout(new GridLayout(0, 1, 0, 25));

    pnGrid.setBackground(Color.WHITE);
    pnGrid.setAutoscrolls(false);

    // add label (is replaced later
    JLabel noMatchesFound = new JLabel("Sorry, no matches found!", SwingConstants.CENTER);
    noMatchesFound.setFont(headerFont);
    noMatchesFound.setForeground(Color.RED);
    pnGrid.add(noMatchesFound, BorderLayout.CENTER);

    // Add the Windows menu
    JMenuBar menuBar = new JMenuBar();
    menuBar.add(new WindowsMenu());
    setJMenuBar(menuBar);

    // show progress of processes
    TaskControllerImpl taskController = (TaskControllerImpl) MZmineCore.getTaskController();

    // add new task table for database matching
    JTable taskTable = new JTable(taskController.getTaskQueue());
    taskTable.setVisible(true);
    taskTable.setCellSelectionEnabled(false);
    taskTable.setColumnSelectionAllowed(false);
    taskTable.setDefaultRenderer(JComponent.class, new ComponentCellRenderer());
    JScrollPane scrollPaneProgressPanel = new JScrollPane(taskTable);
    getContentPane().add(scrollPaneProgressPanel, BorderLayout.SOUTH);
    scrollPaneProgressPanel
        .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPaneProgressPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPaneProgressPanel.setPreferredSize(new Dimension(1200, 120));

    scrollPane = new JScrollPane(pnGrid);
    getContentPane().add(scrollPane, BorderLayout.CENTER);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setViewportView(pnGrid);

    totalMatches = new ArrayList<>();
    matchPanels = new HashMap<>();

    setVisible(true);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    validate();
    repaint();
  }

  /**
   * Creates a new panel for the library match
   * 
   * @param scan
   * @param hit
   * @return
   */
  private JPanel createPanel(SpectralDBPeakIdentity hit) {
    JPanel panel = new JPanel(new BorderLayout());

    JSplitPane spectrumPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    spectrumPane.setPreferredSize(new Dimension(1000, 500));

    // set meta data from identity
    JPanel metaDataPanel = new JPanel();

    LayoutManager layout = new BoxLayout(metaDataPanel, BoxLayout.Y_AXIS);
    metaDataPanel.setLayout(layout);
    metaDataPanel.setMinimumSize(new Dimension(500, 500));

    // add title
    JPanel boxTitlePanel = new JPanel(new BorderLayout());

    Random r1 = new Random();
    Color randomCol = Color.getHSBColor(r1.nextFloat(), 1.0f, 0.6f);
    boxTitlePanel.setBackground(randomCol);
    Box boxTitle = Box.createHorizontalBox();
    boxTitle.add(Box.createHorizontalGlue());

    JPanel panelTitle = new JPanel(new BorderLayout());
    panelTitle.setBackground(randomCol);
    String name = hit.getEntry().getField(DBEntryField.NAME).orElse("N/A").toString();
    SpectralIdentificationSpectralDatabaseTextPane title =
        new SpectralIdentificationSpectralDatabaseTextPane(name);
    title.setFont(headerFont);
    title.setBackground(randomCol);
    title.setForeground(Color.WHITE);
    panelTitle.add(title);

    double simScore = hit.getSimilarity().getCosine();

    // score result
    JPanel panelScore = new JPanel(new BorderLayout());
    panelScore.setLayout(new BoxLayout(panelScore, BoxLayout.Y_AXIS));
    JLabel score = new JLabel(COS_FORM.format(simScore));
    score.setToolTipText("Cosine similarity of raw data scan (top, blue) and database scan: "
        + COS_FORM.format(simScore));
    JLabel scoreLabel = new JLabel("Cosine similarity");
    scoreLabel.setToolTipText("Cosine similarity of raw data scan (top, blue) and database scan: "
        + COS_FORM.format(simScore));
    score.setFont(scoreFont);
    score.setForeground(Color.WHITE);
    scoreLabel.setFont(titleFont);
    scoreLabel.setForeground(Color.WHITE);
    panelScore.setBackground(randomCol);
    panelScore.add(score);
    panelScore.add(scoreLabel);
    boxTitlePanel.add(panelTitle, BorderLayout.WEST);
    boxTitlePanel.add(panelScore, BorderLayout.EAST);
    boxTitle.add(boxTitlePanel);
    metaDataPanel.add(boxTitle);

    // structure preview
    Box structureBox = Box.createHorizontalBox();
    structureBox.add(Box.createHorizontalGlue());
    IAtomContainer molecule;
    JPanel preview2DPanel = new JPanel(new BorderLayout());
    preview2DPanel.setPreferredSize(new Dimension(500, 150));
    JComponent newComponent = null;

    String inchiString = hit.getEntry().getField(DBEntryField.INCHI).orElse("N/A").toString();
    String smilesString = hit.getEntry().getField(DBEntryField.SMILES).orElse("N/A").toString();

    // check for INCHI
    if (inchiString != "N/A") {
      molecule = parseInChi(hit);
    }
    // check for smiles
    else if (smilesString != "N/A") {
      molecule = parseSmiles(hit);
    } else
      molecule = null;

    // try to draw the component
    if (molecule != null) {
      try {
        newComponent = new Structure2DComponent(molecule);
      } catch (Exception e) {
        String errorMessage = "Could not load 2D structure\n" + "Exception: ";
        logger.log(Level.WARNING, errorMessage, e);
        newComponent = new MultiLineLabel(errorMessage);
      }
      preview2DPanel.add(newComponent, BorderLayout.CENTER);
      preview2DPanel.validate();
      structureBox.add(preview2DPanel);
      metaDataPanel.add(structureBox);
    }

    Box boxCompoundAndInstrument = Box.createHorizontalBox();
    // information on compound
    JPanel panelCompounds = new JPanel(new GridLayout(0, 1, 0, 5));
    panelCompounds.setBackground(Color.WHITE);
    JLabel compoundInfo = new JLabel("Compound information");
    compoundInfo.setToolTipText(
        "This section shows all the compound information listed in the used database");
    compoundInfo.setFont(headerFont);
    panelCompounds.add(compoundInfo);


    SpectralIdentificationSpectralDatabaseTextPane synonym =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "Synonym: " + hit.getEntry().getField(DBEntryField.SYNONYM).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.SYNONYM).orElse("N/A") != "N/A")
      panelCompounds.add(synonym);
    SpectralIdentificationSpectralDatabaseTextPane formula =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "Formula: " + hit.getEntry().getField(DBEntryField.FORMULA).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.FORMULA).orElse("N/A") != "N/A")
      panelCompounds.add(formula);
    SpectralIdentificationSpectralDatabaseTextPane molarWeight =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "Molar Weight: " + hit.getEntry().getField(DBEntryField.MOLWEIGHT).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.MOLWEIGHT).orElse("N/A") != "N/A")
      panelCompounds.add(molarWeight);
    SpectralIdentificationSpectralDatabaseTextPane exactMass =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "Exact mass: " + hit.getEntry().getField(DBEntryField.EXACT_MASS).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.EXACT_MASS).orElse("N/A") != "N/A")
      panelCompounds.add(exactMass);
    SpectralIdentificationSpectralDatabaseTextPane ionType =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "Ion type: " + hit.getEntry().getField(DBEntryField.IONTYPE).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.IONTYPE).orElse("N/A") != "N/A")
      panelCompounds.add(ionType);
    SpectralIdentificationSpectralDatabaseTextPane rt =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "Retention time: " + hit.getEntry().getField(DBEntryField.RT).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.RT).orElse("N/A") != "N/A")
      panelCompounds.add(rt);
    SpectralIdentificationSpectralDatabaseTextPane mz =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "m/z: " + hit.getEntry().getField(DBEntryField.MZ).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.MZ).orElse("N/A") != "N/A")
      panelCompounds.add(mz);
    SpectralIdentificationSpectralDatabaseTextPane charge =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "Charge: " + hit.getEntry().getField(DBEntryField.CHARGE).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.CHARGE).orElse("N/A") != "N/A")
      panelCompounds.add(charge);
    SpectralIdentificationSpectralDatabaseTextPane ionMode =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "Ion mode: " + hit.getEntry().getField(DBEntryField.ION_MODE).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.ION_MODE).orElse("N/A") != "N/A")
      panelCompounds.add(ionMode);
    SpectralIdentificationSpectralDatabaseTextPane inchi =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "INCHI: " + hit.getEntry().getField(DBEntryField.INCHI).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.INCHI).orElse("N/A") != "N/A")
      panelCompounds.add(inchi);
    SpectralIdentificationSpectralDatabaseTextPane inchiKey =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "INCHI Key: " + hit.getEntry().getField(DBEntryField.INCHIKEY).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.INCHI).orElse("N/A") != "N/A")
      panelCompounds.add(inchiKey);
    SpectralIdentificationSpectralDatabaseTextPane smiles =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "SMILES: " + hit.getEntry().getField(DBEntryField.SMILES).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.SMILES).orElse("N/A") != "N/A")
      panelCompounds.add(smiles);
    SpectralIdentificationSpectralDatabaseTextPane cas =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "CAS: " + hit.getEntry().getField(DBEntryField.CAS).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.CAS).orElse("N/A") != "N/A")
      panelCompounds.add(cas);
    SpectralIdentificationSpectralDatabaseTextPane numberPeaks =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "Number of peaks: " + hit.getEntry().getField(DBEntryField.NUM_PEAKS).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.NUM_PEAKS).orElse("N/A") != "N/A")
      panelCompounds.add(numberPeaks);

    // instrument info
    JPanel panelInstrument = new JPanel(new GridLayout(0, 1, 0, 5));
    panelInstrument.setBackground(Color.WHITE);
    JLabel instrumentInfo = new JLabel("Instrument information");
    instrumentInfo.setFont(headerFont);
    panelInstrument.add(instrumentInfo);
    SpectralIdentificationSpectralDatabaseTextPane instrumentType =
        new SpectralIdentificationSpectralDatabaseTextPane("Instrument type: "
            + hit.getEntry().getField(DBEntryField.INSTRUMENT_TYPE).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.INSTRUMENT_TYPE).orElse("N/A") != "N/A")
      panelInstrument.add(instrumentType);
    SpectralIdentificationSpectralDatabaseTextPane instrument =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "Instrument: " + hit.getEntry().getField(DBEntryField.INSTRUMENT).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.INSTRUMENT).orElse("N/A") != "N/A")
      panelInstrument.add(instrument);
    SpectralIdentificationSpectralDatabaseTextPane ionSource =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "Ion source: " + hit.getEntry().getField(DBEntryField.ION_SOURCE).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.ION_SOURCE).orElse("N/A") != "N/A")
      panelInstrument.add(ionSource);
    SpectralIdentificationSpectralDatabaseTextPane resolution =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "Resolution: " + hit.getEntry().getField(DBEntryField.RESOLUTION).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.RESOLUTION).orElse("N/A") != "N/A")
      panelInstrument.add(resolution);
    SpectralIdentificationSpectralDatabaseTextPane msLevel =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "MS level: " + hit.getEntry().getField(DBEntryField.MS_LEVEL).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.MS_LEVEL).orElse("N/A") != "N/A")
      panelInstrument.add(msLevel);
    SpectralIdentificationSpectralDatabaseTextPane collision =
        new SpectralIdentificationSpectralDatabaseTextPane("Collision energy: "
            + hit.getEntry().getField(DBEntryField.COLLISION_ENERGY).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.COLLISION_ENERGY).orElse("N/A") != "N/A")
      panelInstrument.add(collision);
    SpectralIdentificationSpectralDatabaseTextPane acquisition =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "Acquisition: " + hit.getEntry().getField(DBEntryField.ACQUISITION).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.ACQUISITION).orElse("N/A") != "N/A")
      panelInstrument.add(acquisition);
    SpectralIdentificationSpectralDatabaseTextPane software =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "Software: " + hit.getEntry().getField(DBEntryField.SOFTWARE).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.SOFTWARE).orElse("N/A") != "N/A")
      panelInstrument.add(software);

    boxCompoundAndInstrument.add(panelCompounds, BorderLayout.WEST);
    boxCompoundAndInstrument.add(panelInstrument, BorderLayout.EAST);
    metaDataPanel.add(boxCompoundAndInstrument);

    Box boxDBAndOther = Box.createHorizontalBox();
    // database links
    JPanel panelDB = new JPanel(new GridLayout(0, 1, 0, 5));
    panelDB.setBackground(Color.WHITE);
    JLabel databaseLinks = new JLabel("Database links");
    databaseLinks
        .setToolTipText("This section shows links to other databases of the matched compound");
    databaseLinks.setFont(headerFont);
    panelDB.add(databaseLinks);
    SpectralIdentificationSpectralDatabaseTextPane pubmed =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "PUBMED: " + hit.getEntry().getField(DBEntryField.PUBMED).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.PUBMED).orElse("N/A") != "N/A")
      panelDB.add(pubmed);
    SpectralIdentificationSpectralDatabaseTextPane pubchem =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "PUBCHEM: " + hit.getEntry().getField(DBEntryField.PUBCHEM).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.PUBCHEM).orElse("N/A") != "N/A")
      panelDB.add(pubchem);
    SpectralIdentificationSpectralDatabaseTextPane mona =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "MONA ID: " + hit.getEntry().getField(DBEntryField.MONA_ID).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.MONA_ID).orElse("N/A") != "N/A")
      panelDB.add(mona);
    SpectralIdentificationSpectralDatabaseTextPane spider =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "Chemspider: " + hit.getEntry().getField(DBEntryField.CHEMSPIDER).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.CHEMSPIDER).orElse("N/A") != "N/A")
      panelDB.add(spider);

    // // Other info
    JPanel panelOther = new JPanel(new GridLayout(0, 1, 0, 5));
    panelOther.setBackground(Color.WHITE);
    JLabel otherInfo = new JLabel("Other information");
    otherInfo.setToolTipText("This section shows all other information of the matched compound");
    otherInfo.setFont(headerFont);
    panelOther.add(otherInfo);
    SpectralIdentificationSpectralDatabaseTextPane investigator =
        new SpectralIdentificationSpectralDatabaseTextPane("Prinziple investigator: "
            + hit.getEntry().getField(DBEntryField.PRINZIPLE_INVESTIGATOR).orElse("N/A"));
    investigator.setToolTipText("Prinziple investigator: "
        + hit.getEntry().getField(DBEntryField.PRINZIPLE_INVESTIGATOR).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.PRINZIPLE_INVESTIGATOR).orElse("N/A") != "N/A")
      panelOther.add(investigator);
    SpectralIdentificationSpectralDatabaseTextPane collector =
        new SpectralIdentificationSpectralDatabaseTextPane("Data collector: "
            + hit.getEntry().getField(DBEntryField.DATA_COLLECTOR).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.DATA_COLLECTOR).orElse("N/A") != "N/A")
      panelOther.add(collector);
    SpectralIdentificationSpectralDatabaseTextPane entry =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "DB entry ID: " + hit.getEntry().getField(DBEntryField.ENTRY_ID).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.ENTRY_ID).orElse("N/A") != "N/A")
      panelOther.add(entry);
    SpectralIdentificationSpectralDatabaseTextPane comment =
        new SpectralIdentificationSpectralDatabaseTextPane(
            "Comment: " + hit.getEntry().getField(DBEntryField.COMMENT).orElse("N/A"));
    if (hit.getEntry().getField(DBEntryField.COMMENT).orElse("N/A") != "N/A")
      panelOther.add(comment);

    boxDBAndOther.add(panelDB, BorderLayout.WEST);
    boxDBAndOther.add(panelOther, BorderLayout.EAST);
    metaDataPanel.add(boxDBAndOther);

    // get mirror spectra window
    MirrorScanWindow mirrorWindow = new MirrorScanWindow();
    mirrorWindow.setScans(hit);

    JScrollPane metaDataPanelScrollPane =
        new JScrollPane(metaDataPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    spectrumPane.add(mirrorWindow.getMirrorSpecrumPlot());
    spectrumPane.add(metaDataPanelScrollPane);
    spectrumPane.setResizeWeight(1);
    spectrumPane.setEnabled(false);
    spectrumPane.setDividerSize(5);
    panel.add(spectrumPane);
    panel.setBorder(BorderFactory.createLineBorder(Color.black));
    return panel;
  }

  /**
   * Add a new match and sort the view
   * 
   * @param scan
   * @param match
   */
  public synchronized void addMatches(SpectralDBPeakIdentity match) {
    if (!totalMatches.contains(match)) {
      // add
      totalMatches.add(match);
      matchPanels.put(match, createPanel(match));

      // sort and show
      sortTotalMatches();
    }
  }

  /**
   * add all matches and sort the view
   * 
   * @param scan
   * @param matches
   */
  public synchronized void addMatches(List<SpectralDBPeakIdentity> matches) {
    // add all
    for (SpectralDBPeakIdentity match : matches) {
      if (!totalMatches.contains(match)) {
        // add
        totalMatches.add(match);
        matchPanels.put(match, createPanel(match));
      }
    }
    // sort and show
    sortTotalMatches();
  }

  /**
   * Sort all matches and renew panels
   * 
   */
  public void sortTotalMatches() {
    if (totalMatches.isEmpty()) {
      return;
    }

    // reversed sorting (highest cosine first
    totalMatches.sort((SpectralDBPeakIdentity a, SpectralDBPeakIdentity b) -> Double
        .compare(b.getSimilarity().getCosine(), a.getSimilarity().getCosine()));

    // renew layout and show
    renewLayout();
  }

  /**
   * Add a spectral library hit
   * 
   * @param ident
   * @param simScore
   */
  public void renewLayout() {
    SwingUtilities.invokeLater(() -> {
      // any number of rows
      JPanel pnGrid = new JPanel(new GridLayout(0, 1, 0, 25));
      pnGrid.setBackground(Color.WHITE);
      pnGrid.setAutoscrolls(false);
      // add all panel in order
      for (SpectralDBPeakIdentity match : totalMatches) {
        JPanel pn = matchPanels.get(match);
        if (pn != null)
          pnGrid.add(pn);
      }
      // show
      scrollPane.setViewportView(pnGrid);
      scrollPane.revalidate();
      scrollPane.repaint();
      this.pnGrid = pnGrid;
    });
  }

  private IAtomContainer parseInChi(SpectralDBPeakIdentity hit) {
    String inchiString = hit.getEntry().getField(DBEntryField.INCHI).orElse("N/A").toString();
    InChIGeneratorFactory factory;
    IAtomContainer molecule;
    if (inchiString != "N/A") {
      try {
        factory = InChIGeneratorFactory.getInstance();
        // Get InChIToStructure
        InChIToStructure inchiToStructure =
            factory.getInChIToStructure(inchiString, DefaultChemObjectBuilder.getInstance());
        molecule = inchiToStructure.getAtomContainer();
        return molecule;
      } catch (CDKException e) {
        String errorMessage = "Could not load 2D structure\n" + "Exception: ";
        logger.log(Level.WARNING, errorMessage, e);
        return null;
      }
    } else
      return null;
  }

  private IAtomContainer parseSmiles(SpectralDBPeakIdentity hit) {
    SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
    String smilesString = hit.getEntry().getField(DBEntryField.SMILES).orElse("N/A").toString();
    IAtomContainer molecule;
    if (smilesString != "N/A") {
      try {
        molecule = smilesParser.parseSmiles(smilesString);
        return molecule;
      } catch (InvalidSmilesException e1) {
        String errorMessage = "Could not load 2D structure\n" + "Exception: ";
        logger.log(Level.WARNING, errorMessage, e1);
        return null;
      }
    } else
      return null;
  }

}
