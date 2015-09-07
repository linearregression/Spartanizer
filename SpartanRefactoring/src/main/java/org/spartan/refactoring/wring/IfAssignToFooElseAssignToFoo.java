package org.spartan.refactoring.wring;

import static org.spartan.refactoring.utils.Funcs.*;

import org.eclipse.jdt.core.dom.*;
import org.spartan.refactoring.utils.Extract;
import org.spartan.refactoring.utils.Subject;

/**
 * A {@link Wring} to convert <code>if (x)
 *   a += 3;
 * else
 *   a += 9;</code> into <code>a += x ? 3 : 9;</code>
 *
 * @author Yossi Gil
 * @since 2015-07-29
 */
public final class IfAssignToFooElseAssignToFoo extends Wring.ReplaceCurrentNode<IfStatement> {
  @Override Statement replacement(final IfStatement s) {
    final Assignment then = Extract.assignment(then(s));
    final Assignment elze = Extract.assignment(elze(s));
    return !compatible(then, elze) ? null : Subject.pair(left(then), Subject.pair(right(then), right(elze)).toCondition(s.getExpression())).toStatement(then.getOperator());
  }
  @Override boolean scopeIncludes(final IfStatement s) {
    return s != null && compatible(Extract.assignment(then(s)), Extract.assignment(elze(s)));
  }
  @Override String description(final IfStatement s) {
    return "Consolidate assignments to " + left(Extract.assignment(then(s)));
  }
}