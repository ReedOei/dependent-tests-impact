package edu.washington.cs.dt.impact.util;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class TestMethodData implements Comparable<TestMethodData> {

    private String methodName;
    private Set<String> allLines;
    private Set<String> currentLines;

    public TestMethodData(String name) {
        this.methodName = name;
        currentLines = new LinkedHashSet<String>();
        allLines = new LinkedHashSet<String>();
    }

    public long getLineCount() {
        return currentLines.size();
    }

    public String getName() {
        return methodName;
    }

    public void addLine(String line) {
        allLines.add(line);
    }

    public void reset() {
        currentLines = new LinkedHashSet<String>(allLines);
    }

    public void removeLines(Set<String> lines) {
        currentLines.removeAll(lines);
    }

    public void retainLines(Set<String> lines) {
        currentLines.retainAll(lines);
    }

    public Set<String> getLines() {
        return Collections.unmodifiableSet(currentLines);
    }

    @Override
    public String toString() {
        return getName() + " : " + getLineCount();
    }

    @Override
    public int compareTo(TestMethodData o) {
        long mLineCount = getLineCount();
        long oLineCount = o.getLineCount();

        if (mLineCount < oLineCount) {
            return 1;
        } else if (mLineCount > oLineCount) {
            return -1;
        } else {
            return 0;
        }
    }
}
