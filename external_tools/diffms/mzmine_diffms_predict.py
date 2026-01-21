import argparse
import json
import os
import re
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import numpy as np
from rdkit import Chem


ATOM_DECODER = ["C", "O", "P", "N", "S", "Cl", "F", "H"]
ATOM_TO_IDX = {a: i for i, a in enumerate(ATOM_DECODER)}
EDGE_DIM = 5  # no bond + (single,double,triple,aromatic)


@dataclass(frozen=True)
class _DatasetInfos:
    input_dims: Dict[str, int]
    output_dims: Dict[str, int]
    nodes_dist: object
    node_types: "torch.Tensor"
    edge_types: "torch.Tensor"
    remove_h: bool
    atom_decoder: List[str]
    valencies: List[int]
    atom_weights: Dict[int, float]
    max_weight: float
    max_n_nodes: int


def _parse_formula_counts(formula: str) -> Dict[str, int]:
    if not formula or not isinstance(formula, str):
        raise ValueError("formula must be a non-empty string")
    counts: Dict[str, int] = {}
    for el, n in re.findall(r"([A-Z][a-z]*)(\d*)", formula):
        counts[el] = counts.get(el, 0) + (int(n) if n else 1)
    if not counts:
        raise ValueError(f"invalid formula: {formula}")
    return counts


def _formula_string(counts: Dict[str, int]) -> str:
    parts = []
    for el, n in counts.items():
        if n <= 0:
            continue
        parts.append(el + (str(n) if n != 1 else ""))
    if not parts:
        return ""
    return "".join(parts)


def _build_graph_from_formula(formula: str):
    import torch
    from torch_geometric.data import Data

    counts = _parse_formula_counts(formula)
    unsupported = sorted([e for e in counts.keys() if e not in ATOM_TO_IDX])
    if unsupported:
        raise ValueError(f"unsupported elements for node-types: {unsupported}")

    nodes: List[int] = []
    for el in ATOM_DECODER:
        if el == "H":
            continue
        nodes.extend([ATOM_TO_IDX[el]] * counts.get(el, 0))
    if not nodes:
        raise ValueError("formula has no supported heavy atoms for node graph")

    x = torch.nn.functional.one_hot(torch.tensor(nodes, dtype=torch.long), num_classes=len(ATOM_DECODER)).float()
    edge_index = torch.empty((2, 0), dtype=torch.long)
    edge_attr = torch.empty((0, EDGE_DIM), dtype=torch.float32)
    return Data(x=x, edge_index=edge_index, edge_attr=edge_attr)


def _cdk_friendly_smiles(mol: "Chem.Mol") -> Optional[str]:
    if mol is None:
        return None
    try:
        frags = Chem.GetMolFrags(mol, asMols=True, sanitizeFrags=True)
    except Exception:
        frags = ()
    if not frags:
        frags = (mol,)
    best = max(frags, key=lambda m: int(m.GetNumHeavyAtoms()))
    if best is None or best.GetNumHeavyAtoms() <= 0:
        return None
    try:
        s = Chem.MolToSmiles(best, isomericSmiles=False, kekuleSmiles=True)
        if s and "." not in s:
            return s
    except Exception:
        pass
    try:
        s = Chem.MolToSmiles(best, isomericSmiles=False)
        if s and "." not in s:
            return s
    except Exception:
        pass
    return None


def _load_diffms_from_dir(diffms_dir: Path):
    diffms_dir = diffms_dir.resolve()
    sys.path.insert(0, str(diffms_dir))
    sys.path.insert(0, str(diffms_dir / "src"))

    from src.diffusion_model_spec2mol import Spec2MolDenoisingDiffusion
    from src.diffusion.extra_features import ExtraFeatures
    from src.diffusion.extra_features_molecular import ExtraMolecularFeatures
    from src.diffusion.distributions import DistributionNodes
    from src.analysis.visualization import MolecularVisualization
    from src.datasets.abstract_dataset import ATOM_TO_VALENCY

    from src.mist.data.featurizers import PeakFormula
    from src.mist.utils.chem_utils import (
        VALID_ELEMENTS,
        VALID_MONO_MASSES,
        element_to_position,
        element_to_ind,
        ELEMENT_TO_MASS,
        ION_LST,
        ion_remap,
        get_ion_idx,
        get_instr_idx,
    )

    return {
        "Spec2MolDenoisingDiffusion": Spec2MolDenoisingDiffusion,
        "ExtraFeatures": ExtraFeatures,
        "ExtraMolecularFeatures": ExtraMolecularFeatures,
        "DistributionNodes": DistributionNodes,
        "MolecularVisualization": MolecularVisualization,
        "PeakFormula": PeakFormula,
        "VALID_ELEMENTS": VALID_ELEMENTS,
        "VALID_MONO_MASSES": VALID_MONO_MASSES,
        "element_to_position": element_to_position,
        "element_to_ind": element_to_ind,
        "ELEMENT_TO_MASS": ELEMENT_TO_MASS,
        "ION_LST": ION_LST,
        "ion_remap": ion_remap,
        "get_ion_idx": get_ion_idx,
        "get_instr_idx": get_instr_idx,
        "ATOM_TO_VALENCY": ATOM_TO_VALENCY,
    }


def _normalize_ion(raw: str, ion_lst: List[str], ion_remap: Dict[str, str]) -> str:
    s = "" if raw is None else str(raw).strip()
    if not s:
        raise ValueError("missing adduct/ion type")
    if s in ion_lst:
        return s
    if s in ion_remap and ion_remap[s] in ion_lst:
        return ion_remap[s]
    s2 = s.replace(" ", "")
    if s2 in ion_lst:
        return s2
    if s2 in ion_remap and ion_remap[s2] in ion_lst:
        return ion_remap[s2]
    raise ValueError(f"unsupported adduct/ion type for DiffMS: {s}")


def _pick_instrument(raw: Optional[object], get_instr_idx) -> str:
    """
    DiffMS/MIST expects instrument names from a fixed vocabulary. We try to use what MZmine provides
    (scan definition / instrument string), but fall back to a known-safe default.
    """
    default = "Unknown (LCMS)"
    if raw is None:
        return default
    s = str(raw).strip()
    if not s:
        return default
    # keep the string short to improve matching stability
    if len(s) > 120:
        s = s[:120]
    try:
        _ = get_instr_idx(s)
        return s
    except Exception:
        return default


def _load_weights(model, ckpt_path: Path):
    import torch

    checkpoint = torch.load(str(ckpt_path), map_location="cpu")
    state_dict = checkpoint["state_dict"] if "state_dict" in checkpoint else checkpoint
    model_state = model.state_dict()
    filtered = {k: v for k, v in state_dict.items() if k in model_state}
    model.load_state_dict(filtered, strict=False)
    return model


def _load_state_dict(ckpt_path: Path) -> Dict[str, "torch.Tensor"]:
    import torch

    checkpoint = torch.load(str(ckpt_path), map_location="cpu")
    if isinstance(checkpoint, dict) and "state_dict" in checkpoint:
        state_dict = checkpoint["state_dict"]
    elif isinstance(checkpoint, dict):
        state_dict = checkpoint
    else:
        raise TypeError("checkpoint must be a dict or a Lightning checkpoint with state_dict")
    if not isinstance(state_dict, dict) or not state_dict:
        raise ValueError("empty checkpoint/state_dict")
    return state_dict


def _infer_encoder_hidden_dim(state_dict: Dict[str, "torch.Tensor"]) -> int:
    w = state_dict.get("encoder.spectra_encoder.0.intermediate_layer.input_layer.weight")
    if w is None:
        return 256
    try:
        return int(w.shape[0])
    except Exception:
        return 256


# Setup globals for workers
global _worker_ctx
_worker_ctx = {}

def init_worker(diffms_dir_str, loaded_data_dict=None):
    # If we passed the data directly (fork), use it. 
    # Otherwise (spawn), reload it.
    if loaded_data_dict:
        _worker_ctx.update(loaded_data_dict)
        return

    diffms_dir = Path(diffms_dir_str)
    mod = _load_diffms_from_dir(diffms_dir)
    _worker_ctx.update(mod)

def process_item_worker(item, subform_dir_str, args_ns):
    # Unpack globals
    ION_LST = _worker_ctx["ION_LST"]
    ion_remap = _worker_ctx["ion_remap"]
    get_ion_idx = _worker_ctx["get_ion_idx"]
    get_instr_idx = _worker_ctx["get_instr_idx"]

    row_id = int(item["rowId"])
    formula = str(item["formula"])
    root_ion = _normalize_ion(item.get("adduct"), ION_LST, ion_remap)
    mzs = item["mzs"]
    intens = item["intensities"]
    
    # ... logic ...
    if len(mzs) != len(intens):
        raise ValueError(f"row {row_id}: mzs/intensities length mismatch")

    polarity = str(item.get("polarity", "POSITIVE")).upper()
    if polarity != "POSITIVE":
            raise ValueError(f"row {row_id}: only POSITIVE polarity is supported")

    instr_raw = item.get("instrument") or item.get("scanDefinition")
    instr = _pick_instrument(instr_raw, get_instr_idx)
    
    # ... preparation ...
    mzs_np = np.asarray(mzs, dtype=np.float64)
    intens_np = np.asarray(intens, dtype=np.float64)
    if mzs_np.size == 0:
        raise ValueError(f"row {row_id}: empty spectrum")
        
    order = np.argsort(intens_np)[::-1][: args_ns.max_ms2_peaks]
    mzs_np = mzs_np[order]
    intens_np = intens_np[order]
    if intens_np.max() > 0:
        intens_np = intens_np / intens_np.max()

    frag_forms = []
    frag_ints = []
    frag_ions = []
    provided = item.get("subformulas")
    
    if isinstance(provided, list) and len(provided) > 0:
        for sf in provided:
            if not isinstance(sf, dict): continue
            f = sf.get("formula")
            inten = sf.get("intensity")
            ion = sf.get("ion")
            if f is None or inten is None: continue
            f = str(f).strip()
            if not f: continue
            frag_forms.append(f)
            frag_ints.append(float(inten))
            frag_ions.append(_normalize_ion(ion, ION_LST, ion_remap) if ion else root_ion)

    if frag_ints:
        imax = max(frag_ints)
        if imax > 0:
            frag_ints = [x / imax for x in frag_ints]

    # Write result
    tree = {"cand_form": formula, "cand_ion": root_ion,
            "output_tbl": {"formula": frag_forms, "ms2_inten": frag_ints, "ions": frag_ions}}
    
    subform_dir = Path(subform_dir_str)
    subform_file = subform_dir / f"{row_id}.json"
    subform_file.write_text(json.dumps(tree))
    
    # Build graph (PyTorch)
    # Note: torch objects across processes usually require spawn or careful handling.
    # But Data object is just tensors.
    g = _build_graph_from_formula(formula)
    
    # Return everything needed to reconstruction _Spec and for later steps
    # We return a dict to avoid pickle issues with custom classes if any
    # We preserve the item but updated/normalized
    
    # We need the normalized instrument, etc. for _Spec
    meta = {
        "rowId": row_id,
        "formula": formula,
        "root_ion": root_ion,
        "instr": instr,
        "collision_energy": item.get("collisionEnergy"),
        "activation_method": item.get("activationMethod"),
        "precursor_mz": item.get("precursorMz"),
        "precursor_charge": item.get("precursorCharge"),
        
        # Logging info
        "ms2Peaks": int(mzs_np.size),
        "subformulasProvided": isinstance(provided, list) and len(provided) > 0,
        "subformulasUsed": len(frag_forms),
        "subformulasExample": ",".join(frag_forms[:5])
    }
    
    return meta, g


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--diffms-dir", required=True)
    p.add_argument("--checkpoint", required=True)
    p.add_argument("--input", required=True)
    p.add_argument("--output", required=True)
    p.add_argument("--top-k", type=int, default=10)
    p.add_argument("--max-ms2-peaks", type=int, default=50)
    p.add_argument("--subformula-tol", type=float, default=0.02)
    p.add_argument("--batch-size", type=int, default=8)
    p.add_argument("--device", default="cpu")
    args = p.parse_args()

    diffms_dir = Path(args.diffms_dir)
    ckpt_path = Path(args.checkpoint)
    in_path = Path(args.input)
    out_path = Path(args.output)

    if not diffms_dir.is_dir():
        raise FileNotFoundError(diffms_dir)
    if not ckpt_path.is_file():
        raise FileNotFoundError(ckpt_path)
    if not in_path.is_file():
        raise FileNotFoundError(in_path)

    mod = _load_diffms_from_dir(diffms_dir)
    Spec2MolDenoisingDiffusion = mod["Spec2MolDenoisingDiffusion"]
    ExtraFeatures = mod["ExtraFeatures"]
    ExtraMolecularFeatures = mod["ExtraMolecularFeatures"]
    DistributionNodes = mod["DistributionNodes"]
    MolecularVisualization = mod["MolecularVisualization"]
    PeakFormula = mod["PeakFormula"]
    ION_LST = mod["ION_LST"]
    ion_remap = mod["ion_remap"]
    get_ion_idx = mod["get_ion_idx"]
    get_instr_idx = mod["get_instr_idx"]
    ATOM_TO_VALENCY = mod["ATOM_TO_VALENCY"]

    import torch
    from torch_geometric.data import Batch, Data
    from omegaconf import OmegaConf
    from concurrent.futures import ProcessPoolExecutor
    import multiprocessing

    device = torch.device(args.device)

    # Workers are now defined at top-level

    state_dict = _load_state_dict(ckpt_path)
    enc_hidden = _infer_encoder_hidden_dim(state_dict)
    enc_magma_modulo = 2048 if enc_hidden >= 512 else 512

    cfg = OmegaConf.create(
        {
            "general": {
                "name": "mzmine_diffms",
                "gpus": 1 if device.type == "cuda" else 0,
                "decoder": None,
                "encoder": None,
                "load_weights": str(ckpt_path),
                "val_samples_to_generate": args.top_k,
                "test_samples_to_generate": args.top_k,
                "log_every_steps": 50,
            },
            "dataset": {
                "denoise_nodes": False,
                "merge": "downproject_4096",
                "morgan_nbits": 2048,
                "remove_h": False,
            },
            "model": {
                "transition": "marginal",
                "diffusion_steps": 500,
                "diffusion_noise_schedule": "cosine",
                "n_layers": 5,
                "hidden_mlp_dims": {"X": 256, "E": 128, "y": 2048},
                "hidden_dims": {
                    "dx": 256,
                    "de": 64,
                    "dy": 1024,
                    "n_head": 8,
                    "dim_ffX": 256,
                    "dim_ffE": 128,
                    "dim_ffy": 1024,
                },
                "encoder_hidden_dim": enc_hidden,
                "encoder_magma_modulo": enc_magma_modulo,
                "lambda_train": [0, 1, 0],
            },
            "train": {"scheduler": "const", "lr": 0.001, "weight_decay": 0.0},
        }
    )

    node_dim = len(ATOM_DECODER)
    edge_dim = EDGE_DIM
    y_dim = 2048

    extra_x_dim = 6
    extra_y_dim = 11
    mol_x_dim = 2
    mol_y_dim = 1
    t_dim = 1
    input_dims = {"X": node_dim + extra_x_dim + mol_x_dim, "E": edge_dim, "y": y_dim + extra_y_dim + mol_y_dim + t_dim}
    output_dims = {"X": node_dim, "E": edge_dim, "y": y_dim}

    max_n_nodes = 150
    node_types = torch.ones(node_dim)
    edge_types = torch.tensor([0.95, 0.03, 0.01, 0.005, 0.005], dtype=torch.float32)
    nodes_hist = torch.zeros(max_n_nodes + 1)
    nodes_hist[10] = 1.0
    nodes_dist = DistributionNodes(nodes_hist)

    valencies = [ATOM_TO_VALENCY.get(a, 0) for a in ATOM_DECODER]
    pt = Chem.GetPeriodicTable()
    atom_weights = {ATOM_TO_IDX[a]: float(pt.GetMostCommonIsotopeMass(a)) for a in ATOM_DECODER}
    max_weight = max(atom_weights.values())

    infos = _DatasetInfos(
        input_dims=input_dims,
        output_dims=output_dims,
        nodes_dist=nodes_dist,
        node_types=node_types,
        edge_types=edge_types,
        remove_h=False,
        atom_decoder=list(ATOM_DECODER),
        valencies=valencies,
        atom_weights=atom_weights,
        max_weight=max_weight,
        max_n_nodes=max_n_nodes,
    )

    extra_features = ExtraFeatures("all", dataset_info=infos)
    domain_features = ExtraMolecularFeatures(dataset_infos=infos)
    visualization_tools = MolecularVisualization(False, dataset_infos=infos)
    train_metrics = object()

    model = Spec2MolDenoisingDiffusion(
        cfg=cfg,
        dataset_infos=infos,
        train_metrics=train_metrics,
        visualization_tools=visualization_tools,
        extra_features=extra_features,
        domain_features=domain_features,
    )
    model_state = model.state_dict()
    filtered = {k: v for k, v in state_dict.items() if k in model_state}
    model.load_state_dict(filtered, strict=False)
    model.eval()
    model.to(device)

    with open(in_path, "r") as f:
        req = json.load(f)
    if not isinstance(req, list):
        raise ValueError("input must be a JSON list")

    with tempfile.TemporaryDirectory(prefix="mzmine_diffms_") as tmp:
        tmp = Path(tmp)
        subform_dir = tmp / "subform"
        subform_dir.mkdir(parents=True, exist_ok=True)

        results = []
        n_total = len(req)
        print("MZMINE_DIFFMS_STAGE model_ready", file=sys.stderr, flush=True)
        print(f"MZMINE_DIFFMS_PROGRESS 0/{n_total}", file=sys.stderr, flush=True)

        peak_featurizer = PeakFormula(
            subform_folder=str(subform_dir),
            augment_data=False,
            remove_prob=0.0,
            inten_prob=0.0,
            cls_type="ms1",
            magma_aux_loss=False,
            max_peaks=args.max_ms2_peaks,
            inten_transform="float",
            cache_featurizers=False,
        )

        instr = "Unknown (LCMS)"
        # instrument might be updated per-row; keep a default that always works
        _ = get_instr_idx(instr)

        # Initialize worker context for main process too, just in case or for sequential fallback
        _worker_ctx.update(mod)

        n_total = len(req)
        valid_data_list = []

        # Heuristic: for very few items, multiprocessing overhead (spawn) outweighs benefits.
        if n_total < 8:
             print(f"MZMINE_DIFFMS_STAGE preprocessing_sequential", file=sys.stderr, flush=True)
             for i, item in enumerate(req):
                 try:
                     # Passing args (Namespace) is fine
                     res = process_item_worker(item, str(subform_dir), args)
                     valid_data_list.append(res)
                 except Exception as e:
                     row_id = item.get("rowId", "unknown")
                     print(f"MZMINE_DIFFMS_LOG Failed to preprocess row {row_id}: {e}", file=sys.stderr, flush=True)
        else:
            # Pre-process all items in parallel to utilize CPU
            import multiprocessing
            # Use 80% of CPUs or at least 1
            num_workers = max(1, int(os.cpu_count() * 0.8))
            
            print(f"MZMINE_DIFFMS_STAGE preprocessing_with_{num_workers}_workers", file=sys.stderr, flush=True)
            
            # We need to determine if we are forking or spawning
            ctx = multiprocessing.get_context('spawn') 
            
            with ProcessPoolExecutor(max_workers=num_workers, mp_context=ctx, 
                                     initializer=init_worker, 
                                     initargs=(str(diffms_dir), None)) as executor:
                
                # Submit all
                futures = [executor.submit(process_item_worker, item, str(subform_dir), args) for item in req]
                
                for i, fut in enumerate(futures):
                    try:
                        res = fut.result()
                        # res is (meta, graph)
                        valid_data_list.append(res)
                        
                        # Optional: Log progress of preprocessing
                        if (i + 1) % 10 == 0:
                             print(f"MZMINE_DIFFMS_LOG Preprocessed {i+1}/{n_total} items", file=sys.stderr, flush=True)

                    except Exception as e:
                        # Log failure but continue?
                        # We should probably skip this row
                        row_id = req[i].get("rowId", "unknown")
                        print(f"MZMINE_DIFFMS_LOG Failed to preprocess row {row_id}: {e}", file=sys.stderr, flush=True)
                        continue

        print("MZMINE_DIFFMS_STAGE inference", file=sys.stderr, flush=True)

        done = 0
        batch_size = args.batch_size
        
        # Now process the valid items in batches for inference
        # valid_data_list contains tuples (meta, graph)
        
        for batch_start in range(0, len(valid_data_list), batch_size):
            batch_tuples = valid_data_list[batch_start : batch_start + batch_size]
            
            feats = []
            graphs = []
            batch_metas = []

            for meta, g in batch_tuples:
                row_id = meta["rowId"]
                
                # Register the subformula file for this row (already written by worker)
                subform_file = subform_dir / f"{row_id}.json"
                if hasattr(peak_featurizer, "spec_name_to_subform_file"):
                    peak_featurizer.spec_name_to_subform_file[str(row_id)] = str(subform_file)
                
                # Define _Spec class locally if needed, or reuse one. 
                # Since we are in main loop now, we can define it or reuse.
                # To keep it simple, we define a quick object or class.
                class _Spec:
                    def __init__(self, m): self.m = m
                    def get_spec_name(self, **_): return str(self.m["rowId"])
                    def get_instrument(self): return self.m["instr"]
                    def get_collision_energy(self): return self.m["collision_energy"]
                    def get_activation_method(self): return self.m["activation_method"]
                    def get_precursor_mz(self): return self.m["precursor_mz"]
                    def get_precursor_charge(self): return self.m["precursor_charge"]
                
                spec = _Spec(meta)
                
                # Featurize (fast enough to be sequential usually, or at least hard to pickle)
                try:
                    feat = peak_featurizer.featurize(spec)
                    feats.append(feat)
                    graphs.append(g)
                    batch_metas.append(meta)
                    
                    # Log as before
                    print(
                        f"MZMINE_DIFFMS_LOG rowId={row_id} formula={meta['formula']} adduct={meta['root_ion']} "
                        f"instrument={meta['instr']} "
                        f"ms2Peaks={meta['ms2Peaks']} subformulasProvided={meta['subformulasProvided']} "
                        f"subformulasUsed={meta['subformulasUsed']} ",
                        file=sys.stderr,
                        flush=True,
                    )
                except Exception as e:
                     print(f"MZMINE_DIFFMS_LOG Failed to featurize row {row_id}: {e}", file=sys.stderr, flush=True)
                     continue

            if not feats:
                continue

            # Batch Processing
            feat_batch = PeakFormula.collate_fn(feats)
            feat_batch = {k: (v.to(device) if hasattr(v, "to") else v) for k, v in feat_batch.items()}
            
            enc_out, _ = model.encoder(feat_batch)
            y_batch = model.merge_function(enc_out)

            top_k = int(args.top_k)
            all_graphs = []
            for g in graphs:
                all_graphs.extend([g] * top_k)
            
            data_batch = Batch.from_data_list(all_graphs).to(device)
            # y_batch is [B, dim], we need [B*top_k, dim]
            data_batch.y = y_batch.repeat_interleave(top_k, dim=0)

            with torch.no_grad():
                mols_flat = model.sample_batch(data_batch)

            # Distribute results
            for i, meta in enumerate(batch_metas):
                row_id = meta["rowId"]
                
                start_idx = i * top_k
                end_idx = (i + 1) * top_k
                
                mols = mols_flat[start_idx:end_idx] if mols_flat else []
                smiles_out: List[str] = []
                
                if mols:
                    for mol in mols:
                        if mol is None:
                            continue
                        smi = _cdk_friendly_smiles(mol)
                        if smi:
                            smiles_out.append(smi)

                res_item = {"rowId": row_id, "smiles": smiles_out}
                results.append(res_item)
                print(f"MZMINE_DIFFMS_RESULT_JSON {json.dumps(res_item)}", file=sys.stderr, flush=True)
                done += 1
                print(f"MZMINE_DIFFMS_PROGRESS {done}/{n_total}", file=sys.stderr, flush=True)

        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(json.dumps(results))


if __name__ == "__main__":
    main()

