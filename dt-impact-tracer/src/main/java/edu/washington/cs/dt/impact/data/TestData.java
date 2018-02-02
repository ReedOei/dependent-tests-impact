package edu.washington.cs.dt.impact.data;

import edu.washington.cs.dt.RESULT;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestData {
    public final String dependentTest;
    public final RESULT intended;
    public final Set<String> beforeTests;
    public final Set<String> afterTests;
    public final RESULT revealed;

    private final List<String> revealingOrder;

    public TestData(final String dependentTest,
                    final RESULT intended,
                    final Set<String> beforeTests,
                    final Set<String> afterTests,
                    final RESULT revealed,
                    final List<String> revealingOrder) {
        this.dependentTest = dependentTest;
        this.intended = intended;
        this.beforeTests = beforeTests;
        this.afterTests = afterTests;
        this.revealed = revealed;
        this.revealingOrder = revealingOrder;
    }

    /**
     * Trims whitespace.
     * @param str The string to parse. Should look like "[a, b, c, d]"
     * @return The stream of strings.
     */
    private static Stream<String> parseStrToStream(final String str) {
        return Arrays.stream(str.substring(1, str.length() - 1).split(","))
                .map(String::trim);
    }

    /**
     * Parses the string for the information contained in the test data object.
     * Format is the same as the output format from TestData#toString().
     */
    public static TestData fromString(final String str) {
        final String[] lines = str.split("\n");

        // Validate we have the correct number of lines and all lines contain ':'. If they don't,
        // then we will get ArgumentOutOfBoundsExceptions below.
        if (lines.length != 6) {
            throw new IllegalArgumentException("The string passed in does not contain the correct " +
                    "number of lines! Expected: 6, Got: " + lines.length);
        }

        if (!Arrays.stream(lines).allMatch(s -> s.contains(":"))) {
            throw new IllegalArgumentException("All lines must contain ':'.");
        }

        final String[] nameLine = lines[0].split(":");
        final String dependentTest = nameLine[1].trim();

        final String[] intendedLine = lines[1].split(":");
        final RESULT intended = RESULT.valueOf(intendedLine[1].trim());

        final String[] beforeTestsLine = lines[2].split(":");
        final Set<String> beforeTests =
                parseStrToStream(beforeTestsLine[1].trim())
                .collect(Collectors.toSet());

        final String[] afterTestsLine = lines[3].split(":");
        final Set<String> afterTests =
                parseStrToStream(afterTestsLine[1].trim())
                .collect(Collectors.toSet());

        final String[] revealedLine = lines[4].split(":");
        final RESULT revealed = RESULT.valueOf(revealedLine[1].trim());

        final String[] revealingOrderLine  = lines[5].split(":");
        final List<String> revealingOrder =
                parseStrToStream(revealingOrderLine[1].trim())
                .collect(Collectors.toList());

        return new TestData(dependentTest, intended, beforeTests, afterTests, revealed, revealingOrder);
    }

    @Override
    public String toString() {
        return  "Test: " + dependentTest + "\n" +
                "Intended behavior: " + intended + "\n" +
                "when executed after: " + beforeTests + "\n" +
                "and executed before: " + afterTests + "\n" +
                "The revealed behavior: " + revealed + "\n" +
                "in the order: " + revealingOrder;
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
                if (testIndex != -1 && testIndex < index) {
                    currentOrder.remove(testIndex);
                    currentOrder.add(index + 1, dependency);
                }
            }
        }
    }

    /**
     * @param testOrder The order to get the indices from.
     * @return a stream containing the indices of each dependency stored in this class, if they are
     * in the order passed in.
     */
    public Stream<Integer> getIndices(final List<String> testOrder) {
        return Stream.concat(
                beforeTests.stream().map(testOrder::indexOf),
                afterTests.stream().map(testOrder::indexOf)
        );
    }

    public Stream<String> getAfterTests() {
        return afterTests.stream();
    }

    public Stream<String> getBeforeTests() {
        return beforeTests.stream();
    }

    @Override
    public final boolean equals(final Object o) {
        if (o instanceof TestData) {
            final TestData other = (TestData) o;

            return  dependentTest.equals(other.dependentTest) &&
                    intended.equals(other.intended) &&
                    beforeTests.equals(other.beforeTests) &&
                    afterTests.equals(other.afterTests) &&
                    revealed.equals(other.revealed) &&
                    revealingOrder.equals(other.revealingOrder);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependentTest, intended, beforeTests, afterTests, revealed, revealingOrder);
    }
}

