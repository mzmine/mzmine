/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.ConfigService;
import java.awt.Font;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

public class Structure2DComponent extends Canvas {

  private static final Logger logger = Logger.getLogger(Structure2DComponent.class.getName());
  private final Structure2DRenderer renderer;
  private final Property<IAtomContainer> molecule = new SimpleObjectProperty<>();
  private final ObjectProperty<Structure2DRenderConfig> renderConfig = new SimpleObjectProperty<>(
      ConfigService.getStructureRenderConfig());

  public static Structure2DComponent create(String structure) throws CDKException, IOException {

    // Create a silent CDK builder
    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

    // Create a new molecule instance
    IAtomContainer molecule = builder.newInstance(IAtomContainer.class);

    // Load the structure into the molecule
    MDLV2000Reader molReader = new MDLV2000Reader(new StringReader(structure));
    molReader.read(molecule);
    molReader.close();

    return new Structure2DComponent(molecule);
  }

  public Structure2DComponent() {
    this(null);
  }

  public Structure2DComponent(@Nullable IAtomContainer container) {
    this(container, StructureRenderService.getGlobalStructureRenderer());
  }

  public Structure2DComponent(@Nullable IAtomContainer container, Font font) {
    this(container, StructureRenderService.createRenderer(font));
  }

  public Structure2DComponent(@Nullable IAtomContainer container,
      final Structure2DRenderer renderer) {
    this.renderer = renderer;
    molecule.addListener((_, _, mol) -> onStructureChange(mol));
    setMolecule(container);
  }

  private void onStructureChange(final IAtomContainer mol) {
    // If the model has no coordinates, let's generate them
    if (mol != null) {
      renderer.prepareStructure2D(mol);
    }
    repaint();
  }

  /**
   * ensures fx thread repaint
   */
  private void repaint() {
    FxThread.runLater(() -> renderer.drawStructure(this, molecule.getValue(), renderConfig.getValue()));
  }

  @Override
  public boolean isResizable() {
    return true;
  }

  @Override
  public double minWidth(double height) {
    return 25d;
  }

  @Override
  public double minHeight(double width) {
    return 25d;
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
    repaint();
  }

  public IAtomContainer getContainer() {
    return this.molecule.getValue();
  }

  public void setMolecule(final IAtomContainer molecule) {
    this.molecule.setValue(molecule);
  }

  public Property<IAtomContainer> moleculeProperty() {
    return molecule;
  }

  public Structure2DRenderConfig getRenderConfig() {
    return renderConfig.get();
  }

  public ObjectProperty<Structure2DRenderConfig> renderConfigProperty() {
    return renderConfig;
  }

  public void setRenderConfig(Structure2DRenderConfig renderConfig) {
    this.renderConfig.set(renderConfig);
  }
}
