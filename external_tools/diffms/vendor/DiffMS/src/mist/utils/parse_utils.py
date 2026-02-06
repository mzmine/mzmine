""" parse_utils.py """
from pathlib import Path
from typing import Tuple, List, Optional
from itertools import groupby

from tqdm import tqdm
import numpy as np


def parse_spectra(spectra_file: str) -> Tuple[dict, List[Tuple[str, np.ndarray]]]:
    """parse_spectra.

    Parses spectra in the SIRIUS format and returns

    Args:
        spectra_file (str): Name of spectra file to parse
    Return:
        Tuple[dict, List[Tuple[str, np.ndarray]]]: metadata and list of spectra
            tuples containing name and array
    """
    lines = [i.strip() for i in open(spectra_file, "r").readlines()]

    group_num = 0
    metadata = {}
    spectras = []
    my_iterator = groupby(
        lines, lambda line: line.startswith(">") or line.startswith("#")
    )

    for index, (start_line, lines) in enumerate(my_iterator):
        group_lines = list(lines)
        subject_lines = list(next(my_iterator)[1])
        # Get spectra
        if group_num > 0:
            spectra_header = group_lines[0].split(">")[1]
            peak_data = [
                [float(x) for x in peak.split()[:2]]
                for peak in subject_lines
                if peak.strip()
            ]
            # Check if spectra is empty
            if len(peak_data):
                peak_data = np.vstack(peak_data)
                # Add new tuple
                spectras.append((spectra_header, peak_data))
        # Get meta data
        else:
            entries = {}
            for i in group_lines:
                if " " not in i:
                    continue
                elif i.startswith("#INSTRUMENT TYPE"):
                    key = "#INSTRUMENT TYPE"
                    val = i.split(key)[1].strip()
                    entries[key[1:]] = val
                else:
                    start, end = i.split(" ", 1)
                    start = start[1:]
                    while start in entries:
                        start = f"{start}'"
                    entries[start] = end

            metadata.update(entries)
        group_num += 1

    metadata["_FILE_PATH"] = spectra_file
    metadata["_FILE"] = Path(spectra_file).stem
    return metadata, spectras


def spec_to_ms_str(
    spec: List[Tuple[str, np.ndarray]], essential_keys: dict, comments: dict = {}
) -> str:
    """spec_to_ms_str.

    Turn spec ars and info dicts into str for output file


    Args:
        spec (List[Tuple[str, np.ndarray]]): spec
        essential_keys (dict): essential_keys
        comments (dict): comments

    Returns:
        str:
    """

    def pair_rows(rows):
        return "\n".join([f"{i} {j}" for i, j in rows])

    header = "\n".join(f">{k} {v}" for k, v in essential_keys.items())
    comments = "\n".join(f"#{k} {v}" for k, v in essential_keys.items())
    spec_strs = [f">{name}\n{pair_rows(ar)}" for name, ar in spec]
    spec_str = "\n\n".join(spec_strs)
    output = f"{header}\n{comments}\n\n{spec_str}"
    return output


def build_mgf_str(
    meta_spec_list: List[Tuple[dict, List[Tuple[str, np.ndarray]]]],
    merge_charges=True,
    parent_mass_keys=["PEPMASS", "parentmass", "PRECURSOR_MZ"],
) -> str:
    """build_mgf_str.

    Args:
        meta_spec_list (List[Tuple[dict, List[Tuple[str, np.ndarray]]]]): meta_spec_list

    Returns:
        str:
    """
    entries = []
    for meta, spec in tqdm(meta_spec_list):
        str_rows = ["BEGIN IONS"]

        # Try to add precusor mass
        for i in parent_mass_keys:
            if i in meta:
                pep_mass = float(meta.get(i, -100))
                str_rows.append(f"PEPMASS={pep_mass}")
                break

        for k, v in meta.items():
            str_rows.append(f"{k.upper().replace(' ', '_')}={v}")

        if merge_charges:
            spec_ar = np.vstack([i[1] for i in spec])
            spec_ar = np.vstack([i for i in sorted(spec_ar, key=lambda x: x[0])])
        else:
            raise NotImplementedError()
        str_rows.extend([f"{i} {j}" for i, j in spec_ar])
        str_rows.append("END IONS")

        str_out = "\n".join(str_rows)
        entries.append(str_out)

    full_out = "\n\n".join(entries)
    return full_out


def parse_spectra_msp(
    mgf_file: str, max_num: Optional[int] = None
) -> List[Tuple[dict, List[Tuple[str, np.ndarray]]]]:
    """parse_spectr_msp.

    Parses spectra in the MSP file format

    Args:
        mgf_file (str) : str
        max_num (Optional[int]): If set, only parse this many
    Return:
        List[Tuple[dict, List[Tuple[str, np.ndarray]]]]: metadata and list of spectra
            tuples containing name and array
    """

    key = lambda x: x.strip().startswith("PEPMASS")
    parsed_spectra = []
    with open(mgf_file, "r", encoding="utf-8") as fp:
        for (is_header, group) in tqdm(groupby(fp, key)):

            if is_header:
                continue
            meta = dict()
            spectra = []
            # Note: Sometimes we have multiple scans
            # This mgf has them collapsed
            cur_spectra_name = "spec"
            cur_spectra = []
            group = list(group)
            for line in group:
                line = line.strip()
                if not line:
                    pass
                elif ":" in line:
                    k, v = [i.strip() for i in line.split(":", 1)]
                    meta[k] = v
                else:
                    mz, intens = line.split()
                    cur_spectra.append((float(mz), float(intens)))

            if len(cur_spectra) > 0:
                cur_spectra = np.vstack(cur_spectra)
                spectra.append((cur_spectra_name, cur_spectra))
                parsed_spectra.append((meta, spectra))
            else:
                pass
                # print("no spectra found for group: ", "".join(group))

            if max_num is not None and len(parsed_spectra) > max_num:
                # print("Breaking")
                break
        return parsed_spectra


def parse_spectra_mgf(
    mgf_file: str, max_num: Optional[int] = None
) -> List[Tuple[dict, List[Tuple[str, np.ndarray]]]]:
    """parse_spectr_mgf.

    Parses spectra in the MGF file formate, with

    Args:
        mgf_file (str) : str
        max_num (Optional[int]): If set, only parse this many
    Return:
        List[Tuple[dict, List[Tuple[str, np.ndarray]]]]: metadata and list of spectra
            tuples containing name and array
    """

    key = lambda x: x.strip() == "BEGIN IONS"
    parsed_spectra = []
    with open(mgf_file, "r") as fp:

        for (is_header, group) in tqdm(groupby(fp, key)):

            if is_header:
                continue

            meta = dict()
            spectra = []
            # Note: Sometimes we have multiple scans
            # This mgf has them collapsed
            cur_spectra_name = "spec"
            cur_spectra = []
            group = list(group)
            for line in group:
                line = line.strip()
                if not line:
                    pass
                elif line == "END IONS" or line == "BEGIN IONS":
                    pass
                elif "=" in line:
                    k, v = [i.strip() for i in line.split("=", 1)]
                    meta[k] = v
                else:
                    mz, intens = line.split()
                    cur_spectra.append((float(mz), float(intens)))

            if len(cur_spectra) > 0:
                cur_spectra = np.vstack(cur_spectra)
                spectra.append((cur_spectra_name, cur_spectra))
                parsed_spectra.append((meta, spectra))
            else:
                pass
                # print("no spectra found for group: ", "".join(group))

            if max_num is not None and len(parsed_spectra) > max_num:
                # print("Breaking")
                break
        return parsed_spectra


def parse_tsv_spectra(spectra_file: str) -> List[Tuple[str, np.ndarray]]:
    """parse_tsv_spectra.

    Parses spectra returned from sirius fragmentation tree

    Args:
        spectra_file (str): Name of spectra tsv file to parse
    Return:
        List[Tuple[str, np.ndarray]]]: list of spectra
            tuples containing name and array. This is used to maintain
            consistency with the parse_spectra output
    """
    output_spec = []
    with open(spectra_file, "r") as fp:
        for index, line in enumerate(fp):
            if index == 0:
                continue
            line = line.strip().split("\t")
            intensity = float(line[1])
            exact_mass = float(line[3])
            output_spec.append([exact_mass, intensity])

    output_spec = np.array(output_spec)
    return_obj = [("sirius_spec", output_spec)]
    return return_obj