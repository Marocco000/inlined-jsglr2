package org.spoofax.jsglr2.inlinedIncremental;

import java.util.*;

import org.metaborg.parsetable.productions.IProduction;
import org.metaborg.parsetable.symbols.IMetaVarSymbol;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr2.JSGLR2Request;
import org.spoofax.jsglr2.imploder.ImplodeResult;
import org.spoofax.jsglr2.imploder.TreeImploder;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.parseforest.ICharacterNode;
import org.spoofax.jsglr2.parseforest.IParseNode;

public class TreeImploderInlined {
////@formatter:off
//        <ParseForest extends IParseForest,
//                ParseNode   extends IParseNode<ParseForest, Derivation>,
//                Derivation  extends IDerivation<ParseForest>,
//                Cache,
//                Tree,
//                Input       extends ImplodeInput> // = IncrementalImplodeInput<IncrementalParseNode, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm>, IStrategoTerm>
////@formatter:on
//        extends
//        AbstractTreeImploder<IncrementalParseForest, IncrementalParseNode, IncrementalDerivation,
//                            TreeImploder.SubTree<IStrategoTerm>,
//                            Void, //IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm>
//                            IStrategoTerm,
//                            ImplodeResult<TreeImploder.SubTree<IStrategoTerm>, Void, IStrategoTerm>> {
//    implements IImploder<IncrementalParseForest, TreeImploder.SubTree<IStrategoTerm>, Void, IStrategoTerm,
//                            ImplodeResult<TreeImploder.SubTree<IStrategoTerm>, Void, IStrategoTerm>>

    //    protected final IImplodeInputFactory<IncrementalImplodeInput<IncrementalParseNode, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm>, IStrategoTerm>> inputFactory;
    protected final StrategoTermTreeFactoryInlined treeFactory;

    public TreeImploderInlined(StrategoTermTreeFactoryInlined treeFactory) {
        this.treeFactory = treeFactory;
    }


    public ImplodeResult<TreeImploder.SubTree<IStrategoTerm>, Void, IStrategoTerm> implode(String input, String fileName, IncrementalParseForest parseForest, Void resultCache) {
        return implode(new JSGLR2Request(input, fileName), parseForest, resultCache);
    }

    public ImplodeResult<TreeImploder.SubTree<IStrategoTerm>, Void, IStrategoTerm> implode(JSGLR2Request request, IncrementalParseForest parseForest) {
        return implode(request, parseForest, null);
    }

    public ImplodeResult<TreeImploder.SubTree<IStrategoTerm>, Void, IStrategoTerm> implode(String input, String fileName, IncrementalParseForest parseForest) {
        return implode(new JSGLR2Request(input, fileName), parseForest);
    }


    public ImplodeResult<TreeImploder.SubTree<IStrategoTerm>, Void, IStrategoTerm> implode(JSGLR2Request request,
                                                                                           IncrementalParseForest parseForest, Void resultCache) {
//        TreeImploder.SubTree<IStrategoTerm> result = implodeParseNode(inputFactory.get(request.input), parseForest, 0);
        // TODO (MARA) optimization new class without cache instead of IncrementalImplodeInputInlined
        TreeImploder.SubTree<IStrategoTerm> result = implodeParseNode(new IncrementalImplodeInputInlined(request.input, null), parseForest, 0);

        return new ImplodeResult<>(result, null, result.tree, result.containsAmbiguity);
    }

    protected TreeImploder.SubTree<IStrategoTerm> implodeParseNode(IncrementalImplodeInputInlined input, IncrementalParseForest parseForest, int startOffset) {
        if (parseForest instanceof ICharacterNode) {
            return new TreeImploder.SubTree<>(treeFactory.createCharacterTerminal(((ICharacterNode) parseForest).character()), null,
                    parseForest.width(), true);
        }

        @SuppressWarnings("unchecked") IncrementalParseNode parseNode = implodeInjection((IncrementalParseNode) parseForest);

        IProduction production = parseNode.production();

        if (production.isContextFree() && !production.isSkippableInParseForest()) {
            List<IncrementalDerivation> filteredDerivations = applyDisambiguationFilters(parseNode);

            if (filteredDerivations.size() > 1) {
                List<IStrategoTerm> trees = new ArrayList<>(filteredDerivations.size());
                List<TreeImploder.SubTree<IStrategoTerm>> subTrees = new ArrayList<>(filteredDerivations.size());

                if (production.isList()) {
                    for (List<IncrementalParseForest> derivationParseForests : implodeAmbiguousLists(filteredDerivations)) {
                        TreeImploder.SubTree<IStrategoTerm> result = implodeDerivationChildren(input, production,
                                getChildParseForests(production, derivationParseForests), startOffset);
                        trees.add(result.tree);
                        subTrees.add(result);
                    }
                } else {
                    for (IncrementalDerivation derivation : filteredDerivations) {
                        TreeImploder.SubTree<IStrategoTerm> result = implodeDerivation(input, derivation, startOffset);
                        trees.add(result.tree);
                        subTrees.add(result);
                    }
                }

                return new TreeImploder.SubTree<>(treeFactory.createAmb(trees), subTrees, null, subTrees.get(0).width, false, true,
                        false);
            } else
                return implodeDerivation(input, filteredDerivations.get(0), startOffset);
        } else {
            int width = parseNode.width();

            return new TreeImploder.SubTree<>(createLexicalTerm(production, input.inputString, startOffset, width), production,
                    width, false);
        }
    }

    protected TreeImploder.SubTree<IStrategoTerm> implodeDerivation(IncrementalImplodeInputInlined input, IncrementalDerivation derivation, int startOffset) {
        IProduction production = derivation.production();

        if (!production.isContextFree())
            throw new RuntimeException("non context free imploding not supported");

        return implodeDerivationChildren(input, production, getChildParseForests(derivation), startOffset);
    }

    protected TreeImploder.SubTree<IStrategoTerm> implodeDerivationChildren(IncrementalImplodeInputInlined input, IProduction production,
                                                                            List<IncrementalParseForest> childParseForests, int startOffset) {

        List<IStrategoTerm> childASTs = new ArrayList<>();
        List<TreeImploder.SubTree<IStrategoTerm>> subTrees = new ArrayList<>();

        for (IncrementalParseForest childParseForest : childParseForests) {
            TreeImploder.SubTree<IStrategoTerm> subTree = this.implodeParseNode(input, childParseForest, startOffset);

            if (subTree.tree != null) {
                childASTs.add(subTree.tree);
            }
            subTrees.add(subTree);
            startOffset += subTree.width;
        }

        IStrategoTerm contextFreeTerm = createContextFreeTerm(production, childASTs);
        return new TreeImploder.SubTree<>(contextFreeTerm, subTrees, production,
                childASTs.size() == 1 && contextFreeTerm == childASTs.get(0));
    }

    protected List<IncrementalParseForest> getChildParseForests(IncrementalDerivation derivation) {
        return getChildParseForests(derivation.production(), Arrays.asList(derivation.parseForests()));
    }

    protected List<IncrementalParseForest> getChildParseForests(IProduction production, List<IncrementalParseForest> parseForests) {
        // Make sure lists are flattened
        if (production.isList()) {
            LinkedList<IncrementalParseForest> listQueueDone = new LinkedList<>();
            LinkedList<IncrementalParseForest> listQueueTodo = new LinkedList<>(parseForests);

            // Check child parse forest from front to back
            while (!listQueueTodo.isEmpty()) {
                IncrementalParseForest childParseForest = listQueueTodo.removeFirst();

                @SuppressWarnings("unchecked") IncrementalParseNode childParseNode = (IncrementalParseNode) childParseForest;

                IProduction childProduction = childParseNode.production();

                // If child is also a list, add all its children to the front of the unprocessed list
                if (childProduction.isList() && childProduction.constructor() == null) {
                    List<IncrementalDerivation> filteredDerivations = applyDisambiguationFilters(childParseNode);
                    if (filteredDerivations.size() <= 1) {
                        listQueueTodo.addAll(0, Arrays.asList(filteredDerivations.get(0).parseForests()));
                        continue;
                    }
                }

                // Else, add child to processed list
                listQueueDone.add(childParseForest);
            }
            return listQueueDone;
        } else {
            return parseForests;
        }
    }

    protected IStrategoTerm createLexicalTerm(IProduction production, String inputString, int startOffset, int width) {
        if (production.isLayout() || production.isLiteral()) {
            return null;
        } else if (production.isLexical()) {
            String substring = inputString.substring(startOffset, startOffset + width);
            if (production.lhs() instanceof IMetaVarSymbol)
                return treeFactory.createMetaVar((IMetaVarSymbol) production.lhs(), substring);
            else
                return treeFactory.createStringTerminal(production.lhs(), substring);
        } else {
            throw new RuntimeException("invalid term type");
        }
    }

    protected IStrategoTerm createContextFreeTerm(IProduction production, List<IStrategoTerm> childASTs) {
        String constructor = production.constructor();

        if (constructor != null)
            return treeFactory.createNonTerminal(production.lhs(), constructor, childASTs);
        else if (production.isOptional())
            return treeFactory.createOptional(production.lhs(), childASTs);
        else if (production.isList())
            return treeFactory.createList(childASTs);
        else if (childASTs.size() == 1)
            return childASTs.get(0);
        else
            return treeFactory.createTuple(childASTs);
    }

    public static class SubTree2<Tree> {

        public final Tree tree;
        public final List<SubTree2<Tree>> children;
        public final IProduction production;
        public final int width;

        /**
         * True whenever the `tree` field of this node and its (only) child node are equal. Tokenizers should annotate
         * ASTs with the sort/cons of the production that is closest to the node. This means that injections should be
         * skipped when adding the ImploderAttachment. E.g. The program `x` with AST `Exp()` should be annotated with
         * `Exp.Exp` and not with `Start` in the following grammar:
         *
         * <code>
         * context-free syntax
         * Start = Stmt
         * Stmt = Exp
         * Exp.Exp = "x"
         * </code>
         */
        public final boolean isInjection;
        public final boolean isAmbiguous;
        public final boolean containsAmbiguity;
        public final boolean isCharacterTerminal;

        public SubTree2(Tree tree, List<SubTree2<Tree>> children, IProduction production, int width, boolean isInjection,
                        boolean isAmbiguous, boolean isCharacterTerminal) {
            this.tree = tree;
            this.children = children;
            this.production = production;
            this.width = width;
            this.isInjection = isInjection;
            this.isAmbiguous = isAmbiguous;
            this.containsAmbiguity = isAmbiguous || childrenContainAmbiguity(children);
            this.isCharacterTerminal = isCharacterTerminal;
        }

        /**
         * This constructor infers the width from the sum of widths of its children.
         */
        public SubTree2(Tree tree, List<SubTree2<Tree>> children, IProduction production, boolean isInjection) {
            this(tree, children, production, sumWidth(children), isInjection, false, false);
        }

        /**
         * This constructor corresponds to a terminal/lexical node without children.
         */
        public SubTree2(Tree tree, IProduction production, int width, boolean isCharacterTerminal) {
            this(tree, Collections.emptyList(), production, width, false, false, isCharacterTerminal);
        }

        private static <Tree> int sumWidth(List<SubTree2<Tree>> children) {
            int result = 0;
            for (SubTree2<Tree> child : children) {
                result += child.width;
            }
            return result;
        }

        private static <Tree> boolean childrenContainAmbiguity(List<SubTree2<Tree>> children) {
            for (SubTree2<Tree> child : children) {
                if (child.containsAmbiguity)
                    return true;
            }
            return false;
        }

    }

    // FROM AbstractTreeImploder
    protected List<List<IncrementalParseForest>> implodeAmbiguousLists(List<IncrementalDerivation> derivations) {
        List<List<IncrementalParseForest>> alternatives = new ArrayList<>();

        for (IncrementalDerivation derivation : derivations) {
            IncrementalParseForest[] children = derivation.parseForests();
            if (children.length == 0) {
                alternatives.add(Collections.emptyList());
            } else if (children.length == 1) {
                alternatives.add(Collections.singletonList(children[0]));
            } else {
                List<IncrementalParseForest> subTrees = Arrays.asList(children);

                IncrementalParseNode head = (IncrementalParseNode) children[0];

                if (head.production().isList() && head.getPreferredAvoidedDerivations().size() > 1) {
                    List<IncrementalParseForest> tail = subTrees.subList(1, subTrees.size());

                    List<List<IncrementalParseForest>> headExpansions =
                            implodeAmbiguousLists(head.getPreferredAvoidedDerivations());

                    for (List<IncrementalParseForest> headExpansion : headExpansions) {
                        List<IncrementalParseForest> headExpansionWithTail = new ArrayList<>(headExpansion);
                        headExpansionWithTail.addAll(tail);
                        alternatives.add(headExpansionWithTail);
                    }
                } else {
                    alternatives.add(subTrees);
                }
            }
        }

        return alternatives;
    }

    protected IncrementalParseNode implodeInjection(IncrementalParseNode parseNode) {
        for (IncrementalDerivation derivation : parseNode.getDerivations()) {
            if (derivation.parseForests().length == 1 && (derivation.parseForests()[0] instanceof IParseNode)) {
                IncrementalParseNode injectedParseNode = (IncrementalParseNode) derivation.parseForests()[0];

                // Meta variables are injected:
                // https://github.com/metaborg/strategoxt/blob/master/strategoxt/stratego-libraries/sglr/lib/stratego/asfix/implode/injection.str#L68-L69
                if (injectedParseNode.production().lhs() instanceof IMetaVarSymbol) {
                    return injectedParseNode;
                }
            }
        }

        return parseNode;
    }

    protected List<IncrementalDerivation> applyDisambiguationFilters(IncrementalParseNode parseNode) {
        if (!parseNode.isAmbiguous())
            return Collections.singletonList(parseNode.getFirstDerivation());

        return parseNode.getPreferredAvoidedDerivations();
    }
}
