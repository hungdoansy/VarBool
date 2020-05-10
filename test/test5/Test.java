package test5;

public class Test extends Thread{
	static int size = 200;
	static int numOfThreads = 4;
	static int iters = 3;

	private int order;

	static IntegerWrapper[] array;

	public Test(int _order) {
		this.order = _order;
	}

	public void phase1() {
		for(int i = 0; i < size; i++) {
			array[i].set(array[i].get() + 4);
		}
	}

	public void phase2() {
		if (order >= numOfThreads)
			return;

		int start = size / numOfThreads * order;
		int end = size / numOfThreads * (order + 1);

		for(int i = start; i < end; i++) {
			array[i].set(array[i].get());
		}
	}

	public void phase3() {
		int start = size / (numOfThreads * 2) * order;
		int end = size / (numOfThreads * 2) * (order + 1);

		for(int i = start; i < end; i++) {
			int temp = array[i].get();
			array[i].set(array[size - i - 1].get());
			array[size - i - 1].set(array[i].get());
		}
	}

	@Override
	public void run() {
		for(int i = 0; i < iters; i++) {
			phase1();
			phase2();
			phase3();
		}
	}

	public static void main(String args[]) throws Exception {
		array = new IntegerWrapper[size];
		for(int i = 0; i < size; i++) {
			array[i] = new IntegerWrapper(i*100 + 500);
		}

		Test[] tests = new Test[numOfThreads];

		for(int k = 0; k < iters; k++) {
			for(int i = 0; i < numOfThreads; i++) {
				tests[i] = new Test(i);
			}

			for(int i = 0; i < numOfThreads; i++) {
				tests[i].start();
			}

			for(int i = 0; i < numOfThreads; i++) {
				tests[i].join();
			}
		}

		System.out.println("Done");
	}
}

class IntegerWrapper {
	int x;

	public IntegerWrapper(int _x) {
		this.x = _x;
	}

	public synchronized int get() {
		return x;
	}

	public synchronized void set(int _x) {
		this.x = _x;
	}
}
