package testpackage;

public class T extends B implements I, J{
	static A g = new A();
	static int g2 = 99999;
    public static void main(String[] a) {
		A v1 = new A();	
		A v2 = new A();	
		v1.f1 = v2;		
		int x = v2.f2;		
		g = v1;
		int y = v2.f2;		
		A v4;
		if (a != null) {
			A v3 = new A();	
			v4 = new A();	
			v3.f1 = v4;		
		} else
			v4 = new A();	
		int z = v4.f2;		
		g2 = z;
    (new T()).bar();
	}

  public int testAbst(){

    //try{
      switch(g2){
        case 500:
          g2++;
          return g2;
        case 501:
          g2++;
        case 502:
          g2 *= 5;
          break;
        default:
          break;

      } 
      return g2;
    //} finally {
    //    System.out.println("END OF testAbst()");
    //}
  }

  public int foo(int i, A a) throws Exception{
    if(a == null){
      try{
        switch(i){
            case 1:
              i++;
              break;
            case 50:
              throw new Exception("50");
            case 99:
              break;
            default:
              throw new Exception("default!");
        }
      } finally {
          System.out.println("Escaping switch!");
      }
    }
    return i + a.f2;
  }

  public I bar(){

    try{
      foo(3, null);
    } catch (Exception e){
      System.out.println("Caught an exception! : " + e);
    }
    (new T_B()).testAbst();
    return this;
  }

  static class T_A extends A {
    int fa; 
  }

  static class T_B extends B {
      int fb;

      public int testAbst(){
          T_A t_a = new T_A();
          if( t_a instanceof A ){
            T_A[] arrT_A = new T_A[10];
            arrT_A[8] = t_a;
            int[] arrInt = new int[100];
            arrInt[40] = 4;
            fb = arrInt[40] + arrT_A.length;
          }
          return fb;
      }
  }

}

class A {
	A f1;
	int f2;
}

abstract class B {

  abstract public int testAbst();

}

interface I {

    char field1 = 0x00;

}

interface J {

    char field1 = 0x01;

}
