#!/bin/bash

##
## This does the learning curves from Figure 3
##

set -ue

ITERS=100    #This is what I used for the lc's

WD=`dirname $0`
source ${WD}/GLOBAL_PARAMS.sh
source ${WD}/CLASSPATH_ENV.sh
source ${WD}/DATA_FILES_ENV.sh
source ${WD}/FEATURE_FILES_ENV.sh
source ${WD}/LANG_ENV.sh
source ${WD}/SCORING_ENV.sh

#We run a process to do the scoring in paralell, this way we don't
#have to wait until all outputs are written before we start scoring
#them (since the scorer is pretty slow, and it we get like 100 outputs
#per job)
PORT=57191
SCORER_SERVER=localhost:$PORT
java -cp $CLASSPATH ims.hotcoref.util.ScoringServer $PORT ${SCORER_BIN[$DEFAULT_SCORER]} 2 &> ./scorer_server.log &
SCORER_SERVER_PID=$!


root=${WD}/experiments/lc
blroot=${root}/bl
mkdir -pv $blroot

##First do baseline
java -cp $CLASSPATH -Xmx${JAVA_MEM} ims.hotcoref.LearnWeights\
     -lang eng\
     -in ${DATA_FILES[eng,train]}\
     -iter $ITERS\
     -cores $CORES\
     -scorer $SCORER_SERVER\
     -testEveryIter\
     -features ${FEATURE_FILES[eng,fo_opt]}\
     -model $blroot/hotcoref.mdl\
     -in2 ${DATA_FILES[eng,dev]}\
     -out ${blroot}/dev.out\
     ${TRAIN_SWITCHES[eng]}\
     2>&1 | tee ${blroot}/train.log

echo
echo
echo

for bsize in 20 100; do
    for features in fo_opt ho_opt; do
	eroot=${root}/eu-${bsize}-${features}
	mkdir -pv $eroot
	last_out=${eroot}/dev.out
	java -cp $CLASSPATH -Xmx${JAVA_MEM} ims.hotcoref.LearnWeights\
             -lang eng\
             -in ${DATA_FILES[eng,train]}\
             -iter $ITERS\
             -cores $CORES\
             -scorer $SCORER_SERVER\
             -testEveryIter\
             -features ${FEATURE_FILES[eng,$features]}\
             -model ${eroot}/hotcoref.mdl\
             -in2 ${DATA_FILES[eng,dev]}\
             -out $last_out\
             ${TRAIN_SWITCHES[eng]}\
             -beam ${bsize}\
             2>&1 | tee ${eroot}/train.log
	echo
	echo
	echo
    done
done

echo "Experiments done, just wait for scoring to finish"
echo

sleep 300 #give it five minutes to finish off the scoring.

echo "Killing scoring server"
kill -9 $SCORER_SERVER_PID

echo
echo -n "ALL DONE at "
date
