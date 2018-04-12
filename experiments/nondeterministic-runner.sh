source ./config.sh

testType=orig
numTimesToRun=9999
crossReferenceClass=edu.washington.cs.dt.impact.tools.CrossReferencer
experimentCP=$DT_TOOLS:$DT_CLASS:$DT_TESTS:$DT_LIBS:

echo -e "Starting experiment: $SUBJ_NAME"
cd $DT_SUBJ
mkdir nondeterministic-output/
cp $SUBJ_NAME-$testType-order nondeterministic-output/deterministic-order

while [[ "$k" -le "$numTimesToRun" ]]
do
    echo '======================= Start ' $k ' ======================='
    clearProjectFiles
    java -cp $experimentCP edu.washington.cs.dt.main.ImpactMain -inputTests nondeterministic-output/deterministic-order > nondeterministic-output/${SUBJ_NAME}-${testType}-order-results.txt
    clearProjectFiles
    java -cp $experimentCP edu.washington.cs.dt.main.ImpactMain -inputTests nondeterministic-output/deterministic-order > nondeterministic-output/${SUBJ_NAME}-${testType}-rerun-results.txt

    java -cp $experimentCP $crossReferenceClass -origOrder nondeterministic-output/${SUBJ_NAME}-${testType}-order-results.txt -testOrder nondeterministic-output/${SUBJ_NAME}-${testType}-rerun-results.txt > nondeterministic-output/cross-referencer-file.txt

    if [[ $((k % 100)) = 0 ]] ; then
      j=$(($j+1))
      echo "" > nondeterministic-output/debug.log$j
    fi

    echo '======================= Start ' $k ' =======================' >> nondeterministic-output/debug.log$j
    cat nondeterministic-output/cross-referencer-file.txt >> nondeterministic-output/debug.log$j
    echo "" >> nondeterministic-output/debug.log$j

    java -cp $experimentCP edu.washington.cs.dt.impact.tools.UndeterministicTestFinder -undeterministicTestFile nondeterministic-output/undeterminisitic-order -deterministicTestFile nondeterministic-output/deterministic-order -crossReferenceFile nondeterministic-output/cross-referencer-file.txt -randomizeDeterministicTests
    k=$(($k+1))
done
clearProjectFiles

