function clearEnv() {
  rm -rf 2013-08-28T20-44-41.156-0700
  rm -rf hi!
  rm -rf ./'-1 ms'
  rm -rf 0\ ms
  rm -rf 1\ ms
  rm -rf 10\ ms
  rm -rf 100\ ms
  rm -rf 382707\ hours\ 44\ min
}

clearEnv
java -cp impact-tools/*:bin/:libs/lib/* edu.washington.cs.dt.main.ImpactMain crystal-tp-statement-absolute.txt > crystal-tp-statement-absolute-results.txt
java -cp impact-tools/*:bin/:libs/lib/* edu.washington.cs.dt.impact.tools.CrossReferencer -origOrder crystal-auto-order-results.txt -testOrder crystal-tp-statement-absolute-results.txt > blah
