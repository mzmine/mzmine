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

import de.unijena.bioinf.chemdb.DBLink;
import io.github.msdk.id.sirius.SiriusIonAnnotation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.swing.ImageIcon;

import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class SiriusCompound
 * May contain different amount of properties
 * 1) if the IonAnnotation is from SiriusIdentificationMethod, then there will be Sirius Score, formula, name == formula
 * 2) if FingerIdWebMethod is used, then name may differ, added SMILES & Inchi and links to DBs
 */
public class SiriusCompound extends SimplePeakIdentity {
  private static final Logger logger = LoggerFactory.getLogger(SiriusCompound.class);
  public static final int PREVIEW_HEIGHT = 100;
  public static final int PREVIEW_WIDTH = 150;

  private final SiriusIonAnnotation annotation;
  private final ImageIcon preview;

  /**
   * Constructor for SiriusCompound
   * @param annotation
   */
  public SiriusCompound(@Nonnull final SiriusIonAnnotation annotation) {
    super(loadProps(annotation));
    this.annotation = annotation;
    this.preview = generateStructurePreview();
  }

  /**
   * Copy constructor
   * @param master - SiriusCompound to copy from
   */
  public SiriusCompound(final SiriusCompound master) {
    super((Hashtable<String, String>) master.getAllProperties());
    this.annotation = master.annotation;
    preview = master.preview;
  }

  /**
   * Construct parameters from SiriusIonAnnotation
   * Amount of params differ, either it is identified by SiriusIdentificationMethod, or also by FingerIdWebMethod
   * @param annotation
   * @return constructed Hashtable
   */
  private static Hashtable<String, String> loadProps(final SiriusIonAnnotation annotation) {
    String formula = MolecularFormulaManipulator.getString(annotation.getFormula());
    String siriusScore = String.format("%.4f", annotation.getSiriusScore());
    String name = null;

    /* Put default properties */
    Hashtable<String, String> props = new Hashtable<>(10);
    props.put(PROPERTY_METHOD, "Sirius");
    props.put(PROPERTY_FORMULA, formula);
    props.put("Sirius score", siriusScore);

    /* Check that annotation is processed by FingerIdWebMethod */
    if (annotation.getFingerIdScore() != null) {
      name = annotation.getDescription();
      String inchi = annotation.getInchiKey();
      String smiles = annotation.getSMILES();

      props.put("SMILES", smiles);
      props.put("Inchi", inchi);
      String fingerScore = String.format("%.4f", annotation.getFingerIdScore());
      props.put("FingerId score", fingerScore);

      DBLink[] links = annotation.getDBLinks();
      Hashtable<String, Integer> dbnames = new Hashtable<>();

      /*
        DBLinks may contain several links to Pubchem (for example)
        And to store them, a trick with <s> #<d> is used, where <d> is amount of times this DB (<s>) has been met.
      */
      for (DBLink link : links) {
        /* Map is used to count indexes of repeating elements */
        if (dbnames.containsKey(link.name)) {
          int amount = dbnames.get(link.name);
          dbnames.put(link.name, ++amount);
        } else {
          dbnames.put(link.name, 1);
        }

        String dbname = String.format("%s #%d", link.name, dbnames.get(link.name));
        props.put(dbname, link.id);
      }
    }

    // Load name param with formula, if FingerId did not identify it
    if (name == null)
      name = formula;
    props.put(PROPERTY_NAME, name);

    return props;
  }

  /**
   * @return cloned object
   */
  public SiriusCompound clone() {
    final SiriusCompound compound = new SiriusCompound(this);
    return compound;
  }

  /**
   * @return description of SiriusIonAnnotation, usually it contains name of the identified compound
   */
  public String getAnnotationDescription() {
    return annotation.getDescription();
  }

  /**
   * @return Inchi string, if exists
   */
  public String getInchi() {
    return getPropertyValue("Inchi");
  }

  /**
   * @return SMILES string, if exists
   */
  public String getSMILES() {
    return getPropertyValue("SMILES");
  }

  /**
   * Render list of dbs in readable form
   * @return one String (rows in form of DB names : db IDs)
   */
  public String[] getDBS() {
    DBLink[] dblinks = getIonAnnotation().getDBLinks();
    if (dblinks == null)
      return null;

    Set<String> dbNames = new TreeSet<String>();

    for (DBLink link: dblinks)
      dbNames.add(link.name);

    String[] dbs = new String[dbNames.size()];
    dbs = dbNames.toArray(dbs);

    return dbs;
  }

  /**
   * Method generates preview image of the sirius compound
   * @return scaled image
   */
  private ImageIcon generateStructurePreview() {
    Image image = generateImage(300, 200);
    if (image == null)
      return null;

    image = image.getScaledInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, Image.SCALE_DEFAULT);
    ImageIcon ic = new ImageIcon(image);
    return ic;
  }

  /**
   * Getter for preview ImageIcon object
   * @return shortcut ImageIcon object
   */
  public ImageIcon getPreview() {
    return preview;
  }

  public Image generateStructureImage(int width, int height) {
    return generateImage(width, height);
  }

  /**
   * Method returns image generated from Chemical Structure IAtomContainer
   * Better to use 3:2 relation of width:height
   * @param width of the image
   * @param height of the image
   * @return new Image object
   */
  private Image generateImage(int width, int height) {
    IAtomContainer molecule = this.annotation.getChemicalStructure();
    if (molecule == null)
      return null;

    /* Form an area for future image */
    Rectangle drawArea = new Rectangle(width, height);
    Image image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    /* Transform AtomContainer into image */
    StructureDiagramGenerator sdg = new StructureDiagramGenerator();
    try {
      sdg.setMolecule(molecule, false);
      sdg.generateCoordinates();
      List generators = new ArrayList();
      generators.add(new BasicSceneGenerator());
      generators.add(new BasicBondGenerator());
      generators.add(new BasicAtomGenerator());
      // the renderer needs to have a toolkit-specific font manager
      AtomContainerRenderer renderer = new AtomContainerRenderer(generators, new AWTFontManager());
      renderer.setup(molecule, drawArea);

      Graphics2D g2 = (Graphics2D) image.getGraphics();
      g2.setColor(Color.WHITE);
      g2.fillRect(0, 0, width, height);
      renderer.paint(molecule, new AWTDrawVisitor(g2));
      return image;
    } catch (Exception ex) {
      logger.info("Exception during ImageIcon construction occured");
    }
    return null;
  }

  /**
   * @return SiriusIonAnnotation object
   */
  public SiriusIonAnnotation getIonAnnotation() {
    return annotation;
  }

  /**
   * @return molecular formula in form of string
   */
  public String getStringFormula() {
    return MolecularFormulaManipulator.getString(annotation.getFormula());
  }

  /**
   * FingerId score had negative value, the closer it is to 0, the better result is (Ex.: -115.23)
   * @return FingerId score, if exists
   */
  public String getFingerIdScore() {
    String val = getPropertyValue("FingerId score");
    if (val == null)
      return "";
    return val;
  }

  /**
   * @return Sirius score
   */
  public String getSiriusScore() {
    return getPropertyValue("Sirius score");
  }
}