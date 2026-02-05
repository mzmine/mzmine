"""splitter.py"""

from pathlib import Path
from typing import List, Tuple, Iterator
import pandas as pd
import numpy as np

from .data import Spectra, Mol

DATASET = List[Tuple[Spectra, Mol]]


def get_splitter(**kwargs):
    """_summary_

    Returns:
        _type_: _description_
    """
    return {"preset": PresetSpectraSplitter,}[
        "preset"
    ](**kwargs)


class SpectraSplitter(object):
    """SpectraSplitter."""

    def __init__(
        self,
        **kwargs,
    ):
        """_summary_

        Returns:
            _type_: _description_
        """

    pass

    def split_from_indices(
        self,
        full_dataset: DATASET,
        train_inds: np.ndarray,
        val_inds: np.ndarray,
        test_inds: np.ndarray,
    ) -> Tuple[DATASET]:
        """_summary_

        Args:
            full_dataset (DATASET): _description_
            train_inds (np.ndarray): _description_
            val_inds (np.ndarray): _description_
            test_inds (np.ndarray): _description_

        Returns:
            Tuple[DATASET]: _description_
        """
        full_dataset = np.array(full_dataset)
        train_sub = full_dataset[train_inds].tolist()
        val_sub = full_dataset[val_inds].tolist()
        test_sub = full_dataset[test_inds].tolist()
        return (train_sub, val_sub, test_sub)


class PresetSpectraSplitter(SpectraSplitter):
    """PresetSpectraSplitter."""

    def __init__(self, split_file: str = None, **kwargs):
        """_summary_

        Args:
            split_file (str, optional): _description_. Defaults to None.

        Raises:
            ValueError: _description_
        """
        super().__init__(**kwargs)
        if split_file is None:
            raise ValueError("Preset splitter requires split_file arg.")

        self.split_file = split_file
        self.split_name = Path(split_file).stem
        self.split_df = pd.read_csv(self.split_file, sep="\t")
        self.name_to_fold = dict(zip(self.split_df["name"], self.split_df["split"]))

    def get_splits(self, full_dataset: DATASET) -> Iterator[Tuple[str, Tuple[DATASET]]]:
        """_summary_

        Args:
            full_dataset (DATASET): _description_

        Returns:
            _type_: _description_

        Yields:
            Iterator[Tuple[str, Tuple[DATASET]]]: _description_
        """
        # Map name to index
        spec_names = [i.get_spec_name() for i, j in full_dataset]
        train_inds = [
            i for i, j in enumerate(spec_names) if self.name_to_fold.get(j) == "train"
        ]
        val_inds = [
            i for i, j in enumerate(spec_names) if self.name_to_fold.get(j) == "val"
        ]
        test_inds = [
            i for i, j in enumerate(spec_names) if self.name_to_fold.get(j) == "test"
        ]
        new_split = self.split_from_indices(
            full_dataset, train_inds, val_inds, test_inds
        )
        return (self.split_name, new_split)