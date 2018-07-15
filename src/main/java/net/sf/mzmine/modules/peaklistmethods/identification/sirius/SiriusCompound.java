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

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import de.unijena.bioinf.chemdb.DBLink;
import io.github.msdk.datamodel.IonAnnotation;
import io.github.msdk.id.sirius.SiriusIonAnnotation;
import java.util.Hashtable;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class SiriusCompound extends SimplePeakIdentity {
  private final Double compoundScore;
  private final SiriusIonAnnotation annotation;

  public SiriusCompound(final IonAnnotation annotation, Double score) {
    super(loadProps(annotation));
    this.annotation = (SiriusIonAnnotation) annotation;

    this.compoundScore = score;
  }

  public SiriusCompound(final SiriusCompound master) {
    super((Hashtable<String, String>) master.getAllProperties());
    this.annotation = master.annotation;
    compoundScore = master.compoundScore;
  }

  private static Hashtable<String, String> loadProps(final IonAnnotation ann) {
    SiriusIonAnnotation annotation = (SiriusIonAnnotation) ann;
    String formula = MolecularFormulaManipulator.getString(annotation.getFormula());
    String siriusScore = String.format("%.4f", annotation.getSiriusScore());
    String name = null;

    Hashtable<String, String> props = new Hashtable<>(10);
    props.put(PROPERTY_METHOD, "Sirius");
    props.put(PROPERTY_FORMULA, formula);
    props.put("Sirius score", siriusScore);

    if (annotation.getFingerIdScore() != null) { // Execute this code if
      name = annotation.getDescription();
      String inchi = annotation.getInchiKey();
      String smiles = annotation.getSMILES();

      props.put("SMILES", smiles);
      props.put("Inchi", inchi);
      String fingerScore = String.format("%.4f", annotation.getFingerIdScore());
      props.put("FingerId score", fingerScore);

      DBLink[] links = annotation.getDBLinks();
      Hashtable<String, Integer> dbnames = new Hashtable<>();
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

    if (name == null)
      name = formula;
    props.put(PROPERTY_NAME, name);

    return props;
  }

  public SiriusCompound clone() {
    final SiriusCompound compound = new SiriusCompound(this);
    return compound;
  }

  public String getAnnotationDescription() {
    return getPropertyValue(PROPERTY_NAME);
  }

  public String getInchi() {
    return getPropertyValue("Inchi");
  }

  public String getSMILES() {
    return getPropertyValue("SMILES");
  }

  public Object getDBS() {
    StringBuilder b = new StringBuilder();
    DBLink[] dblinks = annotation.getDBLinks();
    if (dblinks != null) {
      for (DBLink link : dblinks)
        b.append(String.format("%s : %s, ", link.name, link.id));
      return b.toString();
    }

    return null;
  }

  public SiriusIonAnnotation getIonAnnotation() {
    return annotation;
  }


  public String getStringFormula() {
    return MolecularFormulaManipulator.getString(annotation.getFormula());
  }

  public Object getFingerIdScore() {
    return getPropertyValue("FingerId score");
  }

  public Object getSiriusScore() {
    return getPropertyValue("Sirius score");
  }
}