package tools.varbool;

import acme.util.Util;
import rr.RRMain;
import rr.annotations.Abbrev;
import rr.error.ErrorMessage;
import rr.error.ErrorMessages;
import rr.event.*;
import rr.event.AccessEvent.Kind;
import rr.instrument.classes.ArrayAllocSiteTracker;
import rr.meta.ArrayAccessInfo;
import rr.meta.FieldInfo;
import rr.state.ShadowLock;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.tool.RR;
import rr.tool.Tool;
import acme.util.count.AggregateCounter;
import acme.util.count.ThreadLocalCounter;
import acme.util.option.CommandLine;

import java.util.ArrayList;
import java.util.BitSet;

// The original VarBool without optimization
@Abbrev("VB")

public class VarBool extends Tool{
	private final static boolean COUNT_OPERATIONS = false;
	// COUNT_OPERATIONS = RRMain.slowMode();

	public final static ErrorMessage<FieldInfo> fieldErrors = ErrorMessages.makeFieldErrorMessage("VarBool");
	public final static ErrorMessage<ArrayAccessInfo> arrayErrors = ErrorMessages.makeArrayErrorMessage("VarBool");
 
	// An ArrayList stores VBVarStates
	public final static ArrayList<VBVarState> variables = new ArrayList<>();

	// @to-do:  https://github.com/Netflix/hollow/blob/master/hollow/src/main/java/com/netflix/hollow/core/memory/ThreadSafeBitSet.java
	// locks[i] = true if the lock with id i is being used
	public final static BitSet locks = new BitSet();

	// threads[i] = true if the thread with tid i is being run
	public final static BitSet threads = new BitSet();

	public VarBool(final String name, final Tool next, CommandLine commandLine) {
		super(name, next, commandLine);

		// The main thread has tid=0
		threads.set(0, true);
	}

	/**
	 * Make a ShadowVar for each variable
	 * It is called whenever the very first access to a variable arises.
	 * @param event - the AccessEvent
	 * @return
	 *
	 */
	@Override
	public ShadowVar makeShadowVar(final AccessEvent event) {

		if (event.getKind() == Kind.VOLATILE) {
			// Maybe we do not need to handle volatile
			return super.makeShadowVar(event);
		} else {
			String varInfo = event.getAccessInfo().toString();

            VBVarState newShadowVar = new VBVarState(varInfo, threads, locks);

            synchronized (variables) {
				variables.add(newShadowVar);
			}

			return newShadowVar;
		}
	}

	/**
	 * This function is called when the function run of the thread is called.
	 * It is not called when a thread class is initialized with the 'new' keyword.
	 * This function is called right before the function 'preStart'.
	 * The creation of the main thread also invokes this function.
 	 */
	@Override
	public void create(NewThreadEvent event) {
		int tid = event.getThread().getTid();
		variables.forEach(varState -> varState.ensureCapacityAlphaR(tid));
		super.create(event);
	}


	@Override
	public void preStart(final StartEvent event) {
		ShadowThread st = event.getThread();
		ShadowThread su = event.getNewThread();

		int tTid = st.getTid();
		int uTid = su.getTid();

		synchronized (threads) {
			threads.set(uTid, true);
		}

		variables.forEach(varState -> varState.handleForkEvent(tTid, uTid));

		super.preStart(event);
		if (COUNT_OPERATIONS) fork.inc(st.getTid());
	}

	@Override
	public void postJoin(final JoinEvent event) {
		ShadowThread st = event.getThread();
		ShadowThread su = event.getJoiningThread();

		int tTid = st.getTid();
		int uTid = su.getTid();

		synchronized (threads) {
			threads.set(uTid, false);
		}

		variables.forEach(varState -> varState.handleJoinEvent(tTid, uTid));

		super.postJoin(event);
		if (COUNT_OPERATIONS) join.inc(st.getTid());
	}


	@Override
	public void acquire(final AcquireEvent event)
	{
		ShadowThread st = event.getThread();
		ShadowLock sl = event.getLock();

		int threadId = st.getTid();
		int lockId = sl.hashCode();

		synchronized (locks) {
			locks.set(lockId, true);
		}

		variables.forEach(varState -> varState.handleAcquireEvent(threadId, lockId));

		super.acquire(event);
		if (COUNT_OPERATIONS) acquire.inc(st.getTid());
	}

	@Override
	public void release(final ReleaseEvent event)
	{
		ShadowThread st = event.getThread();
		ShadowLock sl = event.getLock();

		int threadId = st.getTid();
		int lockId = sl.hashCode();

		synchronized (locks) {
			locks.set(lockId, false);
		}

		variables.forEach(varState -> varState.updateWhenReleaseLock(threadId, lockId));

		super.release(event);
		if (COUNT_OPERATIONS) release.inc(st.getTid());
	}

	@Override
	public void access(final AccessEvent event)
	{
		final ShadowThread st = event.getThread();
		final ShadowVar sv = event.getOriginalShadow();

		if (sv instanceof VBVarState) {
			final VBVarState varState = (VBVarState) sv;

			if (event.isWrite()) {
				write(event, st, varState);
			} else {
				read(event, st, varState);
			}
		} else {
			super.access(event);
		}
	}

	protected void read(final AccessEvent event, final ShadowThread st, final VBVarState varState)
	{
		int tid = st.getTid();

		if (varState.isReadAccessValid(tid)) {
			if (COUNT_OPERATIONS) read.inc(st.getTid());
		}
		else {
			if (COUNT_OPERATIONS) readError.inc(st.getTid());
			error(event, varState, "READ | AW at " + tid + " false", "write", tid,"read", tid);
		}
	}

	public static boolean readFastPath(final ShadowVar shadow, final ShadowThread st) {
		if (shadow instanceof VBVarState) {
			int tid = st.getTid();
			VBVarState varState = (VBVarState) shadow;

			if (varState.isReadAccessValid(tid)) {
				return true;
			}
		}

		return false;
	}

	protected void write(final AccessEvent event, final ShadowThread st, final VBVarState varState)
	{
		int tid = st.getTid();

		if (varState.isWriteAccessValid(tid)) {
			if (COUNT_OPERATIONS) write.inc(st.getTid());
		}
		else {
			if (COUNT_OPERATIONS) writeError.inc(st.getTid());
			error(event, varState, "WRITE | AR at " + tid + " false", "read", tid,"write", tid);
		}
	}

	public static boolean writeFastPath(final ShadowVar shadow, final ShadowThread st) {
		if (shadow instanceof VBVarState) {
			int tid = st.getTid();
			VBVarState varState = (VBVarState) shadow;

			if (varState.isWriteAccessValid(tid)) {
				return true;
			}
		}

		return false;
	}

//	@Override
//	public void preJoin(JoinEvent event) {
//		super.preJoin(event);
//	}
//
//	@Override
//	public void stop(ShadowThread st) {
//		super.stop(st);
//	}
//
//	@Override
//	public void fini() {
//		// To-do
//	}

	public static synchronized BitSet getThreads() {
		final BitSet threads = VarBool.threads;

		return threads;
	}


	// Counters for relative frequencies of each rule
	/**
		@todo: Do I need to implement these counters?
	 */
	private static final ThreadLocalCounter read = new ThreadLocalCounter("FTB", "Read Same Epoch", RR.maxTidOption.get());
	private static final ThreadLocalCounter readError = new ThreadLocalCounter("FTB", "ReadShared Same Epoch", RR.maxTidOption.get());

	private static final ThreadLocalCounter write = new ThreadLocalCounter("FTB", "Write Same Epoch", RR.maxTidOption.get());
	private static final ThreadLocalCounter writeError = new ThreadLocalCounter("FTB", "Write Exclusive", RR.maxTidOption.get());

	private static final ThreadLocalCounter acquire = new ThreadLocalCounter("FTB", "Acquire", RR.maxTidOption.get());
	private static final ThreadLocalCounter release = new ThreadLocalCounter("FTB", "Release", RR.maxTidOption.get());
	private static final ThreadLocalCounter fork = new ThreadLocalCounter("FTB", "Fork", RR.maxTidOption.get());
	private static final ThreadLocalCounter join = new ThreadLocalCounter("FTB", "Join", RR.maxTidOption.get());
	private static final ThreadLocalCounter barrier = new ThreadLocalCounter("FTB", "Barrier", RR.maxTidOption.get());
	private static final ThreadLocalCounter wait = new ThreadLocalCounter("FTB", "Wait", RR.maxTidOption.get());
	private static final ThreadLocalCounter vol = new ThreadLocalCounter("FTB", "Volatile", RR.maxTidOption.get());


	private static final ThreadLocalCounter other = new ThreadLocalCounter("FTB", "Other", RR.maxTidOption.get());

	static {
		AggregateCounter reads = new AggregateCounter("FTB", "Total Reads", read, readError);
		AggregateCounter writes = new AggregateCounter("FTB", "Total Writes", write, writeError);
		AggregateCounter accesses = new AggregateCounter("FTB", "Total Access Ops", reads, writes);
		new AggregateCounter("FTB", "Total Ops", accesses, acquire, release, fork, join, barrier, wait, vol, other);
	}


	protected void error(final AccessEvent ae, final VBVarState x, final String description, final String prevOp, final int prevTid, final String curOp, final int curTid) {
		if (ae instanceof FieldAccessEvent) {
			fieldError((FieldAccessEvent)ae, x, description, prevOp, prevTid, curOp, curTid);
		} else {
			arrayError((ArrayAccessEvent)ae, x, description, prevOp, prevTid, curOp, curTid);
		}
	}

	protected void arrayError(final ArrayAccessEvent aae, final VBVarState sx, final String description, final String prevOp, final int prevTid, final String curOp, final int curTid) {
		final ShadowThread st = aae.getThread();
		final Object target = aae.getTarget();

		if (arrayErrors.stillLooking(aae.getInfo())) {
			arrayErrors.error(st,
					aae.getInfo(),
					"Alloc Site", 		ArrayAllocSiteTracker.get(target),
					"Shadow State", 	sx,
					"Current Thread1",	st.getTid(),
					"Array",			Util.objectToIdentityString(target) + "[" + aae.getIndex() + "]",
					"Message", 			description,
					"Previous Op",		prevOp + " " + ShadowThread.get(prevTid),
					"Current Op",		curOp + " " + ShadowThread.get(curTid),
					"Stack",			ShadowThread.stackDumpForErrorMessage(st));
		}

		aae.getArrayState().specialize();

		if (!arrayErrors.stillLooking(aae.getInfo())) {
			advance(aae);
		}
	}

	protected void fieldError(final FieldAccessEvent fae, final VBVarState sx, final String description, final String prevOp, final int prevTid, final String curOp, final int curTid) {
		final FieldInfo fd = fae.getInfo().getField();
		final ShadowThread st = fae.getThread();
		final Object target = fae.getTarget();

		if (fieldErrors.stillLooking(fd)) {
			fieldErrors.error(st,
					fd,
					"Shadow State", 	sx,
					"Current Thread1",	st.getTid(),
					"Class",			(target==null?fd.getOwner():target.getClass()),
					"Field",			Util.objectToIdentityString(target) + "." + fd,
					"Message", 			description,
					"Previous Op",		prevOp + " " + ShadowThread.get(prevTid),
					"Current Op",		curOp + " " + ShadowThread.get(curTid),
					"Stack",			ShadowThread.stackDumpForErrorMessage(st));
		}

		if (!fieldErrors.stillLooking(fd)) {
			advance(fae);
		}
	}

	public static synchronized void debug() {
		System.out.printf("Class: %s | Line: %d\n", "VarBool", Thread.currentThread().getStackTrace()[2].getLineNumber());
	}

	public static synchronized void debug(String message) {
		System.out.printf("Class: %s | Line: %d | Message: %s\n", "VarBool", Thread.currentThread().getStackTrace()[2].getLineNumber(), message);
	}

	public static void printVariables () {
		int size = variables.size();

		if (size != 0) {
				for (int i = 0; i < size; i++) {

					ShadowVar sv = variables.get(i);

					synchronized (sv) {
						System.out.println(i+"|:"+sv);
					}
				}
		} else {
			System.out.println("Variables none");
		}
	}
}
