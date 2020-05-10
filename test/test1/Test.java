package test1;

public class Test extends Thread{

    static int x = 0;
    final static int ITER = 5;

    @Override
    public void run() {
        for (int i = 0; i < ITER; i++) {
            x += 1;
        }
    }

    public static void main(String args[]) throws InterruptedException {
        System.out.println("Before run: x = " + x);

        Test t1 = new Test();
        Test t2 = new Test();

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("After run: x = " + x);
    }
}
