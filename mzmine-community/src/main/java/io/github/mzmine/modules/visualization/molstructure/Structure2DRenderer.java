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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.modules.visualization.molstructure.Structure2DRenderConfig.Sizing;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.canvas.Canvas;
import javax.vecmath.Point2d;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.BoundsCalculator;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.color.CDK2DAtomColors;
import org.openscience.cdk.renderer.elements.IRenderingElement;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.renderer.visitor.AbstractAWTDrawVisitor;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * One renderer can be reused to draw on many graphics objects
 */
public class Structure2DRenderer extends AtomContainerRenderer {

  private static final Logger logger = Logger.getLogger(Structure2DRenderer.class.getName());

  public Structure2DRenderer(final Font font) {
    // Generators make the image elements
    List<IGenerator<IAtomContainer>> generators = new ArrayList<>();
    generators.add(new BasicSceneGenerator());
    generators.add(new StandardGenerator(font));

    // Renderer needs to have a toolkit-specific font manager
    super(generators, new AWTFontManager());

    // Set default atom colors for the renderer
    RendererModel rendererModel = getRenderer2DModel();
    rendererModel.set(StandardGenerator.AtomColor.class, new CDK2DAtomColors());

    // space between atom label and bond line
    rendererModel.set(StandardGenerator.SymbolMarginRatio.class, 1.5d); // smaller space label bond
    rendererModel.set(StandardGenerator.BondSeparation.class, 0.20d); // further apart
    rendererModel.set(StandardGenerator.HashSpacing.class, 2.75d); // bonds | | | spacing
//    rendererModel.set(StandardGenerator.WaveSpacing.class, 2.75d); //
    rendererModel.set(StandardGenerator.WedgeRatio.class, 6d); // end width of > < bonds

    // custom colors
    rendererModel.set(StandardGenerator.AtomColor.class, new MZmine2DAtomColors());

    // bond width usually similar to | pipe symbol thickness
    // bold font makes bonds to thick so use a smaller ratio
    rendererModel.set(StandardGenerator.StrokeRatio.class, 0.95d);
  }

  /**
   * Thread safe structure draw
   *
   * @param canvas   any canvas to draw on, e.g., {@link Structure2DComponent}
   * @param molecule to draw
   * @param config
   */
  public void drawStructure(final Canvas canvas, final IAtomContainer molecule,
      Structure2DRenderConfig config) {
    final JavaFxStructureDrawVisitor visitor = new JavaFxStructureDrawVisitor(canvas);
    visitor.setRounding(true);
    canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    canvas.getGraphicsContext2D().setFill(javafx.scene.paint.Color.WHITE);
    canvas.getGraphicsContext2D().fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    drawStructure(visitor, (int) canvas.getWidth(), (int) canvas.getHeight(), molecule, config);
  }


  /**
   * Thread safe structure draw
   *
   * @param g2
   * @param molecule to draw
   * @param config
   */
  public Rectangle drawStructureNoBounds(final Graphics2D g2, final IAtomContainer molecule,
      Structure2DRenderConfig config) {
    final AWTDrawVisitor visitor = new AWTDrawVisitor(g2);
    visitor.setRounding(false);
    return drawStructureNoBounds(visitor, molecule, config);
  }

  /**
   * Thread safe structure draw
   *
   * @param g2
   * @param width
   * @param height
   * @param molecule to draw
   * @param config
   */
  public void drawStructure(final Graphics2D g2, double width, double height,
      final IAtomContainer molecule, Structure2DRenderConfig config) {
    final AWTDrawVisitor visitor = new AWTDrawVisitor(g2);
    visitor.setRounding(false);
    g2.setColor(Color.WHITE);
    g2.fillRect(0, 0, (int) width + 1, (int) height + 1);
    drawStructure(visitor, (int) width, (int) height, molecule, config);
  }

  public String drawStructureToSvgString(IAtomContainer molecule, @Nullable Integer width,
      @Nullable Integer height, Structure2DRenderConfig config) throws Exception {
    // Prepare 2D coordinates if needed
    prepareStructure2D(molecule);

    // Create SVG document and graphics
    DOMImplementation domImpl = SVGDOMImplementation.getDOMImplementation();
    Document document = domImpl.createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null);
    SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

    if (width == null || height == null) {
      // draw structure without bounds to auto size svg
      Rectangle actualSize = drawStructureNoBounds(svgGenerator, molecule, config);
      svgGenerator.setSVGCanvasSize(actualSize.getSize());
    } else {
      // Render the structure on a fixed size canvas
      svgGenerator.setSVGCanvasSize(new Dimension(width, height));
      drawStructure(svgGenerator, width, height, molecule, config);
    }

    // Convert to SVG string
    StringWriter stringWriter = new StringWriter();
    // skip the <?xml version="1.0" encoding="UTF-8" standalone="no"?>
    // by using getRoot
    svgGenerator.stream(stringWriter, true); // false = use CSS styling

    String svg = stringWriter.toString();
    final int index = svg.indexOf("<svg ");
    if (index > 0) {
      svg = svg.substring(index);
    }

    svg = svg.replace("<!--Generated by the Batik Graphics2D SVG Generator-->",
        "<!--Generated with mzmine-->");

    return svg;
  }

  public String drawStructureToSvgStringFixedSize(IAtomContainer molecule,
      Structure2DRenderConfig config) throws Exception {
    return drawStructureToSvgString(molecule, null, null, config);
  }

  /**
   * Thread safe structure draw
   *
   * @param visitor
   * @param molecule to draw
   * @param config
   */
  public Rectangle drawStructureNoBounds(AbstractAWTDrawVisitor visitor,
      final IAtomContainer molecule, Structure2DRenderConfig config) {
    if (config.mode() != Sizing.FIXED_SIZES_ALWAYS) {
      config = config.withMode(Sizing.FIXED_SIZES_ALWAYS);
    }
    // width and height not really used here when drawing with fixed size
    return drawStructure(visitor, 1, 1, molecule, config);
  }

  /**
   * Thread safe structure draw
   *
   * @param visitor
   * @param width
   * @param height
   * @param molecule to draw
   * @param config
   */
  @Nullable
  public Rectangle drawStructure(AbstractAWTDrawVisitor visitor, int width, int height,
      final IAtomContainer molecule, Structure2DRenderConfig config) {

    if (molecule == null) {
      return null;
    }
    final Rectangle drawArea = new Rectangle(width, height);
    // needs to be synchronized here to avoid concurrent access
    synchronized (this) {
      // this makes the minimum line width smaller and they may disappear if very small
      // but looks a bit clearer
//      final AWTDrawVisitor visitor = AWTDrawVisitor.forVectorGraphics(g2);

      rendererModel.set(BasicSceneGenerator.BondLength.class, config.bondLength());
      setup(molecule, drawArea);

      // zoom important to see structure
      setZoom(config.zoom());

      return switch (config.mode()) {
        case FIT_TO_SIZE -> {
          paint(molecule, visitor, drawArea, true);
          yield drawArea;
        }
        case FIXED_SIZES_IN_BOUNDS, FIXED_SIZES_ALWAYS -> {
          // try paint with fixed size and see how large it is
          Rectangle2D modelBounds = BoundsCalculator.calculateBounds(molecule);
          setupTransformNatural(modelBounds);
          IRenderingElement diagram = generateDiagram(molecule);
          Rectangle diagramSize = convertToDiagramBounds(modelBounds);

          // use boarder margin to start scaling the structures earlier
          double margin = this.rendererModel.get(BasicSceneGenerator.Margin.class) * 2;

          if (config.mode() != Sizing.FIXED_SIZES_ALWAYS && (diagramSize.getWidth() + margin > width
              || diagramSize.getHeight() + margin > height)) {
            // too large - draw with resizing to size
            paint(molecule, visitor, drawArea, true);
            yield drawArea;
          } else {
            // paint with fixed bond length and atom labels because it fits
            // actual bounds are a bit different
            modelBounds = requireNonNullElse(getBounds(diagram), modelBounds);

            if (config.mode() == Sizing.FIXED_SIZES_ALWAYS) {
              // rather put everything so that structure starts in top left corner
              this.setDrawCenter(diagramSize.getWidth() / 2, diagramSize.getHeight() / 2);
              this.setModelCenter(modelBounds.getCenterX(), modelBounds.getCenterY());
            } else {
              // recenter molecule otherwise it is a bit off center
              this.setDrawCenter(drawArea.getCenterX(), drawArea.getCenterY());
              this.setModelCenter(modelBounds.getCenterX(), modelBounds.getCenterY());
            }

            paint(molecule, visitor);
            this.paint(visitor, diagram);
            yield diagramSize.getBounds();
          }
        }
      };
    }
  }

  /**
   * Prepare coordinates in 2D space
   *
   * @param molecule
   */
  public void prepareStructure2D(@Nullable IAtomContainer molecule) {
    if (molecule == null) {
      return;
    }

    // Suppress the hydrogens
    AtomContainerManipulator.suppressHydrogens(molecule);

    // GeometryUtil.has2DCoordinates actually seems like some molecules say they have coordinates
    // but they are all 0 painting all bounds with 0 length and labels on top of the center
    if (!has2DCoordinates(molecule)) {
      try {
        // If the model has no coordinates, let's generate them
        StructureDiagramGenerator sdg = new StructureDiagramGenerator();
        sdg.setMolecule(molecule, false);
        sdg.generateCoordinates();
      } catch (CDKException e) {
        logger.log(Level.WARNING, "Failed to generate structure diagram", e);
      }
    }
  }

  /**
   * GeometryUtil.has2DCoordinates  does not check if bonds length is 0. This method does
   *
   * @param container
   * @return
   */
  public static boolean has2DCoordinates(IAtomContainer container) {
    if (container != null && container.getAtomCount() != 0) {
      final IAtom first = container.getAtom(0);
      final Point2d firstXY = first.getPoint2d();
      boolean allZero = true;

      for (IAtom atom : container.atoms()) {
        // any not defined
        if (atom == null || atom.getPoint2d() == null) {
          return false;
        }

        //
        if (allZero && Math.abs(firstXY.distance(atom.getPoint2d())) > 0) {
          allZero = false;
        }
      }

      // all zero then bonds length is zero
      return !allZero;
    } else {
      return false;
    }
  }
}
