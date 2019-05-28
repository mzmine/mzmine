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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.sf.mzmine.framework.ScrollablePanel;
import net.sf.mzmine.modules.visualization.molstructure.Structure2DComponent;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra.MirrorScanWindow;
import net.sf.mzmine.util.ColorScaleUtil;
import net.sf.mzmine.util.components.MultiLineLabel;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;

/**
 * Window to show all spectral database matches from selected scan or peaklist match
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class SpectraIdentificationResultsWindow extends JFrame {
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private static final long serialVersionUID = 1L;
  public static final int META_WIDTH = 500;
  public static final int ENTRY_HEIGHT = 500;

  // colors
  public static final double MIN_COS_COLOR_VALUE = 0.5;
  public static final double MAX_COS_COLOR_VALUE = 1.0;
  // min color is a darker red
  // max color is a darker green
  public static final Color MAX_COS_COLOR = new Color(0x388E3C);
  public static final Color MIN_COS_COLOR = new Color(0xE30B0B);

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
    setSize(new Dimension(1400, 900));
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

    JPanel spectrumPanel = new JPanel(new BorderLayout());

    // set meta data from identity
    JPanel metaDataPanel = new JPanel();
    metaDataPanel.setLayout(new BoxLayout(metaDataPanel, BoxLayout.Y_AXIS));

    metaDataPanel.setBackground(Color.WHITE);

    // add title
    JPanel boxTitlePanel = new JPanel(new BorderLayout());

    double simScore = hit.getSimilarity().getScore();
    Color gradientCol = ColorScaleUtil.getColor(MIN_COS_COLOR, MAX_COS_COLOR, MIN_COS_COLOR_VALUE,
        MAX_COS_COLOR_VALUE, simScore);
    boxTitlePanel.setBackground(gradientCol);
    Box boxTitle = Box.createHorizontalBox();
    boxTitle.add(Box.createHorizontalGlue());

    JPanel panelTitle = new JPanel(new BorderLayout());
    panelTitle.setBackground(gradientCol);
    String name = hit.getEntry().getField(DBEntryField.NAME).orElse("N/A").toString();
    JLabel title = new JLabel(name);
    title.setFont(headerFont);
    title.setBackground(gradientCol);
    title.setForeground(Color.WHITE);
    panelTitle.add(title);

    // score result
    JPanel panelScore = new JPanel();
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
    panelScore.setBackground(gradientCol);
    panelScore.add(score);
    panelScore.add(scoreLabel);
    boxTitlePanel.add(panelTitle, BorderLayout.WEST);
    boxTitlePanel.add(panelScore, BorderLayout.EAST);
    boxTitle.add(boxTitlePanel);

    // structure preview
    IAtomContainer molecule;
    JPanel preview2DPanel = new JPanel(new BorderLayout());
    preview2DPanel.setPreferredSize(new Dimension(META_WIDTH, 150));
    preview2DPanel.setMinimumSize(new Dimension(META_WIDTH, 150));
    preview2DPanel.setMaximumSize(new Dimension(META_WIDTH, 150));
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
      preview2DPanel.revalidate();

      metaDataPanel.add(preview2DPanel);
    }

    // information on compound
    JPanel panelCompounds =
        extractMetaData("Compound information", hit.getEntry(), DBEntryField.COMPOUND_FIELDS);

    // instrument info
    JPanel panelInstrument =
        extractMetaData("Instrument information", hit.getEntry(), DBEntryField.INSTRUMENT_FIELDS);

    JPanel g1 = new JPanel(new GridLayout(1, 2, 4, 0));
    g1.setBackground(Color.WHITE);
    g1.add(panelCompounds);
    g1.add(panelInstrument);
    metaDataPanel.add(g1);

    // database links
    JPanel panelDB =
        extractMetaData("Database links", hit.getEntry(), DBEntryField.DATABASE_FIELDS);

    // // Other info
    JPanel panelOther =
        extractMetaData("Other information", hit.getEntry(), DBEntryField.OTHER_FIELDS);

    JPanel g2 = new JPanel(new GridLayout(1, 2, 4, 0));
    g2.setBackground(Color.WHITE);
    g2.add(panelDB);
    g2.add(panelOther);
    metaDataPanel.add(g2);

    // get mirror spectra window
    MirrorScanWindow mirrorWindow = new MirrorScanWindow();
    mirrorWindow.setScans(hit);

    // fixed width panel
    ScrollablePanel pn = new ScrollablePanel(new BorderLayout());
    pn.setScrollableWidth(ScrollablePanel.ScrollableSizeHint.FIT);
    pn.setScrollableHeight(ScrollablePanel.ScrollableSizeHint.STRETCH);
    pn.add(metaDataPanel);


    JScrollPane metaDataPanelScrollPane =
        new JScrollPane(pn, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    spectrumPanel.add(mirrorWindow.getMirrorSpecrumPlot());

    metaDataPanelScrollPane.setPreferredSize(new Dimension(META_WIDTH + 20, ENTRY_HEIGHT));
    panel.setPreferredSize(new Dimension(0, ENTRY_HEIGHT));
    boxTitle.setPreferredSize(new Dimension(META_WIDTH, 45));

    panel.add(boxTitle, BorderLayout.NORTH);
    panel.add(spectrumPanel, BorderLayout.CENTER);
    panel.add(metaDataPanelScrollPane, BorderLayout.EAST);
    panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

    metaDataPanelScrollPane.revalidate();
    pn.revalidate();
    panel.revalidate();
    panel.repaint();
    return panel;
  }

  private JPanel extractMetaData(String title, SpectralDBEntry entry, DBEntryField[] other) {
    JPanel panelOther = new JPanel();
    panelOther.setLayout(new BoxLayout(panelOther, BoxLayout.Y_AXIS));
    panelOther.setBackground(Color.WHITE);
    panelOther.setAlignmentY(Component.TOP_ALIGNMENT);
    panelOther.setAlignmentX(Component.TOP_ALIGNMENT);

    for (DBEntryField db : other) {
      Object o = entry.getField(db).orElse("N/A");
      if (!o.equals("N/A")) {
        SpectralIdentificationSpectralDatabaseTextPane pane =
            new SpectralIdentificationSpectralDatabaseTextPane(db.toString() + ": " + o.toString());
        panelOther.add(pane);
      }
    }

    JLabel otherInfo = new JLabel(title);
    otherInfo.setFont(headerFont);
    JPanel pn = new JPanel(new BorderLayout());
    pn.add(otherInfo, BorderLayout.NORTH);
    pn.add(panelOther, BorderLayout.CENTER);
    JPanel pn1 = new JPanel(new BorderLayout());
    pn1.add(pn, BorderLayout.NORTH);
    pn1.setBackground(Color.WHITE);
    return pn1;
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
        .compare(b.getSimilarity().getScore(), a.getSimilarity().getScore()));

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
      pnGrid.revalidate();
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
