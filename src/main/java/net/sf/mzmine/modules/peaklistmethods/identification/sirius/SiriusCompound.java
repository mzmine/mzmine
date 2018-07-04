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
import java.util.TreeSet;
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
    String siriusScore = String.format(".%2f", annotation.getSiriusScore());

    Hashtable<String, String> props = new Hashtable<>(10);
    props.put("Identification method", "Sirius");
    props.put("Formula", formula);
    props.put("Sirius score", siriusScore);


    if (isProcessedByFingerId(annotation)) { // Execute this code if
      String name = annotation.getDescription();
      String inchi = annotation.getInchiKey();
      String smiles = annotation.getSMILES();

      props.put("Name", name);
      props.put("SMILES", smiles);
      props.put("Inchi", inchi);
      String fingerScore = String.format(".%2f", annotation.getFingerIdScore());
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
    } else
      props.put("Name", formula);

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
    return null; // TODO: make list of dbs somehow
  }

  public String getFormula() {
    return MolecularFormulaManipulator.getString(ion.getFormula());
  }

  public void setOnCol(Object value, int col) {
//    SiriusIonAnnotation siriusAnnotation = (SiriusIonAnnotation) ion;
//    switch (col) {
//      case 0:
//        break;
//      case 1:
//        break;
//      case 2:
//        break;
//      case 3:
//        IAtomContainer container = smp.parseSmiles(smilesString);
//        extendedAnnotation.setChemicalStructure(container);
//        String smiles = (String) value;
//        siriusAnnotation.setSMILES(smiles);
//        break;
//      case 4:
//        String inchi = (String) value;
//        siriusAnnotation.setInchiKey(inchi);
//
//        break;
//      case 5:
//        DBLink[] links = (DBLink[]) value;
//        siriusAnnotation.setDBLinks(links);
//        break;
//    }
  }

  public Object getFingerIdScore() {
    final SiriusIonAnnotation siriusAnnotation = (SiriusIonAnnotation) ion;
    return siriusAnnotation.getFingerIdScore();
  }

  public Object getSiriusScore() {
    final SiriusIonAnnotation siriusAnnotation = (SiriusIonAnnotation) ion;
    return siriusAnnotation.getSiriusScore();
  }
}