package test6;

public class Test extends Thread{
	static int size = 500;
	static int numOfThreads = 4;
	static int iters = 5;

	private int order;

	static IntegerWrapper[] array;

	public Test(int _order) {
		this.order = _order;
	}

	public void change() {
		int start = size / numOfThreads * order;
		int end = size / numOfThreads * (order + 1);

		if (order < numOfThreads / 2) {
			for(int i = start; i < end; i++) {
				array[size-i-1].set(array[size-i-1].get() + array[i].get() * 100);
			}
		} else {
			for(int i = end-1; i >= start; i--) {
				array[size-i-1].set(array[size-i-1].get() + array[i].get() * 100);
			}
		}
	}

	@Override
	public void run() {
		for(int i = 0; i < iters; i++) {
			change();
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
