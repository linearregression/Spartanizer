package org.spartan.refactoring.wring;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.spartan.refactoring.utils.Extract.core;
import static org.spartan.refactoring.utils.Funcs.asConditionalExpression;
import static org.spartan.refactoring.utils.Funcs.same;
import static org.spartan.refactoring.utils.Into.c;

import java.util.Collection;

import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.spartan.refactoring.wring.AbstractWringTest.OutOfScope;
import org.spartan.utils.Utils;

/**
 * Unit tests for {@link Wrings#ADDITION_SORTER}.
 *
 * @author Yossi Gil
 * @since 2014-07-13
 */
@SuppressWarnings({ "javadoc", "static-method" }) //
@FixMethodOrder(MethodSorters.NAME_ASCENDING) //
public class TernaryCollapseTest {
  static final Wring<ConditionalExpression> WRING = new TernaryCollapse();
  @Test public void steps() {
    final ConditionalExpression e = c("a ? b ? x : z :z");
    assertNotNull(e);
    final ConditionalExpression then = asConditionalExpression(core(e.getThenExpression()));
    assertNotNull(then);
    final Expression elze = core(e.getElseExpression());
    assertNotNull(elze);
    final Expression thenThen = core(then.getThenExpression());
    assertNotNull(thenThen);
    final Expression thenElse = core(then.getElseExpression());
    assertNotNull(thenElse);
    assertTrue(same(thenElse, elze));
  }

  @RunWith(Parameterized.class) //
  public static class OutOfScope extends AbstractWringTest.OutOfScope.Exprezzion<ConditionalExpression> {
    static String[][] cases = Utils.asArray(//
        Utils.asArray("No boolean", "a?b:c"), //
        Utils.asArray("F X", "a ? false : c"), //
        Utils.asArray("T X", "a ? true : c"), //
        Utils.asArray("X F", "a ? b : false"), //
        Utils.asArray("X T", "a ? b : true"), //
        Utils.asArray("() F X", "a ?( false):true"), //
        Utils.asArray("() T X", "a ? (((true ))): c"), //
        Utils.asArray("() X F", "a ? b : (false)"), //
        Utils.asArray("() X T", "a ? b : ((true))"), //
        Utils.asArray("Actual example", "!inRange(m, e) ? true : inner.go(r, e)"), //
        Utils.asArray("Method invocation first", "a?b():c"), //
        Utils.asArray("Not same function invocation ", "a?b(x):d(x)"), //
        Utils.asArray("Not same function invocation ", "a?x.f(x):x.d(x)"), //
        new String[] { "identical method call", "a ? y.f(b) :y.f(b)" }, //
        new String[] { "identical function call", "a ? f(b) :f(b)" }, //
        new String[] { "identical assignment", "a ? (b=c) :(b=c)" }, //
        new String[] { "identical increment", "a ? b++ :b++" }, //
        new String[] { "identical addition", "a ? b+d :b+ d" }, //
        new String[] { "function call", "a ? f(b,c) : f(c)" }, //
        new String[] { "a method call", "a ? y.f(c,b) :y.f(c)" }, //
        new String[] { "a method call distinct receiver", "a ? x.f(c) : y.f(d)" }, //
        new String[] { "not on MINUS", "a ? -c :-d", }, //
        new String[] { "not on NOT", "a ? !c :!d", }, //
        new String[] { "not on MINUSMINUS 1", "a ? --c :--d", }, //
        new String[] { "not on MINUSMINUS 2", "a ? c-- :d--", }, //
        new String[] { "not on PLUSPLUS", "a ? x++ :y++", }, //
        new String[] { "not on PLUS", "a ? +x : +y", }, //
        new String[] { "Into constructor not same arity", "a ? new S(a,new Integer(4),b) : new S(new Ineger(3))" }, //
        new String[] { "field refernece", "externalImage ? R.string.webview_contextmenu_image_download_action : R.string.webview_contextmenu_image_save_action", }, //
        new String[] { "almost identical function call", "a ? f(b) :f(c)" }, //
        new String[] { "almost identical method call", "a ? y.f(b) :y.f(c)" }, //
        new String[] { "almost identical assignment", "a ? (b=c) :(b=d)", }, //
        new String[] { "almost identical 2 addition", "a ? b+d :b+ c", }, //
        new String[] { "almost identical 3 addition", "a ? b+d +x:b+ c + x", }, //
        new String[] { "almost identical 4 addition last", "a ? b+d+e+y:b+d+e+x", }, //
        new String[] { "almost identical 4 addition second", "a ? b+x+e+f:b+y+e+f", }, //
        new String[] { "different target field refernce", "a ? 1 + x.a : 1 + y.a", }, //
        new String[] { "Into constructor 1/1 location", "a.equal(b) ? new S(new Integer(4)) : new S(new Ineger(3))", }, //
        new String[] { "Into constructor 1/3", "a.equal(b) ? new S(new Integer(4),a,b) : new S(new Ineger(3),a,b)", }, //
        new String[] { "Into constructor 2/3", "a.equal(b) ? new S(a,new Integer(4),b) : new S(a, new Ineger(3), b)", }, //
        new String[] { "Into constructor 3/3", "a.equal(b) ? new S(a,b,new Integer(4)) : new S(a,b,new Ineger(3))", }, //
        null);
    /**
     * Generate test cases for this parameterized class.
     *
     * @return a collection of cases, where each case is an array of three
     *         objects, the test case name, the input, and the file.
     */
    @Parameters(name = DESCRIPTION) //
    public static Collection<Object[]> cases() {
      return collect(cases);
    }
    /** Instantiates the enclosing class ({@link OutOfScope}) */
    public OutOfScope() {
      super(WRING);
    }
  }

  @RunWith(Parameterized.class) //
  @FixMethodOrder(MethodSorters.NAME_ASCENDING) //
  public static class Wringed extends AbstractWringTest.WringedExpression.Conditional {
    private static String[][] cases = Utils.asArray(//
        new String[] { "Vanilla", "a ? b ? x : z :z", "a && b ? x : z" }, //
        new String[] { "Vanilla too", "a ? b ? z : x :z", "a && !b ? x : z" }, //
        new String[] { "Vanilla 3", "a ? z : b ? x :z", "!a && b ? x : z" }, //
        new String[] { "Vanilla for", "a ? z :  b ? z :x", "!a && !b ? x : z" }, //
        null);
    /**
     * Generate test cases for this parameterized class.
     *
     * @return a collection of cases, where each case is an array of three
     *         objects, the test case name, the input, and the file.
     */
    @Parameters(name = DESCRIPTION) //
    public static Collection<Object[]> cases() {
      return collect(cases);
    }
    /** Instantiates the enclosing class ({@link WringedExpression}) */
    public Wringed() {
      super(WRING);
    }
  }
}