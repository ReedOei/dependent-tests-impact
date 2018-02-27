package edu.washington.cs.dt.impact.runner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import edu.washington.cs.dt.RESULT;
import edu.washington.cs.dt.impact.technique.Test;
import edu.washington.cs.dt.impact.tools.ParallelDependentTestFinder;
import edu.washington.cs.dt.impact.data.TestData;

/*
 * TODO 
 * 
 * Right now this class now calls runDTF in DependentTestFinder class in parallelized manner
 * 
 * Constructor needs:
 * number of threads (which is used to append to the names of the temporary files generated (e.g. LOCK_FILE)
 * 
 * When using this class, need to call setParaVars first, which needs:
 * changed test list, 
 * hashmap of each test's original result and name, 
 * current order test list, 
 * original order test list,
 * files to delete, 
 * all dt test list (invoked by OneConfigurationRunner)
 * 
 * TestData implementation still needs to be worked on if DependenTestFinder will be changed
 * to data objects instead of Strings
 * 
 */

public class ParaThreads {
	// list of variables
	private static Set<String> changedTests;
	private static Map<String, RESULT> nameToOrigResultsListHen;
	private static List<String> currentOrderTestListHen;
	private static List<String> origOrderTestListHen;
	private static List<String> filesToDeleteHen;
	private static List<String> allDTListHen;
    private static Map<String, RESULT> nameToNewResults;
    private ArrayList<Thread> threadList = new ArrayList<Thread>(); // arraylist of
															// threads
	int threads; // number of threads to use
	// q (below) is a shared queue between threads, each thread pops a test off
	// of the queue and calls runDTF on it
    private ConcurrentLinkedQueue<String> q = new ConcurrentLinkedQueue<String>();
	// classpaths (below) is a queue of the thread number (as a string) to
	// append to tmp files generated
    private ConcurrentLinkedQueue<String> classpaths = new ConcurrentLinkedQueue<String>();
	private ConcurrentHashMap<String, Set<TestData>> knownDepMap = new ConcurrentHashMap<>();

    // constructor sets number of threads
	public ParaThreads(int threads) {
		this.threads = threads;
	}

	public void setParaVars(Set<String> changedTests, Map<String, RESULT> nameToOrigResultsHen,
                            Map<String, RESULT> nameToNewResults, List<String> currentOrderTestListHen, List<String> origOrderTestListHen, List<String> filesToDeleteHen,
                            List<String> allDTListHen) {
		ParaThreads.changedTests = changedTests;
		ParaThreads.nameToOrigResultsListHen = nameToOrigResultsHen;
		ParaThreads.nameToNewResults = nameToNewResults;
		ParaThreads.currentOrderTestListHen = currentOrderTestListHen;
		ParaThreads.origOrderTestListHen = origOrderTestListHen;
		ParaThreads.filesToDeleteHen = filesToDeleteHen;
		ParaThreads.allDTListHen = allDTListHen;
	}

	// runs threads and returns a list of strings that represent allDTList
	public List<String> runThreads(int machine, Test testObj) {
		// add dependent tests to q
		for (String i : changedTests) {
			System.out.printf("The test added is: %s\n", i);
			q.add(i);
		}

		// create new threads as specified
		for (int j = 0; j < threads; j++) {
			final int threadNum = j;
			threadList.add(new Thread(new Runnable() {
				// each thread's run method defined here
				public void run() {
					try {
					    System.out.println("Thread " + threadNum + " is running!");

						ParallelDependentTestFinder dtFinder = null;

						while (q.peek() != null) {
							String test = q.poll();

							if (dtFinder == null) {
								dtFinder = new ParallelDependentTestFinder(test,
                                        origOrderTestListHen, nameToOrigResultsListHen,
										currentOrderTestListHen, nameToNewResults,
                                        filesToDeleteHen, knownDepMap, threadNum);
								dtFinder.runDTF();
							} else {
								dtFinder = dtFinder.createFinderFor(test);
								dtFinder.runDTF();
							}
						}

						System.out.println("Thread " + threadNum + " is done!");
					} catch (Exception e) {
						System.out.println("Encountered an error: " + e);
					}
				}
			}));
			classpaths.add(Integer.toString(j));
			threadList.get(j).start();
		}
		// wait for threads to finish
		for (Thread t : threadList) {
			try {
				t.join();
			} catch (Exception ignored) {}
		}
		threadList.clear(); // clear list since threads cannot be restarted once
							// stopped
		// need deep copy of allDTSynchList since rejoining to main thread
		classpaths.clear();
		return TestData.generateDTList(knownDepMap);
	}

    public Map<String, Set<TestData>> getKnownDependencies() {
        return knownDepMap;
    }
}
