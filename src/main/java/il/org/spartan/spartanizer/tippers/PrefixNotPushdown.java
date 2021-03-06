package il.org.spartan.spartanizer.tippers;

import static org.eclipse.jdt.core.dom.InfixExpression.Operator.*;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression.*;

import static il.org.spartan.spartanizer.ast.navigate.step.*;

import static il.org.spartan.spartanizer.ast.navigate.extract.*;

import il.org.spartan.spartanizer.ast.create.*;
import il.org.spartan.spartanizer.ast.navigate.*;
import il.org.spartan.spartanizer.dispatch.*;
import il.org.spartan.spartanizer.tipping.*;

/** pushes down "<code>!</code>", the negation operator as much as possible,
 * using the de-Morgan and other simplification rules.
 * @author Yossi Gil
 * @since 2015-7-17 */
public final class PrefixNotPushdown extends ReplaceCurrentNode<PrefixExpression> implements TipperCategory.Idiomatic {
  /** @param o JD
   * @return operator that produces the logical negation of the parameter */
  public static Operator conjugate(final Operator ¢) {
    return ¢ == null ? null
        : ¢.equals(CONDITIONAL_AND) ? CONDITIONAL_OR //
            : ¢.equals(CONDITIONAL_OR) ? CONDITIONAL_AND //
                : ¢.equals(EQUALS) ? NOT_EQUALS
                    : ¢.equals(NOT_EQUALS) ? EQUALS
                        : ¢.equals(LESS_EQUALS) ? GREATER
                            : ¢.equals(GREATER) ? LESS_EQUALS //
                                : ¢.equals(GREATER_EQUALS) ? LESS //
                                    : ¢.equals(LESS) ? GREATER_EQUALS : null;
  }

  /** A utility function, which tries to simplify a boolean expression, whose
   * top most parameter is logical negation.
   * @param x JD
   * @return simplified parameter */
  public static Expression simplifyNot(final PrefixExpression ¢) {
    return pushdownNot(az.not(extract.core(¢)));
  }

  static Expression notOfLiteral(final BooleanLiteral ¢) {
    final BooleanLiteral $ = duplicate.of(¢);
    $.setBooleanValue(!¢.booleanValue());
    return $;
  }

  static Expression perhapsNotOfLiteral(final Expression ¢) {
    return !iz.booleanLiteral(¢) ? null : notOfLiteral(az.booleanLiteral(¢));
  }

  static Expression pushdownNot(final Expression ¢) {
    Expression $;
    return ($ = perhapsNotOfLiteral(¢)) != null//
        || ($ = perhapsDoubleNegation(¢)) != null//
        || ($ = perhapsDeMorgan(¢)) != null//
        || ($ = perhapsComparison(¢)) != null //
            ? $ : null;
  }

  private static Expression comparison(final InfixExpression ¢) {
    return subject.pair(left(¢), right(¢)).to(conjugate(¢.getOperator()));
  }

  private static boolean hasOpportunity(final Expression inner) {
    return iz.booleanLiteral(inner) || az.not(inner) != null || az.andOrOr(inner) != null || az.comparison(inner) != null;
  }

  private static boolean hasOpportunity(final PrefixExpression ¢) {
    return ¢ != null && hasOpportunity(core(step.operand(¢)));
  }

  private static Expression perhapsComparison(final Expression inner) {
    return perhapsComparison(az.comparison(inner));
  }

  private static Expression perhapsComparison(final InfixExpression inner) {
    return inner == null ? null : comparison(inner);
  }

  private static Expression perhapsDeMorgan(final Expression ¢) {
    return perhapsDeMorgan(az.andOrOr(¢));
  }

  private static Expression perhapsDeMorgan(final InfixExpression ¢) {
    return ¢ == null ? null : wizard.applyDeMorgan(¢);
  }

  private static Expression perhapsDoubleNegation(final Expression ¢) {
    return perhapsDoubleNegation(az.not(¢));
  }

  private static Expression perhapsDoubleNegation(final PrefixExpression ¢) {
    return ¢ == null ? null : tryToSimplify(step.operand(¢));
  }

  private static Expression pushdownNot(final PrefixExpression ¢) {
    return ¢ == null ? null : pushdownNot(step.operand(¢));
  }

  private static Expression tryToSimplify(final Expression ¢) {
    final Expression $ = pushdownNot(az.not(¢));
    return $ != null ? $ : ¢;
  }

  @Override public String description(@SuppressWarnings("unused") final PrefixExpression __) {
    return "Pushdown logical negation ('!')";
  }

  @Override public boolean prerequisite(final PrefixExpression ¢) {
    return ¢ != null && az.not(¢) != null && hasOpportunity(az.not(¢));
  }

  @Override public Expression replacement(final PrefixExpression ¢) {
    return simplifyNot(¢);
  }
}