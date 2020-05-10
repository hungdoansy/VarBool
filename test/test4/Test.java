package test4;

public class Test extends Thread{
	static int size = 100;
	static int numOfThreads = 3;
	static int iters = 6;

	private int order;

	static IntegerWrapper[] array;

	public Test(int _order) {
		this.order = _order;
	}

	public void inc() {
		if (order >= numOfThreads)
			return;

		int start = size / numOfThreads * order;
		int end = size / numOfThreads * (order + 1);

		for(int i = start; i < end; i++) {
			array[i].set(array[i].get());
		}
	}

	@Override
	public void run() {
		for(int i = 0; i < iters; i++) {
			inc();
		}
	}

	public static void main(String args[]) throws Exception {
		array = new IntegerWrapper[size];
		for(int i = 0; i < size; i++) {
			array[i] = new IntegerWrapper(i*100);
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
