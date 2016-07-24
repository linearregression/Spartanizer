package il.org.spartan.refactoring.spartanizations;

import static il.org.spartan.Utils.*;
import static il.org.spartan.azzert.*;
import il.org.spartan.*;
import il.org.spartan.refactoring.utils.*;

import java.io.*;
import java.util.*;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jface.text.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** Test cases in which the transformation should not do anything
 * @author Yossi Gil
 * @since 2014/05/24 */
@RunWith(Parameterized.class)//
public class Unchanged {
  /** @return a collection of cases, where each cases is an array of three
   *         objects, the spartanization, the test case name, and the input file */
  @Parameters(name = "{index}: {0} {1}")//
  public static Collection<Object[]> cases() {
    return new FileTestUtils.Files() {
      private boolean dotOutExists(final File d, final String name) {
        return new File(d, name.replaceAll("\\.in$", ".out")).exists();
      }
      @Override Object[] makeCase(final Spartanization s, final File d, final File f, final String name) {
        return name.endsWith(testSuffix) && ast.stringBuilder(f).indexOf(testKeyword) == -1 ? objects(s, name, makeInFile(f)) : name.endsWith(".in") && !dotOutExists(d, name) ? objects(
            name.replaceAll("\\.in$", ""), s, f) : null;
      }
    }.go();
  }
  /** Runs a parameterized test case, based on the instance variables of this
   * instance, and check that no matter what, even if the number of
   * opportunities is zero, the input does not change. */
  @Test public void checkNoChange() {
    azzert.notNull("Cannot instantiate Spartanization object", spartanization);
    if (input.getName().indexOf(FileTestUtils.testSuffix) <= 0)
      azzert.that(TESTUtils.rewrite(spartanization, (CompilationUnit) ast.COMPILIATION_UNIT.from(input), new Document(input())).get(), is(input()));
    else
      azzert.that("", TESTUtils.rewrite(spartanization, (CompilationUnit) ast.COMPILIATION_UNIT.from(input), new Document(ast.string(FileTestUtils.makeInFile(input)))).get(),
          is(ast.string(FileTestUtils.makeInFile(input))));
  }
  /** Runs a parameterized test case, based on the instance variables of this
   * instance, and check that no opportunities are found. */
  @Test public void checkNoOpportunities() {
    azzert.notNull("Cannot instantiate spartanization object", spartanization);
    final ASTNode n = ast.COMPILIATION_UNIT.from(input);
    azzert.that(n, notNullValue());
    azzert.that(n, is(instanceOf(CompilationUnit.class)));
    azzert.that(spartanization.findOpportunities((CompilationUnit) n).size(), is(0));
  }
  private String input() {
    return ast.string(input);
  }

  /** Where the input text can be found */
  @Parameter(2) public File input;
  /** The name of the specific test for this transformation */
  @Parameter(1) public String name;
  /** An object describing the required transformation */
  @Parameter(0) public Spartanization spartanization;
}