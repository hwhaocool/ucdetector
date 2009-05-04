package org.ucdetector.example.bugs;
/**
 * Constants in interfaces can't be private
 */
public interface Bug2269486 {
  /**
   * Due to the Java Language Spec members in interfaces can't be private.
   *   Therefore UCDetector should not propose to set constants to private, when
   *   these constants are only used within the same interface (e.g. when used to
   *   calculate other constant values).
   *   ACTUAL BEHAVIOR
   * 
   *   UCDetector proposes to make constants in interfaces private. This will
   *   produce invalid Java source.
   * @see https://sourceforge.net/tracker/index.php?func=detail&aid=2269486&group_id=219599&atid=1046865
   */
  public static final String BUG_2269486 = "USED";

  public static final String UNUSED_2 = "UNUSED_2" + BUG_2269486; // Marker YES: unused code

}