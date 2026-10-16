# Isotope finder: spectrum selection for ion mobility features

## Intention

The isotope finder searches exactly one spectrum per feature. For IMS features that spectrum used to
be the single most intense mobility scan of the apex frame, with a retry on the frame spectrum when
nothing was found. Neither works:

- a single mobility scan carries too little signal, so weak M+1/M+2 peaks never clear mass detection
  and charge assignment collapses to z=1,
- the frame spectrum is not mobility resolved, so the retry drags in every co-eluting ion and
  produces patterns built from unrelated signals.

The searched spectrum is now the feature's mobility scans merged over the mobility FWHM: mobility
selectivity is kept, counting statistics are recovered.

## Decisions

- **Mobility scope is the mobility FWHM** of the feature's summed mobilogram
  (`IonMobilityUtils.getMobilityFWHM`). When no FWHM can be determined the full mobility range the
  feature covers is used instead. Mobility scans where the feature has no intensity are always
  skipped, so "full range" never means "the whole frame".
- **Retention time scope is switchable** via `IsotopeFinderTask.IMS_MERGE_SCOPE` (`ImsMergeScope`):
  `RT_FWHM` (default) merges the frames within the feature's RT FWHM, `APEX_FRAME` merges only the
  representative frame. Deliberately not a user parameter - it exists so both can be compared on
  real data before one is committed to. Both use the same mobility FWHM, so the two differ only in
  the RT dimension.
- **Merging uses mass lists, not raw mobility scan data.** The existing
  `SpectraMerging.extractSummedMobilityScan` merges raw data (see its todo); a separate
  `extractSummedMobilityScanFromMassLists` was added rather than changing it, so the feature table's
  "Extract spectrum from mobility FWHM" keeps its current behavior. The isotope finder is mass-list
  based everywhere else, and merging raw data would feed it sub-threshold noise it has never seen.
- **No frame fallback when detection finds nothing.** The frame is the interference source this
  change exists to avoid. The frame spectrum is still used when the merge cannot be built at all -
  no `IonMobilogramTimeSeries`, or no mass lists on the mobility scans - because otherwise those
  features would get no pattern at all.
- **The merge is restricted to an m/z window** around the feature m/z
  (`IsotopeFinderTask.IMS_MERGE_MZ_WINDOW_DA`, 50 Da either side). Merging is the dominant per
  feature cost, and the engine only ever reads around the feature signal. The window is generous
  because the pattern search walks outwards as long as it keeps finding plausible spacings, so it is
  not bounded by a fixed offset count; a pattern wider than the window would be truncated for IMS
  but
  not for LC-MS.
- **Intensity normalization is explicit, not inferred from the spectrum type.**
  `IsotopeFinderEngine.detect` gained a `normalizeToHeight` flag. The merged spectrum covers only a
  part of the feature, like a single mobility scan did, so candidate intensities are rescaled to the
  feature height - otherwise the stored isotope pattern would be on an arbitrary scale. Scoring
  itself is scale invariant; every term is relative to the base peak. The old four argument
  `detect` keeps inferring the flag from `spectrum instanceof MobilityScan`.
- **RT-FWHM cross-scan refinement stays LC-MS only.** With `RT_FWHM` the merged spectrum already
  spans the peak width, so refining across RT again would be redundant.
