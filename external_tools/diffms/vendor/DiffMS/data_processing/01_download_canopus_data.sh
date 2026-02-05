# script adapted from MIST repo: https://github.com/samgoldman97/mist/blob/main_v2/data_processing/canopus_train/00_download_canopus_data.sh

# Original data link
#SVM_URL="https://bio.informatik.uni-jena.de/wp/wp-content/uploads/2020/08/svm_training_data.zip"

export_link="https://zenodo.org/record/8316682/files/canopus_train_export_v2.tar"

mkdir data/

cd data/
wget -O canopus_train_export.tar $export_link

tar -xvf canopus_train_export.tar
mv canopus_train_export canopus
rm -f canopus_train_export.tar