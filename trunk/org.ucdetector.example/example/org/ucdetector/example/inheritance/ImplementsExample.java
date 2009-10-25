package org.ucdetector.example.inheritance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * 
 */
public class ImplementsExample implements Observer {
  public static void main(String[] args) {

    new MyComparator().toString();
    new MyObserver().toString();

    List<String> list = new ArrayList<String>();
    Collections.sort(list, new Comparator<String>() {
      /**
       * Implemented method
       */
      public int compare(String s1, String s2) {
        return s1.length() - s2.length();
      }

      @SuppressWarnings("unused")
      // 2009-06-23 Removed detection of anonymous classes
      public int compareUnused(String s1, String s2) {
        return s1.length() - s2.length();
      }
    });
  }

  // [ 2804064 ] Access to enclosing type - make 2743908 configurable
  static class MyComparator implements Comparator<String> {// Marker YES: use private
    // @Override
    public int compare(String o1, String o2) {
      return 0;
    }
  }

  static class MyObserver implements Observer {// Marker YES: use private
    // @Override
    public void update(Observable o, Object arg) {
    }
  }

  /**
   * Implemented method
   */
  public void update(Observable o, Object arg) {

  }

  public void notImplements() { // Marker YES: unused code

  }
}