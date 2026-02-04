"""transformer_layer.py

Hold pairwise attention enabled transformers

"""
import math
from typing import Optional, Union, Callable, Tuple

import torch
from torch import Tensor
from torch.nn import functional as F
from torch.nn import Module, LayerNorm, Linear, Dropout, Parameter
from torch.nn.init import xavier_uniform_, constant_

from torch.nn.modules.linear import NonDynamicallyQuantizableLinear


class TransformerEncoderLayer(Module):
    r"""TransformerEncoderLayer is made up of self-attn and feedforward network.
    This standard encoder layer is based on the paper "Attention Is All You Need".
    Ashish Vaswani, Noam Shazeer, Niki Parmar, Jakob Uszkoreit, Llion Jones, Aidan N Gomez,
    Lukasz Kaiser, and Illia Polosukhin. 2017. Attention is all you need. In Advances in
    Neural Information Processing Systems, pages 6000-6010. Users may modify or implement
    in a different way during application.

    Args:
        d_model: the number of expected features in the input (required).
        nhead: the number of heads in the multiheadattention models (required).
        dim_feedforward: the dimension of the feedforward network model (default=2048).
        dropout: the dropout value (default=0.1).
        activation: the activation function of the intermediate layer, can be a string
            ("relu" or "gelu") or a unary callable. Default: relu
        layer_norm_eps: the eps value in layer normalization components (default=1e-5).
        batch_first: If ``True``, then the input and output tensors are provided
            as (batch, seq, feature). Default: ``False`` (seq, batch, feature).
        norm_first: if ``True``, layer norm is done prior to attention and feedforward
            operations, respectivaly. Otherwise it's done after. Default: ``False`` (after).
        additive_attn: if ``True``, use additive attn instead of scaled dot
            product attention`
        pairwise_featurization: If ``True``
    Examples::
        >>> encoder_layer = nn.TransformerEncoderLayer(d_model=512, nhead=8)
        >>> src = torch.rand(10, 32, 512)
        >>> out = encoder_layer(src)

    Alternatively, when ``batch_first`` is ``True``:
        >>> encoder_layer = nn.TransformerEncoderLayer(d_model=512, nhead=8, batch_first=True)
        >>> src = torch.rand(32, 10, 512)
        >>> out = encoder_layer(src)
    """
    __constants__ = ["batch_first", "norm_first"]

    def __init__(
        self,
        d_model: int,
        nhead: int,
        dim_feedforward: int = 2048,
        dropout: float = 0.1,
        activation: Union[str, Callable[[Tensor], Tensor]] = F.relu,
        layer_norm_eps: float = 1e-5,
        batch_first: bool = False,
        norm_first: bool = False,
        additive_attn: bool = False,
        pairwise_featurization: bool = False,
        device=None,
        dtype=None,
    ) -> None:
        factory_kwargs = {"device": device, "dtype": dtype}
        super(TransformerEncoderLayer, self).__init__()
        self.pairwise_featurization = pairwise_featurization
        self.self_attn = MultiheadAttention(
            d_model,
            nhead,
            dropout=dropout,
            batch_first=batch_first,
            additive_attn=additive_attn,
            pairwise_featurization=self.pairwise_featurization,
            **factory_kwargs,
        )
        # Implementation of Feedforward model
        self.linear1 = Linear(d_model, dim_feedforward, **factory_kwargs)
        self.dropout = Dropout(dropout)
        self.linear2 = Linear(dim_feedforward, d_model, **factory_kwargs)

        self.norm_first = norm_first
        self.norm1 = LayerNorm(d_model, eps=layer_norm_eps, **factory_kwargs)
        self.norm2 = LayerNorm(d_model, eps=layer_norm_eps, **factory_kwargs)
        self.dropout1 = Dropout(dropout)
        self.dropout2 = Dropout(dropout)

        self.activation = activation

    def __setstate__(self, state):
        if "activation" not in state:
            state["activation"] = F.relu
        super(TransformerEncoderLayer, self).__setstate__(state)

    def forward(
        self,
        src: Tensor,
        pairwise_features: Optional[Tensor] = None,
        src_key_padding_mask: Optional[Tensor] = None,
    ) -> Tensor:
        r"""Pass the input through the encoder layer.

        Args:
            src: the sequence to the encoder layer (required).
            pairwise_features: If set, use this to param pariwise features
            src_key_padding_mask: the mask for the src keys per batch (optional).

        Shape:
            see the docs in Transformer class.
        """

        # see Fig. 1 of https://arxiv.org/pdf/2002.04745v1.pdf

        x = src
        if self.norm_first:
            x = x + self._sa_block(
                self.norm1(x), pairwise_features, src_key_padding_mask
            )
            x = x + self._ff_block(self.norm2(x))
        else:
            x = self.norm1(
                x + self._sa_block(x, pairwise_features, src_key_padding_mask)
            )
            x = self.norm2(x + self._ff_block(x))

        return x, pairwise_features

    # self-attention block
    def _sa_block(
        self,
        x: Tensor,
        pairwise_features: Optional[Tensor],
        key_padding_mask: Optional[Tensor],
    ) -> Tensor:

        ## Apply joint featurizer
        x = self.self_attn(
            x,
            x,
            x,
            key_padding_mask=key_padding_mask,
            pairwise_features=pairwise_features,
        )[0]
        return self.dropout1(x)

    # feed forward block
    def _ff_block(self, x: Tensor) -> Tensor:
        x = self.linear2(self.dropout(self.activation(self.linear1(x))))
        return self.dropout2(x)


class MultiheadAttention(Module):
    r"""Allows the model to jointly attend to information
    from different representation subspaces as described in the paper:
    `Attention Is All You Need <https://arxiv.org/abs/1706.03762>`_.

    Multi-Head Attention is defined as:

    .. math::
        \text{MultiHead}(Q, K, V) = \text{Concat}(head_1,\dots,head_h)W^O

    where :math:`head_i = \text{Attention}(QW_i^Q, KW_i^K, VW_i^V)`.

    Args:
        embed_dim: Total dimension of the model.
        num_heads: Number of parallel attention heads. Note that ``embed_dim`` will be split
            across ``num_heads`` (i.e. each head will have dimension ``embed_dim // num_heads``).
        additive_attn: If true, use additive attention instead of scaled dot
            product attention
        dropout: Dropout probability on ``attn_output_weights``. Default: ``0.0`` (no dropout).
        batch_first: If ``True``, then the input and output tensors are provided
            as (batch, seq, feature). Default: ``False`` (seq, batch, feature).
        pairwsie_featurization: If ``True``, use pairwise featurization on the
            inputs

    Examples::

        >>> multihead_attn = nn.MultiheadAttention(embed_dim, num_heads)
        >>> attn_output, attn_output_weights = multihead_attn(query, key, value)
    """

    def __init__(
        self,
        embed_dim,
        num_heads,
        additive_attn=False,
        pairwise_featurization: bool = False,
        dropout=0.0,
        batch_first=False,
        device=None,
        dtype=None,
    ) -> None:
        factory_kwargs = {"device": device, "dtype": dtype}
        super(MultiheadAttention, self).__init__()

        self.embed_dim = embed_dim
        self.kdim = embed_dim
        self.vdim = embed_dim
        self._qkv_same_embed_dim = True
        self.additive_attn = additive_attn
        self.pairwise_featurization = pairwise_featurization

        self.num_heads = num_heads
        self.dropout = dropout
        self.batch_first = batch_first
        self.head_dim = embed_dim // num_heads
        assert (
            self.head_dim * num_heads == self.embed_dim
        ), "embed_dim must be divisible by num_heads"
        if self.additive_attn:
            head_1_input = (
                self.head_dim * 3 if self.pairwise_featurization else self.head_dim * 2
            )
            self.attn_weight_1_weight = Parameter(
                torch.empty(
                    (self.num_heads, head_1_input, self.head_dim), **factory_kwargs
                ),
            )
            self.attn_weight_1_bias = Parameter(
                torch.empty((self.num_heads, self.head_dim), **factory_kwargs),
            )

            self.attn_weight_2_weight = Parameter(
                torch.empty((self.num_heads, self.head_dim, 1), **factory_kwargs),
            )
            self.attn_weight_2_bias = Parameter(
                torch.empty((self.num_heads, 1), **factory_kwargs),
            )
            # self.attn_weight_1 = Linear(head_1_input, self.head_dim)
            # self.attn_weight_2 = Linear(self.head_dim, 1)
        else:
            if self.pairwise_featurization:
                ## Bias term u
                ##
                self.bias_u = Parameter(
                    torch.empty((self.num_heads, self.head_dim), **factory_kwargs),
                )
                self.bias_v = Parameter(
                    torch.empty((self.num_heads, self.head_dim), **factory_kwargs),
                )

        self.in_proj_weight = Parameter(
            torch.empty((3 * embed_dim, embed_dim), **factory_kwargs)
        )
        self.in_proj_bias = Parameter(torch.empty(3 * embed_dim, **factory_kwargs))
        self.out_proj = NonDynamicallyQuantizableLinear(
            embed_dim, embed_dim, bias=True, **factory_kwargs
        )

        self._reset_parameters()

    def _reset_parameters(self):
        """_reset_parameters."""
        xavier_uniform_(self.in_proj_weight)
        constant_(self.in_proj_bias, 0.0)
        constant_(self.out_proj.bias, 0.0)
        if self.additive_attn:
            xavier_uniform_(self.attn_weight_1_weight)
            xavier_uniform_(self.attn_weight_2_weight)
            constant_(self.attn_weight_1_bias, 0.0)
            constant_(self.attn_weight_2_bias, 0.0)
        else:
            if self.pairwise_featurization:
                constant_(self.bias_u, 0.0)
                constant_(self.bias_v, 0.0)

    def forward(
        self,
        query: Tensor,
        key: Tensor,
        value: Tensor,
        key_padding_mask: Optional[Tensor] = None,
        pairwise_features: Optional[Tensor] = None,
    ) -> Tuple[Tensor, Optional[Tensor]]:
        r"""
        Args:
            query: Query embeddings of shape :math:`(L, E_q)` for unbatched input, :math:`(L, N, E_q)` when ``batch_first=False``
                or :math:`(N, L, E_q)` when ``batch_first=True``, where :math:`L` is the target sequence length,
                :math:`N` is the batch size, and :math:`E_q` is the query embedding dimension ``embed_dim``.
                Queries are compared against key-value pairs to produce the output.
                See "Attention Is All You Need" for more details.
            key: Key embeddings of shape :math:`(S, E_k)` for unbatched input, :math:`(S, N, E_k)` when ``batch_first=False``
                or :math:`(N, S, E_k)` when ``batch_first=True``, where :math:`S` is the source sequence length,
                :math:`N` is the batch size, and :math:`E_k` is the key embedding dimension ``kdim``.
                See "Attention Is All You Need" for more details.
            value: Value embeddings of shape :math:`(S, E_v)` for unbatched input, :math:`(S, N, E_v)` when
                ``batch_first=False`` or :math:`(N, S, E_v)` when ``batch_first=True``, where :math:`S` is the source
                sequence length, :math:`N` is the batch size, and :math:`E_v` is the value embedding dimension ``vdim``.
                See "Attention Is All You Need" for more details.
            key_padding_mask: If specified, a mask of shape :math:`(N, S)` indicating which elements within ``key``
                to ignore for the purpose of attention (i.e. treat as "padding"). For unbatched `query`, shape should be :math:`(S)`.
                Binary and byte masks are supported.
                For a binary mask, a ``True`` value indicates that the corresponding ``key`` value will be ignored for
                the purpose of attention. For a byte mask, a non-zero value indicates that the corresponding ``key``
                value will be ignored.
            pairwise_features: If specified, use this in the attention mechanism.
                Handled differently for scalar dot product and additive attn

        Outputs:
            - **attn_output** - Attention outputs of shape :math:`(L, E)` when input is unbatched,
              :math:`(L, N, E)` when ``batch_first=False`` or :math:`(N, L, E)` when ``batch_first=True``,
              where :math:`L` is the target sequence length, :math:`N` is the batch size, and :math:`E` is the
              embedding dimension ``embed_dim``.
            - **attn_output_weights** - Only returned when ``need_weights=True``. If ``average_attn_weights=True``,
              returns attention weights averaged across heads of shape :math:`(L, S)` when input is unbatched or
              :math:`(N, L, S)`, where :math:`N` is the batch size, :math:`L` is the target sequence length, and
              :math:`S` is the source sequence length. If ``average_weights=False``, returns attention weights per
              head of shape :math:`(num_heads, L, S)` when input is unbatched or :math:`(N, num_heads, L, S)`.

            .. note::
                `batch_first` argument is ignored for unbatched inputs.
        """
        is_batched = query.dim() == 3
        if self.batch_first and is_batched:
            query, key, value = [x.transpose(1, 0) for x in (query, key, value)]

        ## Here!
        attn_output, attn_output_weights = self.multi_head_attention_forward(
            query,
            key,
            value,
            self.embed_dim,
            self.num_heads,
            self.in_proj_weight,
            self.in_proj_bias,
            self.dropout,
            self.out_proj.weight,
            self.out_proj.bias,
            training=self.training,
            key_padding_mask=key_padding_mask,
            pairwise_features=pairwise_features,
        )

        if self.batch_first and is_batched:
            return attn_output.transpose(1, 0), attn_output_weights
        else:
            return attn_output, attn_output_weights

    def multi_head_attention_forward(
        self,
        query: Tensor,
        key: Tensor,
        value: Tensor,
        embed_dim_to_check: int,
        num_heads: int,
        in_proj_weight: Tensor,
        in_proj_bias: Optional[Tensor],
        dropout_p: float,
        out_proj_weight: Tensor,
        out_proj_bias: Optional[Tensor],
        training: bool = True,
        key_padding_mask: Optional[Tensor] = None,
        pairwise_features: Optional[Tensor] = None,
    ) -> Tuple[Tensor, Optional[Tensor]]:
        r"""
        Args:
            query, key, value: map a query and a set of key-value pairs to an output.
                See "Attention Is All You Need" for more details.
            embed_dim_to_check: total dimension of the model.
            num_heads: parallel attention heads.
            in_proj_weight, in_proj_bias: input projection weight and bias.
            bias_k, bias_v: bias of the key and value sequences to be added at dim=0.
            add_zero_attn: add a new batch of zeros to the key and
                           value sequences at dim=1.
            dropout_p: probability of an element to be zeroed.
            out_proj_weight, out_proj_bias: the output projection weight and bias.
            training: apply dropout if is ``True``.
            key_padding_mask: if provided, specified padding elements in the key will
                be ignored by the attention. This is an binary mask. When the value is True,
                the corresponding value on the attention layer will be filled with -inf.
            pairwise_features: If provided, include this in the MHA
        Shape:
            Inputs:
            - query: :math:`(L, E)` or :math:`(L, N, E)` where L is the target sequence length, N is the batch size, E is
              the embedding dimension.
            - key: :math:`(S, E)` or :math:`(S, N, E)`, where S is the source sequence length, N is the batch size, E is
              the embedding dimension.
            - value: :math:`(S, E)` or :math:`(S, N, E)` where S is the source sequence length, N is the batch size, E is
              the embedding dimension.
            - key_padding_mask: :math:`(S)` or :math:`(N, S)` where N is the batch size, S is the source sequence length.
              If a ByteTensor is provided, the non-zero positions will be ignored while the zero positions
              will be unchanged. If a BoolTensor is provided, the positions with the
              value of ``True`` will be ignored while the position with the value of ``False`` will be unchanged.
            Outputs:
            - attn_output: :math:`(L, E)` or :math:`(L, N, E)` where L is the target sequence length, N is the batch size,
              E is the embedding dimension.
            - attn_output_weights: Only returned when ``need_weights=True``. If ``average_attn_weights=True``, returns
              attention weights averaged across heads of shape :math:`(L, S)` when input is unbatched or
              :math:`(N, L, S)`, where :math:`N` is the batch size, :math:`L` is the target sequence length, and
              :math:`S` is the source sequence length. If ``average_weights=False``, returns attention weights per
              head of shape :math:`(num_heads, L, S)` when input is unbatched or :math:`(N, num_heads, L, S)`.
        """

        # set up shape vars
        tgt_len, bsz, embed_dim = query.shape
        src_len, _, _ = key.shape
        assert (
            embed_dim == embed_dim_to_check
        ), f"was expecting embedding dimension of {embed_dim_to_check}, but got {embed_dim}"
        if isinstance(embed_dim, torch.Tensor):
            # embed_dim can be a tensor when JIT tracing
            head_dim = embed_dim.div(num_heads, rounding_mode="trunc")
        else:
            head_dim = embed_dim // num_heads
        assert (
            head_dim * num_heads == embed_dim
        ), f"embed_dim {embed_dim} not divisible by num_heads {num_heads}"
        assert (
            key.shape == value.shape
        ), f"key shape {key.shape} does not match value shape {value.shape}"

        q, k, v = F.linear(query, in_proj_weight, in_proj_bias).chunk(3, dim=-1)

        #
        # reshape q, k, v for multihead attention and make em batch first
        #
        q = q.contiguous().view(tgt_len, bsz * num_heads, head_dim).transpose(0, 1)
        k = k.contiguous().view(k.shape[0], bsz * num_heads, head_dim).transpose(0, 1)
        v = v.contiguous().view(v.shape[0], bsz * num_heads, head_dim).transpose(0, 1)

        if pairwise_features is not None:
            # Expand pairwise features, which should have dimension the size of
            # the attn head dim
            # B x L x L x H  => L x L x (B*Nh) x (H/nh)
            pairwise_features = pairwise_features.permute(1, 2, 0, 3).contiguous()
            pairwise_features = pairwise_features.view(
                tgt_len, tgt_len, bsz * num_heads, head_dim
            )

            # L x L x (B*Nh) x (H/nh)  => (B*Nh) x L x L x (H / Nh)
            pairwise_features = pairwise_features.permute(2, 0, 1, 3)

            # Uncomment if we project into hidden dim only
            # pairwise_features = pairwise_features.repeat_interleave(self.num_heads, 0)

        # update source sequence length after adjustments
        src_len = k.size(1)

        # merge key padding and attention masks
        attn_mask = None
        if key_padding_mask is not None:
            assert key_padding_mask.shape == (
                bsz,
                src_len,
            ), f"expecting key_padding_mask shape of {(bsz, src_len)}, but got {key_padding_mask.shape}"
            key_padding_mask = (
                key_padding_mask.view(bsz, 1, 1, src_len)
                .expand(-1, num_heads, -1, -1)
                .reshape(bsz * num_heads, 1, src_len)
            )
            attn_mask = key_padding_mask
            assert attn_mask.dtype == torch.bool

        # adjust dropout probability
        if not training:
            dropout_p = 0.0

        #
        # calculate attention and out projection
        #
        if self.additive_attn:
            attn_output, attn_output_weights = self._additive_attn(
                q, k, v, attn_mask, dropout_p, pairwise_features=pairwise_features
            )
        else:
            attn_output, attn_output_weights = self._scaled_dot_product_attention(
                q, k, v, attn_mask, dropout_p, pairwise_features=pairwise_features
            )
        # Editing
        attn_output = (
            attn_output.transpose(0, 1).contiguous().view(tgt_len * bsz, embed_dim)
        )
        attn_output = F.linear(attn_output, out_proj_weight, out_proj_bias)
        attn_output = attn_output.view(tgt_len, bsz, attn_output.size(1))

        attn_output_weights = attn_output_weights.view(bsz, num_heads, tgt_len, src_len)
        return attn_output, attn_output_weights

    def _additive_attn(
        self,
        q: Tensor,
        k: Tensor,
        v: Tensor,
        attn_mask: Optional[Tensor] = None,
        dropout_p: float = 0.0,
        pairwise_features: Optional[Tensor] = None,
    ) -> Tuple[Tensor, Tensor]:
        """_additive_attn.

        Args:
            q (Tensor): q
            k (Tensor): k
            v (Tensor): v
            attn_mask (Optional[Tensor]): attn_mask
            dropout_p (float): dropout_p
            pairwise_features (Optional[Tensor]): pairwise_features

        Returns:
            Tuple[Tensor, Tensor]:
        """
        r"""
        Computes scaled dot product attention on query, key and value tensors, using
        an optional attention mask if passed, and applying dropout if a probability
        greater than 0.0 is specified.
        Returns a tensor pair containing attended values and attention weights.
        Args:
            q, k, v: query, key and value tensors. See Shape section for shape details.
            attn_mask: optional tensor containing mask values to be added to calculated
                attention. May be 2D or 3D; see Shape section for details.
            dropout_p: dropout probability. If greater than 0.0, dropout is applied.
            pairwise_features: Optional tensor for pairwise
                featurizations
        Shape:
            - q: :math:`(B, Nt, E)` where B is batch size, Nt is the target sequence length,
                and E is embedding dimension.
            - key: :math:`(B, Ns, E)` where B is batch size, Ns is the source sequence length,
                and E is embedding dimension.
            - value: :math:`(B, Ns, E)` where B is batch size, Ns is the source sequence length,
                and E is embedding dimension.
            - attn_mask: either a 3D tensor of shape :math:`(B, Nt, Ns)` or a 2D tensor of
                shape :math:`(Nt, Ns)`.
            - Output: attention values have shape :math:`(B, Nt, E)`; attention weights
                have shape :math:`(B, Nt, Ns)`
        """
        # NOTE: Consider removing position i attending to itself?

        B, Nt, E = q.shape
        # Need linear layer here :/
        # B x Nt x E => B x Nt x Nt x E
        q_expand = q[:, :, None, :].expand(B, Nt, Nt, E)
        v_expand = v[:, None, :, :].expand(B, Nt, Nt, E)
        # B x Nt x Nt x E => B x Nt x Nt x 2E
        cat_ar = [q_expand, v_expand]
        if pairwise_features is not None:
            cat_ar.append(pairwise_features)

        output = torch.cat(cat_ar, -1)
        E_long = E * len(cat_ar)

        output = output.view(-1, self.num_heads, Nt, Nt, E_long)

        # B x Nt x Nt x len(cat_ar)*E => B x Nt x Nt x E
        ## This was a fixed attn weight for each head, now separating
        # output = self.attn_weight_1(output)
        output = torch.einsum("bnlwe,neh->bnlwh", output, self.attn_weight_1_weight)

        output = output + self.attn_weight_1_bias[None, :, None, None, :]

        output = F.leaky_relu(output)

        # B x Nt x Nt x len(cat_ar)*E => B x Nt x Nt
        # attn = self.attn_weight_2(output).squeeze()
        attn = torch.einsum("bnlwh,nhi->bnlwi", output, self.attn_weight_2_weight)
        attn = attn + self.attn_weight_2_bias[None, :, None, None, :]
        attn = attn.contiguous().view(-1, Nt, Nt)
        if attn_mask is not None:
            new_attn_mask = torch.zeros_like(attn_mask, dtype=q.dtype)
            new_attn_mask.masked_fill_(attn_mask, float("-inf"))
            attn += attn_mask
        attn = F.softmax(attn, dim=-1)
        output = torch.bmm(attn, v)
        return output, attn

    def _scaled_dot_product_attention(
        self,
        q: Tensor,
        k: Tensor,
        v: Tensor,
        attn_mask: Optional[Tensor] = None,
        dropout_p: float = 0.0,
        pairwise_features: Optional[Tensor] = None,
    ) -> Tuple[Tensor, Tensor]:
        r"""
        Computes scaled dot product attention on query, key and value tensors, using
        an optional attention mask if passed, and applying dropout if a probability
        greater than 0.0 is specified.
        Returns a tensor pair containing attended values and attention weights.
        Args:
            q, k, v: query, key and value tensors. See Shape section for shape details.
            attn_mask: optional tensor containing mask values to be added to calculated
                attention. May be 2D or 3D; see Shape section for details.
            dropout_p: dropout probability. If greater than 0.0, dropout is applied.
            pairwise_features: Optional tensor for pairwise
                featurizations
        Shape:
            - q: :math:`(B, Nt, E)` where B is batch size, Nt is the target sequence length,
                and E is embedding dimension.
            - key: :math:`(B, Ns, E)` where B is batch size, Ns is the source sequence length,
                and E is embedding dimension.
            - value: :math:`(B, Ns, E)` where B is batch size, Ns is the source sequence length,
                and E is embedding dimension.
            - attn_mask: either a 3D tensor of shape :math:`(B, Nt, Ns)` or a 2D tensor of
                shape :math:`(Nt, Ns)`.
            - Output: attention values have shape :math:`(B, Nt, E)`; attention weights
                have shape :math:`(B, Nt, Ns)`
        """
        B, Nt, E = q.shape
        q = q / math.sqrt(E)

        if self.pairwise_featurization:
            ## Inspired by Graph2Smiles and TransformerXL
            # We use pairwise embedding / corrections
            if pairwise_features is None:
                raise ValueError()

            # B*Nh x Nt x E => B x Nh x Nt x E
            q = q.view(-1, self.num_heads, Nt, E)
            q_1 = q + self.bias_u[None, :, None, :]
            q_2 = q + self.bias_v[None, :, None, :]

            # B x Nh x Nt x E => B*Nh x Nt x E
            q_1 = q_1.view(-1, Nt, E)
            q_2 = q_2.view(-1, Nt, E)

            # B x Nh x Nt x E => B x Nh x Nt x Nt
            a_c = torch.einsum("ble,bwe->blw", q_1, k)

            # pairwise: B*Nh x Nt x Nt x E
            # q_2: B*Nh x Nt x E
            b_d = torch.einsum("ble,blwe->blw", q_2, pairwise_features)

            attn = a_c + b_d
        else:
            # (B, Nt, E) x (B, E, Ns) -> (B, Nt, Ns)
            attn = torch.bmm(q, k.transpose(-2, -1))

        if attn_mask is not None:
            new_attn_mask = torch.zeros_like(attn_mask, dtype=q.dtype)
            new_attn_mask.masked_fill_(attn_mask, float("-inf"))
            attn += attn_mask

        attn = F.softmax(attn, dim=-1)
        if dropout_p > 0.0:
            attn = F.dropout(attn, p=dropout_p)
        # (B, Nt, Ns) x (B, Ns, E) -> (B, Nt, E)
        output = torch.bmm(attn, v)
        return output, attn