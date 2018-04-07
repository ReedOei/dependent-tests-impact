#!/bin/bash

# Runs commands for "Instructions to setup a subject for test selection" section.

# 1. Generate the static information needed by test selection for the old version of the subject.
cd $DT_SUBJ
echo "[DEBUG] hi 12"
java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $DT_TESTS --soot-cp $DT_LIBS:$DT_CLASS:$DT_TESTS:$JAVA_HOME/jre/lib/*: -technique selection
echo "[DEBUG] hi 13"
java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $DT_RANDOOP --soot-cp $DT_LIBS:$DT_CLASS:$DT_RANDOOP:$JAVA_HOME/jre/lib/*: -technique selection
echo "[DEBUG] hi 14"
java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $DT_CLASS --soot-cp $DT_LIBS:$DT_CLASS:$JAVA_HOME/jre/lib/*: -technique selection

# 2. Run the instrumented tests.
echo "[DEBUG] hi 15"
java -cp $DT_TOOLS:$DT_LIBS:sootOutput/: edu.washington.cs.dt.main.ImpactMain -inputTests $SUBJ_NAME-auto-order
mv sootTestOutput/ sootTestOutput-auto-selection
echo "[DEBUG] hi 16"
java -cp $DT_TOOLS:$DT_LIBS:sootOutput/: edu.washington.cs.dt.main.ImpactMain -inputTests $SUBJ_NAME-orig-order
mv sootTestOutput/ sootTestOutput-orig-selection
rm -rf sootOutput/

# 3. Generate the static information needed by test selection for the new version of the subject.
cd $NEW_DT_SUBJ
echo "[DEBUG] hi 17"
java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain --soot-cp $NEW_DT_LIBS:$NEW_DT_CLASS:$JAVA_HOME/jre/lib/*: -inputDir $NEW_DT_CLASS -technique selection
rm -rf sootOutput/
