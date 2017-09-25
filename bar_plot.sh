#!/bin/bash

##
## This does the learning curves from Figure 3
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


root=${WD}/experiments/bp
foroot=${root}/fo
horoot=${root}/ho

mkdir -pv $foroot
mkdir -pv $horoot

function doit {
    eroot=$1
    fs=$2
    switches=$3
    bsize=$4
    echo "Running $eroot"
    mkdir -pv $eroot
    devOut=${eroot}/dev.out
    java -cp $CLASSPATH -Xmx${JAVA_MEM} ims.hotcoref.LearnWeights\
         -lang eng\
         -in ${DATA_FILES[eng,train]}\
         -cores $CORES\
         -features ${FEATURE_FILES[eng,$fs]}\
         -model $eroot/hotcoref.mdl\
         -in2 ${DATA_FILES[eng,dev]}\
         -out $devOut\
         ${TRAIN_SWITCHES[eng]}\
         $switches\
         -beam $bsize
         2>&1 | tee ${eroot}/train.log
    #Call the scorer
    sync
    sync
    ${SCORER_BIN[$DEFAULT_SCORER]} all ${DATA_FILES[eng,dev]} $devOut none &> ${devOut}.scores.$DEFAULT_SCORER &
    echo
    echo
    echo
}

##FO -- the last two should in principle be identical, but I'm
##guessing they're not, due to floating point differences...
doit ${foroot}/eu            fo_opt ""                              $BEAM_SIZE
doit ${foroot}/laso          fo_opt "-beamEarlyIter"                $BEAM_SIZE
doit ${foroot}/delayed-laso  fo_opt "-beamEarlyIter -delayUpdates"  $BEAM_SIZE
doit ${foroot}/bl            fo_opt ""                                   1

##HO
doit ${horoot}/eu            ho_opt ""                             $BEAM_SIZE
doit ${horoot}/laso          ho_opt "-beamEarlyIter"               $BEAM_SIZE
doit ${horoot}/delayed-laso  ho_opt "-beamEarlyIter -delayUpdates" $BEAM_SIZE

wait #wait for last scoring job

echo
echo -n "ALL DONE at "
date
