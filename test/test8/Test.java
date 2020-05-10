package test8;

public class Test extends Thread{
    static final int size = 2000;
    static final int smallerSize = Math.min(size, 100);

    static IntegerWrapper[] numbers = new IntegerWrapper[size];

    @Override
    public void run() {

        for(int i = 0; i < smallerSize; i++) {
            IntegerWrapper n = numbers[i];
            n.inc();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        for(int i = 0; i < size; i++) {
            numbers[i] = new IntegerWrapper(i);
        }

        int nthreads = 4;
        Test[] ts = new Test[nthreads];

        for(int i = 0; i < nthreads; i++) {
            ts[i] = new Test();
        }

        for(int i = 0; i < nthreads; i++) {
            ts[i].start();
        }

        for(int i = 0; i < nthreads; i++) {
            ts[i].join();
        }

        ts = new Test[nthreads];

        for(int i = 0; i < nthreads; i++) {
            ts[i] = new Test();
        }

        for(int i = 0; i < nthreads; i++) {
            ts[i].start();
        }

        for(int i = 0; i < nthreads; i++) {
            ts[i].join();
        }

        ts = new Test[nthreads];

        for(int i = 0; i < nthreads; i++) {
            ts[i] = new Test();
        }

        for(int i = 0; i < nthreads; i++) {
            ts[i].start();
        }

        for(int i = 0; i < nthreads; i++) {
            ts[i].join();
        }
    }
}

class IntegerWrapper {
    int x;

    public IntegerWrapper(int _x) {
        this.x = _x;
    }

    public synchronized void inc() {
        this.x += 1;
    }
}