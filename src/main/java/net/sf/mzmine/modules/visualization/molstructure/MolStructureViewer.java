/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.molstructure;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.desktop.impl.WindowsMenu;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.InetUtils;
import net.sf.mzmine.util.components.MultiLineLabel;
import org.openscience.cdk.interfaces.IAtomContainer;

public class MolStructureViewer extends JFrame {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private JSplitPane splitPane;
  private JLabel loading2Dlabel, loading3Dlabel;

  /**
   * Constructor of MolStructureViewer, loads 2d and 3d structures into JPanel specified by urls
   * @param name
   * @param structure2DAddress
   * @param structure3DAddress
   */
  public MolStructureViewer(String name, final URL structure2DAddress,
      final URL structure3DAddress) {

    super("Structure of " + name);
    setupViewer(name);

    if (structure2DAddress != null) {
      Thread loading2DThread = new Thread(new Runnable() {
        public void run() {
          load2DStructure(structure2DAddress);
        }
      }, "Structure loading thread");
      loading2DThread.start();
    } else {
      loading2Dlabel.setText("2D structure not available");
    }

    if (structure3DAddress != null) {
      Thread loading3DThread = new Thread(new Runnable() {
        public void run() {
          load3DStructure(structure3DAddress);
        }
      }, "Structure loading thread");
      loading3DThread.start();
    } else {
      loading3Dlabel.setText("3D structure not available");
    }

  }

  /**
   * Constructor for MolStructureViewer from AtomContainer and only for 2D object
   * The 3D view will be unavailable
   * @param name
   * @param structure2D AtomContainer
   */
  public MolStructureViewer(String name, final IAtomContainer structure2D) {
    super("Structure of " + name);
    setupViewer(name);

    if (structure2D != null) {
      Thread loading2DThread = new Thread(() -> {
        load2DStructure(structure2D);
      }, "Structure loading thread");
      loading2DThread.start();
    } else {
      loading2Dlabel.setText("2D structure not available");
    }

    loading3Dlabel.setText("3D structure not available");
  }

  /**
   * Load initial parameters for JPanel
   * @param name
   */
  private void setupViewer(String name) {
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    // Main panel - contains a title (compound name) in the top, 2D
    // structure on the left, 3D structure on the right
    JPanel mainPanel = new JPanel(new BorderLayout());

    JLabel labelName = new JLabel(name, SwingConstants.CENTER);
    labelName.setOpaque(true);
    labelName.setBackground(Color.white);
    labelName.setForeground(Color.BLUE);
    Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
    Border two = BorderFactory.createEmptyBorder(5, 5, 5, 5);
    labelName.setBorder(BorderFactory.createCompoundBorder(one, two));
    labelName.setFont(new Font("SansSerif", Font.BOLD, 18));
    mainPanel.add(labelName, BorderLayout.NORTH);

    loading2Dlabel = new JLabel("Loading 2D structure...",
        SwingConstants.CENTER);
    loading2Dlabel.setOpaque(true);
    loading2Dlabel.setBackground(Color.white);
    loading3Dlabel = new JLabel("Loading 3D structure...",
        SwingConstants.CENTER);
    loading3Dlabel.setOpaque(true);
    loading3Dlabel.setBackground(Color.white);

    splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, loading2Dlabel,
        loading3Dlabel);
    splitPane.setResizeWeight(0.5);

    mainPanel.add(splitPane, BorderLayout.CENTER);

    add(mainPanel);

    setPreferredSize(new Dimension(900, 500));

    // Add the Windows menu
    JMenuBar menuBar = new JMenuBar();
    menuBar.add(new WindowsMenu());
    setJMenuBar(menuBar);

    pack();

    // Set the initial splitter location, after the window is packed
    splitPane.setDividerLocation(500);
  }

  /**
   * Load the structure passed as parameter in JChemViewer
   */
  private void load2DStructure(URL url) {

    JComponent newComponent;
    try {
      String structure2D = InetUtils.retrieveData(url);
      if (structure2D.length() < 10) {
        loading2Dlabel.setText("2D structure not available");
        return;
      }
      newComponent = new Structure2DComponent(structure2D);
    } catch (Exception e) {
      String errorMessage = "Could not load 2D structure\n"
          + "Exception: " + ExceptionUtils.exceptionToString(e);
      newComponent = new MultiLineLabel(errorMessage);
    }
    splitPane.setLeftComponent(newComponent);
    splitPane.setDividerLocation(500);
  }

  /**
   * Load the AtomContainer passed as parameter in JChemViewer
   * @param container
   */
  private void load2DStructure(IAtomContainer container) {
    JComponent newComponent;
    try {
      newComponent = new Structure2DComponent(container);
    } catch (Exception e) {
      String errorMessage = "Could not load 2D structure\n"
          + "Exception: " + ExceptionUtils.exceptionToString(e);
      newComponent = new MultiLineLabel(errorMessage);
    }
    splitPane.setLeftComponent(newComponent);
    splitPane.setDividerLocation(500);
  }

  /**
   * Load the structure passed as parameter in JmolViewer
   */
  private void load3DStructure(URL url) {

    try {

      String structure3D = InetUtils.retrieveData(url);

      // If the returned structure is empty or too short, just return
      if (structure3D.length() < 10) {
        loading3Dlabel.setText("3D structure not available");
        return;
      }

      // Check for html tag, to recognize PubChem error message
      if (structure3D.contains("<html>")) {
        loading3Dlabel.setText("3D structure not available");
        return;
      }

      Structure3DComponent new3DComponent = new Structure3DComponent();
      splitPane.setRightComponent(new3DComponent);
      splitPane.setDividerLocation(500);

      // loadStructure must be called after the component is added,
      // otherwise Jmol will freeze waiting for repaint (IMHO this is a
      // Jmol bug introduced in 11.8)
      new3DComponent.loadStructure(structure3D);

    } catch (Exception e) {
      String errorMessage = "Could not load 3D structure\n"
          + "Exception: " + ExceptionUtils.exceptionToString(e);
      MultiLineLabel label = new MultiLineLabel(errorMessage, 10);
      splitPane.setRightComponent(label);
      splitPane.setDividerLocation(500);
    }

  }
}
