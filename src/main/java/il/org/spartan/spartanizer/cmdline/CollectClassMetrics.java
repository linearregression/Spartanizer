package il.org.spartan.spartanizer.cmdline;

import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;

import il.org.spartan.*;
import il.org.spartan.collections.*;
import il.org.spartan.spartanizer.ast.navigate.*;
import il.org.spartan.spartanizer.dispatch.*;
import il.org.spartan.spartanizer.engine.*;
import il.org.spartan.utils.*;

/** Collect basic metrics of files (later on, maybe change to classes)
 * @author Yossi Gil
 * @year 2016 */
public final class CollectClassMetrics {
  private static final String OUTPUT = "/tmp/commons-lang-halstead.CSV";
  private static CSVStatistics output = init();

  public static void main(final String[] where) {
    go(where.length != 0 ? where : new String[] { "." });
    System.err.println("Your output should be here: " + output.close());
  }

  static CompilationUnit spartanize(final CompilationUnit before) {
    final Trimmer tr = new Trimmer();
    assert tr != null;
    final ICompilationUnit $ = (ICompilationUnit) before.getJavaElement();
    tr.setICompilationUnit($);
    assert $ != null;
    try {
      tr.checkAllConditions(null);
    } catch (OperationCanceledException | CoreException e) {
      e.printStackTrace();
    }
    return before;
  }

  private static void go(final File f) {
    try {
      // This line is going to give you trouble if you process class by class.
      output.put("File", f.getName());
      go(FileUtils.read(f));
    } catch (final IOException e) {
      System.err.println(e.getMessage());
    }
  }

  private static void go(final String javaCode) {
    output.put("Characters", javaCode.length());
    report("Before-", (CompilationUnit) makeAST.COMPILATION_UNIT.from(javaCode));
  }

  private static void go(final String[] where) {
    for (final File ¢ : new FilesGenerator(".java").from(where))
      go(¢);
  }

  private static CSVStatistics init() {
    try {
      return new CSVStatistics(OUTPUT, "property");
    } catch (final IOException e) {
      throw new RuntimeException(OUTPUT, e);
    }
  }

  /** fault, what happens if we have many classes in the same file? Also, we do
   * not want to count imports, and package instructions. Write a method that
   * finds all classes, which could be none, at the upper level, and collect on
   * these. Note that you have to print the file name which is common to all
   * classes. Turn this if you like into a documentation
   * @param string */
  // TODO: Yossi, make this even more clever, by using function interfaces..
  private static void report(final String prefix, final CompilationUnit ¢) {
    // TODO Matteo make sure that the counting does not include comments. Do
    // this by adding stuff to the metrics suite.
    output.put(prefix + "Length", ¢.getLength());
    output.put(prefix + "Count", metrics.count(¢));
    output.put(prefix + "Non whites", metrics.countNonWhites(¢));
    output.put(prefix + "Condensed size", metrics.condensedSize(¢));
    output.put(prefix + "Lines", metrics.lineCount(¢));
    output.put(prefix + "Dexterity", metrics.dexterity(¢));
    output.put(prefix + "Leaves", metrics.leaves(¢));
    output.put(prefix + "Nodes", metrics.nodes(¢));
    output.put(prefix + "Internals", metrics.internals(¢));
    output.put(prefix + "Vocabulary", metrics.vocabulary(¢));
    output.put(prefix + "Literacy", metrics.literacy(¢));
    output.put(prefix + "Imports", metrics.countImports(¢));
    output.put(prefix + "No Imports", metrics.countNoImport(¢));
    output.nl();
  }
}