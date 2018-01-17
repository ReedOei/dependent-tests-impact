package edu.washington.cs.dt.impact.data;

import edu.washington.cs.dt.RESULT;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestData {
    private final String dependentTest;
    private final RESULT intended;
    private final Set<String> beforeTests;
    private final Set<String> afterTests;
    private final RESULT revealed;

    public TestData(final String dependentTest,
                    final RESULT intended,
                    final Set<String> beforeTests,
                    final Set<String> afterTests,
                    final RESULT revealed) {
        this.dependentTest = dependentTest;
        this.intended = intended;
        this.beforeTests = beforeTests;
        this.afterTests = afterTests;
        this.revealed = revealed;
    }

    @Override
    public String toString() {
        return  "Test: " + dependentTest + "\n" +
                "Intended behavior: " + intended + "\n" +
                "when executed after: " + beforeTests + "\n" +
                "and executed before: " + afterTests + "\n" +
                "The revealed behavior: " + revealed + "\n";
    }

    public TestData mergeWith(final TestData other) {
        final Set<String> newBeforeTests = new HashSet<>(beforeTests);
        newBeforeTests.addAll(other.beforeTests);

        final Set<String> newAfterTests = new HashSet<>(afterTests);
        newAfterTests.addAll(other.afterTests);

        return new TestData(dependentTest, intended, newBeforeTests, newAfterTests, revealed);
    }

    public void addBefore(final String testName) {
        beforeTests.add(testName);
    }

    public void addAfter(final String testName) {
        afterTests.add(testName);
    }

    public boolean contains(final String testName) {
        return containsBefore(testName) || containsAfter(testName);
    }

    public boolean containsBefore(String testName) {
        return beforeTests.contains(testName);
    }

    public boolean containsAfter(String testName) {
        return afterTests.contains(testName);
    }

    /**
     * Ensures that the current order respects the dependencies contained in this class.
     */
    public void fixOrder(final List<String> currentOrder) {
        int index = currentOrder.indexOf(dependentTest);

        // Only needs fixing if this test is in the order to be run.
        if (index != -1) {
            for (final String dependency : beforeTests) {
                final int testIndex = currentOrder.indexOf(dependency);

                // If the dependent test isn't there, just add it
                if (testIndex == -1) {
                    currentOrder.add(index, dependency);
                } else if (testIndex > index) { // If it is, make sure that it comes before the test
                    currentOrder.remove(testIndex);
                    currentOrder.add(index, dependency);
                }
            }

            // Index could have changed above.
            index = currentOrder.indexOf(dependentTest);

            for (final String dependency : afterTests) {
                final int testIndex = currentOrder.indexOf(dependency);

                // If this test comes before the dependent test, then we need to move it to after.
                if (testIndex < index) {
                    currentOrder.remove(testIndex);
                    currentOrder.add(index + 1, dependency);
                }
            }
        }
    }
}