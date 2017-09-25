#!/usr/bin/bash

##
## Language specific variables
##
## You need to specify the path to ENGLISH_GENDER_DATA below.
##


set -ue

ENGLISH_GENDER_DATA=/home/users0/anders/corpora/gender.data.gz

declare -A TRAIN_SWITCHES
declare -A TEST_SWITCHES

TRAIN_SWITCHES[ara]="-dontClearSpans"
TRAIN_SWITCHES[chi]=""
TRAIN_SWITCHES[eng]="-gender $ENGLISH_GENDER_DATA"
#TRAIN_SWITCHES[eng]=""

TEST_SWITCHES[ara]=""
TEST_SWITCHES[chi]=""
TEST_SWITCHES[eng]=""

export TEST_SWITCHES
export TRAIN_SWITCHES
