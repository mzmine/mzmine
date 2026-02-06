"""
featurizers.py

Hold featurizers & collate fns for various spectra and molecules in a single
file
"""
from pathlib import Path
import logging
from abc import ABC, abstractmethod
from typing import List, Dict, Callable

import h5py

import json
import pandas as pd
import numpy as np
import torch
import torch.nn.functional as F
from torch_geometric.data import Data, Batch

from rdkit import Chem, RDLogger
from rdkit.Chem.rdchem import BondType as BT
from rdkit.Chem import AllChem, DataStructs
from rdkit.Chem.AllChem import GetMorganFingerprintAsBitVect
from rdkit.Chem.rdMolDescriptors import GetMACCSKeysFingerprint

from .. import utils
from . import data

ATOM_DECODER = ['C', 'O', 'P', 'N', 'S', 'Cl', 'F', 'H']
TYPES = {atom: i for i, atom in enumerate(ATOM_DECODER)}
BONDS = {BT.SINGLE: 0, BT.DOUBLE: 1, BT.TRIPLE: 2, BT.AROMATIC: 3}


def get_mol_featurizer(mol_features, **kwargs):
    return {"none": NoneFeaturizer, "fingerprint": FingerprintFeaturizer,}[
        mol_features
    ](**kwargs)


def get_spec_featurizer(spec_features, **kwargs):
    return {
        "none": NoneFeaturizer,
        "binned": BinnedFeaturizer,
        "mz_xformer": MZFeaturizer,
        "peakformula": PeakFormula,
        "peakformula_test": PeakFormulaTest,
    }[spec_features](**kwargs)


def get_paired_featurizer(spec_features, mol_features, **kwargs):
    """get_paired_featurizer.

    Args:
        spec_features (str): Spec featurizer
        mol_features (str): Mol featurizer

    """

    mol_featurizer = get_mol_featurizer(mol_features, **kwargs)
    spec_featurizer = get_spec_featurizer(spec_features, **kwargs)
    paired_featurizer = PairedFeaturizer(spec_featurizer, mol_featurizer, **kwargs)
    return paired_featurizer


class PairedFeaturizer(object):
    """PairedFeaturizer"""

    def __init__(self, spec_featurizer, mol_featurizer, graph_featurizer = None, **kwarg):
        """__init__."""
        self.spec_featurizer = spec_featurizer
        self.mol_featurizer = mol_featurizer
        self.graph_featurizer = graph_featurizer

    def featurize_mol(self, mol: data.Mol, **kwargs) -> Dict:
        return self.mol_featurizer.featurize(mol, **kwargs)

    def featurize_spec(self, mol: data.Mol, **kwargs) -> Dict:
        return self.spec_featurizer.featurize(mol, **kwargs)
    
    def featurize_graph(self, mol: data.Mol, **kwargs) -> Dict:
        if self.graph_featurizer is not None:
            return self.graph_featurizer.featurize(mol, **kwargs)
        else:
            return None

    def get_mol_collate(self) -> Callable:
        return self.mol_featurizer.collate_fn

    def get_spec_collate(self) -> Callable:
        return self.spec_featurizer.collate_fn
    
    def get_graph_collate(self) -> Callable:
        if self.graph_featurizer is not None:
            return self.graph_featurizer.collate_fn
        else:
            return None

    def set_spec_featurizer(self, spec_featurizer):
        self.spec_featurizer = spec_featurizer

    def set_mol_featurizer(self, mol_featurizer):
        self.mol_featurizer = mol_featurizer

    def set_graph_featurizer(self, graph_featurizer):
        self.graph_featurizer = graph_featurizer


class Featurizer(ABC):
    """Featurizer"""

    def __init__(
        self, cache_featurizers: bool = False, **kwargs
    ):
        super().__init__()
        self.cache_featurizers = cache_featurizers
        self.cache = {}

    @abstractmethod
    def _encode(self, obj: object) -> str:
        """Encode object into a string representation"""
        raise NotImplementedError()

    def _featurize(self, obj: object) -> Dict:
        """Internal featurize class that does not utilize the cache"""
        raise {}

    def featurize(self, obj: object, train_mode=False, **kwargs) -> Dict:
        """Featurizer a single object"""
        encoded_obj = self._encode(obj)

        if self.cache_featurizers:
            if encoded_obj in self.cache:
                featurized = self.cache[encoded_obj]
            else:
                featurized = self._featurize(obj)
                self.cache[encoded_obj] = featurized
        else:
            featurized = self._featurize(obj)

        return featurized


class NoneFeaturizer(Featurizer):
    """NoneFeaturizer"""

    def _encode(self, obj) -> str:
        return ""

    @staticmethod
    def collate_fn(objs) -> Dict:
        return {}

    def featurize(self, *args, **kwargs) -> Dict:
        """Override featurize with empty dict return"""
        return {}


class MolFeaturizer(Featurizer):
    """MolFeaturizer"""

    def _encode(self, mol: data.Mol) -> str:
        """Encode mol into smiles repr"""
        smi = mol.get_smiles()
        return smi


class SpecFeaturizer(Featurizer):
    """SpecFeaturizer"""

    def _encode(self, spec: data.Spectra) -> str:
        """Encode spectra into name"""
        return spec.get_spec_name()
    
class GraphFeaturizer(Featurizer):
    """GraphFeaturizer"""

    def __init__(self, **kwargs):
        super().__init__(**kwargs)

        self.morgan_r = kwargs.get("morgan_r", 2)
        self.morgan_nbits = kwargs.get("morgan_nbits", 2048)

    def _encode(self, mol: data.Mol) -> str:
        """Encode graph into name"""
        return mol.inchikey
    
    @staticmethod
    def collate_fn(graphs: List[Data]) -> Batch:
        return Batch.from_data_list(graphs)
    
    def _featurize(self, obj: object) -> Data:
        mol = Chem.MolFromSmiles(obj.get_smiles())
        smi = Chem.MolToSmiles(mol, isomericSmiles=False)
        mol = Chem.MolFromSmiles(smi)

        N = mol.GetNumAtoms()

        type_idx = []
        for atom in mol.GetAtoms():
            type_idx.append(TYPES[atom.GetSymbol()])

        row, col, edge_type = [], [], []
        for bond in mol.GetBonds():
            start, end = bond.GetBeginAtomIdx(), bond.GetEndAtomIdx()
            row += [start, end]
            col += [end, start]
            edge_type += 2 * [BONDS[bond.GetBondType()] + 1] # add one so that 0 is reserved for no edge


        edge_index = torch.tensor([row, col], dtype=torch.long)
        edge_type = torch.tensor(edge_type, dtype=torch.long)
        edge_attr = F.one_hot(edge_type, num_classes=len(BONDS) + 1).to(torch.float)

        permutation = (edge_index[0] * N + edge_index[1]).argsort() # sort by row then by column index
        edge_index = edge_index[:, permutation]
        edge_attr = edge_attr[permutation]

        x = F.one_hot(torch.tensor(type_idx), num_classes=len(TYPES)).float()
        y = torch.tensor(np.asarray(GetMorganFingerprintAsBitVect(mol, self.morgan_r, nBits=self.morgan_nbits), dtype=np.int8)).unsqueeze(0)

        inchi = Chem.MolToInchi(mol)

        data = Data(x=x, edge_index=edge_index, edge_attr=edge_attr, y=y, inchi=inchi)

        return data



class FingerprintFeaturizer(MolFeaturizer):
    """MolFeaturizer"""

    def __init__(self, fp_names: List[str], fp_file: str = None, **kwargs):
        """__init__

        Args:
            fp_names (List[str]): List of
            nbits (int): Number of bits
            fp_file (str): Saved fp file

        """
        super().__init__(**kwargs)
        self._fp_cache = {}
        self._morgan_projection = np.random.randn(50, 2048)
        self.fp_names = fp_names
        self.fp_file = fp_file

        # Only for csi fp
        self._root_dir = Path().resolve()

    @staticmethod
    def collate_fn(mols: List[dict]) -> dict:
        fp_ar = torch.tensor(np.array(mols))
        return {"mols": fp_ar}

    def featurize_smiles(self, smiles: str, **kwargs) -> np.ndarray:
        """featurize_smiles.

        Args:
            smiles (str): smiles
            kwargs:

        Returns:
            Dict:
        """

        mol_obj = data.Mol.MolFromSmiles(smiles)
        return self._featurize(mol_obj)

    def _featurize(self, mol: data.Mol, **kwargs) -> Dict:
        """featurize.

        Args:
            mol (Mol)

        """
        fp_list = []
        for fp_name in self.fp_names:
            # Get all fingerprint bits
            fingerprint = self._get_fingerprint(mol, fp_name)
            fp_list.append(fingerprint)

        fp = np.concatenate(fp_list)
        return fp

    # Fingerprint functions
    def _get_morgan_fp_base(self, mol: data.Mol, nbits: int = 2048, radius=2):
        """get morgan fingeprprint"""

        def fp_fn(m):
            return AllChem.GetMorganFingerprintAsBitVect(m, radius, nBits=nbits)

        mol = mol.get_rdkit_mol()
        fingerprint = fp_fn(mol)
        array = np.zeros((0,), dtype=np.int8)
        DataStructs.ConvertToNumpyArray(fingerprint, array)
        return array

    def _get_morgan_2048(self, mol: data.Mol):
        """get morgan fingeprprint"""
        return self._get_morgan_fp_base(mol, nbits=2048)

    def _get_morgan_projection(self, mol: data.Mol):
        """get morgan fingeprprint"""

        morgan_fp = self._get_morgan_fp_base(mol, nbits=2048)

        output_fp = np.einsum("ij,j->i", self._morgan_projection, morgan_fp)
        return output_fp

    def _get_morgan_1024(self, mol: data.Mol):
        """get morgan fingeprprint"""
        return self._get_morgan_fp_base(mol, nbits=1024)

    def _get_morgan_512(self, mol: data.Mol):
        """get morgan fingeprprint"""
        return self._get_morgan_fp_base(mol, nbits=512)

    def _get_morgan_256(self, mol: data.Mol):
        """get morgan fingeprprint"""
        return self._get_morgan_fp_base(mol, nbits=256)

    def _get_morgan_4096(self, mol: data.Mol):
        """get morgan fingeprprint"""
        return self._get_morgan_fp_base(mol, nbits=4096)

    def _get_morgan_4096_3(self, mol: data.Mol):
        """get morgan fingeprprint"""
        return self._get_morgan_fp_base(mol, nbits=4096, radius=3)

    def _get_maccs(self, mol: data.Mol):
        """get maccs fingerprint"""
        mol = mol.get_rdkit_mol()
        fingerprint = GetMACCSKeysFingerprint(mol)
        array = np.zeros((0,), dtype=np.int8)
        DataStructs.ConvertToNumpyArray(fingerprint, array)
        return array

    def _fill_precomputed_cache_hdf5(self, fp_file):
        """Get precomputed fp cache"""
        if fp_file not in self._fp_cache:
            if not Path(fp_file).exists():
                raise ValueError(f"Cannot find file {fp_file}")

            # Then get hdf5
            logging.info("Loading h5 features")
            dataset = h5py.File(fp_file, "r")
            logging.info("Stored in fp_cache")
            index = {
                i.decode(): ind for ind, i in enumerate(np.array(dataset["ikeys"]))
            }
            num_bits = dataset.attrs["num_bits"]
            self._fp_cache[fp_file] = {
                "index": index,
                "features": dataset["features"],
                "num_bits": num_bits,
            }

    def _get_precomputed_hdf5(self, mol, fp_file):
        """Get precomputed hdf5 of a single molecule"""
        self._fill_precomputed_cache_hdf5(fp_file)
        cache_obj = self._fp_cache[fp_file]
        index = cache_obj["index"]
        feats = cache_obj["features"]
        inchikey = mol.get_inchikey()
        if inchikey in index:
            num_bits = cache_obj["num_bits"]
            out_vec = utils.unpack_bits(feats[index[inchikey]], num_bits=num_bits)
            return out_vec
        else:
            num_bits = cache_obj["num_bits"]
            logging.info(f"Unable to find inchikey {inchikey} in {fp_file}")
            # Create empty vector
            return np.zeros(num_bits)

    def _get_csi(self, mol):
        return self._get_precomputed_hdf5(mol, self.fp_file)

    @classmethod
    def get_fingerprint_size(cls, fp_names: list = [], **kwargs):
        """Get list of fingerprint size"""
        fp_name_to_bits = {
            "morgan256": 256,
            "morgan512": 512,
            "morgan1024": 1024,
            "morgan2048": 2048,
            "morgan_project": 50,
            "morgan4096": 4096,
            "morgan4096_3": 4096,
            "maccs": 167,
            "csi": 5496,
        }
        num_bits = 0
        for fp_name in fp_names:
            num_bits += fp_name_to_bits.get(fp_name)
        return num_bits

    def _get_fingerprint(self, mol: data.Mol, fp_name: str):
        """_get_fingerprint_fn"""
        return {
            "morgan256": self._get_morgan_256,
            "morgan512": self._get_morgan_512,
            "morgan1024": self._get_morgan_1024,
            "morgan2048": self._get_morgan_2048,
            "morgan_project": self._get_morgan_projection,
            "morgan4096": self._get_morgan_4096,
            "morgan4096_3": self._get_morgan_4096_3,
            "maccs": self._get_maccs,
            "csi": self._get_csi,
        }[fp_name](mol)

    def dist(self, mol_1, mol_2) -> np.ndarray:
        """Return 2048 bit molecular fingerprint"""
        fp1 = self.featurize(mol_1)
        fp2 = self.featurize(mol_2)
        tani = 1 - (((fp1 & fp2).sum()) / (fp1 | fp2).sum())
        return tani

    def dist_batch(self, mol_list) -> np.ndarray:
        """Return 2048 bit molecular fingerprint"""

        fps = []
        if len(mol_list) == 0:
            return np.array([[]])

        for mol_temp in mol_list:
            fps.append(self.featurize(mol_temp))

        fps = np.vstack(fps)

        fps_a = fps[:, None, :]
        fps_b = fps[None, :, :]

        intersect = (fps_a & fps_b).sum(-1)
        union = (fps_a | fps_b).sum(-1)
        tani = 1 - intersect / union
        return tani

    def dist_one_to_many(self, mol, mol_list) -> np.ndarray:
        """Return 2048 bit molecular fingerprint"""

        fps = []
        if len(mol_list) == 0:
            return np.array([[]])

        for mol_temp in mol_list:
            fps.append(self.featurize(mol_temp))

        fp_a = self.featurize(mol)

        fps = np.vstack(fps)

        fps_a = fp_a[None, :]
        fps_b = fps

        intersect = (fps_a & fps_b).sum(-1)
        union = (fps_a | fps_b).sum(-1)

        # Compute dist
        tani = 1 - intersect / union
        return tani


class BinnedFeaturizer(SpecFeaturizer):
    """BinnedFeaturizer"""

    def __init__(
        self,
        upper_limit: int = 1500,
        num_bins: int = 2000,
        base_folder: str = "data/paired_spectra",
        **kwargs,
    ):
        """__init__"""
        super().__init__(**kwargs)
        self.upper_limit = upper_limit
        self.num_bins = num_bins

    @staticmethod
    def collate_fn(input_list: List[dict]) -> Dict:
        """collate_fn.

        Input list of dataset outputs

        Args:
            input_list (List[Spectra]): Input list containing spectra to be
                collated
        Return:
            Dictionary containing batched results and list of how many channels are
            in each tensor
        """
        # Determines the number of channels
        names = [j["name"] for j in input_list]
        instrument_tensors = torch.FloatTensor([j["instrument"] for j in input_list])
        input_list = [j["spec"] for j in input_list]
        stacked_batch = torch.vstack([torch.tensor(spectra) for spectra in input_list])
        return_dict = {
            "spectra": stacked_batch,
            "names": names,
            "instruments": instrument_tensors,
        }
        return return_dict

    def convert_spectra_to_ar(self, spec, **kwargs) -> np.ndarray:
        """Converts the spectra to a normalized ar

        Args:
            spec

        Returns:
            np.ndarray of shape where each channel has
        """
        spectra_ar = spec.get_spec()

        binned_spec = utils.bin_spectra(
            spectra_ar, num_bins=self.num_bins, upper_limit=self.upper_limit
        )
        normed_spec = utils.norm_spectrum(binned_spec)

        # Mean over 0 channel
        normed_spec = normed_spec.mean(0)
        return normed_spec

    def _featurize(self, spec: data.Spectra, **kwargs) -> Dict:
        """featurize.

        Args:
            spec (Spectra)

        """
        # return binned spectra
        instrument = utils.get_instr_idx(spec.get_instrument())
        normed_spec = self.convert_spectra_to_ar(spec, **kwargs)
        return {
            "spec": normed_spec,
            "instrument": instrument,
            "name": spec.get_spec_name(),
        }


class MZFeaturizer(SpecFeaturizer):
    """MZFeaturizer"""

    def __init__(
        self,
        upper_limit: int = 1500,
        max_peaks: int = 50,
        base_folder: str = "data/paired_spectra",
        **kwargs,
    ):
        """__init__"""
        super().__init__(**kwargs)

        self.max_peaks = max_peaks
        self.upper_limit = upper_limit

    @staticmethod
    def collate_fn(input_list: List[dict]) -> Dict:
        """collate_fn.

        Input list of dataset outputs

        Args:
            input_list (List[Spectra]): Input list containing spectra to be
                collated
        Return:
            Dictionary containing batched results and list of how many channels are
            in each tensor
        """
        # Determines the number of channels
        names = [j["name"] for j in input_list]
        instrument_tensors = torch.FloatTensor([j["instrument"] for j in input_list])
        input_list = [torch.from_numpy(j["spec"]).float() for j in input_list]

        # Define tensor of input lens
        input_lens = torch.tensor([len(spectra) for spectra in input_list])

        # Pad the input list using torch function
        input_list_padded = torch.nn.utils.rnn.pad_sequence(
            input_list, batch_first=True, padding_value=0
        )

        return_dict = {
            "spectra": input_list_padded,
            "input_lens": input_lens,
            "names": names,
            "instruments": instrument_tensors,
        }
        return return_dict

    def convert_spectra_to_mz(self, spec, **kwargs) -> np.ndarray:
        """Converts the spectra to a normalized ar

        Args:
            spec

        Returns:
            np.ndarray of shape where each channel has
        """
        spectra_ar = spec.get_spec()
        merged = utils.merge_norm_spectra(spectra_ar)

        # Sort the merged peaks by intensity ([:, 1]) and limit to self.maxpeaks
        merged = merged[merged[:, 1].argsort()[::-1][: self.max_peaks]]

        parentmass = spec.parentmass
        # Make sure MS1 is on top with intensity 2
        merged = np.vstack([[parentmass, 2], merged])
        return merged

    def _featurize(self, spec: data.Spectra, **kwargs) -> Dict:
        """featurize.

        Args:
            spec (Spectra)

        """
        # return binned spectra
        normed_spec = self.convert_spectra_to_mz(spec, **kwargs)
        instrument = utils.get_instr_idx(spec.get_instrument())
        return {
            "spec": normed_spec,
            "name": spec.get_spec_name(),
            "instrument": instrument,
        }


class PeakFormula(SpecFeaturizer):
    """PeakFormula."""

    cat_types = {"frags": 0, "loss": 1, "ab_loss": 2, "cls": 3}
    num_inten_bins = 10
    num_types = len(cat_types)
    cls_type = cat_types.get("cls")

    num_adducts = len(utils.ION_LST)

    def __init__(
        self,
        subform_folder: str,
        forward_labels: str = None,
        augment_data: bool = False,
        augment_prob: float = 1,
        remove_prob: float = 0.1,
        remove_weights: float = "uniform",
        inten_prob: float = 0.1,
        cls_type: str = "ms1",
        magma_aux_loss: bool = False,
        magma_folder: str = None, 
        forward_aug_folder: str = None,
        max_peaks: int = None,
        inten_transform: str = "float",
        magma_modulo: int = 512,
        **kwargs,
    ):
        super().__init__(**kwargs)
        self.cls_type = cls_type
        self.forward_labels = forward_labels
        self.augment_data = augment_data
        self.remove_prob = remove_prob
        self.augment_prob = augment_prob
        self.remove_weights = remove_weights
        self.inten_prob = inten_prob
        self.magma_aux_loss = magma_aux_loss
        self.forward_aug_folder = forward_aug_folder
        self.max_peaks = max_peaks
        self.inten_transform = inten_transform
        self.aug_nbits = magma_modulo
        subform_files = list(Path(subform_folder).glob("*.json"))
        self.spec_name_to_subform_file = {i.stem: i for i in subform_files}

        if self.forward_labels is not None and self.forward_aug_folder is not None:
            self.forward_aug_folder = Path(self.forward_aug_folder)
            subform_files = self.forward_aug_folder.glob("*.json")
            self.spec_name_to_subform_file.update({i.stem: i for i in subform_files})

        self.spec_name_to_magma_file = {}
        if self.magma_aux_loss:
            self.magma_folder = Path(magma_folder)
            name_map = {
                i: self.magma_folder / f"{i}.magma"
                for i in self.spec_name_to_subform_file.keys()
            }
            self.spec_name_to_magma_file = {
                k: v for k, v in name_map.items() if v.exists()
            }

    def _get_peak_dict(self, spec: data.Spectra) -> dict:
        """_get_peak_dict.

        Args:
            spec (data.Spectra): spec

        Returns:
            dict:
        """
        spec_name = spec.get_spec_name()

        subform_file = Path(self.spec_name_to_subform_file[spec_name])

        if not subform_file.exists():
            return {}

        with open(subform_file, "r") as fp:
            tree = json.load(fp)

        root_form = tree["cand_form"]
        root_ion = tree["cand_ion"]
        output_tbl = tree["output_tbl"]

        if output_tbl is None:
            frags = []
            intens = []
            ions = []
        else:
            frags = output_tbl["formula"]
            intens = output_tbl["ms2_inten"]
            ions = output_tbl["ions"]

        out_dict = {
            "frags": frags,
            "intens": intens,
            "ions": ions,
            "root_form": root_form,
            "root_ion": root_ion,
        }

        # If we have a max peaks, then we need to filter
        if self.max_peaks is not None:

            # Sort by intensity
            inten_list = list(out_dict["intens"])

            new_order = np.argsort(inten_list)[::-1]
            cutoff_ind = min(len(inten_list) - 1, self.max_peaks)
            new_inds = new_order[:cutoff_ind]

            # Get new frags, intens, ions and assign to outdict
            inten_list = np.array(inten_list)[new_inds].tolist()
            frag_list = np.array(out_dict["frags"])[new_inds].tolist()
            ion_list = np.array(out_dict["ions"])[new_inds].tolist()

            out_dict["frags"] = frag_list
            out_dict["intens"] = inten_list
            out_dict["ions"] = ion_list

        return out_dict

    def augment_peak_dict(self, peak_dict: dict, **kwargs):
        """augment_peak_dict.

        Add peaks, remove, peaks, or rescale peaks

        Args:
            peak_dict (dict): Dictionary containing peak dict info to augment

        Return:
            peak_dict
        """

        # Only scale frags
        frags = np.array(peak_dict["frags"])
        intens = np.array(peak_dict["intens"])
        ions = np.array(peak_dict["ions"])

        # Compute removal probability
        num_modify_peaks = len(frags)  # - 1
        keep_prob = 1 - self.remove_prob
        num_to_keep = np.random.binomial(
            n=num_modify_peaks,
            p=keep_prob,
        )

        if len(frags) == 0:
            return peak_dict
        # Temp
        keep_inds = np.arange(0, num_modify_peaks)  # + 1)

        # Quadratic probability weighting
        if self.remove_weights == "quadratic":
            keep_probs = intens[0:].reshape(-1) ** 2 + 1e-9
            keep_probs = keep_probs / keep_probs.sum()
        elif self.remove_weights == "uniform":
            keep_probs = intens[0:] + 1e-9
            keep_probs = np.ones(len(keep_probs)) / len(keep_probs)
        elif self.remove_weights == "exp":
            # Temp
            # keep_probs = start_intens[1:] + 1e-9
            keep_probs = np.exp(intens[0:].reshape(-1) + 1e-5)
            keep_probs = keep_probs / keep_probs.sum()
        else:
            raise NotImplementedError()

        # Keep indices
        # Add root
        ind_samples = np.random.choice(
            keep_inds, size=num_to_keep, replace=False, p=keep_probs
        )
        # Re-index frags, intens, and ions
        frags, intens, ions = frags[ind_samples], intens[ind_samples], ions[ind_samples]

        rescale_prob = np.random.random(len(intens))
        inten_scalar_factor = np.random.normal(loc=1, size=len(intens))
        inten_scalar_factor[inten_scalar_factor <= 0] = 0

        # Where rescale prob is >= self.inten_prob set inten rescale to 1
        inten_scalar_factor[rescale_prob >= self.inten_prob] = 1

        # Rescale intens
        intens = intens * inten_scalar_factor
        new_max = intens.max() + 1e-12 if len(intens) > 0 else 1
        intens /= new_max
        # Replace peak dict with new values
        peak_dict["intens"] = intens
        peak_dict["frags"] = frags
        peak_dict["ions"] = ions

        return peak_dict

    def _featurize(
        self, spec: data.Spectra, train_mode: bool = False, **kwargs
    ) -> Dict:
        """featurize.

        Args:
            spec (Spectra)

        """
        spec_name = spec.get_spec_name()

        # Return get_peak_formulas output
        peak_dict = self._get_peak_dict(spec)

        # Augment peak dict with chem formulae
        if train_mode and self.augment_data:
            # Only augment certain select peaks
            augment_peak = np.random.random() < self.augment_prob
            if augment_peak:
                peak_dict = self.augment_peak_dict(peak_dict)

        # Add in chemical formuale
        root = peak_dict["root_form"]

        forms_vec = [utils.formula_to_dense(i) for i in peak_dict["frags"]]
        if len(forms_vec) == 0:
            mz_vec = []
        else:
            mz_vec = (np.array(forms_vec) * utils.VALID_MONO_MASSES).sum(-1).tolist()
        root_vec = utils.formula_to_dense(root)
        root_ion = utils.get_ion_idx(peak_dict["root_ion"])
        root_mass = (root_vec * utils.VALID_MONO_MASSES).sum()
        inten_vec = list(peak_dict["intens"])
        ion_vec = [utils.get_ion_idx(i) for i in peak_dict["ions"]]
        type_vec = len(forms_vec) * [self.cat_types["frags"]]
        instrument = utils.get_instr_idx(spec.get_instrument())

        if self.cls_type == "ms1":
            cls_ind = self.cat_types.get("cls")
            inten_vec.append(1.0)
            type_vec.append(cls_ind)
            forms_vec.append(root_vec)
            mz_vec.append(root_mass)
            ion_vec.append(root_ion)

        elif self.cls_type == "zeros":
            cls_ind = self.cat_types.get("cls")
            inten_vec.append(0.0)
            type_vec.append(cls_ind)
            forms_vec.append(np.zeros_like(root_vec))
            mz_vec.append(0)
            ion_vec.append(root_ion)
        else:
            raise NotImplementedError()

        # Featurize all formulae
        mz_dict = dict(zip(mz_vec, forms_vec))
        inten_vec = np.array(inten_vec)
        if self.inten_transform == "float":
            self.inten_feats = 1
        elif self.inten_transform == "zero":
            self.inten_feats = 1
            inten_vec = np.zeros_like(inten_vec)
        elif self.inten_transform == "log":
            self.inten_feats = 1
            inten_vec = np.log(inten_vec + 1e-5)
        elif self.inten_transform == "cat":
            self.inten_feats = self.num_inten_bins
            bins = np.linspace(0, 1, self.num_inten_bins)
            # Digitize inten vec
            inten_vec = np.digitize(inten_vec, bins)
        else:
            raise NotImplementedError()

        forms_vec = np.array(forms_vec)  # / utils.NORM_VEC[None, :]

        # Add in magma supervision!
        magma_file = self.spec_name_to_magma_file.get(spec_name)
        fingerprints = np.zeros((forms_vec.shape[0], self.aug_nbits)) - 1
        if self.magma_aux_loss and magma_file is not None:
            magma_df = pd.read_csv(magma_file, sep="\t")
            if len(magma_df) > 0:
                mz_vec = np.array(mz_vec)
                magma_masses = magma_df["mz_corrected"].values

                # Get closest mz within 1e-4
                diff_mat = np.abs(mz_vec[:, None] - magma_masses[None, :])
                min_inds = diff_mat.argmin(1)
                for row_ind, (mz_val, min_ind) in enumerate(zip(mz_vec, min_inds)):
                    diff_val = diff_mat[row_ind, min_ind]
                    if diff_val < 1e-4:
                        # Get fp!
                        magma_row = magma_df.iloc[min_ind]
                        magma_fp_bits = [
                            int(i) % self.aug_nbits
                            for i in magma_row["frag_fp"].split(",")
                        ]
                        magma_mass = magma_row["mz_corrected"]

                        # Set to base of 0
                        fingerprints[row_ind, :] = 0

                        # Update to 1 where active bit
                        fingerprints[row_ind, magma_fp_bits] = 1

        # Use int featurizer and norm later
        out_dict = {
            "peak_type": np.array(type_vec),
            "form_vec": forms_vec,
            "ion_vec": ion_vec,
            "frag_intens": inten_vec,
            "name": spec_name,
            "magma_fps": fingerprints,
            "magma_aux_loss": self.magma_aux_loss,
            "instrument": instrument,
        }
        return out_dict

    @classmethod
    def get_num_inten_feats(self, inten_transform):
        """_summary_

        Args:
            inten_transform (_type_): _description_

        Raises:
            NotImplementedError: _description_

        Returns:
            _type_: _description_
        """
        if inten_transform == "float":
            inten_feats = 1
        elif inten_transform == "zero":
            inten_feats = 1
        elif inten_transform == "log":
            inten_feats = 1
        elif inten_transform == "cat":
            inten_feats = PeakFormula.num_inten_bins
        else:
            raise NotImplementedError()
        return inten_feats

    def _extract_fingerprint(self, smiles):
        """extract_fingerprints."""
        index = self.fp_index_obj.get(smiles)
        return self.fp_dataset[index]

    def featurize(self, spec: data.Spectra, train_mode=False, **kwargs) -> Dict:
        """Featurizer a single object"""

        encoded_obj = self._encode(spec)
        if train_mode:
            featurized = self._featurize(spec, train_mode=train_mode)
        else:
            if self.cache_featurizers:
                if encoded_obj in self.cache:
                    featurized = self.cache[encoded_obj]
                else:
                    featurized = self._featurize(spec)
                    self.cache[encoded_obj] = featurized
            else:
                featurized = self._featurize(spec)

        return featurized

    @staticmethod
    def collate_fn(input_list: List[dict]) -> Dict:
        """_summary_

        Args:
            input_list (List[dict]): _description_

        Returns:
            Dict: _description_
        """
        # Determines the number of channels
        names = [j["name"] for j in input_list]
        peak_form_tensors = [torch.from_numpy(j["form_vec"]) for j in input_list]
        inten_tensors = [torch.from_numpy(j["frag_intens"]) for j in input_list]
        type_tensors = [torch.from_numpy(j["peak_type"]) for j in input_list]
        instrument_tensors = torch.FloatTensor([j["instrument"] for j in input_list])
        ion_tensors = [torch.FloatTensor(j["ion_vec"]) for j in input_list]

        peak_form_lens = np.array([i.shape[0] for i in peak_form_tensors])
        max_len = np.max(peak_form_lens)
        padding_amts = max_len - peak_form_lens

        type_tensors = [
            torch.nn.functional.pad(i, (0, pad_len))
            for i, pad_len in zip(type_tensors, padding_amts)
        ]
        ion_tensors = [
            torch.nn.functional.pad(i, (0, pad_len))
            for i, pad_len in zip(ion_tensors, padding_amts)
        ]
        inten_tensors = [
            torch.nn.functional.pad(i, (0, pad_len))
            for i, pad_len in zip(inten_tensors, padding_amts)
        ]
        peak_form_tensors = [
            torch.nn.functional.pad(i, (0, 0, 0, pad_len))
            for i, pad_len in zip(peak_form_tensors, padding_amts)
        ]

        # Stack everything (bxd for root, bxp for others)
        type_tensors = torch.stack(type_tensors, dim=0).long()
        peak_form_tensors = torch.stack(peak_form_tensors, dim=0).float()
        ion_tensors = torch.stack(ion_tensors, dim=0).float()

        inten_tensors = torch.stack(inten_tensors, dim=0).float()
        num_peaks = torch.from_numpy(peak_form_lens).long()

        # magma_fps
        use_magma = np.any([i["magma_aux_loss"] for i in input_list])
        magma_dict = {}
        if use_magma:
            magma_fingerprints = [i["magma_fps"] for i in input_list]

            # fingerprints: Batch x max num peaks x fingerprint dimension
            for i in range(len(magma_fingerprints)):
                padded_fp = np.zeros((max_len, magma_fingerprints[0].shape[1]))
                padded_fp[: magma_fingerprints[i].shape[0], :] = magma_fingerprints[i]
                magma_fingerprints[i] = padded_fp

            magma_fingerprints = np.stack(magma_fingerprints, axis=0)
            magma_fingerprints = torch.tensor(magma_fingerprints, dtype=torch.float)
            magma_dict["fingerprints"] = magma_fingerprints

            # Mask for where the spectra doesn't have a peak or has a peak but not a fingerprint
            fingerprint_sum = magma_fingerprints.sum(2)
            fingerprint_mask = fingerprint_sum > 0
            magma_dict["fingerprint_mask"] = fingerprint_mask

        return_dict = {
            "types": type_tensors,
            "form_vec": peak_form_tensors,
            "ion_vec": ion_tensors,
            "intens": inten_tensors,
            "names": names,
            "num_peaks": num_peaks,
            "instruments": instrument_tensors,
        }

        return_dict.update(magma_dict)
        return return_dict


class PeakFormulaTest(PeakFormula):
    """PeakFormula with no Magma"""

    def __init__(self, **kwargs):
        kwargs["magma_aux_loss"] = False
        kwargs["add_forward_specs"] = False
        super().__init__(**kwargs)