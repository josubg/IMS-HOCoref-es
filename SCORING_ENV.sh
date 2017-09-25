#!/usr/bin/bash

##
## Paths to scoring. You need to set at least one, and the default.
##

declare -A SCORER_BIN

SCORER_BIN[v7]=${WD}/reference-coreference-scorers/scorer.pl

DEFAULT_SCORER="v7"

export SCORER_BIN
export DEFAULT_SCORER
