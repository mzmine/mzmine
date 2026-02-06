""" data.py """
import logging
from typing import Optional
import re

from rdkit import Chem
from rdkit.Chem import Descriptors

from mist import utils


class Spectra(object):
    def __init__(
        self,
        spectra_name: str = "",
        spectra_file: str = "",
        spectra_formula: str = "",
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
        self.formula = spectra_formula
        self.instrument = instrument

        ##
        self._is_loaded = False
        self.parentmass = None
        self.num_spectra = None
        self.meta = None
        self.spectrum_names = None
        self.spectra = None

    def get_instrument(self):
        return self.instrument

    def _load_spectra(self):
        """Load the spectra from files"""
        meta, spectrum_tuples = utils.parse_spectra(self.spectra_file)

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
            self.mol_formula = utils.uncharged_formula(self.mol, mol_type="mol")
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

    @classmethod
    def MolFromFormula(cls, formula: str, **kwargs):
        """
        Create a Mol object from a chemical formula.
        This creates a molecule with atoms but no bonds.
            
        Args:
            formula (str): Chemical formula (e.g., "C6H12O6")
            inchikey (str, optional): InChIKey for the molecule. Defaults to None.
            
        Returns:
            Mol: Molecule object with atoms but no bonds
        """            
        # Regular expression to extract element symbols and counts from the formula
        pattern = r'([A-Z][a-z]*)(\d*)'
        matches = re.findall(pattern, formula)
            
        # Create an empty molecule
        mol = Chem.RWMol()
            
        # Add atoms to the molecule
        for element, count in matches:
            # If no count is specified, default to 1
            count = int(count) if count else 1
                
            # Get atomic number for the element
            atomic_num = Chem.GetPeriodicTable().GetAtomicNumber(element)
                
            # Add the atoms to the molecule
            for _ in range(count):
                atom = Chem.Atom(atomic_num)
                mol.AddAtom(atom)
            
        return cls(mol=mol, mol_formula=formula, **kwargs)

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