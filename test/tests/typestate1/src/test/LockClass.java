package test;

public class LockClass {
	int state;
	public LockClass(){
		state = 0; //unlocked
	}

	public void Lock(){
		state = 1;
	}
	public void UnLock(){
		state = 0;
	}
}
