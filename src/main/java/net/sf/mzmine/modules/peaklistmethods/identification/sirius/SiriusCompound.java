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
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class SiriusCompound extends SimplePeakIdentity {
  private Double compoundScore;
  private IonAnnotation ion;

  public SiriusCompound(IonAnnotation ion, Double score) {
    super(loadProps(ion));

    this.ion = ion;
    this.compoundScore = score;
  }

  private static Hashtable<String, String> loadProps(IonAnnotation ion) {
    SiriusIonAnnotation annotation = (SiriusIonAnnotation) ion;
    String formula = MolecularFormulaManipulator.getString(annotation.getFormula());
    String siriusScore = String.format("%.4f", annotation.getSiriusScore());
    String name = null;

    Hashtable<String, String> props = new Hashtable<>(10);
    props.put("Identification method", "Sirius");
    props.put("Formula", formula);
    props.put("Sirius score", siriusScore);


    if (isProcessedByFingerId(annotation)) { // Execute this code if
      name = annotation.getDescription();
      String inchi = annotation.getInchiKey();
      String smiles = annotation.getSMILES();

      props.put("SMILES", smiles);
      props.put("Inchi", inchi);
      String fingerScore = String.format("%.4f", annotation.getFingerIdScore());
      props.put("FingerId score", fingerScore);

      DBLink[] links = annotation.getDBLinks(); //todo: here can be again same PubChems, make it Pubchem #1, Pubchem #2
      Hashtable<String, Integer> dbnames = new Hashtable<>();
      for (DBLink link : links) {
        /* Map is used to count indexes of repeating elements */
        if (dbnames.contains(link.name)) {
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
    props.put("Name", name);

    return props;
  }

  private static boolean isProcessedByFingerId(SiriusIonAnnotation annotation) {
    return annotation.getFingerIdScore() != null;
  }

  public SiriusCompound clone() {
    final SiriusCompound compound = new SiriusCompound(this.ion, this.compoundScore);
    return compound;
  }

  public String getAnnotationDescription() {
    return ion.getDescription();
  }

  public String getInchi() {
    final SiriusIonAnnotation siriusAnnotation = (SiriusIonAnnotation) ion;
    return siriusAnnotation.getInchiKey();
  }

  public String getSMILES() {
    final SiriusIonAnnotation siriusAnnotation = (SiriusIonAnnotation) ion;
    return siriusAnnotation.getSMILES();
  }

  public Object getDBS() {
    StringBuilder b = new StringBuilder();
    SiriusIonAnnotation siriusAnnotation = (SiriusIonAnnotation) ion;
    for (DBLink link: siriusAnnotation.getDBLinks())
      b.append(String.format("%s : %s\n", link.name, link.id));
    return b.toString();
  }

  public String getFormula() {
    return MolecularFormulaManipulator.getString(ion.getFormula());
  }

  public Object getFingerIdScore() {
    final SiriusIonAnnotation siriusAnnotation = (SiriusIonAnnotation) ion;
    return String.format("%.5f", siriusAnnotation.getFingerIdScore());
  }

  public Object getSiriusScore() {
    final SiriusIonAnnotation siriusAnnotation = (SiriusIonAnnotation) ion;
    return String.format("%.5f",siriusAnnotation.getSiriusScore());
  }
}