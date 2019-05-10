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
import java.util.Map;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.desktop.impl.WindowsMenu;
import net.sf.mzmine.modules.visualization.spectra.mirrorspectra.MirrorScanWindow;
import net.sf.mzmine.modules.visualization.spectra.multimsms.pseudospectra.PseudoSpectraRenderer;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;

public class SpectraIdentificationResultsWindow extends JFrame {
  /**
   * Window to show all spectral database matches from selected scan
   * 
   * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
   */

  private static final long serialVersionUID = 1L;
  private JPanel pnGrid;
  private Font titleFont = new Font("Dialog", Font.BOLD, 18);
  private Font scoreFont = new Font("Dialog", Font.BOLD, 36);
  private Font headerFont = new Font("Dialog", Font.BOLD, 16);
  private static final DecimalFormat COS_FORM = new DecimalFormat("0.000");

  public SpectraIdentificationResultsWindow(Scan scan, Map<Double, SpectralDBEntry> matches) {
    setBackground(Color.WHITE);
    setSize(new Dimension(1200, 800));
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout());
    setTitle("Spectra matching for scan#" + scan.getScanNumber());

    pnGrid = new JPanel();
    // any number of rows
    pnGrid.setLayout(new GridLayout(0, 1, 0, 25));
    pnGrid.setAutoscrolls(false);

    // Add the Windows menu
    JMenuBar menuBar = new JMenuBar();
    menuBar.add(new WindowsMenu());
    setJMenuBar(menuBar);

    // add charts
    for (Map.Entry<Double, SpectralDBEntry> match : matches.entrySet()) {
      setScanAndShow(scan, match.getValue(), match.getKey());
    }
    setVisible(true);
    JScrollPane scrollPane = new JScrollPane(pnGrid);
    getContentPane().add(scrollPane, BorderLayout.CENTER);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setViewportView(pnGrid);
    scrollPane.getViewport().setBorder(BorderFactory.createLineBorder(Color.black));

    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    validate();
    repaint();
    pack();
  }

  public boolean setScanAndShow(Scan scan, SpectralDBEntry ident, Double simScore) {
    // clear
    pnGrid.add(addSpectra(scan, ident, simScore));
    // show1
    pnGrid.revalidate();
    pnGrid.repaint();
    return true;
  }

  private JPanel addSpectra(Scan scan, SpectralDBEntry ident, Double simScore) {
    JPanel panel = new JPanel(new BorderLayout());

    JSplitPane spectrumPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    // set meta data from identity
    JPanel metaDataPanel = new JPanel();

    LayoutManager layout = new BoxLayout(metaDataPanel, BoxLayout.Y_AXIS);
    metaDataPanel.setLayout(layout);
    metaDataPanel.setPreferredSize(new Dimension(500, 600));

    // add title
    Random r1 = new Random();
    Color randomCol = Color.getHSBColor(r1.nextFloat(), 1.0f, 0.6f);
    Box boxTitle = Box.createHorizontalBox();
    boxTitle.add(Box.createHorizontalGlue());

    JPanel panelTitle = new JPanel(new BorderLayout());
    panelTitle.setBackground(randomCol);
    panelTitle.setPreferredSize(new Dimension(250, 75));
    panelTitle.setMinimumSize(new Dimension(250, 75));
    panelTitle.setMaximumSize(new Dimension(250, 75));
    String name = (String) ident.getField(DBEntryField.NAME).orElse("N/A");
    JLabel title = new JLabel();
    if (name.length() >= 20) {
      title = new JLabel(name.substring(0, 19) + "...");
    } else {
      title = new JLabel(name);
    }
    title.setToolTipText((String) ident.getField(DBEntryField.NAME).orElse("N/A"));
    title.setFont(headerFont);
    title.setForeground(Color.WHITE);
    panelTitle.add(title);

    // score result
    JPanel panelScore = new JPanel();
    panelScore.setLayout(new BoxLayout(panelScore, BoxLayout.Y_AXIS));
    panelScore.setPreferredSize(new Dimension(250, 75));
    panelScore.setMinimumSize(new Dimension(250, 75));
    panelScore.setMaximumSize(new Dimension(250, 75));
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
    boxTitle.add(panelTitle);
    boxTitle.add(panelScore);
    metaDataPanel.add(boxTitle);


    Box boxCompoundAndInstrument = Box.createHorizontalBox();
    // information on compound
    JPanel panelCompounds = new JPanel(new GridLayout(0, 1, 0, 5));
    panelCompounds.setPreferredSize(new Dimension(250, 400));
    JLabel compoundInfo = new JLabel("Compound information");
    compoundInfo.setToolTipText(
        "This section shows all the compound information listed in the used database");
    compoundInfo.setFont(headerFont);
    panelCompounds.add(compoundInfo);
    JLabel synonym = new JLabel("Synonym: " + ident.getField(DBEntryField.SYNONYM).orElse("N/A"));
    synonym.setText("Synonym: " + ident.getField(DBEntryField.SYNONYM).orElse("N/A"));
    if (ident.getField(DBEntryField.SYNONYM).orElse("N/A") != "N/A")
      panelCompounds.add(synonym);
    JLabel formula = new JLabel("Formula: " + ident.getField(DBEntryField.FORMULA).orElse("N/A"));
    formula.setToolTipText("Formula: " + ident.getField(DBEntryField.FORMULA).orElse("N/A"));
    if (ident.getField(DBEntryField.FORMULA).orElse("N/A") != "N/A")
      panelCompounds.add(formula);
    JLabel molarWeight =
        new JLabel("Molar Weight: " + ident.getField(DBEntryField.MOLWEIGHT).orElse("N/A"));
    molarWeight
        .setToolTipText("Molar Weight: " + ident.getField(DBEntryField.MOLWEIGHT).orElse("N/A"));
    if (ident.getField(DBEntryField.MOLWEIGHT).orElse("N/A") != "N/A")
      panelCompounds.add(molarWeight);
    JLabel exactMass =
        new JLabel("Exact mass: " + ident.getField(DBEntryField.EXACT_MASS).orElse("N/A"));
    exactMass
        .setToolTipText("Exact mass: " + ident.getField(DBEntryField.EXACT_MASS).orElse("N/A"));
    if (ident.getField(DBEntryField.EXACT_MASS).orElse("N/A") != "N/A")
      panelCompounds.add(exactMass);
    JLabel ionType = new JLabel("Ion type: " + ident.getField(DBEntryField.IONTYPE).orElse("N/A"));
    ionType.setToolTipText("Ion type: " + ident.getField(DBEntryField.IONTYPE).orElse("N/A"));
    if (ident.getField(DBEntryField.IONTYPE).orElse("N/A") != "N/A")
      panelCompounds.add(ionType);
    JLabel rt = new JLabel("Retention time: " + ident.getField(DBEntryField.RT).orElse("N/A"));
    rt.setToolTipText("Retention time: " + ident.getField(DBEntryField.RT).orElse("N/A"));
    if (ident.getField(DBEntryField.RT).orElse("N/A") != "N/A")
      panelCompounds.add(rt);
    JLabel mz = new JLabel("m/z: " + ident.getField(DBEntryField.MZ).orElse("N/A"));
    mz.setToolTipText("m/z: " + ident.getField(DBEntryField.MZ).orElse("N/A"));
    if (ident.getField(DBEntryField.MZ).orElse("N/A") != "N/A")
      panelCompounds.add(mz);
    JLabel charge = new JLabel("Charge: " + ident.getField(DBEntryField.CHARGE).orElse("N/A"));
    charge.setToolTipText("Charge: " + ident.getField(DBEntryField.CHARGE).orElse("N/A"));
    if (ident.getField(DBEntryField.CHARGE).orElse("N/A") != "N/A")
      panelCompounds.add(charge);
    JLabel ionMode = new JLabel("Ion mode: " + ident.getField(DBEntryField.ION_MODE).orElse("N/A"));
    ionMode.setToolTipText("Ion mode: " + ident.getField(DBEntryField.ION_MODE).orElse("N/A"));
    if (ident.getField(DBEntryField.ION_MODE).orElse("N/A") != "N/A")
      panelCompounds.add(ionMode);
    JLabel inchi = new JLabel("INCHI: " + ident.getField(DBEntryField.INCHI).orElse("N/A"));
    inchi.setToolTipText("INCHI: " + ident.getField(DBEntryField.INCHI).orElse("N/A"));
    if (ident.getField(DBEntryField.INCHI).orElse("N/A") != "N/A")
      panelCompounds.add(inchi);
    JLabel inchiKey =
        new JLabel("INCHI Key: " + ident.getField(DBEntryField.INCHIKEY).orElse("N/A"));
    inchiKey.setToolTipText("INCHI Key: " + ident.getField(DBEntryField.INCHIKEY).orElse("N/A"));
    if (ident.getField(DBEntryField.INCHI).orElse("N/A") != "N/A")
      panelCompounds.add(inchiKey);
    JLabel smiles = new JLabel("SMILES: " + ident.getField(DBEntryField.SMILES).orElse("N/A"));
    smiles.setToolTipText("SMILES: " + ident.getField(DBEntryField.SMILES).orElse("N/A"));
    if (ident.getField(DBEntryField.SMILES).orElse("N/A") != "N/A")
      panelCompounds.add(smiles);
    JLabel cas = new JLabel("CAS: " + ident.getField(DBEntryField.CAS).orElse("N/A"));
    cas.setToolTipText("CAS: " + ident.getField(DBEntryField.CAS).orElse("N/A"));
    if (ident.getField(DBEntryField.CAS).orElse("N/A") != "N/A")
      panelCompounds.add(cas);
    JLabel numberPeaks =
        new JLabel("Number of peaks: " + ident.getField(DBEntryField.NUM_PEAKS).orElse("N/A"));
    numberPeaks
        .setToolTipText("Number of peaks: " + ident.getField(DBEntryField.NUM_PEAKS).orElse("N/A"));
    if (ident.getField(DBEntryField.NUM_PEAKS).orElse("N/A") != "N/A")
      panelCompounds.add(numberPeaks);

    // instrument info
    JPanel panelInstrument = new JPanel(new GridLayout(0, 1, 0, 5));
    panelInstrument.setPreferredSize(new Dimension(250, 400));
    JLabel instrumentInfo = new JLabel("Instrument information");
    instrumentInfo.setFont(headerFont);
    panelInstrument.add(instrumentInfo);
    instrumentInfo.setToolTipText(
        "This section shows all the instrument information listed in the used database");
    JLabel instrumentType = new JLabel(
        "Instrument type: " + ident.getField(DBEntryField.INSTRUMENT_TYPE).orElse("N/A"));
    instrumentType.setToolTipText(
        "Instrument type: " + ident.getField(DBEntryField.INSTRUMENT_TYPE).orElse("N/A"));
    if (ident.getField(DBEntryField.INSTRUMENT_TYPE).orElse("N/A") != "N/A")
      panelInstrument.add(instrumentType);
    JLabel instrument =
        new JLabel("Instrument: " + ident.getField(DBEntryField.INSTRUMENT).orElse("N/A"));
    instrument
        .setToolTipText("Instrument: " + ident.getField(DBEntryField.INSTRUMENT).orElse("N/A"));
    if (ident.getField(DBEntryField.INSTRUMENT).orElse("N/A") != "N/A")
      panelInstrument.add(instrument);
    JLabel ionSource =
        new JLabel("Ion source: " + ident.getField(DBEntryField.ION_SOURCE).orElse("N/A"));
    ionSource
        .setToolTipText("Ion source: " + ident.getField(DBEntryField.ION_SOURCE).orElse("N/A"));
    if (ident.getField(DBEntryField.ION_SOURCE).orElse("N/A") != "N/A")
      panelInstrument.add(ionSource);
    JLabel resolution =
        new JLabel("Resolution: " + ident.getField(DBEntryField.RESOLUTION).orElse("N/A"));
    resolution
        .setToolTipText("Resolution: " + ident.getField(DBEntryField.RESOLUTION).orElse("N/A"));
    if (ident.getField(DBEntryField.RESOLUTION).orElse("N/A") != "N/A")
      panelInstrument.add(resolution);
    JLabel msLevel = new JLabel("MS level: " + ident.getField(DBEntryField.MS_LEVEL).orElse("N/A"));
    msLevel.setToolTipText("MS level: " + ident.getField(DBEntryField.MS_LEVEL).orElse("N/A"));
    if (ident.getField(DBEntryField.MS_LEVEL).orElse("N/A") != "N/A")
      panelInstrument.add(msLevel);
    JLabel collision = new JLabel(
        "Collision energy: " + ident.getField(DBEntryField.COLLISION_ENERGY).orElse("N/A"));
    collision.setToolTipText(
        "Collision enery: " + ident.getField(DBEntryField.COLLISION_ENERGY).orElse("N/A"));
    if (ident.getField(DBEntryField.COLLISION_ENERGY).orElse("N/A") != "N/A")
      panelInstrument.add(collision);
    JLabel acquisition =
        new JLabel("Acquisition: " + ident.getField(DBEntryField.ACQUISITION).orElse("N/A"));
    acquisition
        .setToolTipText("Acquisition: " + ident.getField(DBEntryField.ACQUISITION).orElse("N/A"));
    if (ident.getField(DBEntryField.ACQUISITION).orElse("N/A") != "N/A")
      panelInstrument.add(acquisition);
    JLabel software =
        new JLabel("Software: " + ident.getField(DBEntryField.SOFTWARE).orElse("N/A"));
    software.setToolTipText("Software: " + ident.getField(DBEntryField.SOFTWARE).orElse("N/A"));
    if (ident.getField(DBEntryField.SOFTWARE).orElse("N/A") != "N/A")
      panelInstrument.add(software);

    boxCompoundAndInstrument.add(panelCompounds, BorderLayout.WEST);
    boxCompoundAndInstrument.add(panelInstrument, BorderLayout.EAST);
    metaDataPanel.add(boxCompoundAndInstrument);

    Box boxDBAndOther = Box.createHorizontalBox();
    // database links
    JPanel panelDB = new JPanel(new GridLayout(0, 1, 0, 5));
    panelDB.setPreferredSize(new Dimension(250, 200));
    JLabel databaseLinks = new JLabel("Database links");
    databaseLinks
        .setToolTipText("This section shows links to other databases of the matched compound");
    databaseLinks.setFont(headerFont);
    panelDB.add(databaseLinks);
    JLabel pubmed = new JLabel("PUBMED: " + ident.getField(DBEntryField.PUBMED).orElse("N/A"));
    pubmed.setToolTipText("PUBMED: " + ident.getField(DBEntryField.PUBMED).orElse("N/A"));
    if (ident.getField(DBEntryField.PUBMED).orElse("N/A") != "N/A")
      panelDB.add(pubmed);
    JLabel pubchem = new JLabel("PUBCHEM: " + ident.getField(DBEntryField.PUBCHEM).orElse("N/A"));
    pubchem.setToolTipText("PUBCHEM: " + ident.getField(DBEntryField.PUBCHEM).orElse("N/A"));
    if (ident.getField(DBEntryField.PUBCHEM).orElse("N/A") != "N/A")
      panelDB.add(pubchem);
    JLabel mona = new JLabel("MONA ID: " + ident.getField(DBEntryField.MONA_ID).orElse("N/A"));
    mona.setToolTipText("MONA ID: " + ident.getField(DBEntryField.MONA_ID).orElse("N/A"));
    if (ident.getField(DBEntryField.MONA_ID).orElse("N/A") != "N/A")
      panelDB.add(mona);
    JLabel spider =
        new JLabel("Chemspider: " + ident.getField(DBEntryField.CHEMSPIDER).orElse("N/A"));
    spider.setToolTipText("Chemspider: " + ident.getField(DBEntryField.CHEMSPIDER).orElse("N/A"));
    if (ident.getField(DBEntryField.CHEMSPIDER).orElse("N/A") != "N/A")
      panelDB.add(spider);

    // // Other info
    JPanel panelOther = new JPanel(new GridLayout(0, 1, 0, 5));
    panelOther.setPreferredSize(new Dimension(250, 200));
    JLabel otherInfo = new JLabel("Other information");
    otherInfo.setToolTipText("This section shows all other information of the matched compound");
    otherInfo.setFont(headerFont);
    panelOther.add(otherInfo);
    JLabel investigator = new JLabel("Prinziple investigator: "
        + ident.getField(DBEntryField.PRINZIPLE_INVESTIGATOR).orElse("N/A"));
    investigator.setToolTipText("Prinziple investigator: "
        + ident.getField(DBEntryField.PRINZIPLE_INVESTIGATOR).orElse("N/A"));
    if (ident.getField(DBEntryField.PRINZIPLE_INVESTIGATOR).orElse("N/A") != "N/A")
      panelOther.add(investigator);
    JLabel collector =
        new JLabel("Data collector: " + ident.getField(DBEntryField.DATA_COLLECTOR).orElse("N/A"));
    collector.setToolTipText(
        "Data collector: " + ident.getField(DBEntryField.DATA_COLLECTOR).orElse("N/A"));
    if (ident.getField(DBEntryField.DATA_COLLECTOR).orElse("N/A") != "N/A")
      panelOther.add(collector);
    JLabel entry =
        new JLabel("DB entry ID: " + ident.getField(DBEntryField.ENTRY_ID).orElse("N/A"));
    entry.setToolTipText("DB entry ID: " + ident.getField(DBEntryField.ENTRY_ID).orElse("N/A"));
    if (ident.getField(DBEntryField.ENTRY_ID).orElse("N/A") != "N/A")
      panelOther.add(entry);
    JLabel comment = new JLabel("Comment: " + ident.getField(DBEntryField.COMMENT).orElse("N/A"));
    comment.setToolTipText("Comment: " + ident.getField(DBEntryField.COMMENT).orElse("N/A"));
    if (ident.getField(DBEntryField.COMMENT).orElse("N/A") != "N/A")
      panelOther.add(comment);

    boxDBAndOther.add(panelDB, BorderLayout.WEST);
    boxDBAndOther.add(panelOther, BorderLayout.EAST);
    metaDataPanel.add(boxDBAndOther);

    // get mirror spectra window
    MirrorScanWindow mirrorWindow = new MirrorScanWindow();
    mirrorWindow.setScans(scan, ident);

    // get spectra plot
    EChartPanel spectraPlots = mirrorWindow.getMirrorSpecrumPlot();
    spectraPlots.setPreferredSize(new Dimension(500, 400));

    // set up renderer
    PseudoSpectraRenderer renderer1 = new PseudoSpectraRenderer(Color.blue, false);
    PseudoSpectraRenderer renderer2 = new PseudoSpectraRenderer(randomCol, false);

    spectraPlots.getChart().getLegend().setVisible(false);
    CombinedDomainXYPlot domainPlot = (CombinedDomainXYPlot) spectraPlots.getChart().getXYPlot();
    NumberAxis axis = (NumberAxis) domainPlot.getDomainAxis();
    axis.setLabel("m/z");
    XYPlot spectrumPlot = (XYPlot) domainPlot.getSubplots().get(0);
    spectrumPlot.setRenderer(renderer1);
    XYPlot databaseSpectrumPlot = (XYPlot) domainPlot.getSubplots().get(1);
    databaseSpectrumPlot.setRenderer(renderer2);

    spectrumPane.add(spectraPlots);
    spectrumPane.add(metaDataPanel);
    spectrumPane.setResizeWeight(1);
    spectrumPane.setEnabled(false);
    spectrumPane.setDividerSize(5);
    panel.add(spectrumPane);
    panel.setBorder(BorderFactory.createLineBorder(Color.black));
    return panel;
  }

}
