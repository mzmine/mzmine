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

package io.github.mzmine.modules.dataprocessing.id_gc_ei_ion_notation;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.identities.iontype.BuildingIonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionGCModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkingTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Assigns EI ion notations to all grouped rows of GC-EI compounds based on the molecular formula of
 * the compound annotation. The molecular ion [M]+ is the starting point, every other row is
 * explained as [M]+ minus a neutral loss that is a sub formula of M. The molecular ion does not
 * need to be detected, which is common in EI spectra.
 */
public class GcEiIonNotationTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(GcEiIonNotationTask.class.getName());

  private final @NotNull ModularFeatureList flist;
  private final @NotNull MZTolerance mzTolerance;

  protected GcEiIonNotationTask(@Nullable final MemoryMapStorage storage,
      @NotNull final Instant moduleCallDate, @NotNull final ParameterSet parameters,
      @NotNull final Class<? extends MZmineModule> moduleClass,
      @NotNull final FeatureList flist) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.flist = (ModularFeatureList) flist;
    mzTolerance = parameters.getValue(GcEiIonNotationParameters.mzTolerance);
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  protected void process() {
    final CompoundList compoundList = flist.getCompoundList();
    if (compoundList == null || !flist.hasCompoundList()) {
      error("Feature list %s has no compounds. Run %s first.".formatted(flist.getName(),
          SpectralDeconvolutionGCModule.NAME));
      return;
    }
    // EI forms radical cations, the notation from [M]+ is only defined in positive mode
    final PolarityType polarity = FeatureListUtils.getPolarity(flist);
    if (polarity == PolarityType.NEGATIVE) {
      error("GC-EI ion notations are only available for positive mode. Feature list %s is negative mode.".formatted(
          flist.getName()));
      return;
    }

    final List<ModularCompoundRow> compounds = compoundList.getRowsCopy();
    totalItems = compounds.size();
    final List<BuildingIonNetwork> networks = new ArrayList<>();
    int withFormula = 0;
    int annotatedRows = 0;
    int totalRows = 0;
    for (final ModularCompoundRow compound : compounds) {
      if (isCanceled()) {
        return;
      }
      final CompoundResult result = annotateCompound(compound);
      if (result != null) {
        withFormula++;
        totalRows += result.memberRows();
        if (result.network() != null) {
          networks.add(result.network());
          annotatedRows += result.network().getNodes().size();
        }
      }
      finishedItems.incrementAndGet();
    }

    flist.addRowType(DataTypes.get(IonIdentityListType.class));
    IonNetworkingTask.addIonIdentitiesToRows(networks,
        flist.getPreferences().getIonTypeRanking());
    IonNetworkLogic.renumberNetworks(flist);

    final int compoundsWithFormula = withFormula;
    final int explained = annotatedRows;
    final int grouped = totalRows;
    logger.info(() -> """
        GC-EI ion notations in %s: %d of %d compounds had an annotation formula, \
        %d of their %d grouped rows were explained by a sub formula.""".formatted(flist.getName(),
        compoundsWithFormula, compounds.size(), explained, grouped));
  }

  /**
   * @return null if the compound is no positive GC-EI compound or has no annotation formula
   */
  private @Nullable CompoundResult annotateCompound(@NotNull final ModularCompoundRow compound) {
    final FeatureListRow representative = compound.getPreferredRow();
    final Scan spectrum = representative.getMostIntenseFragmentScan();
    if (!ScanUtils.isGcEiScan(spectrum) || spectrum.getPolarity() == PolarityType.NEGATIVE) {
      return null;
    }

    final List<FeatureListRow> members = compound.getMemberRows();
    // remove previous notations so that a rerun reflects the current annotation
    members.forEach(FeatureListRow::clearIonIdentites);

    final FeatureAnnotation annotation = representative.getPreferredAnnotation();
    final String formula = annotation == null ? null : annotation.getFormula();
    if (formula == null || formula.isBlank()) {
      return null;
    }

    final double minMz = members.stream().map(FeatureListRow::getAverageMZ)
        .filter(Objects::nonNull).mapToDouble(Double::doubleValue).min().orElse(0d);
    final EiFragmentIonAnnotator annotator = EiFragmentIonAnnotator.create(formula, mzTolerance,
        minMz);
    if (annotator == null) {
      logger.warning(
          () -> "Cannot calculate GC-EI ion notations for compound %d with formula %s".formatted(
              compound.getCompoundId(), formula));
      return null;
    }

    // the molecular ion is the reference even if it was not detected, which is common in EI
    final BuildingIonNetwork network = new BuildingIonNetwork();
    for (final FeatureListRow member : members) {
      final Double mz = member.getAverageMZ();
      if (mz == null) {
        continue;
      }
      final EiFragmentIon ion = annotator.annotate(mz);
      if (ion != null) {
        network.put(member, new IonIdentity(ion.ionType()));
      }
    }
    compound.set(NeutralMassType.class, annotator.getNeutralMass());
    return new CompoundResult(network.getNodes().isEmpty() ? null : network, members.size());
  }

  @Override
  public String getTaskDescription() {
    return "Calculating GC-EI ion notations in " + flist.getName();
  }

  /**
   * @param network    ion identities of all explained rows, null if no row was explained
   * @param memberRows number of grouped rows of the compound
   */
  private record CompoundResult(@Nullable BuildingIonNetwork network, int memberRows) {

  }
}
