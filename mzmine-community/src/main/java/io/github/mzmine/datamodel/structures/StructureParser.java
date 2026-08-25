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

import static io.github.mzmine.datamodel.structures.StructureInputType.INCHI;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.github.mzmine.datamodel.structures.StructureUtils.SmilesFlavor;
import io.github.mzmine.util.StringUtils;
import java.util.HashSet;
import java.util.Set;
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
      .maximumSize(35_000)
//      .recordStats()
      .build();
  private static final Cache<String, MolecularStructure> CLEAN_CACHE = Caffeine.newBuilder()
      .maximumSize(200_000)
//      .recordStats()
      .build();

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
    // add stats to cache above to see
    return RAW_CACHE.stats();
  }

  /**
   * Snapshot of cumulative clean-form cache statistics since JVM start. The clean cache is keyed by
   * canonical SMILES, isomeric SMILES, standard InChI and InChIKey derived after a successful
   * parse. Multiple keys can point to the same {@link MolecularStructure} instance.
   */
  @NotNull
  public static CacheStats getCleanCacheStats() {
    // add stats to cache above to see
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

  /**
   * Parse with cache activated to cache input and clean smiles inchi to structure.
   *
   * @return the structure or null
   */
  @Nullable
  public MolecularStructure parseStructure(@Nullable String structure,
      @NotNull StructureInputType inputType) {
    return parseStructure(structure, inputType, HarmonizationOptions.DEFAULT);
  }

  /**
   * Parse with cache activated to cache input and clean smiles inchi to structure.
   *
   * @param options harmonization to apply. decision: only {@link HarmonizationOptions#DEFAULT} is
   *                cached. The caches are keyed by input string alone, so serving a differently
   *                harmonized structure from them would hand the caller the wrong molecule.
   * @return the structure or null
   */
  @Nullable
  public MolecularStructure parseStructure(@Nullable String structure,
      @NotNull StructureInputType inputType, @NotNull HarmonizationOptions options) {
    if (StringUtils.isBlank(structure)) {
      return null;
    }
    final boolean cacheable = HarmonizationOptions.DEFAULT.equals(options);
    if (!cacheable) {
      return parseStructureWithoutCache(structure, inputType, options);
    }

    // Cache lookup — raw first (smaller, more likely to hit on repeated identical inputs),
    // then clean (hits when the caller already passes a canonical form, e.g. an inchikey).
    MolecularStructure cached = lookupCaches(structure);
    if (cached != null) {
      return cached;
    }

    final MolecularStructure parsed = parseStructureWithoutCache(structure, inputType, options);
    if (parsed == null) {
      return null;
    }

    // Derive the clean forms once. Each generator can fail independently. Both smiles flavors are
    // needed: SmiFlavor.Stereo also changes the traversal, so canonical and isomeric differ even
    // without stereo chemistry.
    final String canonicalSmiles = StructureUtils.getSmiles(SmilesFlavor.CANONICAL,
        parsed.structure());
    final String isomericSmiles = StructureUtils.getSmiles(SmilesFlavor.ISOMERIC,
        parsed.structure());
    final InchiStructure inchiStruct = StructureUtils.getInchiStructure(parsed.structure());

    // decision: keep the derived values instead of discarding them. Generating them for the cache
    // keys costs roughly 1400 us per structure while an on demand inchiKey() call costs another
    // ~220 us and formulaString() ~16 us. Since a cached structure is handed to many callers,
    // storing what was already paid for makes value access about 8x cheaper at zero extra cost.
    final MolecularStructure mol = new PrecomputedMolecularStructure(parsed.structure(),
        parsed.formula(), canonicalSmiles, isomericSmiles,
        inchiStruct != null ? inchiStruct.inchi() : null,
        inchiStruct != null ? inchiStruct.inchiKey() : null, parsed.monoIsotopicMass(),
        parsed.mostAbundantMass(), parsed.totalFormalCharge());

    // Populate CLEAN_CACHE with all derivable clean keys → same MolecularStructure instance.
    final HashSet<String> cleanKeys = HashSet.newHashSet(4);
    putClean(cleanKeys, mol, canonicalSmiles);
    putClean(cleanKeys, mol, isomericSmiles);
    if (inchiStruct != null) {
      putClean(cleanKeys, mol, inchiStruct.inchi());
      putClean(cleanKeys, mol, inchiStruct.inchiKey());
    }

    // Populate RAW_CACHE with the original caller inputs — skip if the input string already
    // appears in CLEAN_CACHE (avoids redundant storage of already-canonical inputs).
    putRaw(cleanKeys, mol, structure);
    return mol;
  }

  @Nullable
  public MolecularStructure parseStructureWithoutCache(@Nullable String structure,
      @NotNull StructureInputType inputType) {
    return parseStructureWithoutCache(structure, inputType, HarmonizationOptions.DEFAULT);
  }

  /**
   * @param options harmonization to apply, see {@link StructureHarmonizer}
   * @return the structure or null
   */
  @Nullable
  public MolecularStructure parseStructureWithoutCache(@Nullable String structure,
      @NotNull StructureInputType inputType, @NotNull HarmonizationOptions options) {
    if (structure == null || structure.isBlank() || structure.equalsIgnoreCase("n/a")
        || structure.equalsIgnoreCase("na")) {
      return null;
    }
    final IAtomContainer parsed = parseToContainer(structure, inputType);
    if (parsed == null || parsed.getAtomCount() == 0) {
      return null;
    }
    // decision: harmonization: Standard inchi disconnects metals and
    // places mobile hydrogens on oxygen, so heme lost its iron and
    // every amide came back as an imidic acid. Tautomer insensitive matching does not need the
    // round trip because the standard inchi key already collapses mobile hydrogen tautomers.
    final IAtomContainer harmonized = StructureHarmonizer.harmonize(parsed, options);
    if (harmonized.getAtomCount() == 0) {
      return null;
    }
    return new SimpleMolecularStructure(harmonized);
  }

  @Nullable
  private IAtomContainer parseToContainer(@NotNull String structure,
      @NotNull StructureInputType inputType) {
    try {
      return switch (inputType) {
        case SMILES -> smilesParser.parseSmiles(structure);
        case INCHI ->
            inchiFactory.getInChIToStructure(structure, DefaultChemObjectBuilder.getInstance())
                .getAtomContainer();
      };
    } catch (CDKException e) {
      final String message = "Cannot parse 'structure' %s as %s".formatted(structure, inputType);
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
    return parseStructure(smiles, inchi, HarmonizationOptions.DEFAULT);
  }

  /**
   * Parse a structure that is described by both a smiles and an inchi.
   *
   * @param options harmonization to apply, see {@link StructureHarmonizer}
   * @return the structure or null if neither source could be parsed
   */
  @Nullable
  public MolecularStructure parseStructure(@Nullable String smiles, @Nullable String inchi,
      @NotNull HarmonizationOptions options) {
    if (HarmonizationOptions.DEFAULT.equals(options)) {
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
    }

    // decision: smiles is the primary source. inchi source mixes tautomer harmonization of amides wrong
    // StructureHarmonizer now corrects that explicitly, and smiles is the better source
    // because it keeps metal coordination (heme, chlorophyll) and the tautomer the depositor drew,
    // which standard inchi both discard.
    final MolecularStructure mol = parseStructure(smiles, StructureInputType.SMILES, options);
    if (mol != null) {
      return mol;
    }
    return parseStructure(inchi, INCHI, options);
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

  private static void putClean(@NotNull Set<String> cleanKeys, @NotNull MolecularStructure mol,
      @Nullable String key) {
    if (key == null || key.isBlank()) {
      return;
    }
    CLEAN_CACHE.put(key, mol);
    cleanKeys.add(key);
  }

  private static void putRaw(@NotNull Set<String> cleanKeys, @NotNull MolecularStructure mol,
      @Nullable String key) {
    if (key == null || key.isBlank() || cleanKeys.contains(key)) {
      return;
    }
    RAW_CACHE.put(key, mol);
  }
}
