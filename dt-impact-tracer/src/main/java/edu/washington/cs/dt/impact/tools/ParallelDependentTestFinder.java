/**
 * Copyright 2014 University of Washington. All Rights Reserved.
 * @author Wing Lam, Reed Oei
 *
 *         Determines and outputs what tests needs to run before and
 *         after a particular test in order for that particular test to
 *         attain the same specified result in a new test execution order.
 */
package edu.washington.cs.dt.impact.tools;

import edu.washington.cs.dt.RESULT;
import edu.washington.cs.dt.TestExecResult;
import edu.washington.cs.dt.impact.data.TestData;
import edu.washington.cs.dt.runners.FixedOrderRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Usages: Get ALL_DT_LIST for just one test:
 *
 * <pre>
 * {
 * 	&#064;code
 * 	ParallelDependentTestFinder dtFinder = new ParallelDependentTestFinder(dependentTestName, origOrder, newOrder,
 * 			filesToDelete);
 *
 * 	Map&lt;String, Set&lt;String&gt;&gt; knownDependencies = dtFinder.runDTF();
 *
 * }
 * </pre>
 *
 * Get known dependencies for multiple tests, using same orig/new/prime order:
 *
 * <pre>
 * {@code
 * List<DependentTestFinder> dtFinders = new ArrayList<>();
 *
 * For first test:
 * ParallelDependentTestFinder dtFinder = new ParallelDependentTestFinder(dependentTestName, origOrder, newOrder, filesToDelete);
 *
 * Map<String, Set<String>> knownDependencies = dtFinder.runDTF();
 *
 * For second, etc.:
 * ParallelDependentTestFinder dtFinder2 = dtFinder.createFinderFor(nextDependentTestName);
 *
 * Map<String, Set<String>> knownDependencies2 = dtFinder.runDTF();
 *
 * // merge known dependencies into one map if desired.
 *
 * }
 *
 * </pre>
 */
public class ParallelDependentTestFinder {
	private class TestOrder {
		private final List<String> testOrder;
		private final Map<String, RESULT> results;

		public TestOrder(final List<String> testOrder, final Map<String, RESULT> results) {
			this.testOrder = testOrder;
			this.results = results;
		}

		public TestOrder(final List<String> testOrder) {
			this.testOrder = testOrder;

			results = runTestOrder(testOrder).getNameToResultsMap();
		}

		public RESULT getResult(final String testName) {
			return results.get(testName);
		}

		public List<String> getTestsBefore(final String testName) {
			return new ArrayList<>(testOrder.subList(0, testOrder.indexOf(testName)));
		}
	}

	private TestExecResult runTestOrder(final List<String> order) {
		return new FixedOrderRunner(order, Integer.toString(threadNum)).run().getExecutionRecords().get(0);
	}

	private final String dependentTestName;
	private final RESULT dependentTestResult;
	private final TestOrder originalOrder;
	private final TestOrder newOrder;
	private final TestOrder primeOrder;

	private final List<String> filesToDelete;

	private final Map<String, Set<TestData>> knownDependencies;

	private final int threadNum;

	public ParallelDependentTestFinder(final String dependentTestName, final List<String> originalOrder,
                                       Map<String, RESULT> nameToOrigResultsListHen, final List<String> newOrder,
                                       Map<String, RESULT> nameToNewResults, final List<String> filesToDelete,
                                       final Map<String, Set<TestData>> knownDependencies, int threadNum) {
        this.threadNum = threadNum;
		this.dependentTestName = dependentTestName;

		this.originalOrder = new TestOrder(originalOrder, nameToOrigResultsListHen);
		this.newOrder = new TestOrder(newOrder, nameToNewResults);

		primeOrder = new TestOrder(generatePrimeOrder(this.originalOrder, this.newOrder));

		this.filesToDelete = filesToDelete;
		this.knownDependencies = knownDependencies;

		dependentTestResult = this.originalOrder.getResult(dependentTestName);
    }

    public ParallelDependentTestFinder(final String dependentTestName, final List<String> originalOrder,
                                       Map<String, RESULT> nameToOrigResultsListHen, final List<String> newOrder,
                                       Map<String, RESULT> nameToNewResults, final List<String> primeOrder,
                                       Map<String, RESULT> primeOrderResults, final List<String> filesToDelete,
                                       final Map<String, Set<TestData>> knownDependencies, int threadNum) {
        this.threadNum = threadNum;
        this.dependentTestName = dependentTestName;

        this.originalOrder = new TestOrder(originalOrder, nameToOrigResultsListHen);
        this.newOrder = new TestOrder(newOrder, nameToNewResults);

        this.primeOrder = new TestOrder(primeOrder, primeOrderResults);

        this.filesToDelete = filesToDelete;
        this.knownDependencies = knownDependencies;

        dependentTestResult = this.originalOrder.getResult(dependentTestName);
    }

	private ParallelDependentTestFinder(final String dependentTestName, final TestOrder originalOrder,
			final TestOrder newOrder, final List<String> filesToDelete,
			final Map<String, Set<TestData>> knownDependencies, final int threadNum) {
        this.threadNum = threadNum;

		this.dependentTestName = dependentTestName;

		this.originalOrder = originalOrder;
		this.newOrder = newOrder;

        primeOrder = new TestOrder(generatePrimeOrder(this.originalOrder, this.newOrder));

		this.filesToDelete = filesToDelete;
		dependentTestResult = this.originalOrder.results.get(dependentTestName);

		this.knownDependencies = knownDependencies;
    }

	/**
	 * Use this to create a new dependent test finder when reusing the same
	 * orig/new/prime orders. Normally, the DependentTestFinder will run each
	 * order when created to get results, using this method makes that
	 * unnecessary (could save a lot of time).
	 *
	 * Also copies filesToDelete.
	 *
	 * @param dependentTestName
	 *            The dependent test that this finder should be trying to find
	 *            dependencies for.
	 * @return A new finder that will find dependencies for the dependent test
	 *         passed in.
	 */
	public ParallelDependentTestFinder createFinderFor(final String dependentTestName) {
		return new ParallelDependentTestFinder(dependentTestName, originalOrder, newOrder,
                filesToDelete, knownDependencies, threadNum);
	}

	private List<String> generatePrimeOrder(final TestOrder originalOrder,
                                            final TestOrder newOrder) {
		final List<String> primeOrder = new ArrayList<>(newOrder.testOrder);

        final int primeIndex = primeOrder.indexOf(dependentTestName);

        // Get all the tests that come before the test in the original order, and put them before
        // the test in the prime list.
        if (primeIndex != -1) {
            final List<String> insertTests = originalOrder.getTestsBefore(dependentTestName);

            // Make sure no tests appear twice.
            insertTests.removeIf(test -> primeOrder.indexOf(test) < primeIndex);
            primeOrder.removeIf(insertTests::contains); // Should only remove tests after the dt

            // Get rid of all tests that come after the dependent test we're interested in
            primeOrder.subList(primeIndex, primeOrder.size()).clear();
            primeOrder.addAll(insertTests);
            primeOrder.add(dependentTestName);
        }

	    System.out.println("Making and running prime order (" + primeOrder.size() + " tests).");

        return primeOrder;
    }

	/**
	 * Generates a test order containing all the tests in order, with the
	 * dependent test coming at the end, respecting all dependencies.
	 *
	 * @param order
	 *            The order of tests to run before the dependent test. Not
	 *            modified.
	 * @return The results from running the tests in that order.
	 */
	private TestExecResult makeAndRunTestOrder(final List<String> order) {
		List<String> temp = new ArrayList<>(order);
        int newIndex = temp.indexOf(dependentTestName);
        if(newIndex == -1)
        {
        	temp.add(dependentTestName);
        }

        return runTestOrder(TestData.fixOrder(knownDependencies, temp));
    }

	/**
	 * Adds a dependency to the known dependencies for the current dependent
	 * test.
	 *
	 * @param dependency
	 *            The new dependency
	 * @param result
	 *            The result of running the test without this dependency in it's
	 *            proper location.
	 */
	 void addDependency(final String dependency,
                               final RESULT result,
                               final List<String> testOrder,
                               final boolean isBefore) {
        knownDependencies.compute(dependentTestName, (testName, dependencies) -> {
            if (dependencies == null) {
                dependencies = new HashSet<>();
            }

            if (isBefore) {
                dependencies.add(new TestData(dependentTestName,
                        dependentTestResult,
                        new HashSet<>(Collections.singletonList(dependency)),
                        new HashSet<>(),
                        result,
                        testOrder));
            } else {
                dependencies.add(new TestData(dependentTestName,
                        dependentTestResult,
                        new HashSet<>(),
                        new HashSet<>(Collections.singletonList(dependency)),
                        result,
                        testOrder));
            }

            return dependencies;
        });
    }

	public Map<String, Set<TestData>> getKnownDependencies() {
		return knownDependencies;
	}

	private boolean isTestResultDifferent(final List<String> orderedTests) {
	    return isTestResultDifferent(orderedTests, true);
    }

	// runs orderedTests and determine whether dependentTestName
	// in the orderTests matches DEPEDENT_TEST_RESULT
	// returns true if dependentTestName's result in orderedTest
	// does not match DEPENDENT_TEST_RESULT, false otherwise
	private boolean isTestResultDifferent(final List<String> orderedTests, final boolean useKnownDeps) {
		TestExecResult result;

		if (useKnownDeps) {
		    result = makeAndRunTestOrder(orderedTests);
        } else {
		    result = runTestOrder(orderedTests);
        }

		RESULT dtIsolateResult = null;
		if (result.isTestPassed(dependentTestName)) {
			dtIsolateResult = RESULT.PASS;
		} else if (result.isTestError(dependentTestName)) {
			dtIsolateResult = RESULT.ERROR;
		} else {
			dtIsolateResult = RESULT.FAILURE;
		}

		return !dtIsolateResult.equals(dependentTestResult);
	}

	private boolean containsDependency(final boolean isOriginalOrder, final List<String> tests) {
	    return containsDependency(isOriginalOrder, makeAndRunTestOrder(tests));
    }

	// returns true if dependentTestName's result in results is the same as
	// DEPENDENT_TEST_RESULT and if it is original order OR
    // if it's not the same and isOriginalOrder is false.
    // Returns false otherwise.
	private boolean containsDependency(boolean isOriginalOrder, TestExecResult results) {
		boolean testResultMatches;
		if (dependentTestResult.equals(RESULT.PASS)) {
			testResultMatches = results.isTestPassed(dependentTestName);
		} else if (dependentTestResult.equals(RESULT.ERROR)) {
			testResultMatches = results.isTestError(dependentTestName);
		} else {
			testResultMatches = results.isTestFailed(dependentTestName);
		}

		return testResultMatches == isOriginalOrder;
	}

	private int getDependencyCount() {
	    return knownDependencies.getOrDefault(dependentTestName, new HashSet<>()).size();
    }

    private boolean stillContainsDep() {
	    return isTestResultDifferent(newOrder.getTestsBefore(dependentTestName));
    }

	/**
	 * Finds all the dependencies for the test that this class was initialized
	 * with.
	 *
	 * @return The new map of dependencies. (i.e., the test still has a
	 *         different result than it did in the original order).
	 */
	public Map<String, Set<TestData>> runDTF() throws DependencyVerificationException {
	    final int initialKnownDependencies = getDependencyCount();

	    System.out.println("Running dependent test finder for: " + dependentTestName + " (" + initialKnownDependencies + " known dependencies already).");

		// If the dt is already in the knownDependencies list, we must have
		// tried the below method to find dependencies and not found all of them.
		if (knownDependencies.containsKey(dependentTestName)) {
			findDependencyInChains();
		} else {
			// If the result is the same, then the dependencies must be some of
			// the tests in the original order
			// that came before this test.
			if (dependentTestResult == primeOrder.getResult(dependentTestName)) {
                System.out.println("Need tests from original order to come before " + dependentTestName + ".");
				dependentTestSolver(originalOrder.getTestsBefore(dependentTestName), true,
                        new ArrayList<>(), new ArrayList<>(),
						newOrder.getResult(dependentTestName), newOrder.testOrder);
			} else {
				// Run the test in isolation
				final Map<String, RESULT> results = runTestOrder(Collections.singletonList(dependentTestName))
						.getNameToResultsMap();

				// If the result is the same with no tests before it, then we
				// don't need any of the original
				// tests, we just need to move some of the new tests to after
				// come before in the original order.
				if (results.get(dependentTestName) == dependentTestResult) {
                    System.out.println("Need tests from new order to come after " + dependentTestName + ".");
					dependentTestSolver(newOrder.getTestsBefore(dependentTestName), false,
                            new ArrayList<>(), new ArrayList<>(),
							newOrder.getResult(dependentTestName), newOrder.testOrder);
				} else {
					// If the result is still different from the original order,
					// then we must need both
					// some/all tests from the original order to come before and
					// some/all tests from the new order to come after.

                    System.out.println("Need tests from original order to come before " + dependentTestName + ".");
                    dependentTestSolver(originalOrder.getTestsBefore(dependentTestName), true,
                            new ArrayList<>(), new ArrayList<>(),
                            newOrder.getResult(dependentTestName), newOrder.testOrder);

                    System.out.println("Need tests from new order to come after " + dependentTestName + ".");
					dependentTestSolver(newOrder.getTestsBefore(dependentTestName), false,
							new ArrayList<>(), new ArrayList<>(),
                            newOrder.getResult(dependentTestName), newOrder.testOrder);
				}
			}
		}

		// If we didn't get any more information, give up, because we won't figure it out.
		if (initialKnownDependencies >= getDependencyCount()) {
		    throw new DependencyVerificationException("Could not find dependencies for " + dependentTestName);
        }

        System.out.println("Checking if all dependencies were found for " + dependentTestName + ".");

		if (stillContainsDep()) {
			return runDTF();
		} else {
		    System.out.println("Success for " + dependentTestName + "!");
			return knownDependencies;
		}
	}

	/**
	 * A dependency chain is a list of tests that comes either between two known
	 * dependencies, before all known dependencies, or after all known
	 * dependencies. This is useful because we can determine which lists of
	 * tests to perform delta debugging on, given that we did not find all
	 * possible dependent tests the first time.
	 *
	 * Ex. A, B are before dependencies of C. The test order is F, A, K, J, B,
	 * L, C. The dependency chains are: [F], [K,J], [L]
	 */
	public List<List<String>> getDependencyChains(final List<String> testOrder) {
        final List<Integer> indices =
                knownDependencies.getOrDefault(dependentTestName, Collections.emptySet()).stream()
                        .flatMap(testData -> testData.getIndices(testOrder))
                        .sorted()
                        .collect(Collectors.toList());

        // Add this so that we get all tests that come before other dependencies.
        indices.add(0, 0);

        final List<List<String>> chains = new ArrayList<>();

        for (int i = 0; i < indices.size(); i++) {
            int lo = indices.get(i);

            int hi = testOrder.size();

            if (i + 1 < indices.size()) {
                hi = indices.get(i + 1) + 1;
            }

            if (lo < hi) {
                chains.add(new ArrayList<>(testOrder.subList(lo, hi)));
            }
        }

        return chains;
    }

	private Stream<List<String>> getAllDependencyChains() {
		return getAllDependencyChains(getDependencyChains(newOrder.getTestsBefore(dependentTestName)));
	}

	public static <T> List<List<T>> tails(final List<T> list) {
		final List<List<T>> result = new ArrayList<>();
		final LinkedList<T> current = new LinkedList<>(list);

		while (!current.isEmpty()) {
			result.add(new ArrayList<>(current));
			current.removeFirst();
		}

		return result;
	}

	private <T> Stream<List<T>> subsequences(final List<T> list) {
	    if (list.isEmpty()) {
	        return Stream.empty();
        }

        final T x = list.get(0);
	    final List<T> xs = list.subList(1, list.size());

        return Stream.concat(
                Stream.of(Collections.singletonList(x)),
                subsequences(xs)
                    .reduce(Stream.empty(), (Stream<List<T>> r, List<T> ys) -> {
                        List<T> temp = new ArrayList<>(ys);
                        temp.add(0, x);

                        return Stream.concat(Stream.concat(Stream.of(ys), Stream.of(temp)), r);
                    }, Stream::concat));
    }

	/**
	 * Takes each individual dependency chain, and generate all possible
	 * subsequences of them, merging chains together to form single test lists.
	 * Generates all single element chains first (so if there are no cross-chain
	 * dependencies, we don't waste much extra time).
	 *
	 * Returns a stream so that we don't have to generate them all at once (lazy
	 * evaluation).
	 *
	 * Ex. Take [1,2,3] and generate [1], [2], [3], [1,2], [1,3], [2,3], [1,2,3]
	 */
	public <T> Stream<List<T>> getAllDependencyChains(final List<List<T>> dependencyChains) {
        final List<Integer> indices = IntStream.range(0, dependencyChains.size()).boxed().collect(Collectors.toList());

        return subsequences(indices)
                .map(is -> is.stream().map(dependencyChains::get)
                            .reduce(Collections.emptyList(), (as, bs) -> {
                                final List<T> t = new ArrayList<>(as);
                                t.addAll(bs);
                                return t;
                            }));
    }

	private void findDependencyInChains() throws DependencyVerificationException {
	    int chainNum = 0;

	    final int initialDepCount = getDependencyCount();

		final Iterator<List<String>> dependencyChains = getAllDependencyChains().iterator();
		while (dependencyChains.hasNext()) {
			final List<String> chain = dependencyChains.next();

            System.out.println("Trying chain " + chainNum + " (" + chain.size() + " tests).");

			// Make sure the result in the chain is actually different
			if (isTestResultDifferent(chain)) {
                dependentTestSolver(chain, false,
                        new ArrayList<>(), new ArrayList<>(),
                        newOrder.getResult(dependentTestName), newOrder.testOrder);

                if (getDependencyCount() > initialDepCount) {
                    System.out.println("Found at least one new dependency in the previous chain, exiting to regenerate chains, if necessary.");
                    return;
                }

                System.out.println("Checking to see if we are done with dependency chains.");
                // Check that we've found all the dependencies, and break out if we
                // have.
                if (stillContainsDep()) {
                    return;
                }
            }

            chainNum++;
		}

		// If we don't find all the dependencies in any of the chains (or any
		// combination of them)
		throw new DependencyVerificationException(dependentTestName);
	}

	private boolean verifyContainsDependency(final List<String> orderedTests) {
	    final TestExecResult justDT = makeAndRunTestOrder(Collections.singletonList(dependentTestName));
	    final TestExecResult orderedTestResult = makeAndRunTestOrder(orderedTests);

	    // If we have a different result in the two orders, there must be more dependencies in the ordered tests.
	    return justDT.getResult(dependentTestName).result != orderedTestResult.getResult(dependentTestName).result;
    }

	private void dependentTestSolver(List<String> tests, boolean isOriginalOrder,
                                     List<String> topAddOnTests, List<String> botAddOnTests,
                                     RESULT revealed, List<String> revealingOrder) {
	    System.out.println("Running dependent test solver with " + tests.size() + " tests, " + topAddOnTests.size() + " top add on tests, and " + botAddOnTests.size() + " bottom add on tests.");

		tests.removeAll(topAddOnTests);
		tests.removeAll(botAddOnTests);

		List<String> topHalf = new LinkedList<>(tests.subList(0, tests.size() / 2));
		List<String> botHalf = new LinkedList<>(tests.subList(tests.size() / 2, tests.size()));

		while (tests.size() > 1) {
		    topHalf.addAll(0, topAddOnTests);
			topHalf.addAll(botAddOnTests);
			topHalf.add(dependentTestName);

			botHalf.addAll(0, topAddOnTests);
			botHalf.addAll(botAddOnTests);
			botHalf.add(dependentTestName);

			final TestExecResult topResults = makeAndRunTestOrder(topHalf);
			boolean topContainsDependency = containsDependency(isOriginalOrder, topResults);

			final TestExecResult botResults = makeAndRunTestOrder(botHalf);
			boolean botContainsDependency = containsDependency(isOriginalOrder, botResults);

			// dependent test depends on more than one test in tests
			if (topContainsDependency && botContainsDependency) {
				List<String> newTopList = new ArrayList<>(tests.subList(0, tests.size() / 2));
				List<String> newBotList = new ArrayList<>(tests.subList(tests.size() / 2, tests.size()));

				System.out.println("Dependencies in both halves, solving top half first (" + newTopList.size() + " tests).");

				// First, find the dependent tests in the top half.
				// We can skip finding exactly which tests are the dependent
				// tests in the bottom half
				// for now by simply running all of them.
				dependentTestSolver(newTopList, isOriginalOrder, topAddOnTests, botAddOnTests,
						topResults.getResult(dependentTestName).result, topHalf);

				if (verifyContainsDependency(botHalf)) {
                    System.out.println("Dependencies in both halves, solving bottom half (" + newBotList.size() + " tests).");
                    dependentTestSolver(newBotList, isOriginalOrder, topAddOnTests, botAddOnTests,
                            botResults.getResult(dependentTestName).result, botHalf);
                } else {
                    System.out.println("No need to solve bottom half, we already found all necessary dependencies.");
                }

				return;
			} else if (!topContainsDependency && !botContainsDependency) {
			    // If we're here, that means that both the top half and the bottom half match.
                // In that case, it must be that there is a dependency in the top and in the bottom
                // half, and they BOTH must come together to cause the problem we're seeing.
                List<String> newTopList = new ArrayList<>(tests.subList(0, tests.size() / 2));
                List<String> newBotList = new ArrayList<>(tests.subList(tests.size() / 2, tests.size()));

                System.out.println("Dependencies in both halves, solving top half first (" + newTopList.size() + " tests).");

                List<String> newBotAddOnTests = new ArrayList<>(botAddOnTests);
                newBotAddOnTests.addAll(newBotList);

                // First, find the dependent tests in the top half.
                // We can skip finding exactly which tests are the dependent
                // tests in the bottom half
                // for now by simply running all of them.
                dependentTestSolver(newTopList, isOriginalOrder, topAddOnTests, newBotAddOnTests,
                        topResults.getResult(dependentTestName).result, topHalf);

                // Now that we know the dependent tests in the top half, those
                // dependencies should be handled by the knownDependencies variable.

                // We want to know if there is still a dependency in the bottom half, or we've already
                // found everything that is necessary.
                final List<String> newTopAddOnTests = new ArrayList<>(topAddOnTests);
                newTopAddOnTests.addAll(newTopList);

                final List<String> orderedTests = new ArrayList<>(newTopAddOnTests);
                orderedTests.addAll(newBotList);
                orderedTests.addAll(botAddOnTests);
                orderedTests.add(dependentTestName);

                if (verifyContainsDependency(orderedTests)) {
                    System.out.println("Dependencies in both halves, solving bottom half (" + newBotList.size() + " tests).");
                    dependentTestSolver(newBotList, isOriginalOrder, newTopAddOnTests, botAddOnTests,
                            botResults.getResult(dependentTestName).result, botHalf);
                } else {
                    System.out.println("No need to solve bottom half, we already found all necessary dependencies.");
                }

                return;
            }

			// If only one half contains dependent tests, ignore all tests not
			// in that half.
			// If the top results match and we're looking for the before tests,
			// then we want the top half.
			// If the top results don't match, and we're looking for the after
			// tests, we still want the top half.
			if (topContainsDependency) {
			    System.out.print("Halving, dependencies are in the top half (");
				tests = topHalf;
				revealed = topResults.getResult(dependentTestName).result;
				revealingOrder = new ArrayList<>(topHalf);
			} else {
                System.out.print("Halving, dependencies are in the bottom half (");
				tests = botHalf;
				revealed = botResults.getResult(dependentTestName).result;
				revealingOrder = new ArrayList<>(botHalf);
			}

			tests.removeAll(topAddOnTests);
            tests.removeAll(botAddOnTests);
			tests.remove(tests.size() - 1);

			System.out.println(tests.size() + " tests left).");

			topHalf = new LinkedList<>(tests.subList(0, tests.size() / 2));
			botHalf = new LinkedList<>(tests.subList(tests.size() / 2, tests.size()));
		}

		if (!tests.isEmpty()) {
			// Verify this is a dependency
            verifyAndAddDependency(tests.get(0), topAddOnTests, botAddOnTests, isOriginalOrder, revealingOrder, revealed);
		}
	}

    private void verifyAndAddDependency(final String testName,
                                        final List<String> topAddOnTests, final List<String> botAddOnTests,
                                        final boolean isOriginalOrder,
                                        final List<String> revealingOrder,
                                        final RESULT revealed) {
	    System.out.println("Trying to verify dependency: " + testName);

	    // If we already know about this, exit.
	    if (knownDependencies.getOrDefault(dependentTestName, new HashSet<>()).stream()
                .anyMatch(td -> td.hasDependency(testName))) {
	        return;
        }

	    final List<String> orderedTests = new ArrayList<>(topAddOnTests);
	    orderedTests.addAll(botAddOnTests);
	    orderedTests.add(dependentTestName);

        final TestExecResult without = makeAndRunTestOrder(orderedTests);

        orderedTests.add(topAddOnTests.size(), testName); // Add right before the botAddOnTests.
        final TestExecResult with = makeAndRunTestOrder(orderedTests);

        if (with.getResult(dependentTestName).result != without.getResult(dependentTestName).result) {
            if (with.getResult(dependentTestName).result == dependentTestResult) {
                System.out.println("Found dependency (before dependency): " + testName);
                addDependency(testName, revealed, revealingOrder, true);
            } else if (without.getResult(dependentTestName).result == dependentTestResult){
                System.out.println("Found dependency (after dependency): " + testName);
                addDependency(testName, revealed, revealingOrder, false);
            }
        }
    }

    private void dependentTestSolveSequential(List<String> tests,
                                              boolean isOriginalOrder,
                                              List<String> addOnTests,
                                              RESULT revealed,
                                              List<String> revealingOrder) {
	    final List<String> verifyContainsDependency = new ArrayList<>(tests);
	    verifyContainsDependency.addAll(addOnTests);
	    verifyContainsDependency.add(dependentTestName);

        if (!containsDependency(isOriginalOrder, verifyContainsDependency)) {
            return;
        }

        final List<String> foundDependencies = new ArrayList<>();

	    // Try removing tests one at a time to see if they affect the result.
        while (tests.size() > 0) {
            final String testName = tests.remove(0);

            final List<String> orderedTests = new ArrayList<>(foundDependencies);

            orderedTests.addAll(tests);
            orderedTests.addAll(addOnTests);
            orderedTests.add(dependentTestName);

            final boolean hasDep = containsDependency(isOriginalOrder, orderedTests);

            if (hasDep) {
                System.out.println("Solving sequentially (" + tests.size() + " tests left).");
            } else {
                System.out.println("Solving sequentially (" + tests.size() + " tests left). Found: " + testName);

                foundDependencies.add(testName);
            }
        }

        for (final String dependency : foundDependencies) {
            addDependency(dependency, revealed, revealingOrder, isOriginalOrder);
        }
    }
}
