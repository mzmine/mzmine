import copy

import torch
import pytorch_lightning as pl
from torch_geometric.data import Dataset
from torch_geometric.loader import DataLoader
from torch_geometric.data.lightning import LightningDataset

import src.utils as utils
from src.diffusion.distributions import DistributionNodes

def kwargs_repr(**kwargs) -> str:
    return ', '.join([f'{k}={v}' for k, v in kwargs.items() if v is not None])

class CustomLightningDataset(LightningDataset):
    def __init__(self, cfg, datasets, **kwargs):
        kwargs.pop('batch_size', None)
        self.kwargs = kwargs

        self.batch_size = cfg.train.batch_size if 'debug' not in cfg.general.name else 2
        self.eval_batch_size = cfg.train.eval_batch_size if 'debug' not in cfg.general.name else 1

        super().__init__(train_dataset=datasets['train'], val_dataset=datasets['val'], test_dataset=datasets['test'],)
        for k, v in kwargs.items(): # overwrite default kwargs from LightningDataset
            self.kwargs[k] = v
        self.kwargs.pop('batch_size', None)

    def dataloader(self, dataset: Dataset, **kwargs) -> DataLoader:
        return DataLoader(dataset, **kwargs)

    def train_dataloader(self) -> DataLoader:
        from torch.utils.data import IterableDataset

        shuffle = not isinstance(self.train_dataset, IterableDataset)
        shuffle &= self.kwargs.get('sampler', None) is None
        shuffle &= self.kwargs.get('batch_sampler', None) is None
        return self.dataloader(self.train_dataset, shuffle=shuffle, batch_size=self.batch_size, **self.kwargs)

    def val_dataloader(self) -> DataLoader:
        kwargs = copy.copy(self.kwargs)
        kwargs.pop('sampler', None)
        kwargs.pop('batch_sampler', None)

        return self.dataloader(self.val_dataset, shuffle=True, batch_size=self.eval_batch_size, **kwargs)

    def test_dataloader(self) -> DataLoader:
        kwargs = copy.copy(self.kwargs)
        kwargs.pop('sampler', None)
        kwargs.pop('batch_sampler', None)

        return self.dataloader(self.test_dataset, shuffle=False,  batch_size=self.eval_batch_size, **kwargs)

    def predict_dataloader(self) -> DataLoader:
        kwargs = copy.copy(self.kwargs)
        kwargs.pop('sampler', None)
        kwargs.pop('batch_sampler', None)

        return self.dataloader(self.pred_dataset, shuffle=False,  batch_size=self.eval_batch_size, **kwargs)

    def __repr__(self) -> str:
        kwargs = kwargs_repr(
            train_dataset=self.train_dataset,
            val_dataset=self.val_dataset,
            test_dataset=self.test_dataset,
            pred_dataset=self.pred_dataset, 
            batch_size=self.batch_size,
            eval_batch_size=self.eval_batch_size,
            **self.kwargs
        )
        
        return f'{self.__class__.__name__}({kwargs})'

class AbstractDataModule(CustomLightningDataset):
    def __init__(self, cfg, datasets):
        super().__init__(cfg, datasets, num_workers=cfg.train.num_workers, pin_memory=getattr(cfg.train, "pin_memory", True))
        self.cfg = cfg
        self.input_dims = None
        self.output_dims = None

    def __getitem__(self, idx):
        return self.train_dataset[idx]

    def node_counts(self, max_nodes_possible=150):
        all_counts = torch.zeros(max_nodes_possible)
        for loader in [self.train_dataloader(), self.val_dataloader()]:
            for data in loader:
                unique, counts = torch.unique(data.batch, return_counts=True)
                for count in counts:
                    all_counts[count] += 1
        max_index = max(all_counts.nonzero())
        all_counts = all_counts[:max_index + 1]
        all_counts = all_counts / all_counts.sum()
        return all_counts

    def node_types(self):
        num_classes = None
        for data in self.train_dataloader():
            num_classes = data.x.shape[1]
            break

        counts = torch.zeros(num_classes)

        for i, data in enumerate(self.train_dataloader()):
            counts += data.x.sum(dim=0)

        counts = counts / counts.sum()
        return counts

    def edge_counts(self):
        num_classes = None
        for data in self.train_dataloader():
            num_classes = data.edge_attr.shape[1]
            break

        d = torch.zeros(num_classes, dtype=torch.float)

        for i, data in enumerate(self.train_dataloader()):
            unique, counts = torch.unique(data.batch, return_counts=True)

            all_pairs = 0
            for count in counts:
                all_pairs += count * (count - 1)

            num_edges = data.edge_index.shape[1]
            num_non_edges = all_pairs - num_edges

            edge_types = data.edge_attr.sum(dim=0)
            assert num_non_edges >= 0
            d[0] += num_non_edges
            d[1:] += edge_types[1:]

        d = d / d.sum()
        return d


class MolecularDataModule(AbstractDataModule):
    def valency_count(self, max_n_nodes):
        valencies = torch.zeros(3 * max_n_nodes - 2)   # Max valency possible if everything is connected

        # No bond, single bond, double bond, triple bond, aromatic bond
        multiplier = torch.tensor([0, 1, 2, 3, 1.5])

        for data in self.train_dataloader():
            n = data.x.shape[0]

            for atom in range(n):
                edges = data.edge_attr[data.edge_index[0] == atom]
                edges_total = edges.sum(dim=0)
                valency = (edges_total * multiplier).sum()
                valencies[valency.long().item()] += 1
        valencies = valencies / valencies.sum()
        return valencies


class AbstractDatasetInfos:
    def complete_infos(self, n_nodes, node_types):
        self.input_dims = None
        self.output_dims = None
        self.num_classes = len(node_types)
        self.max_n_nodes = len(n_nodes) - 1
        self.nodes_dist = DistributionNodes(n_nodes)

    def compute_input_output_dims(self, datamodule, extra_features, domain_features):
        example_batch = next(iter(datamodule.train_dataloader()))
        ex_dense, node_mask = utils.to_dense(example_batch.x, example_batch.edge_index, example_batch.edge_attr,
                                             example_batch.batch)
        example_data = {'X_t': ex_dense.X, 'E_t': ex_dense.E, 'y_t': example_batch['y'], 'node_mask': node_mask}

        self.input_dims = {'X': example_batch['x'].size(1),
                           'E': example_batch['edge_attr'].size(1),
                           'y': example_batch['y'].size(1) + 1}      # + 1 due to time conditioning

        ex_extra_feat = extra_features(example_data)
        self.input_dims['X'] += ex_extra_feat.X.size(-1)
        self.input_dims['E'] += ex_extra_feat.E.size(-1)
        self.input_dims['y'] += ex_extra_feat.y.size(-1)

        ex_extra_molecular_feat = domain_features(example_data)
        self.input_dims['X'] += ex_extra_molecular_feat.X.size(-1)
        self.input_dims['E'] += ex_extra_molecular_feat.E.size(-1)
        self.input_dims['y'] += ex_extra_molecular_feat.y.size(-1)

        self.output_dims = {'X': example_batch['x'].size(1),
                            'E': example_batch['edge_attr'].size(1),
                            'y': example_batch['y'].size(1)}


ATOM_TO_VALENCY = {
    'H': 1,
    'He': 0,
    'Li': 1,
    'Be': 2,
    'B': 3,
    'C': 4,
    'N': 3,
    'O': 2,
    'F': 1,
    'Ne': 0,
    'Na': 1,
    'Mg': 2,
    'Al': 3,
    'Si': 4,
    'P': 3,
    'S': 2,
    'Cl': 1,
    'Ar': 0,
    'K': 1,
    'Ca': 2,
    'Sc': 3,
    'Ti': 4,
    'V': 5,
    'Cr': 2,
    'Mn': 7,
    'Fe': 2,
    'Co': 3,
    'Ni': 2,
    'Cu': 2,
    'Zn': 2,
    'Ga': 3,
    'Ge': 4,
    'As': 3,
    'Se': 2,
    'Br': 1,
    'Kr': 0,
    'Rb': 1,
    'Sr': 2,
    'Y': 3,
    'Zr': 2,
    'Nb': 2,
    'Mo': 2,
    'Tc': 6,
    'Ru': 2,
    'Rh': 3,
    'Pd': 2,
    'Ag': 1,
    'Cd': 1,
    'In': 1,
    'Sn': 2,
    'Sb': 3,
    'Te': 2,
    'I': 1,
    'Xe': 0,
    'Cs': 1,
    'Ba': 2,
    'La': 3,
    'Ce': 3,
    'Pr': 3,
    'Nd': 3,
    'Pm': 3,
    'Sm': 2,
    'Eu': 2,
    'Gd': 3,
    'Tb': 3,
    'Dy': 3,
    'Ho': 3,
    'Er': 3,
    'Tm': 2,
    'Yb': 2,
    'Lu': 3,
    'Hf': 4,
    'Ta': 3,
    'W': 2,
    'Re': 1,
    'Os': 2,
    'Ir': 1,
    'Pt': 1,
    'Au': 1,
    'Hg': 1,
    'Tl': 1,
    'Pb': 2,
    'Bi': 3,
    'Po': 2,
    'At': 1,
    'Rn': 0,
    'Fr': 1,
    'Ra': 2,
    'Ac': 3,
    'Th': 4,
    'Pa': 5,
    'U': 2,
}

ATOM_TO_WEIGHT = {
    'H': 1,
    'He': 4,
    'Li': 7,
    'Be': 9,
    'B': 11,
    'C': 12,
    'N': 14,
    'O': 16,
    'F': 19,
    'Ne': 20,
    'Na': 23,
    'Mg': 24,
    'Al': 27,
    'Si': 28,
    'P': 31,
    'S': 32,
    'Cl': 35,
    'Ar': 40,
    'K': 39,
    'Ca': 40,
    'Sc': 45,
    'Ti': 48,
    'V': 51,
    'Cr': 52,
    'Mn': 55,
    'Fe': 56,
    'Co': 59,
    'Ni': 59,
    'Cu': 64,
    'Zn': 65,
    'Ga': 70,
    'Ge': 73,
    'As': 75,
    'Se': 79,
    'Br': 80,
    'Kr': 84,
    'Rb': 85,
    'Sr': 88,
    'Y': 89,
    'Zr': 91,
    'Nb': 93,
    'Mo': 96,
    'Tc': 98,
    'Ru': 101,
    'Rh': 103,
    'Pd': 106,
    'Ag': 108,
    'Cd': 112,
    'In': 115,
    'Sn': 119,
    'Sb': 122,
    'Te': 128,
    'I': 127,
    'Xe': 131,
    'Cs': 133,
    'Ba': 137,
    'La': 139,
    'Ce': 140,
    'Pr': 141,
    'Nd': 144,
    'Pm': 145,
    'Sm': 150,
    'Eu': 152,
    'Gd': 157,
    'Tb': 159,
    'Dy': 163,
    'Ho': 165,
    'Er': 167,
    'Tm': 169,
    'Yb': 173,
    'Lu': 175,
    'Hf': 178,
    'Ta': 181,
    'W': 184,
    'Re': 186,
    'Os': 190,
    'Ir': 192,
    'Pt': 195,
    'Au': 197,
    'Hg': 201,
    'Tl': 204,
    'Pb': 207,
    'Bi': 209,
    'Po': 209,
    'At': 210,
    'Rn': 222,
    'Fr': 223,
    'Ra': 226,
    'Ac': 227,
    'Th': 232,
    'Pa': 231,
    'U': 238,
    'Np': 237,
    'Pu': 244,
    'Am': 243,
    'Cm': 247,
    'Bk': 247,
    'Cf': 251,
    'Es': 252,
    'Fm': 257,
    'Md': 258,
    'No': 259,
    'Lr': 262,
    'Rf': 267,
    'Db': 270,
    'Sg': 269,
    'Bh': 264,
    'Hs': 269,
    'Mt': 278,
    'Ds': 281,
    'Rg': 282,
    'Cn': 285,
    'Nh': 286,
    'Fl': 289,
    'Mc': 290,
    'Lv': 293,
    'Ts': 294,
    'Og': 294,
}