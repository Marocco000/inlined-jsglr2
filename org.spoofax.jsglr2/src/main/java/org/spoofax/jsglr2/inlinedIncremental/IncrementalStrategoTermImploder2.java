package org.spoofax.jsglr2.inlinedIncremental;

import org.metaborg.parsetable.productions.IProduction;
import org.metaborg.parsetable.symbols.IMetaVarSymbol;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr2.JSGLR2Request;
import org.spoofax.jsglr2.imploder.ImplodeResult;
import org.spoofax.jsglr2.imploder.TreeImploder;
import org.spoofax.jsglr2.imploder.incremental.IncrementalTreeImploder;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.inlinedIncremental.IncrementalImplodeInput2;
import org.spoofax.jsglr2.inlinedIncremental.StrategoTermTreeFactory2;
import org.spoofax.jsglr2.inlinedIncremental.TreeImploder2;
import org.spoofax.jsglr2.parseforest.ICharacterNode;
import org.spoofax.jsglr2.parseforest.IParseNode;

import java.util.*;

public class IncrementalStrategoTermImploder2 {


//        extends
//        IncrementalTreeImploder<IncrementalParseForest, IncrementalParseNode, IncrementalDerivation, IStrategoTerm, IncrementalImplodeInput<IncrementalParseNode, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm>, IStrategoTerm>> {
//        extends
//        TreeImploder<IncrementalParseForest, IncrementalParseNode, IncrementalDerivation,
//                IncrementalStrategoTermImploder2.ResultCache2, IStrategoTerm,
//                IncrementalImplodeInput<IncrementalParseNode, IncrementalStrategoTermImploder2.ResultCache2, IStrategoTerm>> {
//        extends
//        AbstractTreeImploder<IncrementalParseForest, IncrementalParseNode, IncrementalDerivation,
//                TreeImploder.SubTree<IStrategoTerm>,  IncrementalStrategoTermImploder2.ResultCache2,
//                IStrategoTerm, ImplodeResult<TreeImploder.SubTree<IStrategoTerm>, IncrementalStrategoTermImploder2.ResultCache2, IStrategoTerm>> {
//        implements IImploder<IncrementalParseForest, TreeImploder.SubTree<IStrategoTerm>, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm>, IStrategoTerm,
//        ImplodeResult<TreeImploder.SubTree<IStrategoTerm>, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm>, IStrategoTerm>> {


    public final TreeImploder2 regularImplode;

    protected final StrategoTermTreeFactory2 treeFactory;

    public IncrementalStrategoTermImploder2() {
//        super(inputString -> new IncrementalImplodeInput<>(inputString, null), IncrementalImplodeInput::new,
//                new StrategoTermTreeFactory());
//        super(inputString -> new IncrementalImplodeInput<>(inputString, null), new StrategoTermTreeFactory());
        this.treeFactory = new StrategoTermTreeFactory2();
        this.regularImplode = new TreeImploder2(treeFactory);
    }

    ImplodeResult<TreeImploder.SubTree<IStrategoTerm>, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm>, IStrategoTerm> implode(String input, String fileName, IncrementalParseForest parseForest, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm> resultCache) {
        return implode(new JSGLR2Request(input, fileName), parseForest, resultCache);
    }

    ImplodeResult<TreeImploder.SubTree<IStrategoTerm>, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm>, IStrategoTerm> implode(JSGLR2Request request, IncrementalParseForest parseForest) {
        return implode(request, parseForest, null);
    }

    ImplodeResult<TreeImploder.SubTree<IStrategoTerm>, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm>, IStrategoTerm> implode(String input, String fileName, IncrementalParseForest parseForest) {
        return implode(new JSGLR2Request(input, fileName), parseForest);
    }


    public ImplodeResult<TreeImploder.SubTree<IStrategoTerm>, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm>, IStrategoTerm>
    implode(JSGLR2Request request, IncrementalParseForest parseForest, IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm> previousResult) {

        if (!request.isCacheable()) {
            ImplodeResult<TreeImploder.SubTree<IStrategoTerm>, Void, IStrategoTerm> result = regularImplode.implode(request, parseForest);
            return new ImplodeResult<>(result.intermediateResult(), null, result.ast(), result.isAmbiguous());
        }

        IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm> resultCache = previousResult == null ? new IncrementalTreeImploder.ResultCache<IncrementalParseForest, IStrategoTerm>() : previousResult;

        TreeImploder.SubTree<IStrategoTerm> result =
                implodeParseNode(new IncrementalImplodeInput2(request.input, resultCache), parseForest, 0);

        return new ImplodeResult<>(result, resultCache, result.tree, result.containsAmbiguity);
    }

    protected TreeImploder.SubTree<IStrategoTerm> implodeParseNode(IncrementalImplodeInput2 input, IncrementalParseForest parseNode, int startOffset) {
        if (input.resultCache.cache.containsKey(parseNode))
            return input.resultCache.cache.get(parseNode);

        TreeImploder.SubTree<IStrategoTerm> result = superImplodeParseNode(input, parseNode, startOffset);
        input.resultCache.cache.put(parseNode, result);
        return result;
    }

    // from TreeImploder
    protected TreeImploder.SubTree<IStrategoTerm> superImplodeParseNode(IncrementalImplodeInput2 input, IncrementalParseForest parseForest, int startOffset) {
        if (parseForest instanceof ICharacterNode) {
            return new TreeImploder.SubTree<>(treeFactory.createCharacterTerminal(((ICharacterNode) parseForest).character()), null,
                    parseForest.width(), true);
        }

        // implode injection
//        @SuppressWarnings("unchecked") IncrementalParseNode parseNode = implodeInjection((IncrementalParseNode) parseForest);
        IncrementalParseNode parseNode = (IncrementalParseNode) parseForest;
        for (IncrementalDerivation derivation : parseNode.getDerivations()) {
            if (derivation.parseForests().length == 1 && (derivation.parseForests()[0] instanceof IParseNode)) {
                IncrementalParseNode injectedParseNode = (IncrementalParseNode) derivation.parseForests()[0];

                // Meta variables are injected:
                // https://github.com/metaborg/strategoxt/blob/master/strategoxt/stratego-libraries/sglr/lib/stratego/asfix/implode/injection.str#L68-L69
                if (injectedParseNode.production().lhs() instanceof IMetaVarSymbol) {
                    parseNode = injectedParseNode;
                }
            }
        }


        IProduction production = parseNode.production();

        if (production.isContextFree() && !production.isSkippableInParseForest()) {
            // applyDisambiguationFilters
            List<IncrementalDerivation> filteredDerivations;
            if (!parseNode.isAmbiguous()) {
                filteredDerivations = Collections.singletonList(parseNode.getFirstDerivation());
            } else {
                filteredDerivations = parseNode.getPreferredAvoidedDerivations();
            }


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

            // create lexical term
            IStrategoTerm lexicalTerm;

            if (production.isLayout() || production.isLiteral()) {
                lexicalTerm = null;
            } else if (production.isLexical()) {
                String substring = input.inputString.substring(startOffset, startOffset + width);
                if (production.lhs() instanceof IMetaVarSymbol)
                    lexicalTerm = treeFactory.createMetaVar((IMetaVarSymbol) production.lhs(), substring);
                else
                    lexicalTerm = treeFactory.createStringTerminal(production.lhs(), substring);
            } else {
                throw new RuntimeException("invalid term type");
            }

            return new TreeImploder.SubTree<>(lexicalTerm, production,
                    width, false);

        }
    }

    // From TreeImploder

    protected TreeImploder.SubTree<IStrategoTerm> implodeDerivation(IncrementalImplodeInput2 input, IncrementalDerivation derivation, int startOffset) {
        IProduction production = derivation.production();

        if (!production.isContextFree())
            throw new RuntimeException("non context free imploding not supported");

//        return implodeDerivationChildren(input, production, getChildParseForests(derivation), startOffset);
        return implodeDerivationChildren(input, production, getChildParseForests(derivation.production(), Arrays.asList(derivation.parseForests())), startOffset);
    }

    protected TreeImploder.SubTree<IStrategoTerm> implodeDerivationChildren(IncrementalImplodeInput2 input, IProduction production,
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

        // create context free term
        IStrategoTerm contextFreeTerm;// = createContextFreeTerm(production, childASTs);
        String constructor = production.constructor();
        if (constructor != null)
            contextFreeTerm = treeFactory.createNonTerminal(production.lhs(), constructor, childASTs);
        else if (production.isOptional())
            contextFreeTerm = treeFactory.createOptional(production.lhs(), childASTs);
        else if (production.isList())
            contextFreeTerm = treeFactory.createList(childASTs);
        else if (childASTs.size() == 1)
            contextFreeTerm = childASTs.get(0);
        else
            contextFreeTerm = treeFactory.createTuple(childASTs);


        return new TreeImploder.SubTree<>(contextFreeTerm, subTrees, production,
                childASTs.size() == 1 && contextFreeTerm == childASTs.get(0));
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
                    // applyDisambiguationFilters
                    List<IncrementalDerivation> filteredDerivations;
                    if (!childParseNode.isAmbiguous()) {
                        filteredDerivations = Collections.singletonList(childParseNode.getFirstDerivation());
                    } else {
                        filteredDerivations = childParseNode.getPreferredAvoidedDerivations();
                    }


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

    // from AbstractTreeImploder
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

}