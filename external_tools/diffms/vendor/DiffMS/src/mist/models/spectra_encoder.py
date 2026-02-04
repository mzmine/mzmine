from typing import Tuple

import torch
from torch import nn

from . import modules

class SpectraEncoder(nn.Module):
    """SpectraEncoder."""
    def __init__(
        self,
        form_embedder: str = "float",
        output_size: int = 4096,
        hidden_size: int = 50,
        spectra_dropout: float = 0.0,
        top_layers: int = 1,
        refine_layers: int = 0,
        magma_modulo: int = 2048,
        **kwargs,
    ):
        super(SpectraEncoder, self).__init__()

        spectra_encoder_main = modules.FormulaTransformer(
            hidden_size=hidden_size,
            spectra_dropout=spectra_dropout,
            form_embedder=form_embedder,
            **kwargs,
        )

        fragment_pred_parts = []
        for _ in range(top_layers - 1):
            fragment_pred_parts.append(nn.Linear(hidden_size, hidden_size))
            fragment_pred_parts.append(nn.ReLU())
            fragment_pred_parts.append(nn.Dropout(spectra_dropout))

        fragment_pred_parts.append(nn.Linear(hidden_size, magma_modulo))

        fragment_predictor = nn.Sequential(*fragment_pred_parts)

        top_layer_parts = []
        for _ in range(top_layers - 1):
            top_layer_parts.append(nn.Linear(hidden_size, hidden_size))
            top_layer_parts.append(nn.ReLU())
            top_layer_parts.append(nn.Dropout(spectra_dropout))
        top_layer_parts.append(nn.Linear(hidden_size, output_size))
        top_layer_parts.append(nn.Sigmoid())
        spectra_predictor = nn.Sequential(*top_layer_parts)

        self.spectra_encoder = nn.ModuleList([spectra_encoder_main, fragment_predictor, spectra_predictor])


    def forward(self, batch: dict) -> Tuple[torch.Tensor, dict]:
        """Forward pass."""
        encoder_output, aux_out = self.spectra_encoder[0](batch, return_aux=True)

        pred_frag_fps = self.spectra_encoder[1](aux_out["peak_tensor"])
        aux_outputs = {"pred_frag_fps": pred_frag_fps}

        output = self.spectra_encoder[2](encoder_output)
        aux_outputs["h0"] = encoder_output

        return output, aux_outputs
    
    
class SpectraEncoderGrowing(nn.Module):
    """SpectraEncoder."""
    def __init__(
        self,
        form_embedder: str = "float",
        output_size: int = 4096,
        hidden_size: int = 50,
        spectra_dropout: float = 0.0,
        top_layers: int = 1,
        refine_layers: int = 0,
        magma_modulo: int = 2048,
        **kwargs,
    ):
        super(SpectraEncoderGrowing, self).__init__()

        spectra_encoder_main = modules.FormulaTransformer(
            hidden_size=hidden_size,
            spectra_dropout=spectra_dropout,
            form_embedder=form_embedder,
            **kwargs,
        )

        fragment_pred_parts = []
        for _ in range(top_layers - 1):
            fragment_pred_parts.append(nn.Linear(hidden_size, hidden_size))
            fragment_pred_parts.append(nn.ReLU())
            fragment_pred_parts.append(nn.Dropout(spectra_dropout))

        fragment_pred_parts.append(nn.Linear(hidden_size, magma_modulo))

        fragment_predictor = nn.Sequential(*fragment_pred_parts)

        spectra_predictor = modules.FPGrowingModule(
                hidden_input_dim=hidden_size,
                final_target_dim=output_size,
                num_splits=refine_layers,
                reduce_factor=2,
            )

        self.spectra_encoder = nn.ModuleList([spectra_encoder_main, fragment_predictor, spectra_predictor])

    def forward(self, batch: dict) -> Tuple[torch.Tensor, dict]:
        """Forward pass."""
        encoder_output, aux_out = self.spectra_encoder[0](batch, return_aux=True)
        pred_frag_fps = self.spectra_encoder[1](aux_out["peak_tensor"])
        aux_outputs = {"pred_frag_fps": pred_frag_fps}

        output = self.spectra_encoder[2](encoder_output)
        intermediates = output[:-1]
        final_output = output[-1]
        aux_outputs["int_preds"] = intermediates
        output = final_output
        aux_outputs["h0"] = encoder_output
        
        return output, aux_outputs # aux_outputs["int_preds"][-1]

    

