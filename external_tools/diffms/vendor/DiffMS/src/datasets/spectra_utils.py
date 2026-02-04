import re
from pathlib import Path
from itertools import groupby
import re
from pathlib import Path
import pickle
import logging
from functools import partial
from typing import Any, Sequence, Optional, List, Tuple, Set, Callable

import numpy as np
import pandas as pd
from tqdm import tqdm
from rdkit import Chem
from rdkit.Chem.rdMolDescriptors import CalcMolFormula
from rdkit.Chem import Descriptors


class Spectra(object):
    def __init__(
        self,
        spectra_name: str = "",
        spectra_file: str = "",
        spectra_formula: str = "",
        spectra_smiles: str = "",
        instrument: str = "",
        **kwargs,
    ):
        """_summary_

        Args:
            spectra_name (str, optional): _description_. Defaults to "".
            spectra_file (str, optional): _description_. Defaults to "".
            spectra_formula (str, optional): _description_. Defaults to "".
            instrument (str, optional): _description_. Defaults to "".
        """
        self.spectra_name = spectra_name
        self.spectra_file = spectra_file
        self.spectra_formula = spectra_formula
        self.spectra_smiles = spectra_smiles
        self.formula = spectra_formula
        self.instrument = instrument

        ##
        self._is_loaded = False
        self.parentmass = None
        self.num_spectra = None
        self.meta = None
        self.spectrum_names = None
        self.spectra = None

        self._load_spectra()

    def get_instrument(self):
        return self.instrument

    def _load_spectra(self):
        """Load the spectra from files"""
        meta, spectrum_tuples = parse_spectra(self.spectra_file)

        self.meta = meta
        self.parentmass = None
        for parent_kw in ["parentmass", "PEPMASS"]:
            self.parentmass = self.meta.get(parent_kw, None)
            if self.parentmass is not None:
                break

        if self.parentmass is None:
            logging.info(f"Unable to find precursor mass for {self.spectrum_name}")
            self.parentmass = 0
        else:
            self.parentmass = float(self.parentmass)

        # Store all the spectrum names (e.g., e.v.) and spectra arrays
        self.spectrum_names, self.spectra = zip(*spectrum_tuples)
        self.num_spectra = len(self.spectra)
        self._is_loaded = True

    def get_spec_name(self, **kwargs):
        """get_spec_name."""
        return self.spectra_name

    def get_spec(self, **kwargs):
        """get_spec."""
        if not self._is_loaded:
            self._load_spectra()

        return self.spectra

    def get_meta(self, **kwargs):
        """get_meta."""
        if not self._is_loaded:
            self._load_spectra()
        return self.meta

    def get_spectra_formula(self):
        """Get spectra formula."""
        return self.formula


class Mol(object):
    """
    Object to store a compound, including possibly multiple mass spectrometry
    spectra.
    """

    def __init__(
        self,
        mol: Chem.Mol,
        smiles: Optional[str] = None,
        inchikey: Optional[str] = None,
        mol_formula: Optional[str] = None,
    ):
        """_summary_

        Args:
            mol (Chem.Mol): _description_
            smiles (Optional[str], optional): _description_. Defaults to None.
            inchikey (Optional[str], optional): _description_. Defaults to None.
            mol_formula (Optional[str], optional): _description_. Defaults to None.
        """
        self.mol = mol

        self.smiles = smiles
        if self.smiles is None:
            # Isomeric smiles handled in preprocessing
            self.smiles = Chem.MolToSmiles(mol)

        self.inchikey = inchikey
        if self.inchikey is None and self.smiles != "":
            self.inchikey = Chem.MolToInchiKey(mol)

        self.mol_formula = mol_formula
        if self.mol_formula is None:
            self.mol_formula = uncharged_formula(self.mol, mol_type="mol")
        self.num_hs = None

    @classmethod
    def MolFromInchi(cls, inchi: str, **kwargs):
        """_summary_

        Args:
            inchi (str): _description_

        Returns:
            _type_: _description_
        """
        mol = Chem.MolFromInchi(inchi)

        # Catch exception
        if mol is None:
            return None

        return cls(mol=mol, smiles=None, **kwargs)

    @classmethod
    def MolFromSmiles(cls, smiles: str, **kwargs):
        """_summary_

        Args:
            smiles (str): _description_

        Returns:
            _type_: _description_
        """
        if not smiles or isinstance(smiles, float):
            smiles = ""

        mol = Chem.MolFromSmiles(smiles)
        # Catch exception
        if mol is None:
            return None

        return cls(mol=mol, smiles=smiles, **kwargs)

    def get_smiles(self) -> str:
        """_summary_

        Returns:
            str: _description_
        """
        return self.smiles

    def get_inchikey(self) -> str:
        """_summary_

        Returns:
            str: _description_
        """
        return self.inchikey

    def get_molform(self) -> str:
        """_summary_

        Returns:
            str: _description_
        """
        return self.mol_formula

    def get_num_hs(self):
        """_summary_

        Raises:
            ValueError: _description_

        Returns:
            _type_: _description_
        """
        """get_num_hs."""
        if self.num_hs is None:
            num = re.findall("H([0-9]*)", self.mol_formula)
            if num is None:
                out_num_hs = 0
            else:
                if len(num) == 0:
                    out_num_hs = 0
                elif len(num) == 1:
                    num = num[0]
                    out_num_hs = 1 if num == "" else int(num)
                else:
                    raise ValueError()
            self.num_hs = out_num_hs
        else:
            out_num_hs = self.num_hs

        return out_num_hs

    def get_mol_mass(self):
        """_summary_

        Returns:
            _type_: _description_
        """
        return Descriptors.MolWt(self.mol)

    def get_rdkit_mol(self) -> Chem.Mol:
        """_summary_

        Returns:
            Chem.Mol: _description_
        """
        return self.mol


def get_paired_spectra(
    labels_file: str,
    spec_folder: str = None, 
    max_count: Optional[int] = None,
    allow_none_smiles: bool = False,
    prog_bars: bool = True,
    **kwargs,
) -> Tuple[List[Spectra], List[Mol]]:
    """_summary_

    Args:
        labels_file (str): _description_
        spec_folder (str): _description_
        max_count (Optional[int], optional): _description_. Defaults to None.
        allow_none_smiles (bool, optional): _description_. Defaults to False.
        prog_bars (bool, optional): _description_. Defaults to True.

    Returns:
        Tuple[List[Spectra], List[Mol]]: _description_
    """
    # First get labels
    compound_id_file = pd.read_csv(labels_file, sep="\t").astype(str)
    name_to_formula = dict(compound_id_file[["spec", "formula"]].values)

    name_to_smiles = {}
    if "smiles" in compound_id_file.keys():
        name_to_smiles = dict(compound_id_file[["spec", "smiles"]].values)

    name_to_inchikey = {}
    if "inchikey" in compound_id_file.keys():
        name_to_inchikey = dict(compound_id_file[["spec", "inchikey"]].values)

    name_to_instrument = {}
    if "instrument" in compound_id_file.keys():
        name_to_instrument = dict(compound_id_file[["spec", "instrument"]].values)

    # Note, loading has moved to the dataloader itself
    logging.info(f"Loading paired specs")
    spec_folder = Path(spec_folder) if spec_folder is not None else None

    # Resolve for full path
    if spec_folder is not None and spec_folder.exists():
        spectra_files = [Path(i).resolve() for i in spec_folder.glob("*.ms")]
    else:
        logging.info(
            f"Unable to find spec folder {str(spec_folder)}, adding placeholders"
        )
        spectra_files = [Path(f"{i}.ms") for i in name_to_formula]

    if max_count is not None:
        spectra_files = spectra_files[:max_count]

    # Get file name from Path obj
    get_name = lambda x: x.name.split(".")[0]

    # Just added!
    spectra_files = [i for i in spectra_files if i.stem in name_to_formula]


    spectra_names = [get_name(spectra_file) for spectra_file in spectra_files]
    spectra_formulas = [name_to_formula[spectra_name] for spectra_name in spectra_names]
    spectra_instruments = [
        name_to_instrument.get(spectra_name, "") for spectra_name in spectra_names
    ]

    logging.info(f"Converting paired samples into Spectra objects")

    tq = tqdm if prog_bars else lambda x: x

    spectra_list = [
        Spectra(
            spectra_name=spectra_name,
            spectra_file=str(spectra_file),
            spectra_formula=spectra_formula,
            instrument=instrument,
            **kwargs,
        )
        for spectra_name, spectra_file, spectra_formula, instrument in tq(
            zip(spectra_names, spectra_files, spectra_formulas, spectra_instruments)
        )
    ]

    # Create molecules
    spectra_smiles = [name_to_smiles.get(j, None) for j in spectra_names]
    spectra_inchikey = [name_to_inchikey.get(j, None) for j in spectra_names]
    if not allow_none_smiles:
        mol_list = [
            Mol.MolFromSmiles(smiles, inchikey=inchikey)
            for smiles, inchikey in tq(zip(spectra_smiles, spectra_inchikey))
            if smiles is not None
        ]
        spectra_list = [
            spec
            for spec, smi in tq(zip(spectra_list, spectra_smiles))
            if smi is not None
        ]
    else:
        mol_list = [
            Mol.MolFromSmiles(smiles, inchikey=inchikey)
            if smiles is not None
            else Mol.MolFromSmiles("")
            for smiles, inchikey in tq(zip(spectra_smiles, spectra_inchikey))
        ]
        spectra_list = [spec for spec, smi in tq(zip(spectra_list, spectra_smiles))]

    logging.info("Done creating spectra objects")
    return (spectra_list, mol_list)


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

def uncharged_formula(mol, mol_type="mol") -> str:
    """Compute uncharged formula"""
    if mol_type == "mol":
        chem_formula = CalcMolFormula(mol)
    elif mol_type == "smiles":
        mol = Chem.MolFromSmiles(mol)
        if mol is None:
            return None
        chem_formula = CalcMolFormula(mol)
    else:
        raise ValueError()

    return re.findall(r"^([^\+,^\-]*)", chem_formula)[0]