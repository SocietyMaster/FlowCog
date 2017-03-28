#!/bin/bash
if [ "$#" -ne 1 ]; then
  echo "./commit.sh message "
  exit 1
fi

git add -u
git add *.java
git add *.txt
git commit -m $1
