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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.smarts.SmartsPattern;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * Parsing of inchi and smiles structures harmonized
 */
public class StructureParser {

  private static final Logger logger = Logger.getLogger(StructureParser.class.getName());

  public static final Pattern SMILES_SPECIAL_CHARS = Pattern.compile("[\\[\\]()=#\\-./:\\\\@+%*]");

  // decision: two-tier cache. RAW_CACHE stores the original (un-harmonized) input strings the
  // caller passed in — these may repeat exactly across imports but are not reused for downstream
  // computations, so a smaller bound is fine. CLEAN_CACHE stores the canonical/isomeric SMILES,
  // standard InChI and InChIKey derived after a successful parse; downstream code uses these
  // strings repeatedly (lookups, joins, comparisons), so they get a much larger bound. Multiple
  // clean keys point to the SAME MolecularStructure instance — instance identity is intentional.
  // assumption: callers treat MolecularStructure as immutable. Mutating a returned structure
  // will corrupt cached entries for other callers.
  private static final Cache<String, MolecularStructure> RAW_CACHE = Caffeine.newBuilder()
      .maximumSize(20_000).recordStats().build();
  private static final Cache<String, MolecularStructure> CLEAN_CACHE = Caffeine.newBuilder()
      .maximumSize(200_000).recordStats().build();

  private final InChIGeneratorFactory inchiFactory;
  private final boolean verbose;
  private final SmilesParser smilesParser;

  private static final StructureParser SILENT_INSTANCE = new StructureParser(false);


  public StructureParser() {
    this(false);
  }

  public StructureParser(boolean verbose) {
    this.verbose = verbose;
    // Parse the SMILES and create an IAtomContainer
    IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();
    this.smilesParser = new SmilesParser(builder);
    InChIGeneratorFactory inchiFactory = null;
    try {
      inchiFactory = InChIGeneratorFactory.getInstance();
    } catch (Exception e) {
      logger.warning(
          "Failed to load InChI generator factory in structure parser. SMILES will work");
    }
    this.inchiFactory = inchiFactory;
  }

  /**
   * Default silent instance of structure parser
   */
  public static StructureParser silent() {
    return SILENT_INSTANCE;
  }

  /**
   * Snapshot of cumulative raw-input cache statistics since JVM start. The raw cache is keyed by
   * the original (un-harmonized) smiles/inchi strings the caller passed in.
   */
  @NotNull
  public static CacheStats getRawCacheStats() {
    return RAW_CACHE.stats();
  }

  /**
   * Snapshot of cumulative clean-form cache statistics since JVM start. The clean cache is keyed by
   * canonical SMILES, isomeric SMILES, standard InChI and InChIKey derived after a successful
   * parse. Multiple keys can point to the same {@link MolecularStructure} instance.
   */
  @NotNull
  public static CacheStats getCleanCacheStats() {
    return CLEAN_CACHE.stats();
  }

  /**
   * @return current number of entries in the raw-input cache (best-effort under concurrent access).
   */
  public static long getRawCacheSize() {
    return RAW_CACHE.estimatedSize();
  }

  /**
   * @return current number of entries in the clean-form cache (best-effort under concurrent
   * access). Note: this counts cache keys, not distinct structures — each structure contributes up
   * to four keys.
   */
  public static long getCleanCacheSize() {
    return CLEAN_CACHE.estimatedSize();
  }

  /**
   *
   * @return true if any special char matches
   */
  public static boolean containsSmilesSpecialChars(@NotNull String input) {
    return SMILES_SPECIAL_CHARS.matcher(input).find();
  }

  @Nullable
  public MolecularStructure parseStructure(@Nullable String structure,
      @NotNull StructureInputType inputType) {
    if (structure == null || structure.isBlank() || structure.equalsIgnoreCase("n/a")
        || structure.equalsIgnoreCase("na")) {
      return null;
    }
    try {
      IAtomContainer molecule = switch (inputType) {
        case SMILES -> smilesParser.parseSmiles(structure);
        case INCHI ->
            inchiFactory.getInChIToStructure(structure, DefaultChemObjectBuilder.getInstance())
                .getAtomContainer();
      };

      return molecule == null || molecule.getAtomCount() == 0 ? null
          : new SimpleMolecularStructure(molecule);
    } catch (CDKException e) {
      String message = "Cannot parse 'structure' %s as %s".formatted(structure, inputType);
      if (verbose) {
        logger.log(Level.WARNING, message, e);
      } else {
        logger.log(Level.WARNING, message);
      }
      return null;
    }
  }

  @Nullable
  public SmartsMolecularStructure parseSmarts(@Nullable String smarts) {
    if (smarts == null || smarts.isBlank() || smarts.equalsIgnoreCase("n/a")
        || smarts.equalsIgnoreCase("na")) {
      return null;
    }
    try {
      final SmartsPattern smartsPattern = SmartsPattern.create(smarts,
          DefaultChemObjectBuilder.getInstance());
      smartsPattern.setPrepare(true);
      return new SmartsMolecularStructure(smartsPattern, smarts);
    } catch (Exception e) {
      String message = "Cannot parse 'smarts' %s as SMARTS".formatted(smarts);
      if (verbose) {
        logger.log(Level.WARNING, message, e);
      } else {
        logger.log(Level.WARNING, message);
      }
    }
    return null;
  }


  public InChIGeneratorFactory getInchiFactory() {
    return inchiFactory;
  }

  public boolean isVerbose() {
    return verbose;
  }

  @Nullable
  public MolecularStructure parseStructure(@Nullable String smiles, @Nullable String inchi) {
    // Cache lookup — raw first (smaller, more likely to hit on repeated identical inputs),
    // then clean (hits when the caller already passes a canonical form, e.g. an inchikey).
    MolecularStructure cached = lookupCaches(smiles);
    if (cached != null) {
      return cached;
    }
    cached = lookupCaches(inchi);
    if (cached != null) {
      return cached;
    }

    // Miss everywhere — parse. Try smiles first, fall back to inchi.
    MolecularStructure mol = parseStructure(smiles, StructureInputType.SMILES);
    if (mol == null) {
      mol = parseStructure(inchi, StructureInputType.INCHI);
    }
    if (mol == null) {
      // do not cache failed/blank parses
      return null;
    }

    // Suppress the hydrogens once before generating clean forms / caching
    try {
      AtomContainerManipulator.suppressHydrogens(mol.structure());
    } catch (Exception exception) {
      logger.log(Level.WARNING, "Failed to suppress hydrogens for smiles %s   inchi %s".formatted(
          smiles != null ? smiles : "", inchi != null ? inchi : ""), exception.getMessage());
    }

    // Populate CLEAN_CACHE with all derivable clean keys → same MolecularStructure instance.
    // Each generator can fail independently; cache whatever succeeds.
    final java.util.HashSet<String> cleanKeys = new java.util.HashSet<>(4);
    putClean(cleanKeys, mol,
        StructureUtils.getSmiles(StructureUtils.SmilesFlavor.CANONICAL, mol.structure()));
    putClean(cleanKeys, mol,
        StructureUtils.getSmiles(StructureUtils.SmilesFlavor.ISOMERIC, mol.structure()));
    final InchiStructure inchiStruct = StructureUtils.getInchiStructure(mol.structure());
    if (inchiStruct != null) {
      putClean(cleanKeys, mol, inchiStruct.inchi());
      putClean(cleanKeys, mol, inchiStruct.inchiKey());
    }

    // Populate RAW_CACHE with the original caller inputs — skip if the input string already
    // appears in CLEAN_CACHE (avoids redundant storage of already-canonical inputs).
    putRaw(cleanKeys, mol, smiles);
    putRaw(cleanKeys, mol, inchi);

    return mol;
  }

  @Nullable
  private static MolecularStructure lookupCaches(@Nullable String input) {
    if (input == null) {
      return null;
    }
    final MolecularStructure raw = RAW_CACHE.getIfPresent(input);
    if (raw != null) {
      return raw;
    }
    return CLEAN_CACHE.getIfPresent(input);
  }

  private static void putClean(@NotNull java.util.Set<String> cleanKeys,
      @NotNull MolecularStructure mol, @Nullable String key) {
    if (key == null || key.isBlank()) {
      return;
    }
    CLEAN_CACHE.put(key, mol);
    cleanKeys.add(key);
  }

  private static void putRaw(@NotNull java.util.Set<String> cleanKeys,
      @NotNull MolecularStructure mol, @Nullable String key) {
    if (key == null || key.isBlank() || cleanKeys.contains(key)) {
      return;
    }
    RAW_CACHE.put(key, mol);
  }
}
