/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import io.github.mzmine.main.ConfigService;
import java.awt.Font;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
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

  private final BooleanProperty contextMenuEnabled = new SimpleBooleanProperty(true);

  // overlay drawn after the structure in the top-right corner; null/empty = no overlay
  private final StringProperty topRightText = new SimpleStringProperty(null);

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
    // overlay text changes trigger a full repaint (the renderer clears the canvas background, so
    // we cannot just append; the structure has to be redrawn too)
    topRightText.addListener((_, _, _) -> repaint());
    setMolecule(container);

    // Create context menu
    final ContextMenu contextMenu = new ContextMenu();
    MenuItem saveSvg = new MenuItem("Save structure as svg");
    saveSvg.setOnAction(e -> {
      final IAtomContainer mol = molecule.getValue();
      if (mol == null) {
        return;
      }
      StructureGraphicsExportModule.exportToSvg(mol);
    });
    contextMenu.getItems().addAll(saveSvg);

    // Show context menu on right click if enabled
    setOnContextMenuRequested(e -> {
      if (isContextMenuEnabled()) {
        contextMenu.show(this, e.getScreenX(), e.getScreenY());
      }
    });
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
    renderer.drawStructure(this, molecule.getValue(), renderConfig.getValue());
    drawTopRightOverlay();
  }

  // Draws topRightText (if set) on top of the rendered structure. Called after the renderer fills
  // and paints, so the overlay sits above the structure.
  private void drawTopRightOverlay() {
    final String text = topRightText.get();
    if (text == null || text.isBlank()) {
      return;
    }
    final GraphicsContext gc = getGraphicsContext2D();
    final double padding = 4d;
    final javafx.scene.text.Font font = javafx.scene.text.Font.font(11d);
    gc.save();
    gc.setFont(font);
    gc.setTextAlign(TextAlignment.RIGHT);
    gc.setTextBaseline(VPos.TOP);
    gc.setFill(Color.BLACK);
    gc.fillText(text, getWidth() - padding, padding);
    gc.restore();
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

  public boolean isContextMenuEnabled() {
    return contextMenuEnabled.get();
  }

  public BooleanProperty contextMenuEnabledProperty() {
    return contextMenuEnabled;
  }

  public void setContextMenuEnabled(boolean contextMenuEnabled) {
    this.contextMenuEnabled.set(contextMenuEnabled);
  }

  public @Nullable String getTopRightText() {
    return topRightText.get();
  }

  public StringProperty topRightTextProperty() {
    return topRightText;
  }

  /**
   * Set an optional short label drawn over the structure in the top-right corner. Pass null or
   * empty to clear.
   */
  public void setTopRightText(@Nullable String text) {
    this.topRightText.set(text);
  }
}
