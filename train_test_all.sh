#!/bin/bash

##
## This script trains and tests for every language. 
##

set -ue

WD=`dirname $0`
source ${WD}/GLOBAL_PARAMS.sh
source ${WD}/CLASSPATH_ENV.sh
source ${WD}/DATA_FILES_ENV.sh
source ${WD}/FEATURE_FILES_ENV.sh
source ${WD}/LANG_ENV.sh
source ${WD}/SCORING_ENV.sh

PERL5LIB=`dirname ${SCORER_BIN[$DEFAULT_SCORER]}`/lib
export PERL5LIB

EXP_ROOT=${WD}/experiments

mkdir -pv $EXP_ROOT

for train_set in "train+dev" "train"; do
    for l in eng ara chi; do
	for fs in fo_opt ho_opt; do
	    #Setup
	    features=${FEATURE_FILES[${l},${fs}]}
	    TRAINING_DATA=${DATA_FILES[${l},${train_set}]}
	    prefix=${train_set}-`basename $features`
	    root=${EXP_ROOT}/${prefix}
	    mkdir -pv $root
	    model=$root/${prefix}.mdl

#	    echo "Training on $TRAINING_DATA"
#	    echo "Out root: $root"

	    IS_FO_MODEL=0
	    LEARNING_SWITCHES=""
	    TESTING_SWITCHES=""
	    if [[ $fs  =~ ^fo ]]; then 
		IS_FO_MODEL=1; 
	    else
		LEARNING_SWITCHES=${LEARNING_SWITCHES}" -delayUpdates -beamEarlyIter -beam $BEAM_SIZE"
		TESTING_SWITCHES=${TESTING_SWITCHES}" -beam $BEAM_SIZE"
	    fi

	    echo -n "Training for $prefix at "
	    date
	    echo "IS FO MODEL: $IS_FO_MODEL";
	    #Train.
	    echo ${DATA_FILES[${l},train]}
	    trainlog=${root}/train.log
	    java -classpath $CLASSPATH -Xmx${JAVA_MEM} ims.hotcoref.LearnWeights\
              -lang $l\
              -in $TRAINING_DATA\
              -model $model\
              -features $features\
              -cores $CORES \
		${TRAIN_SWITCHES[$l]} \
		$LEARNING_SWITCHES \
		2>&1 | tee $trainlog
	    #Test on dev.
	    devOut=${root}/dev.out
	    devlog=${root}/dev.out.log
	    java -cp $CLASSPATH -Xmx${JAVA_MEM} ims.hotcoref.Test\
             -model $model\
             -in ${DATA_FILES[${l},dev]}\
             -out $devOut\
             -cores $CORES\
             ${TESTING_SWITCHES}\
             2>&1 | tee $devlog
	    #Start scorer (this can run in parallel with the next job)
	    #But we call sync first to make sure the file was written
	    sync
	    sync
	    ${SCORER_BIN[$DEFAULT_SCORER]} all ${DATA_FILES[${l},dev]} $devOut none &> ${devOut}.scores.$DEFAULT_SCORER &
	    #Test on test.
	    testOut=${root}/test.out
	    testlog=${root}/test.out.log
	    java -cp $CLASSPATH -Xmx${JAVA_MEM} ims.hotcoref.Test\
             -model $model\
             -in ${DATA_FILES[${l},test_auto]}\
             -out $testOut\
             -cores $CORES\
             ${TESTING_SWITCHES}\
             2>&1 | tee $testlog
	    ##
	    ##Start scorer
	    sync
	    sync
	    ${SCORER_BIN[$DEFAULT_SCORER]} all ${DATA_FILES[${l},test_gold]} $testOut none &> ${testOut}.scores.$DEFAULT_SCORER &
	    echo
	    echo
	    echo
	done
    done
done

echo "Waiting for forked jobs..."
wait

echo
echo -n "ALL DONE at "
date
