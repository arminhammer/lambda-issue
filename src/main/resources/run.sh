#!/bin/bash
BASEDIR="$(dirname `readlink -f -- ${0}`)"

cd ${BASEDIR}
${BASEDIR}/hello32
echo "return code is: $?"
