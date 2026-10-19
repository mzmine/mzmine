# Project metadata persistence

## Intention

Preserve the project's current metadata table across project save/load independently of the
metadata file or extraction settings that originally populated it. This includes values edited or
entered manually after raw data import.

## Decisions

- Save a project-level `project_metadata.tsv` snapshot in MZmine's internal metadata export format
  inside the zipped project and import it after raw data and feature lists have finished loading.
- Keep loading projects without the snapshot for backward compatibility; those projects retain the
  previous reconstruction behavior.
- Merge the snapshot into the project so loading with the merge option preserves metadata already
  present for other raw data files while the saved snapshot remains authoritative for its files.
- Preserve existing raw-import persistence semantics: standalone projects embed metadata source
  files referenced by their import history, while referencing projects keep the external path and
  fail to load if that metadata file is missing.
