package test3;

public class Test {

    static final int size = 10;
    static final IntegerWrapper[] integerWrappers = new IntegerWrapper[size];

    public static void main(String[] args) throws InterruptedException {
        for(int i = 0; i < size; i++) {
            integerWrappers[i] = new IntegerWrapper(i * 1000);
        }

        final int numberOfThreads = 2;

        Thread1[] t1s = new Thread1[numberOfThreads];
        for(int i = 0; i < numberOfThreads; i++) {
            t1s[i] = new Thread1();
        }

        for(int i = 0; i < numberOfThreads; i++) {
            t1s[i].start();
        }

        for(int i = 0; i < numberOfThreads; i++) {
            t1s[i].join();
        }


        Thread2[] t2s = new Thread2[numberOfThreads];
        for(int i = 0; i < numberOfThreads; i++) {
            t2s[i] = new Thread2();
        }

        for(int i = 0; i < numberOfThreads; i++) {
            t2s[i].start();
        }

        for(int i = 0; i < numberOfThreads; i++) {
            t2s[i].join();
        }

        Thread1 t1 = new Thread1();
        t1.start();
        t1.join();

        Thread2 t2 = new Thread2();
        t2.start();
        t2.join();
    }
}

class IntegerWrapper {
    int x;

    public IntegerWrapper(int _x) {
        this.x = _x;
    }
}

class Thread1 extends Thread {
    @Override
    public void run() {
        for (int i = 0; i < Test.size; i++) {
            IntegerWrapper number = Test.integerWrappers[i];
            inc(number);

            synchronized (number) {
                number.x += 1;
            }
        }

        synchronized (Test.integerWrappers[0]) {
            Test.integerWrappers[0].x += 1;
            Test.integerWrappers[1].x += 1;
        }

        synchronized (Test.integerWrappers[0]) {
            Test.integerWrappers[0].x += 1;
            Test.integerWrappers[2].x += 1;
        }
    }

    public static synchronized void inc(IntegerWrapper number) {
        number.x += 1;
    }
}

class Thread2 extends Thread {
    @Override
    public void run() {
        for (int i = 0; i < Test.size; i++) {
            IntegerWrapper number = Test.integerWrappers[i];
            add(number);
        }
    }

    public static synchronized void add(IntegerWrapper number) {
        number.x += 10;
    }
}
