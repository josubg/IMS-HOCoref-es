#!/usr/bin/bash

##
## Paths to feature files
##

set -ue

declare -A FEATURE_FILES

FEATURE_ROOT=${WD}/features/

#Arabic
FEATURE_FILES[ara,fo_opt]=$FEATURE_ROOT/ara-fo-opt
FEATURE_FILES[ara,ho_opt]=$FEATURE_ROOT/ara-nho6-opt
#Chinese
FEATURE_FILES[chi,fo_opt]=$FEATURE_ROOT/chi-fo-opt
FEATURE_FILES[chi,ho_opt]=$FEATURE_ROOT/chi-nho6-opt
#English
FEATURE_FILES[eng,fo_opt]=$FEATURE_ROOT/eng-fo-opt
FEATURE_FILES[eng,ho_opt]=$FEATURE_ROOT/eng-nho7-opt

export FEATURE_FILES
