for dataset in hmdb dss coconut moses canopus msg combined
do
    mkdir data/fp2mol/$dataset/
    mkdir data/fp2mol/$dataset/preprocessed/
    mkdir data/fp2mol/$dataset/processed/
    mkdir data/fp2mol/$dataset/stats/
done

cd data_processing/
python build_fp2mol_datasets.py