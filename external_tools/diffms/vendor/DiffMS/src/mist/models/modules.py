""" modules.py"""
import copy
import torch
import numpy as np
from torch import nn

from ..utils.chem_utils import max_instr_idx as MAX_INSTR_IDX
from . import transformer_layer, form_embedders

EPS = 1e-9

def get_num_inten_feats(inten_transform):
    if inten_transform == "float":
        inten_feats = 1
    elif inten_transform == "zero":
        inten_feats = 1
    elif inten_transform == "log":
        inten_feats = 1
    elif inten_transform == "cat":
        inten_feats = 10 # different from original implementation, MAY BE WRONG
    else:
        raise NotImplementedError()
    return inten_feats


class MLPBlocks(nn.Module):
    def __init__(
        self,
        input_size: int,
        hidden_size: int,
        dropout: float,
        num_layers: int,
    ):
        super().__init__()
        self.activation = nn.ReLU()
        self.dropout_layer = nn.Dropout(p=dropout)
        self.input_layer = nn.Linear(input_size, hidden_size)
        middle_layer = nn.Linear(hidden_size, hidden_size)
        self.layers = _get_clones(middle_layer, num_layers - 1)

    def forward(self, x):
        output = x
        output = self.input_layer(x)
        output = self.dropout_layer(output)
        output = self.activation(output)
        for layer_index, layer in enumerate(self.layers):
            output = layer(output)
            output = self.dropout_layer(output)
            output = self.activation(output)
        return output


class FormulaTransformer(nn.Module):
    """FormulaTransformer"""

    def __init__(
        self,
        hidden_size: int,
        peak_attn_layers: int,
        set_pooling: str = "intensity",
        spectra_dropout: float = 0.1,
        pairwise_featurization: bool = False,
        num_heads: int = 8,
        output_size=2048,
        form_embedder: str = "float",
        embed_instrument: bool = False,
        inten_transform: str = "float",
        no_diffs: bool = False,
        **kwargs
    ):
        """_summary_
        Args:
                    hidden_size (int): _description_
                    peak_attn_layers (int): _description_
                    set_pooling (str, optional): _description_. Defaults to "intensity".
                        intensity: Weight by intensity of each chem formula
                        root: Only take the root node
                        cls: Only take at first class node
                        mean: Take the mean of all nodes
                    spectra_dropout (float, optional): _description_. Defaults to 0.1.
                    pairwise_featurization (bool, optional): _description_. Defaults to False.
                    num_heads (int, optional): _description_. Defaults to 8.
                    output_size (int, optional): _description_. Defaults to 2048.
                    form_embedder (str, optional): _description_. Defaults to "float".
                    embed_instrument (bool, optional): _description_. Defaults to False.
                    inten_transform (str, optional): _description_. Defaults to "float".
                    no_diffs (str): If true, do not use diff representations

                Raises:
                    ValueError: _description_
        """
        nn.Module.__init__(self)
        self.hidden_size = hidden_size
        self.attn_heads = num_heads
        self.dim_feedforward = self.hidden_size * 4
        self.spectra_dropout = spectra_dropout
        self.set_pooling = set_pooling
        self.output_size = output_size
        self.no_diffs = no_diffs

        self.form_embedder = form_embedder
        self.form_embedder_mod = form_embedders.get_embedder(self.form_embedder)

        self.embed_instrument = embed_instrument
        self.instr_dim = MAX_INSTR_IDX
        self.instrument_embedder = nn.Parameter(torch.eye(self.instr_dim))

        self.inten_transform = inten_transform
        ########### CHANGED FROM ORIGINAL IMPLEMENTATION ############
        self.inten_feats = get_num_inten_feats(
            self.inten_transform
        )
        self.num_types = 4
        self.cls_type = 3

        self.adduct_dim = 8
        ############### END OF CHANGES ###############################

        self.pairwise_featurization = pairwise_featurization

        # Define dense encoders and root formula encoder
        self.formula_dim = self.form_embedder_mod.full_dim
        self.input_dim = (
            self.formula_dim * 2
            + self.num_types
            + self.instr_dim
            + self.inten_feats
            + self.adduct_dim
        )

        self.intermediate_layer = MLPBlocks(
            input_size=self.input_dim,
            hidden_size=self.hidden_size,
            dropout=self.spectra_dropout,
            num_layers=2,
        )
        self.pairwise_featurizer = None
        if self.pairwise_featurization:
            self.pairwise_featurizer = MLPBlocks(
                input_size=self.formula_dim,
                hidden_size=self.hidden_size,
                dropout=self.spectra_dropout,
                num_layers=2,
            )

        # Multihead attention block with residuals
        peak_attn_layer = transformer_layer.TransformerEncoderLayer(
            d_model=self.hidden_size,
            nhead=self.attn_heads,
            dim_feedforward=self.dim_feedforward,
            dropout=self.spectra_dropout,
            pairwise_featurization=pairwise_featurization,
        )
        self.peak_attn_layers = _get_clones(peak_attn_layer, peak_attn_layers)
        self.bin_encoder = None

    def forward(self, batch: dict, return_aux=False):
        """forward."""
        # Step 1: Create embeddings
        num_peaks = batch["num_peaks"]
        peak_types = batch["types"]
        instruments = batch["instruments"]

        device = num_peaks.device
        batch_dim = num_peaks.shape[0]
        peak_dim = peak_types.shape[-1]
        adducts = batch["ion_vec"]

        cls_token_mask = peak_types == self.cls_type

        # Get form vec and get diffs
        orig_form_vec = batch["form_vec"][:, :, :]
        form_diffs = orig_form_vec[:, :, None, :] - orig_form_vec[:, None, :, :]

        # Embed formulae based upon peak type
        abs_diffs = form_diffs[cls_token_mask]
        form_vec = self.form_embedder_mod(orig_form_vec)
        diff_vec = self.form_embedder_mod(abs_diffs)  # .fill_(0)

        if self.no_diffs:
            diff_vec = diff_vec.fill_(0)

        intens_temp = batch["intens"]

        if self.inten_transform == "cat":
            inten_tensor = torch.eye(self.num_inten_bins + 1, device=device)[
                intens_temp.long()
            ]
        else:
            inten_tensor = intens_temp[:, :, None]

        one_hot_types = nn.functional.one_hot(peak_types, self.num_types)
        one_hot_adducts = nn.functional.one_hot(adducts.long(), self.adduct_dim)

        embedded_instruments = self.instrument_embedder[instruments.long()]
        if self.embed_instrument:
            embedded_instruments = embedded_instruments[:, None, :].repeat(
                1, peak_dim, 1
            )
        else:
            embedded_instruments = torch.zeros(batch_dim, peak_dim, self.instr_dim).to(
                device
            )

        input_vec = [
            form_vec,
            diff_vec,
            one_hot_types,
            one_hot_adducts,
            inten_tensor,
            embedded_instruments,
        ]
        input_vec = torch.cat(input_vec, dim=-1)
        peak_tensor = self.intermediate_layer(input_vec)

        # Step 3: Run transformer
        # B x Np x d -> Np x B x d
        peak_tensor = peak_tensor.transpose(0, 1)
        # Mask before summing
        peak_dim = peak_tensor.shape[0]
        peaks_aranged = torch.arange(peak_dim).to(device)

        # batch x num peaks
        attn_mask = ~(peaks_aranged[None, :] < num_peaks[:, None])
        pairwise_features = None
        if self.pairwise_featurization:
            # Make sure to _only_ consider subset fragments, rather than only
            # partial additions/subtractions from across the tree branches
            # Comment out to consider _any_ loss embedding with new parameters
            same_sign = torch.all(form_diffs >= 0, -1) | torch.all(form_diffs <= 0, -1)
            form_diffs[~same_sign].fill_(0)
            form_diffs = torch.abs(form_diffs)
            pairwise_features = self.pairwise_featurizer(
                self.form_embedder_mod(form_diffs)
            )

        # Np x B x d
        aux_output = {}
        for peak_attn_layer in self.peak_attn_layers:
            peak_tensor, pairwise_features = peak_attn_layer(
                peak_tensor,
                pairwise_features=pairwise_features,
                src_key_padding_mask=attn_mask,
            )

        # Step 4: Pool output
        output, peak_tensor = self._pool_out(
            peak_tensor, inten_tensor, peak_types, attn_mask, batch_dim
        )
        aux_output["peak_tensor"] = peak_tensor.transpose(0, 1)

        # Now convert into output dim
        if return_aux:
            output = (output, aux_output)

        return output

    def _pool_out(self, peak_tensor, inten_tensor, peak_types, attn_mask, batch_dim):
        """_pool_out.

        pool the output of the network

        Return:
            (output (B x H), peak_tensor : L x B x H)

        """

        #  Np x B x d
        zero_mask = attn_mask[:, :, None].repeat(1, 1, self.hidden_size).transpose(0, 1)

        # Mask over NaN
        peak_tensor[zero_mask] = 0
        # if torch.any(num_peaks == 0).item():
        #    raise ValueError("Unexpected example with zero peaks")
        if self.set_pooling == "intensity":
            inten_tensor = inten_tensor.reshape(batch_dim, -1)
            intensities_sum = inten_tensor.sum(1).reshape(-1, 1) + EPS
            inten_tensor = inten_tensor / intensities_sum
            pool_factor = inten_tensor * ~attn_mask
        elif self.set_pooling == "mean":
            inten_tensor = inten_tensor.reshape(batch_dim, -1)
            pool_factor = torch.clone(inten_tensor).fill_(1)
            pool_factor = pool_factor * ~attn_mask
            # Replace all zeros with 1
            pool_factor[pool_factor == 0] = 1
            pool_factor = pool_factor / pool_factor.sum(1).reshape(-1, 1)
        elif self.set_pooling == "root":
            # Reshape to have batch dim, -1
            inten_tensor = inten_tensor.reshape(batch_dim, -1)
            pool_factor = torch.zeros_like(inten_tensor)
            pool_factor[:, 0] = 1
        elif self.set_pooling == "cls":
            pool_factor = (peak_types == self.cls_type).float()
        else:
            raise NotImplementedError()

        # Weighted average over peak intensities
        output = torch.einsum("nbd,bn->bd", peak_tensor, pool_factor)
        return output, peak_tensor


class FPGrowingModule(nn.Module):
    """FPGrowingModule.

    Accept an input hidden dim and progressively grow by powers of 2 s.t.

    We eventually get to the final output size...

    """

    def __init__(
        self,
        hidden_input_dim: int = 256,
        final_target_dim: int = 4096,
        num_splits=4,
        reduce_factor=2,
    ):
        super().__init__()

        self.hidden_input_dim = hidden_input_dim
        self.final_target_dim = final_target_dim
        self.num_splits = num_splits
        self.reduce_factor = reduce_factor

        final_output_size = self.final_target_dim

        # Creates an array where we end with final_size and have num_splits + 1
        # different entries in it (e.g., num_splits = 1 with final dim 4096 has
        # [2048, 4096])
        layer_dims = [
            int(np.ceil(final_output_size / (reduce_factor**num_split)))
            for num_split in range(num_splits + 1)
        ][::-1]

        # Start by predicting into the very first layer dim (e.g., 256  -> 256)
        self.output_dims = layer_dims

        # Define initial predict module
        self.initial_predict = nn.Sequential(
            nn.Linear(
                hidden_input_dim,
                layer_dims[0],
            ),
            nn.Sigmoid(),
        )
        predict_bricks = []
        gate_bricks = []
        for layer_dim_ind, layer_dim in enumerate(layer_dims[:-1]):
            out_dim = layer_dims[layer_dim_ind + 1]

            # Need to update nn.Linear layer to be fixed if the right param is
            # called
            lin_predict = nn.Linear(layer_dim, out_dim)
            predict_brick = nn.Sequential(lin_predict, nn.Sigmoid())

            gate_bricks.append(
                nn.Sequential(nn.Linear(hidden_input_dim, out_dim), nn.Sigmoid())
            )
            predict_bricks.append(predict_brick)

        self.predict_bricks = nn.ModuleList(predict_bricks)
        self.gate_bricks = nn.ModuleList(gate_bricks)

    def forward(self, hidden):
        """forward.

        Return dict mapping output dim to the

        """
        cur_pred = self.initial_predict(hidden)
        output_preds = [cur_pred]
        for _out_dim, predict_brick, gate_brick in zip(
            self.output_dims[1:], self.predict_bricks, self.gate_bricks
        ):
            gate_outs = gate_brick(hidden)
            pred_out = predict_brick(cur_pred)
            cur_pred = gate_outs * pred_out
            output_preds.append(cur_pred)
        return output_preds


def _get_clones(module, N):
    return nn.ModuleList([copy.deepcopy(module) for i in range(N)])