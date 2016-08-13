package il.org.spartan.refactoring.utils;

import static il.org.spartan.refactoring.utils.Funcs.*;
import static il.org.spartan.refactoring.utils.expose.*;
import static il.org.spartan.utils.Utils.*;

import java.util.*;

import org.eclipse.jdt.core.dom.*;

import il.org.spartan.utils.*;

/** A utility class for finding occurrences of an {@link Expression} in an
 * {@link ASTNode}.
 * @author Boris van Sosin <boris.van.sosin @ gmail.com>
 * @author Yossi Gil <yossi.gil @ gmail.com> (major refactoring 2013/07/10)
 * @since 2013/07/01 */
public enum Collect {
  /** collects semantic (multiple uses for loops) uses of an variable */
  USES_SEMANTIC {
    @Override ASTVisitor[] collectors(final SimpleName n, final List<SimpleName> into) {
      return asArray(new UsesCollector(into, n));
    }
  },
  /** collects assignments of an variable */
  DEFINITIONS {
    @Override ASTVisitor[] collectors(final SimpleName n, final List<SimpleName> into) {
      return asArray(definitionsCollector(into, n));
    }
  },
  /** collects assignments AND semantic (multiple uses for loops) uses of a
   * variable */
  BOTH_SEMANTIC {
    @Override ASTVisitor[] collectors(final SimpleName n, final List<SimpleName> into) {
      return asArray(new UsesCollector(into, n), lexicalUsesCollector(into, n), definitionsCollector(into, n));
    }
  },
  /** collects assignments AND lexical (single use for loops) uses of an
   * expression */
  BOTH_LEXICAL {
    @Override ASTVisitor[] collectors(final SimpleName n, final List<SimpleName> into) {
      return asArray(lexicalUsesCollector(into, n), definitionsCollector(into, n));
    }
  };
  /** An abstract class to carry out the collection process. Should not be
   * instantiated or used directly by clients, other than the use as part of
   * fluent API.
   * @author Yossi Gil
   * @since 2015-09-06 */
  public abstract static class Collector {
    protected final SimpleName name;

    Collector(final SimpleName name) {
      this.name = name;
    }
    public abstract List<SimpleName> in(final ASTNode... ns);
  }

  /** An auxiliary class which makes it possible to use an easy invocation
   * sequence for the various offerings of the containing class. This class
   * should never be instantiated or inherited by clients.
   * <p>
   * This class realizes the function object concept; an instance of it records
   * the value we search for; it represents the function that, given a location
   * for the search, will carry out the search for the captured value in its
   * location parameter.
   * @see Collect#of
   * @author Yossi Gil <yossi.gil @ gmail.com>
   * @since 2013/14/07 */
  public static abstract class Of {
    /** Determine whether this instance occurs in a bunch of expressions
     * @param ns JD
     * @return <code><b>true</b></code> <i>iff</i> this instance occurs in the
     *         Parameter. */
    public boolean existIn(final ASTNode... ns) {
      return !in(ns).isEmpty();
    }
    /** the method that will carry out the search
     * @param ns where to search
     * @return a list of occurrences of the captured value in the parameter. */
    public abstract List<SimpleName> in(ASTNode... ns);
  }

  static final ASTMatcher matcher = new ASTMatcher();

  /** Creates an ASTVisitor that adds to the provided SimpleName list all the
   * identifiers of variable declarations expressions, which are identical the
   * provided ASTNode's.
   * @param into - The ASTVisitor's output parameter
   * @param n JD
   * @return <b>ASTVisitor</b> as described above. */
  static ASTVisitor declarationsCollector(final List<SimpleName> into, final ASTNode n) {
    return new MethodExplorer.IgnoreNestedMethods() {
      /** Adds to the list provided by the closure (into) the name of the given
       * candidate.
       * @param candidate to be inserter to the list provided by the closure
       *        (into).
       * @return <code><b>true</b></code> <i>iff</i> the identifier of the given
       *         {@SimpleName} is equal to the ASTnode's provided by the closure
       *         (n) */
      boolean add(final SimpleName candidate) {
        if (same(candidate, n))
          into.add(candidate);
        return true;
      }
      /** Tries to add to the list provided by the closure (into) the names of
       * the {@VariableDeclarationFragment}s given in the param (fs).
       * @param fs is a {@link List} of a {@link VariableDeclarationFragment} */
      void addFragments(final List<VariableDeclarationFragment> fs) {
        for (final VariableDeclarationFragment f : fs)
          add(f.getName());
      }
      /** Tries to add to the list provided by the closure (into) the
       * identifiers from all the {@link VariableDeclarationExpression}s from
       * the given list (es).
       * @param es is a {@link List} of any type which extends a
       *        {@link Expression}
       * @return <code><b>true</b></code> <i>iff</i> addFragment() succeeds with
       *         the {@link VariableDeclarationFragment}s from each (extended)
       *         Expression in the param (es). */
      boolean consider(final List<? extends Expression> es) {
        for (final Expression e : es)
          addFragments(fragments(asVariableDeclarationExpression(e)));
        return true;
      }
      @Override public boolean visit(final ForStatement s) {
        return consider(initializers(s));
      }
      @Override public boolean visit(final TryStatement s) {
        return consider(resources(s));
      }
      @Override public boolean visit(final VariableDeclarationFragment f) {
        return add(f.getName());
      }
      @Override public boolean visit(final VariableDeclarationStatement s) {
        addFragments(fragments(s));
        return true;
      }
    };
  }
  /** Creates a new Collector which holds the occurrences of the provided name
   * in declarations.
   * @param n JD
   * @return A {@link Collector}, with the uses of the provided identifier
   *         within declarations. */
  public static Collector declarationsOf(final SimpleName n) {
    return new Collector(n) {
      @Override public List<SimpleName> in(final ASTNode... ns) {
        final List<SimpleName> $ = new ArrayList<>();
        for (final ASTNode ¢ : ns)
          ¢.accept(declarationsCollector($, name));
        return $;
      }
    };
  }
  /** @see {@link declarationsCollector} specific comments are provided to
   *      methods which are not taking place in the
   *      {@link declarationsCollector}. */
  static ASTVisitor definitionsCollector(final List<SimpleName> into, final ASTNode n) {
    return new MethodExplorer.IgnoreNestedMethods() {
      boolean add(final SimpleName candidate) {
        if (same(candidate, n))
          into.add(candidate);
        return true;
      }
      void addFragments(final List<VariableDeclarationFragment> fs) {
        for (final VariableDeclarationFragment f : fs)
          add(f.getName());
      }
      /** ThiWs function is needed cause a definition can be not in a
       * declaration form, and then #asVariableDeclarationExpression() will fail
       * @param e JD
       * @return <code><b>true</b></code> <i>iff</i> the identifier of the given
       *         {@link Expression} is equal to the ASTnode's provided by the
       *         closure (n) */
      boolean consider(final Expression e) {
        return add(asSimpleName(e));
      }
      boolean consider(final List<? extends Expression> initializers) {
        for (final Expression e : initializers)
          addFragments(fragments(asVariableDeclarationExpression(e)));
        return true;
      }
      @Override public boolean visit(final Assignment a) {
        return consider(left(a));
      }
      @Override public boolean visit(final ForStatement s) {
        return consider(initializers(s));
      }
      /** {@link PostfixExpression} can be only INCREMENT OR DECREMENT.
       * @param it JD
       * @return the identifier of the operand. */
      @Override public boolean visit(final PostfixExpression it) {
        // return !in(it.getOperator(), PostfixExpression.Operator.INCREMENT,
        // PostfixExpression.Operator.DECREMENT) || consider(it.getOperand());
        return consider(it.getOperand());
      }
      /** {@link PrefixExpression} can be more then only INCREMENT OR DECREMENT,
       * but only on that cases it is a definition.
       * @param it JD
       * @return the identifier of the operand. */
      @Override public boolean visit(final PrefixExpression it) {
        // return consider(it.getOperand());
        return !in(it.getOperator(), PrefixExpression.Operator.INCREMENT, PrefixExpression.Operator.DECREMENT) || consider(it.getOperand());
      }
      @Override public boolean visit(final TryStatement s) {
        return consider(resources(s));
      }
      @Override public boolean visit(final VariableDeclarationFragment f) {
        return add(f.getName());
      }
      @Override public boolean visit(final VariableDeclarationStatement s) {
        addFragments(fragments(s));
        return true;
      }
    };
  }
  /** @see declarationsOf */
  public static Collector definitionsOf(final SimpleName n) {
    return new Collector(n) {
      @Override public List<SimpleName> in(final ASTNode... ns) {
        final List<SimpleName> $ = new ArrayList<>();
        for (final ASTNode ¢ : ns)
          ¢.accept(definitionsCollector($, name));
        return $;
      }
    };
  }
  /** Finds all the rest (not declarations or definitions) identifier (n) uses.
   * @param n same as "name"
   * @return {@link Collector} of all occurrences which are not definitions. */
  public static Collector forAllOccurencesExcludingDefinitions(final SimpleName n) {
    return new Collector(n) {
      @Override public List<SimpleName> in(final ASTNode... ns) {
        final List<SimpleName> $ = new ArrayList<>();
        for (final ASTNode ¢ : ns)
          ¢.accept(new UsesCollectorIgnoreDefinitions($, name));
        return $;
      }
    };
  }
  // didn't find any use case in which it will be different of usesCollector
  /** Creates an ASTVisitor that adds all explicit uses (by name) of a
   * SimpleName to the provided list.
   * @param into JD
   * @param what JD
   * @return ASTVisitor that adds uses by name of the SimpleName 'what' to the
   *         list 'into' */
  static ASTVisitor lexicalUsesCollector(final List<SimpleName> into, final SimpleName what) {
    return usesCollector(what, into, true);
  }
  /** finds all the occurrences of the given name (n) in which it is a
   * {@link ClassInstanceCreation}
   * @param n JD
   * @return a Collector with all unsafe uses of the identifier (n) */
  public static Collector unsafeUsesOf(final SimpleName n) {
    return new Collector(n) {
      @Override public List<SimpleName> in(final ASTNode... ns) {
        final List<SimpleName> $ = new ArrayList<>();
        for (final ASTNode ¢ : ns)
          ¢.accept(new UnsafeUsesCollector($, name));
        return $;
      }
    };
  }
  /** Creates an ASTVisitor that returns all the instances in which the provided
   * SimpleName was used. The instances will be inserted into the provided list.
   * @param what JD
   * @param into JD
   * @param lexicalOnly - True if only explicit matches (by name) are required.
   * @return ASTVisitor that adds all the uses of the SimpleName to the provided
   *         list. */
  private static ASTVisitor usesCollector(final SimpleName what, final List<SimpleName> into, final boolean lexicalOnly) {
    return new ASTVisitor() {
      private int loopDepth = 0;

      boolean add(final Object o) {
        return collect((Expression) o);
      }
      boolean collect(final Expression e) {
        collectExpression(e);
        return true;
      }
      boolean collect(@SuppressWarnings("rawtypes") final List os) {
        for (final Object o : os)
          add(o);
        return true;
      }
      void collectExpression(final Expression e) {
        if (e instanceof SimpleName)
          collectExpression((SimpleName) e);
      }
      void collectExpression(final SimpleName n) {
        if (!same(what, n))
          return;
        into.add(n);
        if (repeated())
          into.add(n);
      }
      @Override public void endVisit(@SuppressWarnings("unused") final DoStatement __) {
        --loopDepth;
      }
      @Override public void endVisit(@SuppressWarnings("unused") final EnhancedForStatement __) {
        --loopDepth;
      }
      @Override public void endVisit(@SuppressWarnings("unused") final ForStatement __) {
        --loopDepth;
      }
      @Override public void endVisit(@SuppressWarnings("unused") final WhileStatement __) {
        --loopDepth;
      }
      List<VariableDeclarationFragment> getFieldsOfClass(final ASTNode classNode) {
        final List<VariableDeclarationFragment> $ = new ArrayList<>();
        classNode.accept(new ASTVisitor() {
          @Override public boolean visit(final FieldDeclaration d) {
            $.addAll(fragments(d));
            return false;
          }
        });
        return $;
      }
      boolean repeated() {
        return !lexicalOnly && loopDepth > 0;
      }
      @Override public boolean visit(final AnonymousClassDeclaration d) {
        for (final VariableDeclarationFragment f : getFieldsOfClass(d))
          if (f.getName().subtreeMatch(matcher, what))
            return false;
        return true;
      }
      @Override public boolean visit(final Assignment a) {
        return collect(right(a));
      }
      @Override public boolean visit(final CastExpression e) {
        return collect(e.getExpression());
      }
      @Override public boolean visit(final ClassInstanceCreation c) {
        collect(c.getExpression());
        return collect(c.arguments());
      }
      @Override public boolean visit(final DoStatement s) {
        ++loopDepth;
        return collect(s.getExpression());
      }
      @Override public boolean visit(@SuppressWarnings("unused") final EnhancedForStatement __) {
        ++loopDepth;
        return true;
      }
      @Override public boolean visit(final FieldAccess n) {
        collect(n.getExpression());
        return false;
      }
      @Override public boolean visit(@SuppressWarnings("unused") final ForStatement __) {
        ++loopDepth;
        return true;
      }
      @Override public boolean visit(final InstanceofExpression e) {
        return collect(left(e));
      }
      @Override public boolean visit(final MethodDeclaration d) {
        /* Now: this is a bit complicated. Java allows declaring methods in
         * anonymous classes in which the formal parameters hide variables in
         * the enclosing scope. We don't want to collect them as uses of the
         * variable */
        for (final Object o : d.parameters())
          if (((SingleVariableDeclaration) o).getName().subtreeMatch(matcher, what))
            return false;
        return true;
      }
      @Override public boolean visit(final MethodInvocation i) {
        collect(i.getExpression());
        collect(i.arguments());
        return false;
      }
      @Override public boolean visit(final QualifiedName n) {
        collectExpression(n.getName());
        return false;
      }
      @Override public boolean visit(final SimpleName n) {
        return collect(n);
      }
      @Override public boolean visit(@SuppressWarnings("unused") final WhileStatement __) {
        ++loopDepth;
        return true;
      }
    };
  }
  /** Creates a new Collector which holds all the occurrences of the provided
   * name.
   * @param n JD
   * @return A {@link Collector}, with the uses of the provided identifier
   *         within the provided {@link ASTNode)s array to the in function.. */
  public static Collector usesOf(final SimpleName n) {
    return new Collector(n) {
      @Override public List<SimpleName> in(final ASTNode... ns) {
        final List<SimpleName> $ = new ArrayList<>();
        for (final ASTNode ¢ : ns)
          ¢.accept(new UsesCollector($, name));
        return $;
      }
    };
  }
  /** Lists the required occurrences
   * @param what the expression to search for
   * @param ns the n in which to counted
   * @return the list of uses */
  final List<SimpleName> collect(final SimpleName what, final ASTNode... ns) {
    final List<SimpleName> $ = new ArrayList<>();
    for (final ASTNode n : ns)
      for (final ASTVisitor v : collectors(what, $))
        n.accept(v);
    Utils.removeDuplicates($);
    Collections.sort($, (e1, e2) -> e1.getStartPosition() - e2.getStartPosition());
    return $;
  }
  abstract ASTVisitor[] collectors(final SimpleName n, final List<SimpleName> into);
  /** Creates a function object for searching for a given value.
   * @param n what to search for
   * @return a function object to be used for searching for the parameter in a
   *         given location */
  public Of of(final SimpleName n) {
    return new Of() {
      @Override public List<SimpleName> in(final ASTNode... ns) {
        return collect(n, ns);
      }
    };
  }
  /** Creates a function object for searching for a given {@link SimpleName}, as
   * specified by the {@link VariableDeclarationFragment},
   * @param f JD
   * @return a function object to be used for searching for the
   *         {@link SimpleName} embedded in the parameter. */
  public Of of(final VariableDeclarationFragment f) {
    return of(f.getName());
  }
}