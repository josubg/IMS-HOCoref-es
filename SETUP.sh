#!/usr/bin/bash

set -ue

WD=`dirname $0`

##
## You have to specify these paths appropriately. And the conll files
## need to be there (i.e., the skeleton2conll stuff has to have been
## run).
##

TRAIN_SET_ROOT=/home/users0/anders/corpora/ontonotes5/TRAIN_DEV/conll-2012/v4/data/train/data/
DEV_SET_ROOT=/home/users0/anders/corpora/ontonotes5/TRAIN_DEV/conll-2012/v4/data/development/data/
TEST_SET_BLIND_ROOT=/home/users0/anders/corpora/ontonotes5/TEST_AUTO/conll-2012/v9/data/test/data/
TEST_SET_GOLD_ROOT=/home/users0/anders/corpora/ontonotes5/TEST_KEY/conll-2012/v4/data/test/data/

DATA_DIR=${WD}/data
mkdir -pv $DATA_DIR

source ${WD}/DATA_FILES_ENV.sh


##
## In some early version of the English training data there was a
## "bug" in the surface forms, where a single token in the tc data was
## 'nullp' instead of 'When'. The sed pipe below fixes this.
##
function catCoNLL {
    root=$1
    glob=$2
    output=$3
    IS_ENGLISH_TRAIN=0
    if [ "$output" == "${DATA_FILES[eng,train]}" ]; then
	IS_ENGLISH_TRAIN=1
    fi
    echo $root
    echo $glob
    echo $output
    echo "Writing to $output..."
    if [ $IS_ENGLISH_TRAIN -eq 1 ]; then
	find $root -name "$glob" | sort | xargs cat | sed 's#tc/ch/00/ch_0011   2   0    nullp   WRB#tc/ch/00/ch_0011   2   0     When   WRB#' > $output
    else
	find $root -name "$glob" | sort | xargs cat > $output
    fi
    echo "Created $output:"
    echo -n "  # docs:       "
    grep -c "^#begin document" $output
    echo -n "  # sentences:  "
    grep -c "^$" $output
    echo -n "  # tokens:     "
    grep -v "^#" $output | grep -c -v "^$"
}


for l in arabic chinese english; do
    echo "Creating files for $l"
    short=${l:0:3}
    catCoNLL ${TRAIN_SET_ROOT}/${l}/       "*.v4_auto_conll"   ${DATA_FILES[${short},train]}
    catCoNLL ${DEV_SET_ROOT}/${l}/         "*.v4_auto_conll"   ${DATA_FILES[${short},dev]}
    catCoNLL ${TEST_SET_BLIND_ROOT}/${l}/  "*.v9_auto_conll"   ${DATA_FILES[${short},test_auto]}
    catCoNLL ${TEST_SET_GOLD_ROOT}/${l}/   "*.v4_gold_conll"   ${DATA_FILES[${short},test_gold]}
    #Train + dev
    echo "Cat train + dev to ${DATA_FILES[${short},train+dev]}"
#    cat /dev/null >| ${DATA_FILES[${short},train+dev]}
    cat ${DATA_FILES[${short},train]} ${DATA_FILES[${short},dev]} > ${DATA_FILES[${short},train+dev]}

    echo "Done -- $l"
    echo
done
