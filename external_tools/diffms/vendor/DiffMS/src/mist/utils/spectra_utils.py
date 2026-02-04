""" spectra_utils.py"""
import logging
import numpy as np
from typing import List


from .chem_utils import (
    vec_to_formula,
    get_all_subsets,
    ion_to_mass,
    ION_LST,
    clipped_ppm,
)


def bin_spectra(
    spectras: List[np.ndarray], num_bins: int = 2000, upper_limit: int = 1000
) -> np.ndarray:
    """bin_spectra.

    Args:
        spectras (List[np.ndarray]): Input list of spectra tuples
            [(header, spec array)]
        num_bins (int): Number of discrete bins from [0, upper_limit)
        upper_limit (int): Max m/z to consider featurizing

    Return:
        np.ndarray of shape [channels, num_bins]
    """
    bins = np.linspace(0, upper_limit, num=num_bins)
    binned_spec = np.zeros((len(spectras), len(bins)))
    for spec_index, spec in enumerate(spectras):

        # Convert to digitized spectra
        digitized_mz = np.digitize(spec[:, 0], bins=bins)

        # Remove all spectral peaks out of range
        in_range = digitized_mz < len(bins)
        digitized_mz, spec = digitized_mz[in_range], spec[in_range, :]

        # Add the current peaks to the spectra
        # Use a loop rather than vectorize because certain bins have conflicts
        # based upon resolution
        for bin_index, spec_val in zip(digitized_mz, spec[:, 1]):
            binned_spec[spec_index, bin_index] += spec_val

    return binned_spec


def merge_norm_spectra(spec_tuples, precision=4) -> np.ndarray:
    """merge_norm_spectra.

    Take a list of mz, inten tuple arrays and merge them by 4 digit precision

    Note this uses _max_ merging

    """
    mz_to_inten_pair = {}
    for i in spec_tuples:
        for tup in i:
            mz, inten = tup
            mz_ind = np.round(mz, precision)
            cur_pair = mz_to_inten_pair.get(mz_ind)
            if cur_pair is None:
                mz_to_inten_pair[mz_ind] = tup
            elif inten > cur_pair[1]:
                mz_to_inten_pair[mz_ind] = (mz_ind, inten)
            else:
                pass

    merged_spec = np.vstack([v for k, v in mz_to_inten_pair.items()])
    merged_spec[:, 1] = merged_spec[:, 1] / merged_spec[:, 1].max()
    return merged_spec


def norm_spectrum(binned_spec: np.ndarray) -> np.ndarray:
    """norm_spectrum.

    Normalizes each spectral channel to have norm 1
    This change is made in place

    Args:
        binned_spec (np.ndarray) : Vector of spectras

    Return:
        np.ndarray where each channel has max(1)
    """

    spec_maxes = binned_spec.max(1)

    non_zero_max = spec_maxes > 0

    spec_maxes = spec_maxes[non_zero_max]
    binned_spec[non_zero_max] = binned_spec[non_zero_max] / spec_maxes.reshape(-1, 1)

    return binned_spec


def process_spec_file(meta, tuples, precision=4, max_inten=0.001, max_peaks=60):
    """process_spec_file."""

    if "parentmass" in meta:
        parentmass = meta.get("parentmass", None)
    elif "PARENTMASS" in meta:
        parentmass = meta.get("PARENTMASS", None)
    elif "PEPMASS" in meta:
        parentmass = meta.get("PEPMASS", None)
    else:
        logging.debug(f"missing parentmass for spec")
        parentmass = 1000000

    parentmass = float(parentmass)

    # First norm spectra
    fused_tuples = [x for _, x in tuples if x.size > 0]

    if len(fused_tuples) == 0:
        return

    mz_to_inten_pair = {}
    new_tuples = []
    for i in fused_tuples:
        for tup in i:
            mz, inten = tup
            mz_ind = np.round(mz, precision)
            cur_pair = mz_to_inten_pair.get(mz_ind)
            if cur_pair is None:
                mz_to_inten_pair[mz_ind] = tup
                new_tuples.append(tup)
            elif inten > cur_pair[1]:
                cur_pair[1] = inten
            else:
                pass

    merged_spec = np.vstack(new_tuples)
    merged_spec = merged_spec[merged_spec[:, 0] <= (parentmass + 1)]
    merged_spec[:, 1] = merged_spec[:, 1] / merged_spec[:, 1].max()

    # Sqrt intensities here
    merged_spec[:, 1] = np.sqrt(merged_spec[:, 1])

    merged_spec = max_inten_spec(
        merged_spec, max_num_inten=max_peaks, inten_thresh=max_inten
    )
    return merged_spec


def max_inten_spec(spec, max_num_inten: int = 60, inten_thresh: float = 0):
    """max_inten_spec.

    Args:
        spec: 2D spectra array
        max_num_inten: Max number of peaks
        inten_thresh: Min intensity to alloow in returned peak

    Return:
        Spec filtered down


    """
    spec_masses, spec_intens = spec[:, 0], spec[:, 1]

    # Make sure to only take max of each formula
    # Sort by intensity and select top subpeaks
    new_sort_order = np.argsort(spec_intens)[::-1]
    if max_num_inten is not None:
        new_sort_order = new_sort_order[:max_num_inten]

    spec_masses = spec_masses[new_sort_order]
    spec_intens = spec_intens[new_sort_order]

    spec_mask = spec_intens > inten_thresh
    spec_masses = spec_masses[spec_mask]
    spec_intens = spec_intens[spec_mask]
    spec = np.vstack([spec_masses, spec_intens]).transpose(1, 0)
    return spec


def max_thresh_spec(spec: np.ndarray, max_peaks=100, inten_thresh=0.003):
    """max_thresh_spec.

    Args:
        spec (np.ndarray): spec
        max_peaks: Max num peaks to keep
        inten_thresh: Min inten to keep
    """

    spec_masses, spec_intens = spec[:, 0], spec[:, 1]

    # Make sure to only take max of each formula
    # Sort by intensity and select top subpeaks
    new_sort_order = np.argsort(spec_intens)[::-1]
    new_sort_order = new_sort_order[:max_peaks]

    spec_masses = spec_masses[new_sort_order]
    spec_intens = spec_intens[new_sort_order]

    spec_mask = spec_intens > inten_thresh
    spec_masses = spec_masses[spec_mask]
    spec_intens = spec_intens[spec_mask]
    out_ar = np.vstack([spec_masses, spec_intens]).transpose(1, 0)
    return out_ar


def assign_subforms(form, spec, ion_type, mass_diff_thresh=15):
    """_summary_

    Args:
        form (_type_): _description_
        spec (_type_): _description_
        ion_type (_type_): _description_
        mass_diff_thresh (int, optional): _description_. Defaults to 15.

    Returns:
        _type_: _description_
    """
    cross_prod, masses = get_all_subsets(form)
    spec_masses, spec_intens = spec[:, 0], spec[:, 1]

    ion_masses = ion_to_mass[ion_type]
    masses_with_ion = masses + ion_masses
    ion_types = np.array([ion_type] * len(masses_with_ion))

    mass_diffs = np.abs(spec_masses[:, None] - masses_with_ion[None, :])

    formula_inds = mass_diffs.argmin(-1)
    min_mass_diff = mass_diffs[np.arange(len(mass_diffs)), formula_inds]
    rel_mass_diff = clipped_ppm(min_mass_diff, spec_masses)

    # Filter by mass diff threshold (ppm)
    valid_mask = rel_mass_diff < mass_diff_thresh
    spec_masses = spec_masses[valid_mask]
    spec_intens = spec_intens[valid_mask]
    min_mass_diff = min_mass_diff[valid_mask]
    rel_mass_diff = rel_mass_diff[valid_mask]
    formula_inds = formula_inds[valid_mask]

    formulas = np.array([vec_to_formula(j) for j in cross_prod[formula_inds]])
    formula_masses = masses_with_ion[formula_inds]
    ion_types = ion_types[formula_inds]

    # Build mask for uniqueness on formula and ionization
    # note that ionization are all the same for one subformula assignment
    # hence we only need to consider the uniqueness of the formula
    formula_idx_dict = {}
    uniq_mask = []
    for idx, formula in enumerate(formulas):
        uniq_mask.append(formula not in formula_idx_dict)
        gather_ind = formula_idx_dict.get(formula, None)
        if gather_ind is None:
            continue
        spec_intens[gather_ind] += spec_intens[idx]
        formula_idx_dict[formula] = idx

    spec_masses = spec_masses[uniq_mask]
    spec_intens = spec_intens[uniq_mask]
    min_mass_diff = min_mass_diff[uniq_mask]
    rel_mass_diff = rel_mass_diff[uniq_mask]
    formula_masses = formula_masses[uniq_mask]
    formulas = formulas[uniq_mask]
    ion_types = ion_types[uniq_mask]

    # To calculate explained intensity, preserve the original normalized
    # intensity
    if spec_intens.size == 0:
        output_tbl = None
    else:
        output_tbl = {
            "mz": list(spec_masses),
            "ms2_inten": list(spec_intens),
            "mono_mass": list(formula_masses),
            "abs_mass_diff": list(min_mass_diff),
            "mass_diff": list(rel_mass_diff),
            "formula": list(formulas),
            "ions": list(ion_types),
        }
    output_dict = {
        "cand_form": form,
        "cand_ion": ion_type,
        "output_tbl": output_tbl,
    }
    return output_dict


def get_output_dict(
    spec_name: str,
    spec: np.ndarray,
    form: str,
    mass_diff_type: str,
    mass_diff_thresh: float,
    ion_type: str,
) -> dict:
    """_summary_

    This function attemps to take an array of mass intensity values and assign
    formula subsets to subpeaks

    Args:
        spec_name (str): _description_
        spec (np.ndarray): _description_
        form (str): _description_
        mass_diff_type (str): _description_
        mass_diff_thresh (float): _description_
        ion_type (str): _description_

    Returns:
        dict: _description_
    """
    assert mass_diff_type == "ppm"
    # This is the case for some erroneous MS2 files for which proc_spec_file return None
    # All the MS2 subpeaks in these erroneous MS2 files has mz larger than parentmass
    output_dict = {"cand_form": form, "cand_ion": ion_type, "output_tbl": None}
    if spec is not None and ion_type in ION_LST:
        output_dict = assign_subforms(
            form, spec, ion_type, mass_diff_thresh=mass_diff_thresh
        )
    return output_dict