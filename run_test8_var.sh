file_name="test8/Test.java"
module_name="test8/Test"

function write_file() {
    local num=$1


    content=$(cat <<-END
package test8;

public class Test extends Thread{
    static final int size = $num;
    static final int smallerSize = size;

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

        int nthreads = 5;
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
END
    )

    echo "$content" > $file_name
}

# start from here -----------------------------------------------------------------------
cd test

for n in 1 5 10 100 200 500 1000 2000
do
    echo "Running test8 with size=$n"
    write_file $n
    head $file_name
    javac $file_name
    java $module_name
done