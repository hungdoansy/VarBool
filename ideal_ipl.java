class VarState extends ShadowVar {
  BitSet alphaW;
  ArrayList<BitSet> alphaR;

  BitSet betaW;
  ArrayList<BitSet> betaR;

  BitSet threads;

  synchronized void ensureCapacity(ArrayList<BitSet> list, int minCapacity) {
    for (int i = 0; i<= minCapacity - list.size(); ++i) list.add(new BitSet());
  }

  synchronized void handleForkEvent(int t, int u) {
    threads.set(u, true);

    alphaW.set(u, alphaW.get(t));

    alphaR.get(t).set(t, true);
    alphaR.set(u, (BitSet) alphaR.get(t).clone());
    alphaR.forEach(bs -> bs.set(u, true));

    betaR.forEach(bs -> bs.set(u, bs.get(t)));
  }

  synchronized void handleJoinEvent(int t, int u) {
    threads.set(u, false);

    alphaW.set(t, alphaW.get(t) | alphaW.get(u));
    alphaW.set(u, false);

    BitSet alphaRAtT = alphaR.get(t);
    BitSet alphaRAtU = alphaR.get(u);

    alphaRAtT.or(alphaRAtU);
    alphaRAtT.set(t, true);
    alphaRAtT.set(u, false);

    alphaRAtU.clear();
    alphaR.forEach(bs -> bs.set(u, false));

    betaR.forEach(bs -> {
      bs.set(t, bs.get(t) | bs.get(u));
      bs.set(u, false);
    });
  }

  synchronized void handleAcquireEvent(int t, int l) {
    if (betaW.get(l))
      alphaW.set(t, true);

    boolean isLockJustAdded = betaR.size()<= l;

    if (isLockJustAdded) {
      ensureCapacity(betaR, l);
    } else {
      alphaR.get(t).or(betaR.get(l));
    }
  }

  synchronized void handleReleaseEvent(int t, int l) {
    if (alphaW.get(t))
      betaW.set(l, true);
    betaR.get(l).or(alphaR.get(t));
  }

  synchronized boolean isReadAccessValid(int t) {
    boolean isValid = alphaW.get(t);

    alphaR.forEach(bs -> bs.set(t, false));
    alphaR.get(t).set(t, true);
    betaR.forEach(bs -> bs.set(t, false));

    return isValid;
  }

  synchronized boolean isWriteAccessValid(int t) {
    boolean isValid = true;

    if (!alphaW.get(t)) {
      isValid = false;
    }

    alphaR.get(t).set(t, true);

    if (!alphaR.get(t).equals(threads))
      isValid = false;

    alphaW.clear();
    alphaW.set(t, true);
    betaW.clear();
    return isValid;
  }
}

class VarBool extends Tool {
  ArrayList<VarState> variables;

  BitSet threads;
  BitSet locks;

  void create(NewThreadEvent event) {
    int u = event.getThread().getTid();
    variables.forEach(var ->
      var.ensureCapacity(var.alphaR, u));
  }

  /* Fork */
  void preStart(final StartEvent event) {
    int t = event.getThread().getTid();
    int u = event.getNewThread().getTid();

    synchronized(threads) {
      threads.set(u, true);
    }

    variables.forEach(var ->
      var.handleForkEvent(t, u));
  }

  /* Join */
  void postJoin(final JoinEvent event) {
    int t = event.getThread().getTid();
    int u = event.getJoiningThread().getTid();

    synchronized(threads) {
      threads.set(u, false);
    }

    variables.forEach(var ->
      var.handleJoinEvent(t, u));
  }

  void acquire(final AcquireEvent event) {
    int t = event.getThread();
    int l = event.getLock();

    synchronized(locks) {
      locks.set(l, true);
    }

    variables.forEach(var ->
      var.handleAcquireEvent(t, l));
  }

  void release(final ReleaseEvent event) {
    int t = event.getThread();
    int l = event.getLock();

    synchronized(locks) {
      locks.set(l, false);
    }

    variables.forEach(var ->
      var.handleAcquireEvent(t, l));
  }

  void write(final ShadowThread st, final VarState varState) {
    int t = st.getTid();
    assert(varState.isWriteAccessValid(t) == true);
  }

  void read(final ShadowThread st, final VarState varState) {
    int t = st.getTid();
    assert(varState.isReadAccessValid(t) == true);
  }
}