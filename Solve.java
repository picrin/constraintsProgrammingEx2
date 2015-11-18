import java.util.Scanner;
import java.util.InputMismatchException;
import java.io.File;
import java.io.IOException;
import org.chocosolver.solver.*;
import org.chocosolver.solver.variables.*;
import org.chocosolver.solver.constraints.*;
import org.chocosolver.solver.search.loop.monitors.SearchMonitorFactory;
import org.chocosolver.solver.search.strategy.*;
import org.chocosolver.solver.search.strategy.selectors.*;
import org.chocosolver.solver.search.strategy.selectors.variables.*;
import org.chocosolver.solver.search.strategy.strategy.*;

public class Solve {
	public static void main(String[] args) {
		new Solve()	.readProblem(args)
					.setupSolver()
					.addVariables()
					.findPotentiallyParallelMeetings()
					.addConstraints()
					.timeout(args)
					.chooseHeuristic(args)
					.solveProblem()
					.printSolution()
					.printStats()
					.exitSuccessfuly();
	}

	final static String USAGE = "Usage is Solve|Optimise <problemFilename> [<timeout>] [<strategy>]";

	public Solve() {

	}

	String terminationMessage;

	public Solve thisOrDie() {
		if (terminationMessage != null) {
			System.err.println(terminationMessage);
			System.exit(-1);
		}
		return this;
	}

	int noMeetings;
	int noAgents;
	int noTimeslots;
	IntVar[] meetings;
	int[][] distance;
	boolean[][] canOccurInParallel;
	boolean[][] isAttended;

	public Solve readProblem(String[] args) {
		int i, j, value;
		boolean isAttending;
		try (Scanner sc = new Scanner(new File(args[0]))) {
			noMeetings = sc.nextInt();
			noAgents = sc.nextInt();
			noTimeslots = sc.nextInt();
			distance = new int[noMeetings][noMeetings];
			canOccurInParallel = new boolean[noMeetings][noMeetings];
			isAttended = new boolean[noMeetings][noAgents];
			for (i = 0; i < noAgents; i++) {
				sc.next();
				for (j = 0; j < noMeetings; j++) {
					value = sc.nextInt();
					isAttending = (value == 1);
					isAttended[j][i] = isAttending;
				}
			}
			for (i = 0; i < noMeetings; i++) {
				sc.next();
				for (j = 0; j < noMeetings; j++) {
					value = sc.nextInt();
					distance[i][j] = value;
				}
			}
		} catch (IOException e) {
			terminationMessage = "Can't open file " + args[0]
					+ ". Check that file exists";
		} catch (NullPointerException e) {
			terminationMessage = USAGE;
		} catch (ArrayIndexOutOfBoundsException e) {
			terminationMessage = USAGE;
		} catch (InputMismatchException e) {
			terminationMessage = "Make sure your input has appropriate format.";
		}
		return thisOrDie();
	}

	Solver solver;
	boolean solutionFound = false;

	public Solve setupSolver() {
		solver = new Solver("Meeting Scheduling Problem");
		return thisOrDie();
	}

	public Solve addVariables() {
		meetings = VF.enumeratedArray("all meetings", noMeetings, 0,
				noTimeslots - 1, solver);
		return thisOrDie();
	}

	public boolean canOccurInParallel(int meetingAIndex, int meetingBIndex) {
		for (int agent = 0; agent < noAgents; agent++) {
			boolean agentGoesToMeetingA = isAttended[meetingAIndex][agent];
			boolean agentGoesToMeetingB = isAttended[meetingBIndex][agent];
			if (agentGoesToMeetingA && agentGoesToMeetingB) {
				return false;
			}
		}
		return true;
	}

	public Solve findPotentiallyParallelMeetings() {
		for (int meetingA = 0; meetingA < noMeetings; meetingA++) {
			for (int meetingB = 0; meetingB < noMeetings; meetingB++) {
				canOccurInParallel[meetingA][meetingB] = canOccurInParallel(
						meetingA, meetingB);
			}
		}
		return thisOrDie();
	}

	public Solve addConstraints() {
		for (int meetingA = 0; meetingA < noMeetings; meetingA++) {
			for (int meetingB = meetingA
					+ 1; meetingB < noMeetings; meetingB++) {
				if (!canOccurInParallel[meetingA][meetingB]) {
					Constraint c1 = ICF.arithm(meetings[meetingA], "-",
							meetings[meetingB], ">",
							distance[meetingA][meetingB]);
					Constraint c2 = ICF.arithm(meetings[meetingB], "-",
							meetings[meetingA], ">",
							distance[meetingA][meetingB]);
					Constraint or = LCF.or(c1, c2);
					solver.post(or);
				}

			}
		}
		return thisOrDie();
	}

	public Solve chooseHeuristic(String[] args) {
		String strategyStr = null;
		if (args.length > 1) {
			try {
				Integer.parseInt(args[1]);
				if (args.length > 2) {
					strategyStr = args[2];
				} else {
					strategyStr = "DomOverWDeg";
				}
			} catch (NumberFormatException e) {
				strategyStr = args[1];
			}
		} else {
			strategyStr = "FirstFail";
		}
		AbstractStrategy<IntVar> strategy = null;
		VariableSelector<IntVar> variableSelector = null;
		IntValueSelector valueSelector = null;
		if (meetings.length > 1) {
			switch (strategyStr) {
			case "DomOverWDeg":
				valueSelector = IntStrategyFactory.min_value_selector();
				strategy = new DomOverWDeg(meetings, 100, valueSelector);
				break;
			case "FirstFail":
				variableSelector = new FirstFail();
				valueSelector = IntStrategyFactory.min_value_selector();
				strategy = IntStrategyFactory.custom(variableSelector,
						valueSelector, meetings);
				break;
			case "Occurrence":
				variableSelector = new Occurrence<IntVar>();
				valueSelector = IntStrategyFactory.min_value_selector();
				strategy = IntStrategyFactory.custom(variableSelector,
						valueSelector, meetings);
				break;
			case "AntiFirstFail":
				variableSelector = new AntiFirstFail();
				valueSelector = IntStrategyFactory.min_value_selector();
				strategy = IntStrategyFactory.custom(variableSelector,
						valueSelector, meetings);
				break;
			case "None":
				return thisOrDie();
			case "ActivityBased":
				strategy = new ActivityBased(solver, meetings, 0.5, 0.5, 1, 1,
						100);
				break;
			default:
				terminationMessage = "you've chosen incorrect strategy, try DomOverWDeg, FirstFail, Occurrence or AntiFirstFail, None or ActivityBased";
				break;
			}
		}
		solver.set(strategy);
		return thisOrDie();

	}

	public Solve solveProblem() {
		solutionFound = solver.findSolution();
		return thisOrDie();
	}

	public Solve printSolution() {
		if (solutionFound) {
			for (int i = 0; i < noMeetings; i++) {
				System.out.println(i + " " + meetings[i].getValue());
			}

		} else {
			System.out.println(false);
		}
		return thisOrDie();
	}

	public Solve printStats() {
		System.out.println();
		System.out.println(solver	.getMeasures()
									.getNodeCount());
		System.out.println(solver	.getMeasures()
									.getTimeCount());
		return thisOrDie();
	}

	public Solve exitSuccessfuly() {
		System.exit(0);
		return this;
	}

	/**
	 * Stub of a method to be overridden by Optimise.
	 */

	long timeout;
	
	public Solve timeout(String[] args) {
		if (args.length > 1) {
			try {
				timeout = Integer.parseInt(args[1]);
				SearchMonitorFactory.limitTime(solver, timeout);
			} catch (NumberFormatException e) {
			}
		}
		return thisOrDie();
	}
}
