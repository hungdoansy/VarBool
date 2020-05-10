package test7;

public class Test extends Thread{

	// numOfThreads = (lengthOfArray - widthOfEachChunk) / step + 1

	static int size /*lengthOfArray*/ = 1000;
	static int widthOfEachChunk = 400;
	static int step = 120;
	static int numOfThreads = 6;
	static int iters = 4;
	

	private int order;

	static IntegerWrapper[] array;

    


	public Test(int _order) {
		this.order = _order;
	}

	public void update() {
		int start = order * step;
		int end = start + widthOfEachChunk;

		for(int i = start; i < end; i++) {
			array[i].set(array[i].get() + array[size - i - 1].get());
		}
	}

	@Override
	public void run() {
		for(int i = 0; i < iters; i++) {
			update();
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
