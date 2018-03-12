# Usage: compile.sh $DT_ROOT

source ./constants.sh

#!/bin/bash

compileRandoop() {
  cd $DT_RANDOOP
  javac -cp $DT_LIBS:$DT_CLASS:$DT_TOOLS: ErrorTest*.java RegressionTest*.java

  cd $NEW_DT_RANDOOP
  javac -cp $NEW_DT_LIBS:$NEW_DT_CLASS:$DT_TOOLS: ErrorTest*.java RegressionTest*.java
}

DT_ROOT=$1

source ./subj-constants.sh $DT_ROOT

startTime=`date`

# ======================================================
echo "[INFO] Compiling old version of ant subjects."

index=0
count=${#antExp[@]}
while [ "$index" -lt "$count" ]; do
  ANT_BASE_DIR=${antExp[$index]}

  ant -Dbasedir=$ANT_BASE_DIR -f $ANT_BASE_DIR/build.xml

  compileRandoop

  let "index++"
done

# ======================================================
echo "[INFO] Compiling new version of ant subjects."

index=0
count=${#antNextExp[@]}
while [ "$index" -lt "$count" ]; do
  ANT_BASE_DIR=${antNextExp[$index]}

  ant -Dbasedir=$ANT_BASE_DIR -f $ANT_BASE_DIR/build.xml

  compileRandoop

  let "index++"
done

# ======================================================
echo "[INFO] Compiling old version of Maven subjects."

index=0
count=${#antMvnExp[@]}
while [ "$index" -lt "$count" ]; do
  ANT_BASE_DIR=${antMvnExp[$index]}

  ant -Dbasedir=$ANT_BASE_DIR -f $DT_ROOT/scripts/data/build.xml

  compileRandoop

  let "index++"
done

# ======================================================
echo "[INFO] Compiling new version of Maven subjects."

index=0
count=${#antMvnNextExp[@]}
while [ "$index" -lt "$count" ]; do
  ANT_BASE_DIR=${antMvnNextExp[$index]}

  ant -Dbasedir=$ANT_BASE_DIR -f $DT_ROOT/scripts/data/build.xml
  
  compileRandoop

  let "index++"
done

# ======================================================


echo "[INFO] Script has finished running."

echo "[INFO] Start time was ${startTime}"

endTime=`date`
echo "[INFO] End time is ${endTime}"
