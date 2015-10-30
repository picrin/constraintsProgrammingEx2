import org.chocosolver.solver.*;
import org.chocosolver.solver.variables.*;
import org.chocosolver.solver.constraints.*;

public class Optimise extends Solve {

	public static void main(String[] args) {
		new Optimise()	.readProblem(args)
						.setupSolver()
						.addVariables()
						.findPotentiallyParallelMeetings()
						.addConstraints()
						.solveProblem()
						.printSolution()
						.exitSuccessfuly();
	}

	/**
	 * 
	 * <p>
	 * We have to adjust the variable {@link #noTimeslots} to an upper bound
	 * such that we can be sure the problem can be satisfied. I have thought
	 * about 2 possible ways: take the distance graph and run Dijkstra's
	 * algorithm for a couple of random pairs of points, take the pair of points
	 * with the shortest path, and set the path as the new noTimeslots.
	 * </p>
	 *
	 * <p>
	 * The alternative I went with is simpler to implement, and was suggested as
	 * a hint: we can simply sum all entries in the first diagonal below the
	 * main diagonal, which gives us a length of a complete path, which visits
	 * every node in the graph in lexicographical order (meeting 0, meeting 1,
	 * etc.), and thus is guaranteed to satisfy the problem.
	 * </p>
	 * 
	 * <p>
	 * We can subsequently introduce a new IntVar, {@link #timeslots}, which
	 * will be optimised by the solver.
	 * </p>
	 * 
	 * TODO is second approach sensible?
	 */
	@Override
	public Solve addVariables() {
		noTimeslots = 0;
		for (int i = 0; i < noMeetings - 1; i++) {
			noTimeslots += (distance[i][i + 1] + 1);
		}
		noTimeslots += 1;
		timeslots = VF.bounded("timeslots", 0, noTimeslots - 1, solver);
		System.out.println(noTimeslots);
		super.addVariables();
		return thisOrDie();
	}

	/**
	 * <p>
	 * We have to add one more constraint per meeting, in a manner, which is
	 * going to limit the domain to at most {@link #timeslots}.
	 * </p>
	 * <p>
	 * {@link #timeslots} will be chosen by the solver during each subsequent
	 * iteration of Choco's optimisation loop.
	 * </p>
	 */
	@Override
	public Solve addConstraints() {
		super.addConstraints();
		for (int i = 0; i < noMeetings; i++) {
			Constraint c = ICF.arithm(meetings[i], "<=", timeslots);
			solver.post(c);
		}
		return thisOrDie();
	}

	/**
	 * @see Solve#solveProblem()
	 */
	@Override
	public Solve solveProblem() {
		solver.findOptimalSolution(ResolutionPolicy.MINIMIZE, timeslots);
		solutionFound = true;
		return thisOrDie();
	}
	
	@Override
	public Solve printSolution() {
		super.printSolution();
		System.out.println(timeslots);
		return thisOrDie();
	}

	IntVar timeslots;
}
