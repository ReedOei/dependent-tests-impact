package edu.washington.cs.dt.impact.figure.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import edu.washington.cs.dt.impact.data.Project;
import edu.washington.cs.dt.impact.data.ProjectEnhancedResults;
import edu.washington.cs.dt.impact.data.ProjectNumDependentTests;
import edu.washington.cs.dt.impact.data.ProjectPrecomputedDependencesTime;
import edu.washington.cs.dt.impact.tools.FileTools;
import edu.washington.cs.dt.impact.util.Constants;

public abstract class FigureGenerator {
    protected static final DecimalFormat apfdFormat = new DecimalFormat(".00");
    protected static final DecimalFormat timeFormat = new DecimalFormat("#\\%");
    protected static final DecimalFormat percentFormat = new DecimalFormat("0");
    protected static boolean allowNegatives = false;
    protected static final int NUM_PRIOR_TECHNIQUES = 4;
    protected static final int NUM_SELE_TECHNIQUES = 6;
    protected static final int NUM_PARA_TECHNIQUES = 8;
    protected static final int NUM_PARA_NO_K_TECHNIQUES = 2;

    /*
     * A public method to search a file for a keyword and return the value that follows
     * that keyword
     *
     * @return the data value without any leading or trailing whitespaces, null if keyword not found
     */
    public static String parseFile(File file, String keyword) {
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String currLine = scanner.nextLine();
                if (currLine.contains(keyword)) {
                    // gets numeric value of data
                    String data = currLine.substring(keyword.length(), currLine.length());
                    scanner.close(); // close Scanner before returning
                    // trim away any whitespaces leading or after the data value
                    return data.trim();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(2);
        } finally {
            scanner.close();
        }
        return null; // none of the lines contained the keyword
    }

    public static void exitWithError(String message) {
        System.err.println(message);
        Thread.dumpStack();
        System.exit(0);
    }

    public static String formatAPFD(double num) {
        double val = num;
        if (!allowNegatives && val < 0.0) {
            val = 0.0;
        }
        String output = apfdFormat.format(val);
        if (output.equals("-.00")) {
            output = ".00";
        }
        return output;
    }

	public static Map<String, Integer> parseDTFile(String fileName) {
		List<String> dtFile = FileTools.parseFileToList(new File(fileName));
		Map<String, Integer> projectToDT = new HashMap<String, Integer>();
		for (String s : dtFile) {
			String[] sArr = s.split("\\|");
			projectToDT.put(sArr[0], Integer.parseInt(sArr[1]));
		}
		return projectToDT;
	}

	/*
     * A public method to search a file for a keyword and return the value that follows
     * that keyword
     *
     * @return the data value without any leading or trailing whitespaces, null if keyword not found
     */
    public static int parseFileForNumOfDTs(File file, String keyword) {
        int numDTs = 0;
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String currLine = scanner.nextLine();
                if (currLine.contains(keyword)) {
                    // gets numeric value of data
                    String data = currLine.substring(keyword.length(), currLine.length());
                    // trim away any whitespaces leading or after the data value
                    numDTs += Integer.valueOf(data.trim());
                }
            }
            scanner.close(); // close Scanner before returning
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(2);
        } finally {
            scanner.close();
        }
        return numDTs; // none of the lines contained the keyword
    }
    
    public static List<String> parseFileForDTs(File file, String keyword, boolean findLast) {
        List<String> DTs = new ArrayList<String>();
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String currLine = scanner.nextLine();
                if (currLine.contains(keyword)) {
                	// Ex. [randoop.jfreechart.RandoopTest1.test300, randoop.jfreechart.RandoopTest4.test270, randoop.jfreechart.RandoopTest0.test79]
                	currLine = scanner.nextLine();
                	currLine = currLine.substring(1, currLine.length() - 1);
                	if (findLast) {
                		DTs.clear();
                	}
                	DTs.addAll(Arrays.asList(currLine.split(", ")));
                }
            }
            scanner.close(); // close Scanner before returning
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(2);
        } finally {
            scanner.close();
        }
        return DTs; // none of the lines contained the keyword
    }

    public static double parseFileForMaxTime(File file, String keyword) {
        double maxTime = 0.0;
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String currLine = scanner.nextLine();
                if (currLine.contains(keyword)) {
                    double data = Double.parseDouble(currLine.substring(keyword.length(), currLine.length()).trim());
                    if (data > maxTime) {
                        maxTime = data;
                    }
                }
            }
            scanner.close(); // close Scanner before returning
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(2);
        } finally {
            scanner.close();
        }
        return maxTime; // none of the lines contained the keyword
    }

    /*
     * A public method to search a file for a keyword and return the value that follows
     * that keyword
     *
     * @return the data value without any leading or trailing whitespaces, null if keyword not found
     */
    public static String getNextLine(File file, String currLineKeyword, String nextLineKeyword) {
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String currLine = scanner.nextLine();
                if (currLine.contains(currLineKeyword)) {
                    currLine = scanner.nextLine();
                    while (!currLine.contains(nextLineKeyword)) {
                        currLine = scanner.nextLine();
                    }
                    return scanner.nextLine();
                }
            }
            scanner.close(); // close Scanner before returning
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(2);
        } finally {
            scanner.close();
        }
        return null; // none of the lines contained the keyword
    }

    protected static String formatPercent(double num, boolean addPhantomZeros) {
    	StringBuilder sb = new StringBuilder();

        String diffStringFormat = timeFormat.format(num);
        if (diffStringFormat.equals("-0\\%")) {
            diffStringFormat = "0\\%";
        }
        
        if (addPhantomZeros) {
            if (num < 1.0) {
            	sb.append("\\z");
            }
            
            if (num < 0.10) {
            	sb.append("\\z");
            }
        }
        
        sb.append(diffStringFormat);
        
        return sb.toString();
    }

    /*
     * a public method that gets the line with all the flags in the file
     * that line starts with "-technique"
     */
    public static String getFlagsLine(File file, String keyword, boolean getSameLine) {
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String currLine = scanner.nextLine();
                if (currLine.contains(keyword)) {
                	if (!getSameLine) {
                        currLine = scanner.nextLine();                		
                	}
                    scanner.close(); // close Scanner before returning
                    return currLine;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(2);
        } finally {
            scanner.close();
        }

        return null; // none of the lines contained the keyword -technique
    }

    /*
     * A public method that returns the name of the argument specified by the flag
     *
     * @return the name of the argument corresponding to flag
     */
    public static String mustGetArgName(List<String> argsList, String flag) {
        String flagName = null;
        int index = argsList.indexOf(flag);
        if (index != -1) {
            int nameIndex = index + 1;
            if (nameIndex >= argsList.size()) {
                System.err.println(flag + " argument is specified but flagName is not." + " Please use the format: "
                        + flag + " flagName");
                System.exit(0);
            }
            flagName = argsList.get(nameIndex);
        } else {
            System.err
                    .println("No " + flag + " argument is specified." + " Please use the format: " + flag + " flagName");
            System.exit(0);
        }
        return flagName;
    }
    
    public static String getArgName(List<String> argsList, String flag) {
        String flagName = null;
        int index = argsList.indexOf(flag);
        if (index != -1) {
            int nameIndex = index + 1;
            if (nameIndex >= argsList.size()) {
                System.err.println(flag + " argument is specified but flagName is not." + " Please use the format: "
                        + flag + " flagName");
                return null;
            }
            flagName = argsList.get(nameIndex);
        } else {
            System.err
                    .println("No" + flag + " argument is specified." + " Please use the format: " + flag + " flagName");
            return null;
        }
        return flagName;
    }

    public static int getK(int i) {
        if (i == 0) {
            return 2;
        } else if (i == 2) {
            return 4;
        } else if (i == 4) {
            return 8;
        } else if (i == 6) {
            return 16;
        }
        return 1;
    }

    /*
     * a public method that searches a List<Project> objects for the project that matches projName
     *
     * @return -1 if no match found, otherwise index of the project with projName
     */

    public static int indexOfByName(List<Project> projList, String projName) {
        int index = 0;
        for (Project temp : projList) {
            if (temp.getName().equals(projName)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public static void sortList(List<Project> projList, List<Project> sortedList, String keyword) {
        for (Project temp : projList) {
            if (temp.getName().equals(keyword)) {
                sortedList.add(temp);
                return;
            }
        }
    }
    
	protected static String flagsInFile;
	protected static String techniqueName;
	protected static String coverageName;
	protected static String orderName;
	protected static String projectName;
	protected static String testType;
	protected static List<Project> currProjList;
	protected static Project currProj;
	protected static String timeInFile;
	protected static double maxTimeInFile;
	protected static int numOfFixedDTs;
	protected static String resolveDependences;
	protected static int numTotal;
	protected static List<String> totalDTs;
	protected static File file;
	protected static String coverageInFile;
	protected static List<String> flagsList;

	/**
	 * @param files
	 * @param fg
	 * @param ignoreDTFFlag false to ignore files that have the -dependentTestFile flag.
	 * True to consider the file regardless of whether it has the -dependentTestFile or not,
	 * @param proj_orig_arrayList
	 * @param proj_auto_arrayList
	 */
	protected static void parseFiles(File[] files, FigureGenerator fg, boolean ignoreDTFFlag,
			List<Project> proj_orig_arrayList, List<Project> proj_auto_arrayList) {
        for (File file : files) {
            if (file.isFile()) {
            	FigureGenerator.file = file;
				// String containing all the flags
				flagsInFile = getFlagsLine(file, Constants.ARGUMENT_STRING, false);
				if (flagsInFile == null) {
					continue;
				}
				// get rid of square brackets
				flagsInFile = flagsInFile.substring(1, flagsInFile.length() - 1);
				String[] flags = flagsInFile.split(",");
				flagsList = Arrays.asList(flags);
				// get rid of whitespaces
				for (int i = 0; i < flagsList.size(); i++) {
					flagsList.set(i, flagsList.get(i).trim());
				}
				int index = flagsList.indexOf("-technique");
				// index + 1 because want arg after the index of flag
				techniqueName = flagsList.get(index + 1);

				index = flagsList.indexOf("-coverage");
				coverageName = flagsList.get(index + 1);

				index = flagsList.indexOf("-order");
				orderName = flagsList.get(index + 1);

				index = flagsList.indexOf("-project");
				projectName = flagsList.get(index + 1);

				index = flagsList.indexOf("-testType");
				testType = flagsList.get(index + 1);

				index = flagsList.indexOf("-dependentTestFile");
				if (index != -1 && !ignoreDTFFlag) { // only count files without dependentTestFile
					continue;
				}

                resolveDependences = null;
                // if index = -1, flag not present
                if (index != -1) {
                    resolveDependences = flagsList.get(index);
                }

                coverageInFile = getFlagsLine(file, Constants.COVERAGE_STRING, false);

				// see if List needs to orig or auto generated one
				currProjList = null;
				if (testType.equals("auto")) {
					currProjList = proj_auto_arrayList;
				} else if (testType.equals("orig")) {
					currProjList = proj_orig_arrayList;
				} else {
					exitWithError("Invalid test type. Test type is " + testType);
				}

				// index of this project in the arrayList, might be -1 if not
				// found
				int indexOfProj = indexOfByName(currProjList, projectName);

				// Project Object that corresponds to the current project name
				// in this file
				Project currProj2 = null;

				if (indexOfProj != -1) {
					currProj2 = currProjList.get(indexOfProj);
				} else {// projectName not seen before
					if (fg.getClass().equals(NumDependentTestsFigureGenerator.class)) {
						currProj2 = new ProjectNumDependentTests(projectName);
					} else if (fg.getClass().equals(EnhancedResultsFigureGenerator.class)) {
						currProj2 = new ProjectEnhancedResults(projectName);
					} else if (fg.getClass().equals(PrecomputedTimeFigureGenerator.class)) {
						currProj2 = new ProjectPrecomputedDependencesTime(projectName);
					} else {
						// current figure generator doesn't care about the project type
						currProj2 = new ProjectNumDependentTests(projectName);
					}
					currProjList.add(currProj2);
				}
				currProj = currProj2;

				// get the number of dts
				totalDTs = parseFileForDTs(file, Constants.NOT_FIXED_DTS, false);

				numTotal = parseFileForNumOfDTs(file, Constants.NUM_NOT_FIXED_DTS);

				timeInFile =  getFlagsLine(file, Constants.TIME_STRING, false);

				numOfFixedDTs = parseFileForNumOfDTs(file, Constants.FIXED_DTS);
				maxTimeInFile = parseFileForMaxTime(file, Constants.TIME_INCL_DTF);
                if (techniqueName.equals("parallelization")) {
                	fg.doParaCalculations();
                } // selection technique, figure 18
                else if (techniqueName.equals("selection")) {
                	fg.doSeleCalculations();
                } // prioritization techinque, figure 17
                else if (techniqueName.equals("prioritization")) {
                	fg.doPrioCalculations();
                } else {
                    exitWithError("Unexpected techniqueName: " + techniqueName);
                }
            }
        }
	}

	public abstract void doParaCalculations();
	public abstract void doPrioCalculations();
	public abstract void doSeleCalculations();

    private static Comparator<Project> ALPHABETICAL_ORDER = new Comparator<Project>() {
        public int compare(Project str1, Project str2) {
            int res = String.CASE_INSENSITIVE_ORDER.compare(str1.getName(), str2.getName());
            if (res == 0) {
                res = str1.getName().compareTo(str2.getName());
            }
            return res;
        }
    };
    
    public static void sortList(List<Project> projList) {
        Collections.sort(projList, ALPHABETICAL_ORDER);
    }

    /*
     * a public method that writes to the output file specified using the given latex string
     *
     */

    public static void writeToLatexFile(String latex, String outputFileName, boolean append) {
        try {
            File outputFile = new File(outputFileName);
            FileWriter writer = new FileWriter(outputFile, append);
            BufferedWriter bw = new BufferedWriter(writer);
            bw.write(latex);
            bw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(2);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
        }
    }

}
