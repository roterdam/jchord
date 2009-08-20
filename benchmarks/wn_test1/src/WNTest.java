import java.util.LinkedList;
import java.util.Queue;

class BoundedBuffer2<E>{
	private Queue<E> buf;
	private int cursize = 0;
	private int maxsize;
	
	public BoundedBuffer2(int max){
		buf = new LinkedList<E>();
		maxsize = max;
	}
	
	public synchronized void reSize(int newMax){
		maxsize = newMax;
	}
	
	public synchronized void put(E elem){
		while(!(cursize < maxsize)){
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		buf.add(elem);
		cursize++;
		notifyAll();
	}
	
	public synchronized E get(){
		while(!(cursize > 0)){
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		E elem = buf.remove();
		cursize--;
		notifyAll();
		return elem;
	}
}

class Producer2 extends Thread{
	private BoundedBuffer2 bf;
	
	public Producer2(BoundedBuffer2 buf){
		bf = buf;
	}
	
	public void run(){
		for(int i = 0; i < 100; i++){
			bf.put(new Integer(i));
		}
	}
}

class Consumer2 extends Thread{
	private BoundedBuffer2 bf;
	
	public Consumer2(BoundedBuffer2 buf){
		bf = buf;
	}
	
	public void run(){
		for(int i = 0; i < 100; i++){
			bf.get();
		}
	}
}

class RandomThread2 extends Thread{
	private BoundedBuffer2 bf;
	
	public RandomThread2(BoundedBuffer2 buf){
		bf = buf;
	}
	
	public void run(){
		bf.reSize(25);
	}
}


public class WNTest {
	public static void main(String[] args){
		BoundedBuffer2<Integer> bf = new BoundedBuffer2<Integer>(20);
		Producer2 p = new Producer2(bf);
		Consumer2 c = new Consumer2(bf);
		RandomThread2 rt = new RandomThread2(bf);
		c.start();
		p.start();
		rt.start();
	}
}

