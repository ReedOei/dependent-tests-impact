#!/bin/bash

# Runs commands for "Instructions to setup a subject for test selection" section.

# 1. Generate the static information needed by test selection for the old version of the subject.
cd $DT_SUBJ
echo "[DEBUG] Instrumenting subject for selection (\$DT_TESTS)."
java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $DT_TESTS --soot-cp $DT_LIBS:$DT_CLASS:$DT_TESTS:$JAVA_HOME/jre/lib/*: -technique selection
echo "[DEBUG] Instrumenting subject for selection (\$DT_RANDOOP)."
java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $DT_RANDOOP --soot-cp $DT_LIBS:$DT_CLASS:$DT_RANDOOP:$JAVA_HOME/jre/lib/*: -technique selection
echo "[DEBUG] Instrumenting subject for selection (\$DT_CLASS)."
java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $DT_CLASS --soot-cp $DT_LIBS:$DT_CLASS:$JAVA_HOME/jre/lib/*: -technique selection

# 2. Run the instrumented tests.
echo "[DEBUG] Running the instrumented auto tests."
java -cp $DT_TOOLS:$DT_LIBS:sootOutput/: edu.washington.cs.dt.main.ImpactMain -inputTests $SUBJ_NAME-auto-order
mv sootTestOutput/ sootTestOutput-auto-selection
echo "[DEBUG] Running the instrumented human-written tests."
java -cp $DT_TOOLS:$DT_LIBS:sootOutput/: edu.washington.cs.dt.main.ImpactMain -inputTests $SUBJ_NAME-orig-order
mv sootTestOutput/ sootTestOutput-orig-selection
rm -rf sootOutput/

# 3. Generate the static information needed by test selection for the new version of the subject.
cd $NEW_DT_SUBJ
echo "[DEBUG] Instrumenting the new subject for selection."
java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain --soot-cp $NEW_DT_LIBS:$NEW_DT_CLASS:$JAVA_HOME/jre/lib/*: -inputDir $NEW_DT_CLASS -technique selection
rm -rf sootOutput/
