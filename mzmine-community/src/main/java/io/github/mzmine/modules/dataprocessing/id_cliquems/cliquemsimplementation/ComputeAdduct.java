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


package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.id_cliquems.CliqueMSTask;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.mutable.MutableDouble;

/**
 * Class to compute top 5 possible adducts from the ionization list
 * <p>
 * See https://github.com/osenan/cliqueMS/blob/master/R/findAnnotation.R for Rcpp code corresponding
 * to this class
 */
public class ComputeAdduct {

  public static final int numofAnnotation = 5;
  private final AnClique anClique;
  private final CliqueMSTask driverTask;
  private final Logger logger = Logger.getLogger(getClass().getName());
  private final MutableDouble progress;

  public ComputeAdduct(AnClique anClique, CliqueMSTask driverTask, MutableDouble progress) {
    this.anClique = anClique;
    this.driverTask = driverTask;
    this.progress = progress;
  }


  /**
   * function to compute ions of polarity type 'polarity'
   *
   * @param polarity polarity positive or negative
   * @return returns IonizationType of polarity type 'polarity'
   */
  private List<IonizationType> partAdInfo(PolarityType polarity) {

    List<IonizationType> returnAdInfo = new ArrayList<>();

    if (polarity.equals(PolarityType.POSITIVE)) {
      // Separate them in adducts with charge >1,
      // adducts with nmol >1 && charge ==1,
      // and adducts with charge and nmol = 1
      List<IonizationType> chargeAd = new ArrayList<>();
      EnumSet.allOf(IonizationType.class)
          .forEach(ion -> {
            if (ion.getCharge() > 1 && ion.getPolarity().equals(polarity)) {
              chargeAd.add(ion);
            }
          });
      Collections.sort(chargeAd, Comparator.comparingDouble(IonizationType::getAddedMass));

      List<IonizationType> numMolAd = new ArrayList<>();
      EnumSet.allOf(IonizationType.class)
          .forEach(ion -> {
            if (ion.getNumMol() > 1 && ion.getPolarity().equals(polarity)) {
              numMolAd.add(ion);
            }
          });
      Collections.sort(numMolAd, Comparator.comparingDouble(IonizationType::getAddedMass));

      List<IonizationType> normalAd = new ArrayList<>();
      EnumSet.allOf(IonizationType.class)
          .forEach(ion -> {
            if (ion.getCharge() == 1 && ion.getNumMol() == 1 && ion.getPolarity()
                .equals(polarity)) {
              normalAd.add(ion);
            }
          });

      // Sort them from adducts with charge >1,
      // normal and adducts with nummol > 1,
      // trying to virtually sort all adducts from smaller massDiff to bigger
      returnAdInfo.addAll(chargeAd);
      returnAdInfo.addAll(normalAd);
      returnAdInfo.addAll(numMolAd);
    } else if (polarity.equals(PolarityType.NEGATIVE)) {
      // Separate them in adducts with charge < -1,
      // adducts with nmol >1 && charge == -1
      // and adducts with charge and nmol = 1

      List<IonizationType> chargeAd = new ArrayList<>();
      EnumSet.allOf(IonizationType.class)
          .forEach(ion -> {
            if (ion.getCharge() < -1 && ion.getPolarity().equals(polarity)) {
              chargeAd.add(ion);
            }
          });
      Collections.sort(chargeAd, Comparator.comparingDouble(IonizationType::getAddedMass));

      List<IonizationType> numMolAd = new ArrayList<>();
      EnumSet.allOf(IonizationType.class)
          .forEach(ion -> {
            if (ion.getNumMol() > 1 && ion.getPolarity().equals(polarity)) {
              numMolAd.add(ion);
            }
          });
      Collections.sort(numMolAd, Comparator.comparingDouble(IonizationType::getAddedMass));

      List<IonizationType> normalAd = new ArrayList<>();
      EnumSet.allOf(IonizationType.class)
          .forEach(ion -> {
            if (ion.getCharge() == -1 && ion.getNumMol() == 1 && ion.getPolarity()
                .equals(polarity)) {
              normalAd.add(ion);
            }
          });

      // Sort them from adducts with charge >1
      // normal and adducts with nummol > 1
      // trying to virtually sort all adducts from smaller massDiff to bigger

      returnAdInfo.addAll(chargeAd);
      returnAdInfo.addAll(normalAd);
      returnAdInfo.addAll(numMolAd);
    } else {
      driverTask.setErrorMessage("Polarity must be \"positive\" or \"negative\" only.");
      driverTask.setStatus(TaskStatus.ERROR);
    }
    return returnAdInfo;
  }


  /**
   * Return adduct annotation information (annotation, mass, score) for all features
   *
   * @param topMassTotal   All neutral masses in the group are ordered based on their adduct
   *                       log-frequencies and their number of adducts. From that list, a number of
   *                       these many masses are considered for the final annotation.
   * @param topMassf       In addition to 'topMassTotal', for each feature the list of ordered
   *                       neutral masses is subsetted to the masses with an adduct in that
   *                       particular feature. For each sublist, these number neutral masses are
   *                       also selected for the final annotation.
   * @param sizeAnG        After neutral mass selection, if a clique group has a number of
   *                       monoisotopic features bigger than this parameter,  the annotation group
   *                       is divided into non-overlapping annotation groups. Each subdivision is
   *                       annotated independently.
   * @param polarity       Adduct polarity
   * @param tol            Tolerance in mass according to which we consider two or more features
   *                       compatible with a neutral mass and two or more adducts from Adduct List
   * @param filter         This parameter removes redundant annotations. If two neutral masses in
   *                       the same annotation group have a relative mass difference smaller than
   *                       this parameter and the same features and adducts, drop the neutral mass
   *                       with less adducts
   * @param emptyS         Score given to non annotated features. The value should not be larger
   *                       than any adduct log frequency (therefore given default value of -6.0)
   * @param normalizeScore If 'TRUE', the reported score is normalized and scaled. Normalized score
   *                       goes from 0, when it means that the raw score is close to the minimum
   *                       score (all features with empty annotations), up to 100, which is the
   *                       score value of the theoretical maximum annotation (all the adducts of the
   *                       list with the minimum number of neutral masses)
   * @return
   */
  public List<AdductInfo> getAnnotation(int topMassTotal, int topMassf, int sizeAnG,
      PolarityType polarity, MZTolerance tol, double filter,
      double emptyS, boolean normalizeScore) {
    if (anClique.anFound) {
      logger.log(Level.WARNING, "Annotation has already been computed for this object.");
    }
    if (!anClique.isoFound) {
      logger.log(Level.WARNING,
          "Isotopes have not been annotated. This could lead to some errors in adduct annotation");
    }
    if (!anClique.cliquesFound) {
      logger.log(Level.WARNING,
          "Cliques have not been computed. This could lead to long computing times for adduct annotation");
    }
    logger.log(Level.FINEST, "Computing annotation.");

    List<AdductInfo> addInfos = new ArrayList<>();
    List<IonizationType> orderAdInfo = partAdInfo(polarity);

    if (driverTask.isCanceled()) {
      return addInfos;
    }

    HashMap<Integer, PeakData> nodeIDtoPeakMap = new HashMap<>();

    for (PeakData pd : this.anClique.getPeakDataList()) {
      nodeIDtoPeakMap.put(pd.getNodeID(), pd);
    }
    int done = 0;
    for (Integer cliqueID : this.anClique.cliques.keySet()) {
      //progress update
      this.progress.setValue(driverTask.EIC_PROGRESS + driverTask.MATRIX_PROGRESS +
          driverTask.NET_PROGRESS + driverTask.ISO_PROGRESS + (
          driverTask.ANNOTATE_PROGRESS * ((double) (done++)) / (this.anClique.cliques.keySet()
              .size())));
      //check if task cancelled
      if (driverTask.isCanceled()) {
        return addInfos;
      }
      List<PeakData> dfClique = new ArrayList<>();
      for (Integer nodeID : this.anClique.cliques.get(cliqueID)) {
        dfClique.add(nodeIDtoPeakMap.get(nodeID));
      }
      dfClique.removeIf(pd -> !pd.getIsotopeAnnotation().startsWith("M0"));
      Collections.sort(dfClique, Comparator
          .comparingDouble(PeakData::getMz));//sorting in order of mz values, in decreasing order

      AdductAnnotationCliqueMS ad = new AdductAnnotationCliqueMS();
      OutputAn outAn = ad
          .returnAdductAnnotation(dfClique, orderAdInfo, topMassf, topMassTotal, sizeAnG, tol,
              filter, emptyS, normalizeScore);

      for (Integer itv : outAn.features) {
        List<String> annotations = new ArrayList<>();
        List<Double> masses = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        for (int x = 0; x < numofAnnotation; x++) {
          annotations.add(outAn.ans.get(x).get(itv));
          masses.add(outAn.masses.get(x).get(itv));
          scores.add(outAn.scores.get(x).get(itv));
        }
        AdductInfo adInfo = new AdductInfo(itv, annotations, masses, scores);
        addInfos.add(adInfo);
      }

    }

    // Isotopes of grade > 0 are excluded from annotation This function adds the annotation, in case
    // it exists, to all isotopes of grade > 0 as they should have the same annotation than its
    // isotope of grade 0
    List<IsotopeInfo> isoInfos = this.anClique.getIsoInfos();
    List<Integer> isoFeatures = new ArrayList<>();
    for (IsotopeInfo isoInfo : isoInfos) {
      if (isoInfo.grade.equals(0)) {
        isoFeatures.add(isoInfo.feature);
      }
    }
    for (Integer feature : isoFeatures) {
      Integer cluster = null;
      for (IsotopeInfo isoInfo : isoInfos) {
        if (isoInfo.feature.equals(feature)) {
          cluster = isoInfo.cluster;
        }
      }
      List<Integer> extraF = new ArrayList<>();
      for (IsotopeInfo isoInfo : isoInfos) {
        if (isoInfo.cluster.equals(cluster)) {
          extraF.add(isoInfo.feature);
        }
      }
      AdductInfo mainAdInfo = null;
      for (AdductInfo adInfo : addInfos) {
        if (adInfo.feature.equals(feature)) {
          mainAdInfo = adInfo;
        }
      }

      for (Integer f : extraF) {
        // if same feature with grade 0, no need to convert it
        if (f.equals(feature)) {
          continue;
        }
        List<String> annotations = new ArrayList<>(mainAdInfo.annotations);
        List<Double> masses = new ArrayList<>(mainAdInfo.masses);
        List<Double> scores = new ArrayList<>(mainAdInfo.scores);
        addInfos.add(new AdductInfo(f, annotations, masses, scores));
      }
    }
    //add data to adinfo class.
    this.anClique.setAdInfos(addInfos);
    return addInfos;
  }
}
