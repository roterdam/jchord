package test;

class Node {
  int x;
  Node prev;
  Node next;
}

class A extends java.lang.Thread {
  static Node g1;
  public void run() {
    Node local = new Node();
    g1 = local;
    g1 = new Node(); // Reclaim
    local.next = local;
  }
}

public class T {
	int f;
  static Node g1;
  static Node g2;
  //static Node g3 = new Node();
  static Node[] g4;

  static Node newNode() { return new Node(); }

  static void breakHeapAllocSiteAbstraction() {
    // True answer: b escapes; a,c local
    // However, a and b are treated using the same heap abstraction so everything escapes.
    Node a = newNode();
    Node b = newNode();
    Node c = new Node();
    a.next = c; // Query: a not escaping
    g1 = b; // Escape
    b.next = b; // Query: b is escaping
    c.prev = a; // Query: c is not escaping, but will appear to escape under heap allocation sites
    // This can be solved with 1-CFA
  }

  // Any of the reachability predicates will do great on this
  // Recency works too (by luck)
  // Heap allocation sites will never work
  static void reachabilityExample() {
    Node head = new Node(); // h1
    Node ptr = head;
    for (int i = 0; i < 2; i++) {
      Node x = new Node(); // h2
      ptr.next = x;
      ptr = x;
    }
    // Heap: h1 -> h2 -> h2 <- h3
    // Can't tell the 2 h2's apart unless we consider what's pointing to them
    // Escape second element, first element should be safe, but can't detect with allocation sites or any amount of context
    g1 = head.next.next;
    head.next.x = 3; // Query: should be safe
  }

  static void useArray() {
    Node[] nodes = new Node[5];
    for(int i = 0; i < nodes.length; i++) {
      nodes[i] = new Node();
      if (i > 0) {
        nodes[i-1].next = nodes[i];
        nodes[i].prev = nodes[i-1];
      }
    }
    //g4 = nodes; // Escape! - Doesn't work (array indices don't work)
    Node a = nodes[0];
    Node b = nodes[1];
    Node c = nodes[2];
    g1 = a; // Escape only one of them
    g2 = a; // Escape only one of them
  }

  static void test1() {
    // Both x and y escape
    Node x = new Node();
    Node y = new Node();
    x.next = y;
    y.prev = x;
    Node w = x.next;
    g1 = w;
  }

  static void test2() {
    Node x = new Node();
    g1 = x;
    g1 = null; // Reclaim
    x.next = x;
  }

  static void test3() {
    Node x = new Node();
    Node y = x;
    x = y;
    y = x;
    x = y;
    y = x;
    x = y;
    y = x;
    x = y;
    y = x;
    x = y;
    y = x;
    x = y;
    y = x;
    x = y;
    y = x;
    x = y;
    y = x;
  }

  static void testLib() {
    java.util.List l = new java.util.ArrayList(); 
  }

  static void testThread() throws Exception {
    A.g1 = new Node();
    A a = new A();
    a.start();
    a.join();
  }

  // Shows importance of strong updates
  static void strongWeak() {
    Node a = new Node();
    g1 = a;
    g1 = null;
    a.next = a; // Query: should be not escaping (but weak updates will not detect this)
  }

 static T ggg;
  public static void main(String[] args) throws Exception {
	System.out.println("AAA");
/*
		for (int i = 0; i < 10; i++) {
			Object x = new Object();
			System.out.println(x);
			// if (i == 500000)
			// System.gc();
		}
*/
    //strongWeak();
    //breakHeapAllocSiteAbstraction();
    // reachabilityExample();
    //useArray();
    //test1();
    //test2();
    //test3();
    //testLib();
    //testThread();
  }
}
