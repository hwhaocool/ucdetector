package org.ucdetector.example;
/**
 */
public class ConstructorExample {
  /** we ignore default constructors! */
  public ConstructorExample() {
  }

  /** we dont't ignore constructors with parameter! */
  public ConstructorExample(String parameter) { // Marker YES
  }
}
