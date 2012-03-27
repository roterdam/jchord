package test;

public class Test {
	public static LockClass lock3 = null;
	public static void main(String[] args) {
		//Test Case:Expected Pass
		LockClass lock1 = new LockClass();
		LockClass lock2 = new LockClass();
		lock3 = lock1;
		lock2.Lock();
		lock3.Lock();
		for(int i=0;i<4;i++)
		{
			lock3.UnLock();
			lock3.Lock();
		}
	}
}
