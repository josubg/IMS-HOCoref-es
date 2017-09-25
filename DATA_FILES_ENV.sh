#!/usr/bin/bash

##
## Paths to all data files
##

set -ue

declare -A DATA_FILES

DATA_DIR=`dirname $0`/data/

for l in arabic chinese english; do
    short=${l:0:3}
    DATA_FILES[${short},train]=$DATA_DIR/${short}_train_v4_auto_conll
    DATA_FILES[${short},dev]=$DATA_DIR/${short}_dev_v4_auto_conll
    DATA_FILES[${short},test_auto]=$DATA_DIR/${short}_test_v9_auto_conll
    DATA_FILES[${short},test_gold]=$DATA_DIR/${short}_test_v4_gold_conll
    DATA_FILES[${short},train+dev]=${DATA_FILES[${short},train]}+`basename ${DATA_FILES[${short},dev]}`
done

export DATA_FILES
