cd $NEW_DT_SUBJ

java -Xmx8000M -cp $DT_TOOLS:$NEW_DT_TESTS:$NEW_DT_CLASS:$NEW_DT_LIBS: edu.washington.cs.dt.main.Main --randomize=true --round=1000 --tests=${SUBJ_NAME}-orig-order --report=./${SUBJ_NAME}-randomize.txt


