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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import org.jfree.fx.FXGraphics2D;
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
import javafx.scene.canvas.Canvas;

public class Structure2DComponent extends Canvas {

  public static final Font FONT = new Font("Verdana", Font.PLAIN, 14);

  private AtomContainerRenderer renderer;
  private IAtomContainer molecule;

  public Structure2DComponent(String structure) throws CDKException, IOException {

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

  public Structure2DComponent(IAtomContainer container) throws CDKException {
    this(container, FONT);
  }

  public Structure2DComponent(IAtomContainer container, Font font) throws CDKException {
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
  public boolean isResizable() {
    return true;
  }

  @Override
  public double minWidth(double height) {
    return 100d;
  }

  @Override
  public double minHeight(double width) {
    return 50d;
  }

  @Override
  public double maxHeight(double width) {
    return Double.POSITIVE_INFINITY;
  }

  @Override
  public double maxWidth(double height) {
    return Double.POSITIVE_INFINITY;
  }

  @Override
  public double prefWidth(double height) {
    return getWidth();
  }

  @Override
  public double prefHeight(double width) {
    return getHeight();
  }

  @Override
  public void resize(double width, double height) {

    super.setWidth(width);
    super.setHeight(height);

    Graphics2D g2 = new FXGraphics2D(this.getGraphicsContext2D());

    g2.setColor(Color.WHITE);
    g2.fillRect(0, 0, (int) width, (int) height);

    final Rectangle drawArea = new Rectangle((int) width, (int) height);
    renderer.setup(molecule, drawArea);
    renderer.paint(molecule, new AWTDrawVisitor(g2), drawArea, true);
  }
  public IAtomContainer getContainer()
  {
    return this.molecule;
  }
  
}
