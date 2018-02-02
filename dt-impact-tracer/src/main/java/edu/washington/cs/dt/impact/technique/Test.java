/**
 * Copyright 2014 University of Washington. All Rights Reserved.
 * @author Wing Lam
 *
 *         Contains methods and fields used by all test techniques.
 */

package edu.washington.cs.dt.impact.technique;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import edu.washington.cs.dt.impact.data.TestData;
import edu.washington.cs.dt.impact.data.TestFunctionStatement;
import edu.washington.cs.dt.impact.order.Standard;
import edu.washington.cs.dt.impact.tools.FileTools;
import edu.washington.cs.dt.impact.util.Constants;
import edu.washington.cs.dt.impact.util.Constants.COVERAGE;

public class Test {

    // all lines specific to the type of COVERAGE specified
    protected Set<String> allCoverageLines;
    protected List<TestFunctionStatement> methodList;
    protected List<TestFunctionStatement> allMethodList;
    protected Standard orderObj;
    private final Map<String, Set<String>> testToAllLines;
    protected List<String> origOrderList = null;

    public Test(File folder, COVERAGE coverage, File dependentTestsFile, File origOrder) {
    	origOrderList = FileTools.parseFileToList(origOrder);
        allCoverageLines = new HashSet<String>();
        testToAllLines = new HashMap<String, Set<String>>();
        if (allMethodList == null) {
            setAllLines(folder);
            allMethodList = listFilesForFolder(coverage);
        }
        methodList = new ArrayList<TestFunctionStatement>(allMethodList);
        if (dependentTestsFile != null) {
            processDependentTests(dependentTestsFile, allMethodList);
        }
    }

    public Test(COVERAGE coverage, Map<String, Set<TestData>> knownDependencies, File folder) {
        testToAllLines = new HashMap<String, Set<String>>();
        allCoverageLines = new HashSet<String>();
        setAllLines(folder);
        allMethodList = listFilesForFolder(coverage);
        methodList = new ArrayList<TestFunctionStatement>(allMethodList);

        processDependentTests(knownDependencies,allMethodList);
    }

    public void resetDTList(final Map<String, Set<TestData>> knownDependencies) {
        if (knownDependencies != null) {
            processDependentTests(knownDependencies, allMethodList);
        }
    }

    private void processDependentTests(File dependentTestsFile,
                                       List<TestFunctionStatement> dtMethodList) {
        processDependentTests(parseDependentTestsFile(dependentTestsFile),
                              dtMethodList);
    }

    private void processDependentTests(Map<String, Set<TestData>> knownDependencies,
                                       List<TestFunctionStatement> dtMethodList) {
        // list of tests that when executed before reveals the dependent test
        Map<String, List<String>> execBefore = new HashMap<String, List<String>>();
        // list of tests that when executed after reveals the dependent test
        Map<String, List<String>> execAfter = new HashMap<String, List<String>>();

        Map<String, TestFunctionStatement> nameToMethodData = new HashMap<String, TestFunctionStatement>();
        for (TestFunctionStatement methodData : dtMethodList) {
            nameToMethodData.put(methodData.getName(), methodData);
        }

        processKnownDependencies(knownDependencies, execBefore, execAfter);

        for (String testName : execBefore.keySet()) {
            if (nameToMethodData.get(testName) == null && origOrderList.contains(testName)) {
                TestFunctionStatement tmd = new TestFunctionStatement(testName);
                nameToMethodData.put(testName, tmd);
            }
            for (String dtTest : execBefore.get(testName)) {
                TestFunctionStatement tmd = nameToMethodData.get(dtTest);
                if (tmd == null && origOrderList.contains(dtTest)) {
                    tmd = new TestFunctionStatement(dtTest);
                    nameToMethodData.put(dtTest, tmd);
                }
                if (nameToMethodData.get(testName) != null && tmd != null) {
                    nameToMethodData.get(testName).addDependentTest(tmd, true);
                }
            }
            if (nameToMethodData.get(testName) != null) {
                nameToMethodData.get(testName).reset();
            }
        }

        for (String testName : execAfter.keySet()) {
            if (nameToMethodData.get(testName) == null && origOrderList.contains(testName)) {
                TestFunctionStatement tmd = new TestFunctionStatement(testName);
                nameToMethodData.put(testName, tmd);
            }
            for (String dtTest : execAfter.get(testName)) {
                TestFunctionStatement tmd = nameToMethodData.get(dtTest);
                if (tmd == null && origOrderList.contains(dtTest)) {
                    tmd = new TestFunctionStatement(dtTest);
                    nameToMethodData.put(dtTest, tmd);
                }
                if (nameToMethodData.get(testName) != null && tmd != null) {
                    nameToMethodData.get(testName).addDependentTest(tmd, false);
                }
            }
            if (nameToMethodData.get(testName) != null) {
                nameToMethodData.get(testName).reset();
            }
        }
    }

    private void processKnownDependencies(Map<String, Set<TestData>> knownDependencies,
                                          Map<String, List<String>> execBefore,
                                          Map<String, List<String>> execAfter) {
        for (final Map.Entry<String, Set<TestData>> dependency : knownDependencies.entrySet()) {
            final Set<TestData> dependencies = dependency.getValue();

            execAfter.put(
                    dependency.getKey(),
                    dependencies.stream()
                            .flatMap(TestData::getAfterTests)
                            .collect(Collectors.toList()));

            execBefore.put(
                    dependency.getKey(),
                    dependencies.stream()
                            .flatMap(TestData::getBeforeTests)
                            .collect(Collectors.toList()));
        }
    }

    private Map<String, Set<TestData>> parseDependentTestsFile(File dependentTestsFile) {
        final Map<String, Set<TestData>> knownDependencies = new HashMap<>();

        try {
            final String contents =
                    new String(Files.readAllBytes(Paths.get(dependentTestsFile.getCanonicalPath())), StandardCharsets.UTF_8);

            for (final String testDataStr : contents.split("\n\n")) {
                final TestData testData = TestData.fromString(testDataStr);

                knownDependencies.merge(testData.dependentTest, Collections.singleton(testData),
                        (testName, dependencies) -> {
                            dependencies.add(testData);
                            return dependencies;
                        });
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return knownDependencies;
    }

    private List<TestFunctionStatement> listFilesForFolder(final COVERAGE coverage) {
        List<TestFunctionStatement> methodList = new LinkedList<TestFunctionStatement>();
        for (String testName : testToAllLines.keySet()) {
            TestFunctionStatement methodData = new TestFunctionStatement(testName);
            for (String line : testToAllLines.get(testName)) {
                if (coverage == COVERAGE.STATEMENT) {
                    allCoverageLines.add(line);
                    methodData.addLine(line);
                } else if (coverage == COVERAGE.FUNCTION) {
                    String functionName = line.split(" ")[0];
                    allCoverageLines.add(functionName);
                    methodData.addLine(functionName);
                }
            }
            methodData.reset();
            methodList.add(methodData);
        }
        return methodList;
    }

    private void setAllLines(final File inputTestFolder) {
        if (inputTestFolder == null) {
            throw new RuntimeException("sootOutput is missing some required classes.");
        }
        for (final File fileEntry : inputTestFolder.listFiles()) {
            if (fileEntry.isFile() && !fileEntry.getName().startsWith(".") && !fileEntry.isHidden()) {
                BufferedReader br;
                try {
                    if (origOrderList.contains(fileEntry.getName())) {
                        br = new BufferedReader(new FileReader(fileEntry));
                        Set<String> lines = new HashSet<String>();

                        String line;
                        while ((line = br.readLine()) != null) {
                            lines.add(line);
                        }
                        testToAllLines.put(fileEntry.getName(), lines);
                        br.close();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                continue;
            }
        }
    }

    protected void parseOrigOrderListToMethodList(List<String> origOrder,
            Map<String, TestFunctionStatement> nameToMethodData) {
        methodList.clear();
        for (String line : origOrder) {
            if (nameToMethodData.containsKey(line)) {
                methodList.add(nameToMethodData.get(line));
            }
        }
    }

    protected Map<String, TestFunctionStatement> getNameToMethodData(List<TestFunctionStatement> methodList) {
        Map<String, TestFunctionStatement> nameToMethodData = new HashMap<String, TestFunctionStatement>();
        for (TestFunctionStatement methodData : methodList) {
            nameToMethodData.put(methodData.getName(), methodData);
        }
        return nameToMethodData;
    }

    public void printResults() {
        orderObj.applyDeps();
        orderObj.printResults();
    }

    public List<TestFunctionStatement> getResults(int machine) {
        orderObj.applyDeps();
        return orderObj.getMethodList();
    }

    public List<TestFunctionStatement> getCoverage(int machine) {
        orderObj.applyDeps();
        return orderObj.getCoverage(false);
    }
}
