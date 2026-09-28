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

package io.github.mzmine.datamodel.structures;

import io.github.mzmine.datamodel.structures.StructureUtils.SmilesFlavor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.Aromaticity.Model;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.isomorphism.Transform;
import org.openscience.cdk.smirks.Smirks;
import org.openscience.cdk.smirks.SmirksOption;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * Brings structures from different sources (SMILES, InChI, ...) into one comparable representation
 * suited for mass spectrometry.
 * <p>
 * Historically mzmine harmonized SMILES by converting them to a standard InChI and parsing that
 * back, so both sources would end up in the tautomer form that InChI picks. That round trip has
 * three problems this class avoids: standard InChI disconnects metals, so heme, chlorophyll and
 * cobalamin lose their central ion; the mobile hydrogen position InChI picks is a canonical
 * labelling artifact rather than a chemically preferred form, so every amide bond of a peptide
 * comes back as an imidic acid; and it fails outright for pseudo atoms and above 1024 atoms.
 * <p>
 * A standard InChIKey already collapses mobile hydrogen tautomers on its own, so tautomer
 * insensitive matching does not need that round trip. This class therefore only works on the
 * connection table and leaves identity to {@link StructureUtils#getInchiStructure}.
 * <p>
 * All operations happen in place on the given container. Callers that need to keep the input have
 * to clone it first.
 */
public final class StructureHarmonizer {

  private static final Logger logger = Logger.getLogger(StructureHarmonizer.class.getName());

  /**
   * Counter ions and solvates are small. Fragments above this many heavy atoms are never looked up
   * in {@link CounterIons}, which keeps SMILES generation out of the common path.
   */
  private static final int MAX_COUNTER_ION_HEAVY_ATOMS = 14;

  /**
   * Atoms that can take an extra proton when a negative charge is neutralized. Carbanions and
   * halide anions are deliberately absent: adding a hydrogen there would change the species rather
   * than its protonation state.
   */
  private static final int[] PROTONATABLE_ELEMENTS = {7, 8, 15, 16, 34};

  /**
   * decision: RECOMPUTE_HYDROGENS lets the rules be written without explicit hydrogen counts. CDK
   * otherwise carries the hydrogen count of the matched atom over to the product, which silently
   * turns {@code [O-]>>[OH]} into {@code [OH-]} - right charge, wrong mass.
   */
  private static final EnumSet<SmirksOption> SMIRKS_OPTIONS = EnumSet.of(
      SmirksOption.RECOMPUTE_HYDROGENS);

  /**
   * Representation rules applied to every structure. None of them changes the molecular formula,
   * only how a group is drawn, so that a structure coming from SMILES and the same structure coming
   * from InChI end up identical.
   * <p>
   * assumption: a compiled {@link Transform} is immutable and can be shared between threads. Its
   * plan holds only final fields and {@code apply} keeps all mutable state local.
   */
  private static final List<Transform> NORMALIZATION_RULES = List.of( //
      // pentavalent nitro to charge separated, which is what the Daylight valence model expects.
      // InChI hands back the pentavalent form.
      compile("[*:1][N;X3;v5:2](=[O:3])=[O;X1:4]>>[*:1][N+1:2](=[O:3])[O-1:4]"),
      // pentavalent amine oxide to charge separated
      compile("[#6:1][N;X4;v5:2]=[O;X1:3]>>[#6:1][N+1:2][O-1:3]"),
      // imidic acid back to the amide tautomer. This rule makes InChI sourced amides and peptides
      // match their SMILES counterparts, since InChI places the mobile hydrogen on oxygen.
      // Separate rules for secondary and primary so nitrogen substitution stays explicit.
      compile("[O;X2;H1:1][C:2]=[N;X2;H0:3]>>[O:1]=[C:2][N:3]"),
      compile("[O;X2;H1:1][C:2]=[N;X2;H1:3]>>[O:1]=[C:2][N:3]"));

  /**
   * Most heavy atoms first, then heaviest, then lowest canonical SMILES so the result is stable for
   * fragments that are otherwise indistinguishable.
   */
  private static final Comparator<IAtomContainer> FRAGMENT_ORDER = Comparator //
      .comparingInt((IAtomContainer fragment) -> CounterIons.heavyAtomCount(fragment)).reversed() //
      .thenComparing(Comparator.comparingDouble(
          (IAtomContainer fragment) -> StructureUtils.getMonoIsotopicMass(fragment)).reversed()) //
      .thenComparing((IAtomContainer fragment) -> {
        final String smiles = StructureUtils.getSmiles(StructureUtils.SmilesFlavor.CANONICAL,
            fragment);
        return smiles == null ? "" : smiles;
      });

  private StructureHarmonizer() {
  }

  @NotNull
  private static Transform compile(@NotNull final String smirks) {
    final Transform transform = new Transform();
    // note: parse() returns false only on error. A non null message() can also be a warning and the
    // imidic rule legitimately warns about a valence change, so the boolean is the only check.
    if (!Smirks.parse(transform, smirks, SMIRKS_OPTIONS)) {
      throw new IllegalStateException(
          "Cannot compile structure harmonization rule %s: %s".formatted(smirks,
              transform.message()));
    }
    return transform;
  }

  /**
   * Harmonize with {@link HarmonizationOptions#DEFAULT}.
   *
   * @param mol modified in place, may be replaced by one of its fragments
   * @return the harmonized structure, which may be a different instance than the input
   */
  @NotNull
  public static IAtomContainer harmonize(@NotNull final IAtomContainer mol) {
    return harmonize(mol, HarmonizationOptions.DEFAULT);
  }

  /**
   * @param mol     modified in place, may be replaced by one of its fragments
   * @param options what to apply
   * @return the harmonized structure, which may be a different instance than the input
   */
  @NotNull
  public static IAtomContainer harmonize(@NotNull IAtomContainer mol,
      @NotNull final HarmonizationOptions options) {
    // implicit hydrogens keep the connection table small and keep generated SMILES free of
    // explicit [H]. Has to happen before the rules so hydrogen counts are comparable. Query
    // structures keep their explicit hydrogens because OH asks for more than O does.
    if (options.suppressHydrogens()) {
      suppressHydrogens(mol);
    }
    perceiveRingsAndAromaticity(mol);

    if (options.normalizeFunctionalGroups()) {
      for (final Transform rule : NORMALIZATION_RULES) {
        rule.apply(mol);
      }
    }

    final boolean cutMetalBonds = disconnectSaltLikeMetals(mol, options.metalPolicy());

    mol = switch (options.fragmentPolicy()) {
      case MAJOR_FRAGMENT -> selectMajorFragment(mol);
      case KEEP_ALL -> mol;
    };

    final boolean neutralized = options.neutralizeCharges() && neutralizeCharges(mol);

    if (cutMetalBonds || neutralized) {
      // bonds and charges changed, so ring and aromaticity flags may be stale
      perceiveRingsAndAromaticity(mol);
    }
    return mol;
  }

  private static void suppressHydrogens(@NotNull final IAtomContainer mol) {
    try {
      AtomContainerManipulator.suppressHydrogens(mol);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to suppress hydrogens: " + e.getMessage(), e);
    }
  }

  private static void perceiveRingsAndAromaticity(@NotNull final IAtomContainer mol) {
    try {
      Cycles.markRingAtomsAndBonds(mol);
      Aromaticity.apply(Model.Daylight, mol);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to perceive rings and aromaticity: " + e.getMessage(), e);
    }
  }

  // ---------------------------------------------------------------- metals

  /**
   * Cuts metal-ligand bonds that describe a salt rather than a coordination complex. Databases
   * often write salts without the fragment separator, for example {@code C(N[Na])=O} instead of
   * {@code C(N)=O.[Na+]}, and without this step the sodium would count as part of the analyte.
   *
   * @return true if any bond was removed
   */
  private static boolean disconnectSaltLikeMetals(@NotNull final IAtomContainer mol,
      @NotNull final MetalPolicy policy) {
    if (policy == MetalPolicy.KEEP_ALL) {
      return false;
    }
    // collect first, removing while iterating the bonds would invalidate the iterator
    final List<IBond> toRemove = new ArrayList<>(2);
    for (final IBond bond : mol.bonds()) {
      if (bond.getAtomCount() != 2) {
        continue;
      }
      final IAtom metal = pickMetal(bond.getBegin(), bond.getEnd());
      if (metal == null) {
        continue;
      }
      final IAtom ligand = metal == bond.getBegin() ? bond.getEnd() : bond.getBegin();
      // never cut metal-carbon bonds, those are organometallics like methylmercury
      if (isElement(ligand, 6)) {
        continue;
      }
      final boolean cut = switch (policy) {
        case DISCONNECT_ALL -> true;
        case KEEP_CENTRAL_IONS -> isSaltLikeMetal(metal, mol);
        case KEEP_ALL -> false;
      };
      if (cut) {
        toRemove.add(bond);
      }
    }

    for (final IBond bond : toRemove) {
      final IAtom metal = pickMetal(bond.getBegin(), bond.getEnd());
      if (metal == null) {
        continue;
      }
      final IAtom ligand = metal == bond.getBegin() ? bond.getEnd() : bond.getBegin();
      final int order = bond.getOrder() == null ? 1 : bond.getOrder().numeric();
      // decision: only move charge when both atoms are neutral. A bond already drawn between
      // charged atoms (CC(=O)[O-][Na+]) is malformed input where the arithmetic would produce
      // nonsense charges, so there the bond is only cut.
      if (charge(metal) == 0 && charge(ligand) == 0) {
        metal.setFormalCharge(order);
        ligand.setFormalCharge(-order);
      }
      mol.removeBond(bond);
    }
    return !toRemove.isEmpty();
  }

  /**
   * A metal bond is salt like when the metal is not held by a ring, is not organometallic, and is
   * either an alkali/alkaline earth metal or bound by a single hetero atom donor. Iron in heme,
   * magnesium in chlorophyll and cobalt in cobalamin sit in a macrocycle and are therefore kept,
   * while sodium, potassium and calcium carboxylates are cut.
   */
  static boolean isSaltLikeMetal(@NotNull final IAtom metal, @NotNull final IAtomContainer mol) {
    if (metal.isInRing()) {
      return false; // chelated central ion
    }
    int donors = 0;
    for (final IAtom neighbour : mol.getConnectedAtomsList(metal)) {
      if (isElement(neighbour, 6)) {
        return false; // organometallic, keep intact
      }
      donors++;
    }
    if (donors == 0) {
      return false; // bare ion, nothing to cut
    }
    if (isAlkaliOrAlkalineEarth(metal)) {
      return true;
    }
    // a transition or post transition metal held by two or more donors is treated as a complex
    return donors < 2;
  }

  @Nullable
  private static IAtom pickMetal(@NotNull final IAtom a, @NotNull final IAtom b) {
    final boolean metalA = isMetal(a);
    final boolean metalB = isMetal(b);
    if (metalA == metalB) {
      return null; // neither, or a metal-metal bond which is left alone
    }
    return metalA ? a : b;
  }

  // ---------------------------------------------------------------- fragments

  /**
   * Splits at all bonds that are absent and keeps the component with the most heavy atoms. Known
   * counter ions and solvates sort out first so they cannot win against a small analyte, unless
   * every component is a counter ion, in which case size decides again.
   */
  @NotNull
  private static IAtomContainer selectMajorFragment(@NotNull final IAtomContainer mol) {
    if (ConnectivityChecker.isConnected(mol)) {
      return mol;
    }
    final IAtomContainerSet parts = ConnectivityChecker.partitionIntoMolecules(mol);
    if (parts.getAtomContainerCount() < 2) {
      return mol;
    }
    // decision: canonical SMILES are only generated for fragments small enough to be a counter
    // ion. A large analyte wins on size alone and then costs no SMILES generation at all.
    final List<IAtomContainer> allParts = new ArrayList<>(parts.getAtomContainerCount());
    final List<IAtomContainer> analytes = new ArrayList<>(parts.getAtomContainerCount());
    for (final IAtomContainer fragment : parts.atomContainers()) {
      allParts.add(fragment);
      final int heavyAtomCount = CounterIons.heavyAtomCount(fragment);
      final String smiles =
          heavyAtomCount <= MAX_COUNTER_ION_HEAVY_ATOMS ? StructureUtils.getSmiles(
              SmilesFlavor.CANONICAL, fragment) : null;
      if (!CounterIons.isCounterIon(fragment, smiles)) {
        analytes.add(fragment);
      }
    }
    // everything looked like a counter ion, so fall back to plain size over all fragments
    final List<IAtomContainer> candidates = analytes.isEmpty() ? allParts : analytes;
    candidates.sort(FRAGMENT_ORDER);
    return candidates.getFirst();
  }

  // ---------------------------------------------------------------- charges

  /**
   * Protonates anions and deprotonates cations so the neutral molecule is described, which is what
   * {@link MolecularStructure#monoIsotopicMass()} has to report for adduct and m/z calculations.
   * <p>
   * Charges that cannot be moved are preserved and, importantly, keep their counter charge: betaine
   * stays a zwitterion because its quaternary nitrogen cannot lose a hydrogen, and an ion pair like
   * {@code CC(=O)[O-].[Na+]} stays an ion pair when fragments are kept.
   *
   * @return true if any charge was changed
   */
  private static boolean neutralizeCharges(@NotNull final IAtomContainer mol) {
    final List<IAtom> anions = new ArrayList<>(2);
    final List<IAtom> cations = new ArrayList<>(2);
    int permanentPositive = 0;
    int permanentNegative = 0;

    for (final IAtom atom : mol.atoms()) {
      final int q = charge(atom);
      if (q == 0) {
        continue;
      }
      // a charged neighbour means a charge separated group such as nitro, azide, diazonium or an
      // N-oxide. Those charges belong to the drawing convention and have to stay.
      if (hasChargedNeighbour(mol, atom)) {
        continue;
      }
      if (q < 0) {
        if (isProtonatable(atom)) {
          anions.add(atom);
        } else {
          permanentNegative -= q;
        }
      } else if (implicitHydrogens(atom) > 0) {
        cations.add(atom);
      } else {
        permanentPositive += q; // quaternary nitrogen, bare metal cation
      }
    }

    // only neutralize the excess, what is left has to balance the charges that cannot be moved
    int protonBudget = anions.stream().mapToInt(atom -> -charge(atom)).sum() - permanentPositive;
    int deprotonBudget =
        cations.stream().mapToInt(StructureHarmonizer::charge).sum() - permanentNegative;
    boolean changed = false;

    for (final IAtom atom : anions) {
      if (protonBudget <= 0) {
        break;
      }
      final int add = Math.min(-charge(atom), protonBudget);
      atom.setImplicitHydrogenCount(implicitHydrogens(atom) + add);
      atom.setFormalCharge(charge(atom) + add);
      protonBudget -= add;
      changed = true;
    }
    for (final IAtom atom : cations) {
      if (deprotonBudget <= 0) {
        break;
      }
      final int remove = Math.min(Math.min(charge(atom), implicitHydrogens(atom)), deprotonBudget);
      if (remove == 0) {
        continue;
      }
      atom.setImplicitHydrogenCount(implicitHydrogens(atom) - remove);
      atom.setFormalCharge(charge(atom) - remove);
      deprotonBudget -= remove;
      changed = true;
    }
    return changed;
  }

  private static boolean hasChargedNeighbour(@NotNull final IAtomContainer mol,
      @NotNull final IAtom atom) {
    for (final IAtom neighbour : mol.getConnectedAtomsList(atom)) {
      if (charge(neighbour) != 0) {
        return true;
      }
    }
    return false;
  }

  // ---------------------------------------------------------------- element helpers

  private static int charge(@NotNull final IAtom atom) {
    final Integer formalCharge = atom.getFormalCharge();
    return formalCharge == null ? 0 : formalCharge;
  }

  private static int implicitHydrogens(@NotNull final IAtom atom) {
    final Integer count = atom.getImplicitHydrogenCount();
    return count == null ? 0 : count;
  }

  private static boolean isElement(@NotNull final IAtom atom, final int atomicNumber) {
    final Integer z = atom.getAtomicNumber();
    return z != null && z == atomicNumber;
  }

  private static boolean isProtonatable(@NotNull final IAtom atom) {
    final Integer z = atom.getAtomicNumber();
    if (z == null) {
      return false;
    }
    for (final int element : PROTONATABLE_ELEMENTS) {
      if (z == element) {
        return true;
      }
    }
    return false;
  }

  static boolean isAlkaliOrAlkalineEarth(@NotNull final IAtom atom) {
    final Integer z = atom.getAtomicNumber();
    if (z == null) {
      return false;
    }
    return switch (z) {
      case 3, 11, 19, 37, 55, 87 -> true; // Li Na K Rb Cs Fr
      case 4, 12, 20, 38, 56, 88 -> true; // Be Mg Ca Sr Ba Ra
      default -> false;
    };
  }

  /**
   * Metals for the purpose of salt splitting. Metalloids such as boron, silicon, arsenic, antimony
   * and tellurium are excluded because they form covalent bonds.
   */
  static boolean isMetal(@NotNull final IAtom atom) {
    final Integer z = atom.getAtomicNumber();
    if (z == null) {
      return false;
    }
    if (isAlkaliOrAlkalineEarth(atom)) {
      return true;
    }
    return (z >= 21 && z <= 30) // Sc..Zn
        || (z >= 39 && z <= 48) // Y..Cd
        || (z >= 57 && z <= 80) // La..Hg including lanthanides
        || (z >= 89 && z <= 112) // Ac..Cn including actinides
        || z == 13 || z == 31 || z == 49 || z == 50 || z == 81 || z == 82
        || z == 83; // Al Ga In Sn Tl Pb Bi
  }
}
