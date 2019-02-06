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

package net.sf.mzmine.modules.peaklistmethods.identification.sirius.table;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.color.CDK2DAtomColors;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;

/**
 * PreviewCell for chemical structure images
 */
public class PreviewCell extends JComponent {
  private final IAtomContainer molecule;
  private static final AtomContainerRenderer renderer;

  static {
    // Generators make the image elements
    Font font = new Font("Verdana", Font.PLAIN, 14);
    List<IGenerator<IAtomContainer>> generators = new ArrayList<>();
    generators.add(new BasicSceneGenerator());
    generators.add(new StandardGenerator(font));

    // Renderer needs to have a toolkit-specific font manager
    renderer = new AtomContainerRenderer(generators, new AWTFontManager());
  }

  /**
   * Define renderers and save molecule`s container
   * @param molecule
   */
  public PreviewCell(IAtomContainer molecule) {
    this.molecule = molecule;
  }

  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    g2.setColor(Color.WHITE);
    g2.fillRect(0, 0, getWidth(), getHeight());

    final Rectangle drawArea = new Rectangle(getWidth(), getHeight());
    renderer.setup(molecule, drawArea);
    renderer.paint(molecule, new AWTDrawVisitor(g2), drawArea, true);

    // Set default atom colors for the renderer
    RendererModel rendererModel = renderer.getRenderer2DModel();
    rendererModel.set(StandardGenerator.AtomColor.class, new CDK2DAtomColors());
  }
}
