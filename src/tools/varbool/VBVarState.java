package tools.varbool;

import rr.state.ShadowVar;

import java.lang.reflect.Array;
import java.util.BitSet;
import java.util.ArrayList;

public class VBVarState implements ShadowVar {

    // Alpha Write
    public BitSet alphaW;

    // Alpha Read
    public ArrayList<BitSet> alphaR;

    // Beta Write
    public BitSet betaW;

    // Beta Read
    public ArrayList<BitSet> betaR;

    // Var name
    private final String varInfo;

    private BitSet threads;

    public VBVarState(String _varInfo, BitSet _threads, BitSet _locks) {
        this.varInfo = _varInfo;

        threads = (BitSet) _threads.clone();

        alphaW = (BitSet) threads.clone();
        alphaR = new ArrayList<>();

        int[] tids = threads.stream().toArray();
        for(int tid : tids) {
            alphaR.add(tid, (BitSet) threads.clone());
        }

        betaW = new BitSet();
        betaR = new ArrayList<>();

        int[] lockIds = _locks.stream().toArray();
        for(int lockId : lockIds) {
            betaR.add(lockId, (BitSet) threads.clone());
        }
    }

    private synchronized void ensureCapacity(ArrayList<BitSet> list, int minCapacity) {
        int currentCapacity = list.size();

        for(int i = 0; i <= (minCapacity - currentCapacity); ++i) list.add(new BitSet());
    }

    private boolean isCapacitySufficient(ArrayList<BitSet> list, int minCapacity) {
        return list.size() > minCapacity;
    }

    public void ensureCapacityAlphaR(int minCapacity) {
        ensureCapacity(alphaR, minCapacity);
    }

    // PreStart - Fork
    public synchronized void handleForkEvent(int tidParentThread, int tidNewThread) {
        int tTid = tidParentThread;
        int uTid = tidNewThread;

        threads.set(tidNewThread);

        // Update alphaW
        alphaW.set(uTid, alphaW.get(tTid));

        // alphaR.get(tTid).set(uTid, true);
        alphaR.get(tTid).set(tTid, true);
        alphaR.set(uTid, (BitSet) alphaR.get(tTid).clone());
        alphaR.forEach(bitSet -> bitSet.set(uTid, true));

        // No need to update betaW
        // Update betaR
        betaR.forEach(lockBitSet -> lockBitSet.set(uTid, lockBitSet.get(tTid)));
    }

    // Join
    public synchronized void handleJoinEvent(int tidParentThread, int tidJoiningThread) {
        int tTid = tidParentThread;
        int uTid = tidJoiningThread;

        threads.set(uTid, false);

        // Update alphaW
        alphaW.set(tTid, alphaW.get(tTid) | alphaW.get(uTid));
        alphaW.set(uTid, false); // This uTid is no longer in use

        // Update alphaR
        BitSet alphaROfParentThread = alphaR.get(tTid);
        BitSet alphaROfJoiningThread = alphaR.get(uTid);

        alphaROfParentThread.or(alphaROfJoiningThread);
        alphaROfParentThread.set(tTid, true);
        alphaROfParentThread.set(uTid, false);

        // alphaR.set(uTid, null); // This uTid is no longer in use
        alphaR.get(uTid).clear();
        alphaR.forEach(bitSet ->  bitSet.set(uTid, false));

        // Update betaR
        betaR.forEach(lockBitSet -> {
            lockBitSet.set(tTid, lockBitSet.get(tTid) | lockBitSet.get(uTid));
            lockBitSet.set(uTid, false);
        });
    }

    // Acquire
    public synchronized void handleAcquireEvent(int threadId, int lockId) {
        if (betaW.get(lockId))
            alphaW.set(threadId, true);

        boolean isLockNewlyAdded = !isCapacitySufficient(betaR, lockId);

        if (isLockNewlyAdded) {
            ensureCapacity(betaR, lockId);
        }
        else {
            BitSet alphaROfAcquiringThread = alphaR.get(threadId);
            BitSet betaROfLock = betaR.get(lockId);
            alphaROfAcquiringThread.or(betaROfLock);
        }
    }

    // Release
    public synchronized void updateWhenReleaseLock(int threadId, int lockId) {
        if (alphaW.get(threadId))
            betaW.set(lockId, true);

        BitSet alphaROfReleasingThread = alphaR.get(threadId);
        BitSet betaROfLock = betaR.get(lockId);

        betaROfLock.or(alphaROfReleasingThread);
    }

    public synchronized boolean isReadAccessValid(int tid) {
        boolean isValid = alphaW.get(tid);

        alphaR.forEach(bitSet -> bitSet.set(tid, false));
        alphaR.get(tid).set(tid, true);

        betaR.forEach(bitSet -> bitSet.set(tid, false));

        return isValid;
    }

    public synchronized boolean isWriteAccessValid(int tid) {
        boolean isValid = true;

        if (!alphaW.get(tid)) {
            isValid = false;
        }

        BitSet alphaROfWritingThread = alphaR.get(tid);
        alphaROfWritingThread.set(tid);

        if (!alphaROfWritingThread.equals(threads)) {
            isValid = false;
        }

        alphaW.clear();
        alphaW.set(tid, true);

        betaW.clear();

        return isValid;
    }

    public String getVarInfo() {
        return varInfo;
    }

    @Override
    public String toString() {
        return String.format("[--%s-- \n alphaW=%s \n betaW=%s \n alphaR=%s \n betaR=%s]", getVarInfo(), alphaW.toString(), betaW.toString(), alphaR.toString(), betaR.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        VBVarState other = (VBVarState) obj;

        return true;
    }
}
