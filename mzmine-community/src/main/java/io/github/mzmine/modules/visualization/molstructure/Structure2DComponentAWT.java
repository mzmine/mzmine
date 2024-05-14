/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.molstructure;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryUtil;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.color.CDK2DAtomColors;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

public class Structure2DComponentAWT extends JComponent {

  private static final long serialVersionUID = 1L;

  public static final Font FONT = new Font("Verdana", Font.PLAIN, 14);

  private AtomContainerRenderer renderer;
  private IAtomContainer molecule;

  public Structure2DComponentAWT(String structure) throws CDKException, IOException {

    // Create a silend CDK builder
    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

    // Create a new molecule instance
    molecule = builder.newInstance(IAtomContainer.class);

    // Load the structure into the molecule
    MDLV2000Reader molReader = new MDLV2000Reader(new StringReader(structure));
    molReader.read(molecule);
    molReader.close();

    // Suppress the hydrogens
    AtomContainerManipulator.suppressHydrogens(molecule);

    // If the model has no coordinates, let's generate them
    if (!GeometryUtil.has2DCoordinates(molecule)) {
      StructureDiagramGenerator sdg = new StructureDiagramGenerator();
      sdg.setMolecule(molecule, false);
      sdg.generateCoordinates();
    }

    // Generators make the image elements
    Font font = new Font("Verdana", Font.PLAIN, 14);
    List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
    generators.add(new BasicSceneGenerator());
    generators.add(new StandardGenerator(font));

    // Renderer needs to have a toolkit-specific font manager
    renderer = new AtomContainerRenderer(generators, new AWTFontManager());

    // Set default atom colors for the renderer
    RendererModel rendererModel = renderer.getRenderer2DModel();
    rendererModel.set(StandardGenerator.AtomColor.class, new CDK2DAtomColors());

  }

  public Structure2DComponentAWT(IAtomContainer container) throws CDKException {
    this(container, FONT);
  }

  public Structure2DComponentAWT(IAtomContainer container, Font font) throws CDKException {
    molecule = container;

    // Suppress the hydrogens
    AtomContainerManipulator.suppressHydrogens(molecule);

    // If the model has no coordinates, let's generate them
    if (!GeometryUtil.has2DCoordinates(molecule)) {
      StructureDiagramGenerator sdg = new StructureDiagramGenerator();
      sdg.setMolecule(molecule, false);
      sdg.generateCoordinates();
    }

    // Generators make the image elements
    List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
    generators.add(new BasicSceneGenerator());
    generators.add(new StandardGenerator(font));

    // Renderer needs to have a toolkit-specific font manager
    renderer = new AtomContainerRenderer(generators, new AWTFontManager());

    // Set default atom colors for the renderer
    RendererModel rendererModel = renderer.getRenderer2DModel();
    rendererModel.set(StandardGenerator.AtomColor.class, new CDK2DAtomColors());
  }

  @Override
  protected void paintComponent(Graphics g) {

    Graphics2D g2 = (Graphics2D) g;
    g2.setColor(Color.WHITE);
    g2.fillRect(0, 0, getWidth(), getHeight());

    final Rectangle drawArea = new Rectangle(getWidth(), getHeight());
    renderer.setup(molecule, drawArea);
    renderer.paint(molecule, new AWTDrawVisitor(g2), drawArea, true);
  }

}
