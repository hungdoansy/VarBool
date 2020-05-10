package test2;

public class Test extends Thread {
    static final int size = 5;
    static IntegerWrapper[] numbers = new IntegerWrapper[size];

    @Override
    public void run() {
        synchronized (numbers[0]) {
            System.out.println("Reach 0");

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            synchronized (numbers[1]) {
                System.out.println("Reach 1");

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                synchronized (numbers[2]) {
                    System.out.println("Reach 2");
                    numbers[2].setX(numbers[0].getX() + numbers[1].getX());
                    System.out.println("Updated 2: " + numbers[2].getX());

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
//        IntegerWrapper[] numbers = new IntegerWrapper[size];

        for(int i = 0; i < size; i++) {
            numbers[i] = new IntegerWrapper(i);
        }

        Test[] ts = new Test[3];
        for(int i = 0; i < 3; i++) {
            ts[i] = new Test();
        }

        for(int i = 0; i < 3; i++) {
            ts[i].start();
        }

        for(int i = 0; i < 3; i++) {
            ts[i].join();
        }
    }
}

class IntegerWrapper {
    private int x;

    public IntegerWrapper(int _x) {
        this.setX(_x);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }
}

