package org.ucdetector.example.classes;

import java.util.ArrayList;

public class AnonymousClass {
  public static void main(String[] args) {
    //
    ArrayList<String> list = new ArrayList<String>() {
      private static final long serialVersionUID = 1L;

      /** ignore, because it is overridden */
      @Override
      public int size() {
        changeToPrivate();
        return 2;
      }

      public void changeToPrivate() { // Marker YES: use private

      }
    };
    System.out.println("size=" + list);
    //    
    ArrayList<String> list2 = new ArrayList<String>() {
      private static final long serialVersionUID = 1L;

      @Override
      public int size() {
        return 2;
      }

      @SuppressWarnings("unused")
      public void unused() { // Marker YES: unused code

      }
    };
    System.out.println("size=" + list2);
  }
}