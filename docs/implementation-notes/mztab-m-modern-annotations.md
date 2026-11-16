# mzTab-M modern annotation export

## Intention

Export saved MZmine feature lists using their selected modern annotations while preserving measured
features that have no annotation. The bundled workshop project is the measured integration fixture:
120 aligned rows, two assays, and eight selected annotations.

## Decisions

- Use `FeatureListRow.getPreferredAnnotation()` so the existing annotation priority and explicit user
  selection apply. Fall back to the deprecated `FeatureIdentity` only when no modern annotation is
  selected. Export one selected annotation per row; do not invent additional candidates.
- The user chose a custom reliability scheme, `MZmine annotation status`, with `annotated` and
  `unannotated` values. These describe annotation presence and do not assert an MSI confidence level.
  Keep original numeric scores and metric names; spectral similarity and other matching scores have
  separate CV-defined confidence columns with nulls in inapplicable columns.
- Mixed lists use `M+S+F+E`, with one summary and feature per selected row and evidence only for
  annotated rows. Entirely unidentified lists use `M+F`, which avoids fabricated compound evidence.
  The annotated-only option filters rows before constructing any exported records.
- Qualify accessions with the declared database prefix. Missing accessions use the mzTab-M
  `prefix:null` sentinel. Preserve raw accessions separately; compound names never become accessions.
  Where the source has no external URI, a `mzmine:annotation-source/...` URI describes local
  provenance, while the compound URI remains absent. Unknown database versions remain absent.
- Export neutral Hill formulas using MZmine's existing hydrogen-neutralization utility. Preserve
  actual modern annotated precursor m/z values. The legacy experimental-m/z fallback is retained
  for compatibility with older identities that lack a precursor property.
- Convert standard SMF retention times from minutes to seconds; keep the legacy `peak_rt` optional
  columns in MZmine's minutes for importer compatibility. Resolve known row charges before
  per-file charges. Preserve one abundance cell per assay; missing measurements stay null and are
  excluded from each study-variable mean and variation calculation. Declare the arithmetic
  mean and variation-coefficient CV terms; report the coefficient as a ratio, not a percentage.
- Derive study variables from the metadata column selected under `Group by` in `MS data files`.
  Capture that column when scheduling the export and read its values from the owning project's
  metadata, considering only exported assays. Use the column title as the factor parameter name
  and each distinct value as its group name and parameter value, including zero and singleton
  groups. Sort numeric values numerically and other values deterministically; place missing or
  blank values in `undefined` last, as chosen by the user. `No grouping`, manual grouping,
  headless exports and unavailable selected columns use one `undefined` group containing all
  exported assays. The user chose this fallback rather than a hardcoded `factor` column.
  Compute each group's mean and sample coefficient of variation from its own measured areas;
  factors group samples and never rescale areas. Singleton groups have zero variation, while
  groups with no measurements have null mean and variation.
- Raw-file references retain filenames without source directories, as requested by the user.
  Spectral matches reference their actual query scans; merged queries resolve to measured source
  scans. Spectrum indices are zero-based positions in each raw file, not native scan numbers.
- Treat writer validation messages as export failures because the default validating writer can
  otherwise suppress output while returning normally.
- Declare both MS and PRIDE vocabularies, with version strings matching immutable ontology URIs.
  The writer requires a CV version, so an unversioned `master` URI must not imply an older version.

## Integration verification

The workshop test uses the unchanged bundled project and normal headless startup with a configured
MZmine login. It is explicitly enabled with `-Dmzmine.test.workshopExport=true`, so ordinary unit
tests do not depend on a personal account. No authentication checks or project loaders are bypassed.
The test checks measured values, the two groups obtained by selecting `factor` (`1.5` and `2.0`),
one `qc` group when selecting `mzmine_sample_type`, annotation preservation, references and parser
diagnostics; it does not perform network-dependent CV-semantic validation.
