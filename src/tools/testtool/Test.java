package tools.testtool;

import rr.RRMain;
import rr.annotations.Abbrev;
import rr.barrier.BarrierEvent;
import rr.barrier.BarrierListener;
import rr.barrier.BarrierMonitor;
import rr.error.ErrorMessage;
import rr.error.ErrorMessages;
import rr.event.AccessEvent;
import rr.event.AccessEvent.Kind;
import rr.event.AcquireEvent;
import rr.event.ArrayAccessEvent;
import rr.event.ClassAccessedEvent;
import rr.event.ClassInitializedEvent;
import rr.event.Event;
import rr.event.FieldAccessEvent;
import rr.event.JoinEvent;
import rr.event.NewThreadEvent;
import rr.event.ReleaseEvent;
import rr.event.StartEvent;
import rr.event.VolatileAccessEvent;
import rr.event.WaitEvent;
import rr.instrument.classes.ArrayAllocSiteTracker;
import rr.meta.ArrayAccessInfo;
import rr.meta.ClassInfo;
import rr.meta.FieldInfo;
import rr.meta.MetaDataInfoMaps;
import rr.meta.OperationInfo;
import rr.state.ShadowLock;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.state.ShadowVolatile;
import rr.tool.RR;
import rr.tool.Tool;
import tools.util.Epoch;
import tools.util.VectorClock;
import acme.util.Assert;
import acme.util.Util;
import acme.util.Yikes;
import acme.util.count.AggregateCounter;
import acme.util.count.Counter;
import acme.util.count.ThreadLocalCounter;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.DecorationFactory.Type;
import acme.util.decorations.DefaultValue;
import acme.util.decorations.NullDefault;
import acme.util.io.XMLWriter;
import acme.util.option.CommandLine;

/*
 * A revised FastTrack Tool.  This makes several improvements over the original:
 *   - Simpler synchronization scheme for VarStates.  (The old optimistic scheme
 *     no longer has a performance benefit and was hard to get right.)
 *   - Rephrased rules to:
 *       - include a Read-Shared-Same-Epoch test.
 *       - eliminate an unnecessary update on joins (this was just for the proof).
 *       - remove the Read-Shared to Exclusive transition.
 *     The last change makes the correctness argument easier and that transition
 *     had little to no performance impact in practice.
 *   - Properly replays events when the fast paths detect an error in all cases.
 *   - Supports long epochs for larger clock values.
 *   - Handles tid reuse more precisely.
 *
 * The performance over the JavaGrande and DaCapo benchmarks is more or less
 * identical to the old implementation (within ~1% overall in our tests).
 */
@Abbrev("TEST")
public class Test extends Tool{

    public Test(final String name, final Tool next, CommandLine commandLine) {
        super(name, next, commandLine);
    }

    @Override
    public ShadowVar makeShadowVar(final AccessEvent event) {

        var.inc(1);

        if (event.getKind() == Kind.VOLATILE) {
            return super.makeShadowVar(event);
        } else {
            return new TESTVarState();
        }
    }


    @Override
    public void create(NewThreadEvent event) {
        super.create(event);
    }

    @Override
    public void acquire(final AcquireEvent event) {
        acquire.inc(event.getThread().getTid());
        super.acquire(event);
    }

    @Override
    public void release(final ReleaseEvent event) {
        release.inc(event.getThread().getTid());
        super.release(event);
    }

    @Override
    public void access(final AccessEvent event) {
        super.access(event);
    }


    // Counters for relative frequencies of each rule
    private static final ThreadLocalCounter var = new ThreadLocalCounter("TEST", "Variables", RR.maxTidOption.get());

    private static final ThreadLocalCounter read = new ThreadLocalCounter("TEST", "Read", RR.maxTidOption.get());
    private static final ThreadLocalCounter write = new ThreadLocalCounter("TEST", "Write", RR.maxTidOption.get());
    private static final ThreadLocalCounter readFast = new ThreadLocalCounter("TEST", "Read Fast", RR.maxTidOption.get());
    private static final ThreadLocalCounter writeFast = new ThreadLocalCounter("TEST", "Write Fast", RR.maxTidOption.get());

    private static final ThreadLocalCounter acquire = new ThreadLocalCounter("TEST", "Acquire", RR.maxTidOption.get());
    private static final ThreadLocalCounter release = new ThreadLocalCounter("TEST", "Release", RR.maxTidOption.get());
    private static final ThreadLocalCounter fork = new ThreadLocalCounter("TEST", "Fork", RR.maxTidOption.get());
    private static final ThreadLocalCounter join = new ThreadLocalCounter("TEST", "Join", RR.maxTidOption.get());
    private static final ThreadLocalCounter barrier = new ThreadLocalCounter("TEST", "Barrier", RR.maxTidOption.get());
    private static final ThreadLocalCounter wait = new ThreadLocalCounter("TEST", "Wait", RR.maxTidOption.get());
    private static final ThreadLocalCounter vol = new ThreadLocalCounter("TEST", "Volatile", RR.maxTidOption.get());


    private static final ThreadLocalCounter other = new ThreadLocalCounter("TEST", "Other", RR.maxTidOption.get());

    static {
        new AggregateCounter("TEST", "Variables", var);
        AggregateCounter reads = new AggregateCounter("TEST", "Total Reads", read, readFast);
        AggregateCounter writes = new AggregateCounter("TEST", "Total Writes", write, writeFast);
        AggregateCounter accesses = new AggregateCounter("TEST", "Total Access Ops", reads, writes);
        new AggregateCounter("TEST", "Total Ops", accesses, acquire, release, fork, join, barrier, wait, vol, other);
    }

    public static boolean readFastPath(final ShadowVar shadow, final ShadowThread st) {
        readFast.inc(1);
        return true;
    }

    // only count events when returning true;
    public static boolean writeFastPath(final ShadowVar shadow, final ShadowThread st) {
        writeFast.inc(1);
        return true;
    }

    /*****/

    @Override
    public void volatileAccess(final VolatileAccessEvent event) {
        super.volatileAccess(event);
    }

    @Override
    public void preStart(final StartEvent event) {
        super.preStart(event);
    }

    @Override
    public void stop(ShadowThread st) {
        super.stop(st);
    }

    @Override
    public void postJoin(final JoinEvent event) {
        super.postJoin(event);
    }

    public static String toString(final ShadowThread td) {
        return String.format("[tid=%-2d", td.getTid());
    }

    @Override
    public void classInitialized(ClassInitializedEvent event) {
        super.classInitialized(event);
    }

    @Override
    public void classAccessed(ClassAccessedEvent event) {
        super.classAccessed(event);
    }

    @Override
    public void printXML(XMLWriter xml) {
        for (ShadowThread td : ShadowThread.getThreads()) {
            xml.print("thread", toString(td));
        }
    }
}
