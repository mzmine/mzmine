package net.sf.mzmine.modules.datapointprocessing;

import java.util.ArrayList;
import java.util.List;

public class test {

  static List<String> list;
  
  public static void main(String[] args) {
    list = new ArrayList<String>();
    list.add("ab");
    list.add("cd");
    list.add("efg");
    
    synchronized (list) {
      System.out.println("first");
      String a = list.get(0);
      remove(a);
    }
    System.out.println("got there.");
    System.out.println(list.toString());
  }
  
  public static void remove(String s) {
    if(!list.contains(s))
      return;
    
    synchronized (list) {
      System.out.println("second");
      list.remove(s);
    }
  }
}
