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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.Canvas;
import org.jfree.fx.FXGraphics2D;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.color.CDK2DAtomColors;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;

public class Structure2DRenderer {

  private final AtomContainerRenderer renderer;

  public Structure2DRenderer(final Font font) {
    // Generators make the image elements
    List<IGenerator<IAtomContainer>> generators = new ArrayList<>();
    generators.add(new BasicSceneGenerator());
    generators.add(new StandardGenerator(font));

    // Renderer needs to have a toolkit-specific font manager
    renderer = new AtomContainerRenderer(generators, new AWTFontManager());

    // Set default atom colors for the renderer
    RendererModel rendererModel = renderer.getRenderer2DModel();
    rendererModel.set(StandardGenerator.AtomColor.class, new CDK2DAtomColors());
  }

  /**
   * Thread safe structure draw
   *
   * @param canvas   any canvas to draw on, e.g., {@link Structure2DComponent}
   * @param molecule to draw
   */
  public void drawStructure(final Canvas canvas, final IAtomContainer molecule) {
    Graphics2D g2 = new FXGraphics2D(canvas.getGraphicsContext2D());

    int width = (int) canvas.getWidth();
    int height = (int) canvas.getHeight();
    g2.setColor(Color.WHITE);
    g2.fillRect(0, 0, width, height);

    if (molecule == null) {
      return;
    }
    final Rectangle drawArea = new Rectangle(width, height);
    // needs to be synchronized here to avoid concurrent access
    synchronized (renderer) {
      renderer.setup(molecule, drawArea);
      renderer.paint(molecule, new AWTDrawVisitor(g2), drawArea, true);
    }
  }

  public AtomContainerRenderer getRenderer() {
    return renderer;
  }
}
