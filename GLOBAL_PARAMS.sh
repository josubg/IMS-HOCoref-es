#!/bin/bash

set -ue

CORES=10 #Change this to whatever amount of cores you want to assign the coreference resolver.
BEAM_SIZE=20 #This is what I used for all experiments with beam search
JAVA_MEM=20g #Should be like 5-20g, depends on language and feature set. 20g is enough for all experiments.

export CORES
export BEAM_SIZE
export JAVA_MEM
