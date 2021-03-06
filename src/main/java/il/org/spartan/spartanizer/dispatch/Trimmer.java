package il.org.spartan.spartanizer.dispatch;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.*;
import org.eclipse.jface.text.*;
import org.eclipse.text.edits.*;

import il.org.spartan.plugin.*;
import il.org.spartan.spartanizer.ast.navigate.*;
import il.org.spartan.spartanizer.cmdline.*;
import il.org.spartan.spartanizer.engine.*;
import il.org.spartan.spartanizer.tipping.*;
import il.org.spartan.spartanizer.utils.*;

/** @author Yossi Gil
 * @since 2015/07/10 */
public class Trimmer extends GUI$Applicator {
  /** Disable laconic tips, used to indicate that no spartanization should be
   * made to node */
  public static final String disablers[] = { "[[SuppressWarningsSpartan]]", //
  };
  /** Enable spartanization identifier, overriding a disabler */
  public static final String enablers[] = { "[[EnableWarningsSpartan]]", //
  };
  static final String disabledPropertyId = "Trimmer_disabled_id";

  /** A recursive scan for disabled nodes. Adds disabled property to disabled
   * nodes and their sub trees.
   * <p>
   * Algorithm:
   * <ol>
   * <li>Visit all nodes that contain an annotation.
   * <li>If a node has a disabler, disable all nodes below it using
   * {@link hop#descendants(ASTNode)}
   * <li>Disabling is done by setting a node property, and is carried out
   * <li>If a node which was previously disabled contains an enabler, enable all
   * all its descendants.
   * <li>If a node which was previously enabled, contains a disabler, disable
   * all nodes below it, and carry on.
   * <li>Obviously, the visit needs to be pre-order, i.e., visiting the parent
   * before the children.
   * </ol>
   * The disabling information is used later by the tip/fixing mechanisms, which
   * should know little about this class.
   * @param n an {@link ASTNode}
   * @author Ori Roth
   * @since 2016/05/13 */
  public static void disabledScan(final ASTNode n) {
    n.accept(new DispatchingVisitor() {
      @Override protected <N extends ASTNode> boolean go(final N ¢) {
        if (!(¢ instanceof BodyDeclaration) || !isDisabledByIdentifier((BodyDeclaration) ¢))
          return true;
        disable((BodyDeclaration) ¢);
        return false;
      }
    });
  }

  /** @param n an {@link ASTNode}
   * @return true iff the node is spartanization disabled */
  public static boolean isDisabled(final ASTNode ¢) {
    return NodeData.has(¢, disabledPropertyId);
  }

  public static boolean prune(final Tip r, final List<Tip> rs) {
    if (r != null) {
      r.pruneIncluders(rs);
      rs.add(r);
    }
    return true;
  }

  /** The recursive disabling process. Returns to {@link Trimmer#disabledScan}
   * upon reaching an enabler.
   * @param d disabled {@link BodyDeclaration} */
  static void disable(final BodyDeclaration d) {
    d.accept(new DispatchingVisitor() {
      @Override protected <N extends ASTNode> boolean go(final N ¢) {
        if (¢ instanceof BodyDeclaration && isEnabledByIdentifier((BodyDeclaration) ¢)) {
          disabledScan(¢);
          return false;
        }
        NodeData.set(¢, disabledPropertyId);
        return true;
      }
    });
  }

  static boolean hasJavaDocIdentifier(final BodyDeclaration d, final String[] ids) {
    if (d == null || d.getJavadoc() == null)
      return false;
    final String s = d.getJavadoc() + "";
    for (final String ¢ : ids)
      if (s.contains(¢))
        return true;
    return false;
  }

  static boolean isDisabledByIdentifier(final BodyDeclaration ¢) {
    return hasJavaDocIdentifier(¢, disablers);
  }

  static boolean isEnabledByIdentifier(final BodyDeclaration ¢) {
    return !hasJavaDocIdentifier(¢, disablers) && hasJavaDocIdentifier(¢, enablers);
  }

  public final Toolbox toolbox;

  /** Instantiates this class */
  public Trimmer() {
    this(Toolbox.defaultInstance());
  }

  public Trimmer(final Toolbox toolbox) {
    super("Apply");
    this.toolbox = toolbox;
  }

  @Override public void consolidateTips(final ASTRewrite r, final CompilationUnit u, final IMarker m) {
    Toolbox.refresh();
    u.accept(new DispatchingVisitor() {
      @Override protected <N extends ASTNode> boolean go(final N n) {
        progressMonitor.worked(1);
        TrimmerLog.visitation(n);
        if (!inRange(m, n) || isDisabled(n))
          return true;
        final Tipper<N> w = Toolbox.defaultInstance().firstTipper(n);
        if (w == null)
          return true;
        Tip s = null;
        try {
          s = w.tip(n, exclude);
          TrimmerLog.tip(w, n);
        } catch (final TipperFailure f) {
          monitor.debug(this, f);
        }
        if (s != null) {
          if (LogManager.isActive())
            LogManager.getLogWriter().printRow(u.getJavaElement().getElementName(), s.description, s.lineNumber + "");
          TrimmerLog.application(r, s);
        }
        return true;
      }

      @Override protected void initialization(final ASTNode ¢) {
        disabledScan(¢);
      }
    });
  }

  public String fixed(final String from) {
    for (final Document $ = new Document(from);;) {
      final CompilationUnit u = (CompilationUnit) makeAST.COMPILATION_UNIT.from($.get());
      final ASTRewrite r = createRewrite(u);
      final TextEdit e = r.rewriteAST($, null);
      try {
        e.apply($);
      } catch (final MalformedTreeException | IllegalArgumentException | BadLocationException x) {
        monitor.logEvaluationError(this, x);
        throw new AssertionError(x);
      }
      if (!e.hasChildren())
        return $.get();
    }
  }

  @Override protected ASTVisitor makeTipsCollector(final List<Tip> $) {
    return new DispatchingVisitor() {
      @Override protected <N extends ASTNode> boolean go(final N n) {
        progressMonitor.worked(1);
        if (isDisabled(n))
          return true;
        final Tipper<N> w = Toolbox.defaultInstance().firstTipper(n);
        if (w != null)
          progressMonitor.worked(5);
        try {
          return w == null || w.cantTip(n) || prune(w.tip(n, exclude), $);
        } catch (final TipperFailure f) {
          monitor.debug(this, f);
        }
        return false;
      }

      @Override protected void initialization(final ASTNode ¢) {
        disabledScan(¢);
      }
    };
  }

  public abstract class With {
    public Trimmer trimmer() {
      return Trimmer.this;
    }
  }
}
