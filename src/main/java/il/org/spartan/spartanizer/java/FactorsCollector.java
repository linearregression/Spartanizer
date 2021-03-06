package il.org.spartan.spartanizer.java;

import static il.org.spartan.lisp.*;

import java.util.*;

import org.eclipse.jdt.core.dom.*;

import static il.org.spartan.spartanizer.ast.navigate.extract.*;

import il.org.spartan.spartanizer.ast.create.*;
import il.org.spartan.spartanizer.ast.navigate.*;

// TOOD Niv: Who wrote this class?
public final class FactorsCollector {
  public static boolean isLeafFactor(final Expression ¢) {
    return !iz.infixTimes(¢) && !iz.infixDivide(¢);
  }

  private final List<Expression> multipliers = new ArrayList<>();
  private final List<Expression> dividers = new ArrayList<>();
  private final List<Factor> all = new ArrayList<>();

  public FactorsCollector(final InfixExpression e) {
    collect(e);
  }

  FactorsCollector() {
    /* For internal use only */
  }

  public List<Factor> all() {
    return all;
  }

  public List<Expression> dividers() {
    return dividers;
  }

  public List<Expression> multipliers() {
    return multipliers;
  }

  FactorsCollector collect(final InfixExpression ¢) {
    if (¢ != null && !isLeafFactor(¢))
      collectTimesNonLeaf(¢);
    return this;
  }

  Void collectTimesNonLeaf(final InfixExpression ¢) {
    assert ¢ != null;
    assert !isLeafFactor(¢);
    assert iz.infixTimes(¢) || iz.infixDivide(¢);
    return iz.infixTimes(¢) ? collectTimesPrefixTimesExpression(¢) //
        : collectTimesPrefixDivdeExpression(¢);
  }

  Void collectTimesPrefixDivdeExpression(final InfixExpression x) {
    assert x != null;
    assert !isLeafFactor(x);
    assert iz.infixDivide(x);
    final List<Expression> es = hop.operands(x);
    addMultiplierFactor(core(first(es)));
    return collectDividersFactors(rest(es));
  }

  private Void addDivide(final Expression x) {
    assert x != null;
    final Expression ¢ = minus.level(x) % 2 == 0 ? minus.peel(x) : subject.operand(minus.peel(x)).to(wizard.MINUS1);
    all.add(Factor.divide(¢));
    dividers.add(¢);
    return null;
  }

  private Void addDivideFactor(final Expression ¢) {
    assert ¢ != null;
    return collectDividePrefix(¢);
  }

  private Void addMultiplierFactor(final Expression ¢) {
    return isLeafFactor(¢) ? addTimesFactor(¢) : collectTimesNonLeaf(az.infixExpression(¢));
  }

  private Void addTimes(final Expression x) {
    assert x != null;
    final Expression ¢ = minus.level(x) % 2 == 0 ? minus.peel(x) : subject.operand(minus.peel(x)).to(wizard.MINUS1);
    multipliers.add(¢);
    all.add(Factor.times(¢));
    return null;
  }

  private Void addTimesFactor(final Expression ¢) {
    assert ¢ != null;
    return collectTimesPrefix(¢);
  }

  private Void collectDividePrefix(final Expression ¢) {
    assert ¢ != null;
    return isLeafFactor(¢) ? addDivide(¢) : collectDividePrefix(az.infixExpression(¢));
  }

  private Void collectDividePrefix(final InfixExpression ¢) {
    assert ¢ != null;
    assert !isLeafFactor(¢);
    return iz.infixTimes(¢) ? collectDividePrefixTimesExpression(¢) : collectDividePrefixDivideExprssion(¢);
  }

  private Void collectDividePrefixDivideExprssion(final InfixExpression x) {
    assert x != null;
    final List<Expression> es = hop.operands(x);
    collectDividerFactor(core(first(es)));
    return collectMultiplierFactors(rest(es));
  }

  private Void collectDividePrefixTimesExpression(final InfixExpression ¢) {
    assert ¢ != null;
    assert !isLeafFactor(¢);
    assert iz.infixTimes(¢);
    return collectDividersFactors(hop.operands(¢));
  }

  private Void collectDividerFactor(final Expression ¢) {
    assert ¢ != null;
    return isLeafFactor(¢) ? addDivideFactor(¢) : collectDividePrefix(az.infixExpression(¢));
  }

  private Void collectDividersFactors(final Iterable<Expression> xs) {
    assert xs != null;
    for (final Expression ¢ : xs)
      collectDividerFactor(core(¢));
    return null;
  }

  private Void collectMultiplierFactors(final Iterable<Expression> xs) {
    assert xs != null;
    for (final Expression ¢ : xs)
      addMultiplierFactor(core(¢));
    return null;
  }

  private Void collectTimesPrefix(final Expression ¢) {
    assert ¢ != null;
    return isLeafFactor(¢) ? addTimes(¢) : collectTimesNonLeaf(az.infixExpression(¢));
  }

  private Void collectTimesPrefixTimesExpression(final InfixExpression ¢) {
    assert ¢ != null;
    assert !isLeafFactor(¢);
    assert iz.infixTimes(¢);
    return collectMultiplierFactors(hop.operands(¢));
  }
}
