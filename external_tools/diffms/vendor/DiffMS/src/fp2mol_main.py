import os
import sys
import pathlib
import warnings
import logging

import torch
torch.cuda.empty_cache()
try:
    torch.set_float32_matmul_precision('medium')
    logging.info("Enabled float32 matmul precision - medium")
except:
    logging.info("Could not enable float32 matmul precision - medium")
import hydra
from omegaconf import DictConfig
from pytorch_lightning import Trainer
from pytorch_lightning.callbacks import ModelCheckpoint, LearningRateMonitor
from pytorch_lightning.loggers import CSVLogger, WandbLogger
from pytorch_lightning.utilities.warnings import PossibleUserWarning

from src import utils
from src.diffusion_model_fp2mol import FP2MolDenoisingDiffusion
from src.diffusion.extra_features import DummyExtraFeatures, ExtraFeatures


warnings.filterwarnings("ignore", category=PossibleUserWarning)


def get_resume(cfg, model_kwargs):
    """ Resumes a run. It loads previous config without allowing to update keys (used for testing). """
    saved_cfg = cfg.copy()
    name = cfg.general.name + '_resume'
    resume = cfg.general.test_only
    val_samples_to_generate = cfg.general.val_samples_to_generate
    test_samples_to_generate = cfg.general.test_samples_to_generate
    if cfg.model.type == 'discrete':
        model = FP2MolDenoisingDiffusion.load_from_checkpoint(resume, **model_kwargs)
    else:
        raise NotImplementedError("Only discrete diffusion models are supported for FP2Mol dataset currently")
    cfg = model.cfg
    cfg.general.test_only = resume
    cfg.general.name = name
    cfg.general.val_samples_to_generate = val_samples_to_generate
    cfg.general.test_samples_to_generate = test_samples_to_generate
    cfg = utils.update_config_with_new_keys(cfg, saved_cfg)
    return cfg, model


def get_resume_adaptive(cfg, model_kwargs):
    """ Resumes a run. It loads previous config but allows to make some changes (used for resuming training)."""
    saved_cfg = cfg.copy()
    # Fetch path to this file to get base path
    current_path = os.path.dirname(os.path.realpath(__file__))
    root_dir = current_path.split('outputs')[0]

    resume_path = os.path.join(root_dir, cfg.general.resume)

    model = FP2MolDenoisingDiffusion.load_from_checkpoint(resume_path, **model_kwargs)
    new_cfg = model.cfg

    for category in cfg:
        for arg in cfg[category]:
            new_cfg[category][arg] = cfg[category][arg]

    new_cfg.general.resume = resume_path
    new_cfg.general.name = new_cfg.general.name + '_resume'

    new_cfg = utils.update_config_with_new_keys(new_cfg, saved_cfg)
    return new_cfg, model

def load_decoder_from_lightning_ckpt(model, ckpt_path):
    """ Load a model from a PyTorch Lightning checkpoint. """
    state_dict = torch.load(ckpt_path, map_location='cpu')["state_dict"]
    cleaned_state_dict = {}
    for k, v in state_dict.items():
        if k.startswith('model.'):
            k = k[6:]
            cleaned_state_dict[k] = v

    model.model.load_state_dict(cleaned_state_dict, strict=True)
    logging.info(f"Loaded model from: '{ckpt_path}'")


@hydra.main(version_base='1.3', config_path='../configs', config_name='config')
def main(cfg: DictConfig):
    from rdkit import RDLogger
    RDLogger.DisableLog('rdApp.*')

    logger = logging.getLogger("msms_main")
    logger.setLevel(logging.INFO)

    formatter = logging.Formatter(
        "%(asctime)s.%(msecs)03d %(levelname)s: %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )

    ch = logging.StreamHandler(stream=sys.stdout)
    ch.setFormatter(formatter)
    logger.addHandler(ch)

    path = os.path.join("msms_main.log")
    fh = logging.FileHandler(path)
    fh.setFormatter(formatter)

    logger.addHandler(fh)

    logging.info(cfg)

    dataset_config = cfg["dataset"]

    if dataset_config["name"] != "fp2mol":
        raise NotImplementedError("Unknown dataset {}".format(cfg["dataset"]))
    
    from metrics.molecular_metrics import TrainMolecularMetrics, SamplingMolecularMetrics
    from metrics.molecular_metrics_discrete import TrainMolecularMetricsDiscrete
    from diffusion.extra_features_molecular import ExtraMolecularFeatures
    from analysis.visualization import MolecularVisualization

    from datasets import fp2mol_dataset
        
    datamodule = fp2mol_dataset.FP2MolDataModule(cfg)
    logging.info("Dataset loaded")
    logging.info(f"Train Size: {len(datamodule.train_dataloader())}, Val Size: {len(datamodule.val_dataloader())}, Test Size: {len(datamodule.test_dataloader())}")
    dataset_infos = fp2mol_dataset.FP2Mol_infos(datamodule, cfg, recompute_statistics=False)

    domain_features = ExtraMolecularFeatures(dataset_infos=dataset_infos)
    if cfg.model.extra_features is not None:
        extra_features = ExtraFeatures(cfg.model.extra_features, dataset_info=dataset_infos)
    else:
        extra_features = DummyExtraFeatures()

    dataset_infos.compute_input_output_dims(datamodule=datamodule, extra_features=extra_features, domain_features=domain_features)

    logging.info("Dataset infos:", dataset_infos.output_dims)
    train_metrics = TrainMolecularMetricsDiscrete(dataset_infos)

    visualization_tools = MolecularVisualization(cfg.dataset.remove_h, dataset_infos=dataset_infos)

    model_kwargs = {'dataset_infos': dataset_infos, 'train_metrics': train_metrics,
                    'visualization_tools': visualization_tools, 'extra_features': extra_features, 'domain_features': domain_features}

    if cfg.general.test_only:
        # When testing, previous configuration is fully loaded
        cfg, _ = get_resume(cfg, model_kwargs)
        os.chdir(cfg.general.test_only.split('checkpoints')[0])
    elif cfg.general.resume is not None:
        # When resuming, we can override some parts of previous configuration
        cfg, _ = get_resume_adaptive(cfg, model_kwargs)
        try:
            os.chdir(cfg.general.resume.split('checkpoints')[0])
        except:
            logging.info("Could not change directory to resume path. Using current directory.")

    os.makedirs('preds/', exist_ok=True)
    os.makedirs('models/', exist_ok=True)
    os.makedirs('logs/', exist_ok=True)
    os.makedirs('logs/' + cfg.general.name, exist_ok=True)

    model = FP2MolDenoisingDiffusion(cfg=cfg, **model_kwargs)
            
    callbacks = []
    callbacks.append(LearningRateMonitor(logging_interval='step'))
    if cfg.train.save_model:
        checkpoint_callback = ModelCheckpoint(dirpath=f"checkpoints/{cfg.general.name}", # best (top-5) checkpoints
                                              filename='{epoch}',
                                              monitor='val/NLL',
                                              save_top_k=5,
                                              mode='min',
                                              every_n_epochs=1)
        last_ckpt_save = ModelCheckpoint(dirpath=f"checkpoints/{cfg.general.name}", filename='last', every_n_epochs=1) # most recent checkpoint
        callbacks.append(last_ckpt_save)
        callbacks.append(checkpoint_callback)

    if cfg.train.ema_decay > 0: # TODO: Implement EMA for FP2Mol
        ema_callback = utils.EMA(decay=cfg.train.ema_decay)
        callbacks.append(ema_callback)

    name = cfg.general.name
    if name == 'debug':
        logging.warning("Run is called 'debug' -- it will run with fast_dev_run. ")

    loggers = [
        CSVLogger(save_dir=f"logs/{name}", name=name),
        WandbLogger(name=name, save_dir=f"logs/{name}", project=cfg.general.wandb_name, log_model=False, config=utils.cfg_to_dict(cfg))
    ]

    use_gpu = cfg.general.gpus > 0 and torch.cuda.is_available()
    trainer = Trainer(gradient_clip_val=cfg.train.clip_grad,
                      strategy="ddp",
                      accelerator='gpu' if use_gpu else 'cpu',
                      devices=cfg.general.gpus if use_gpu else 1,
                      max_epochs=cfg.train.n_epochs,
                      check_val_every_n_epoch=cfg.general.check_val_every_n_epochs,
                      fast_dev_run=cfg.general.name == 'debug',
                      callbacks=callbacks,
                      log_every_n_steps=50 if name != 'debug' else 1,
                      logger=loggers)

    if not cfg.general.test_only:
        trainer.fit(model, datamodule=datamodule, ckpt_path=cfg.general.resume)
        if cfg.general.name not in ['debug', 'test']:
            trainer.test(model, datamodule=datamodule)
    else:
        # Start by evaluating test_only_path
        trainer.test(model, datamodule=datamodule, ckpt_path=cfg.general.test_only)
        if cfg.general.evaluate_all_checkpoints:
            directory = pathlib.Path(cfg.general.test_only).parents[0]
            logging.info("Directory:", directory)
            files_list = os.listdir(directory)
            for file in files_list:
                if '.ckpt' in file:
                    ckpt_path = os.path.join(directory, file)
                    if ckpt_path == cfg.general.test_only:
                        continue
                    logging.info("Loading checkpoint", ckpt_path)
                    trainer.test(model, datamodule=datamodule, ckpt_path=ckpt_path)


if __name__ == '__main__':
    main()
