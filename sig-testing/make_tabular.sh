#!/bin/bash

set -ue

##Scorer path
SCORER=/mount/arbeitsdaten14/projekte/sfb-732/d8/anders/slask/reference-coreference-scorers-read-only/scorer.pl
PERL5LIB=`dirname $SCORER`/lib
export PERL5LIB
HOME=`dirname $0`

##Arguments
GOLD_FILE=$1
PRED_FILE=$2
if [ $# -ge 3 ]; then
    METRIC=$3
else
    METRIC="all"
fi

metrics=$METRIC

if [ "$METRIC" == "all" ]; then
    metrics="muc bcub ceafm ceafe"
elif [ "$METRIC" == "conll" ]; then
    metrics="muc bcub ceafe"
fi

for m in $metrics; do
    echo "Metric $m to file " ${PRED_FILE}.doctab.${m}
    $SCORER $m $GOLD_FILE $PRED_FILE | grep -v "^Repe" | tail -n +2 | head -n -6 | perl ${HOME}/extract_doc_scores.pl > ${PRED_FILE}.doctab.${m}
done
