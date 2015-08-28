package org.spartan.refactoring.wring;

import static org.eclipse.jdt.core.dom.InfixExpression.Operator.AND;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.OR;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.XOR;
import static org.spartan.utils.Utils.in;

import java.util.List;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.spartan.refactoring.utils.ExpressionComparator;
import org.spartan.refactoring.utils.Extract;
import org.spartan.refactoring.utils.Subject;

/**
 * A {@link Wring} that sorts the arguments of an expression using the same
 * sorting order as {@link Operator#PLUS} expression, except that we do not
 * worry about commutativity. Unlike {@link InfixSortAddition}, we know that the
 * reordering is always possible.
 *
 * @see InfixSortAddition
 * @author Yossi Gil
 * @since 2015-07-17
 */
public final class InfixSortPseudoAddition extends Wring.Replacing<InfixExpression> {
  private static boolean sort(final InfixExpression e) {
    return sort(Extract.allOperands(e));
  }
  private static boolean sort(final List<Expression> es) {
    return Wrings.sort(es, ExpressionComparator.ADDITION);
  }
  @Override boolean eligible(final InfixExpression e) {
    return sort(e);
  }
  @Override Expression replacement(final InfixExpression e) {
    final List<Expression> operands = Extract.allOperands(e);
    return !sort(operands) ? null : Subject.operands(operands).to(e.getOperator());
  }
  @Override boolean scopeIncludes(final InfixExpression e) {
    return in(e.getOperator(), OR, XOR, AND);
  }
  @Override String description(final InfixExpression e) {
    return "Reorder operands of " + e.getOperator();
  }
}