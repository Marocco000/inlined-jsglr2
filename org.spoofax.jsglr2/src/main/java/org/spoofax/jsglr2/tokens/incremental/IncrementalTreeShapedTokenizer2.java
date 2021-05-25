package org.spoofax.jsglr2.tokens.incremental;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr2.JSGLR2Request;
import org.spoofax.jsglr2.imploder.ITokenizer;
import org.spoofax.jsglr2.imploder.TokenizeResult;
import org.spoofax.jsglr2.imploder.TreeImploder;
import org.spoofax.jsglr2.parser.Position;
import org.spoofax.jsglr2.tokens.treeshaped.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.spoofax.jsglr.client.imploder.IToken.Kind.TK_NO_TOKEN_KIND;
import static org.spoofax.jsglr2.tokens.treeshaped.TreeTokens.EMPTY_RANGE;

public class IncrementalTreeShapedTokenizer2
        implements ITokenizer<TreeImploder.SubTree<IStrategoTerm>, IncrementalTreeTokens> {
//{

//    TokenizeResult<IncrementalTreeTokens> tokenize(JSGLR2Request request, TreeImploder.SubTree<IStrategoTerm> tree) {
//        return tokenize(request, tree, null);
//    }

    public TokenizeResult<IncrementalTreeTokens> tokenize(JSGLR2Request input,
                                                          TreeImploder.SubTree<IStrategoTerm> tree, IncrementalTreeTokens previousResult) {

        IncrementalTreeTokens tokens =
                !input.isCacheable() || previousResult == null ? new IncrementalTreeTokens(input) : previousResult;
        tokens.setInput(input.input);

//        TokenTree tokenTree =
//                (input.isCacheable() ? this : regularTokenizer).tokenizeInternal(tokens, tree, Position.START_POSITION);
        TokenTree tokenTree = null;
        if (input.isCacheable()) {
//            tokenTree = this.tokenizeInternal(tokens, tree, Position.START_POSITION);
            IncrementalTreeTokens incrementalTreeTokens = tokens;
            if (incrementalTreeTokens.resultCache.containsKey(tree))
                tokenTree = incrementalTreeTokens.resultCache.get(tree);
            else {
                TokenTree result = tokenizeInternalFromSuper(incrementalTreeTokens, tree, Position.START_POSITION);

                incrementalTreeTokens.resultCache.put(tree, result);
                tokenTree = result;
            }
        } else
//            tokenTree = regularTokenizer.tokenizeInternal(tokens, tree, Position.START_POSITION);
            tokenTree = tokenizeInternalFromSuper(tokens, tree, Position.START_POSITION);

//      finalize(tree, tokens, tokenTree);
        // finalize
        TokenTree res = new TokenTree(null,
                Arrays.asList(new TokenTree(null, tokens.startToken), tokenTree, new TokenTree(null, tokens.endToken)),
                tokens.startToken, tokens.endToken, tokenTree.positionRange);
        for (TokenTree child : res.children) {
            child.parent = res;
        }
        tokens.startToken.setAstNode(tree.tree);
        tokens.endToken.setAstNode(tree.tree);

        tokens.startToken.tree = res.children.get(0);
        tokens.endToken.tree = res.children.get(2);
        tokens.tree = res;

        return new TokenizeResult<>(tokens);
    }

//    public TokenizeResult<TreeTokens> regularTokenizerTokenize(JSGLR2Request input, TreeImploder.SubTree<IStrategoTerm> tree,
//                                               TreeTokens previousResult) {
//
//        TreeTokens tokens = new TreeTokens(input);
//
//        TokenTree tokenTree = tokenizeInternal(tokens, tree, Position.START_POSITION);
//        finalize(tree, tokens, tokenTree);
//
//        return new TokenizeResult<>(tokens);
//    }

//    public TokenTree tokenizeInternal(TreeTokens tokens, TreeImploder.SubTree<IStrategoTerm> tree,
//                                      Position pivotPosition) {
//        IncrementalTreeTokens incrementalTreeTokens = (IncrementalTreeTokens) tokens;
//        if (incrementalTreeTokens.resultCache.containsKey(tree))
//            return incrementalTreeTokens.resultCache.get(tree);
//
//        TokenTree result = tokenizeInternalFromSuper(incrementalTreeTokens, tree, pivotPosition);
//
//        incrementalTreeTokens.resultCache.put(tree, result);
//        return result;
//    }

    // from AbstractTreeShapedTokenizer
    public TokenTree tokenizeInternalFromSuper(IncrementalTreeTokens tokens, TreeImploder.SubTree<IStrategoTerm> tree,
                                               Position pivotPosition) {
        if (tree.production != null && !tree.production.isContextFree() || tree.isCharacterTerminal) {
            if (tree.width > 0) {
                Position endPosition = pivotPosition.step(tokens.getInput(), tree.width);
//                Position positionRange = positionRange(pivotPosition, endPosition);
                Position positionRange = new Position(endPosition.offset - pivotPosition.offset, endPosition.line - pivotPosition.line + 1,
                        pivotPosition.line == endPosition.line ? endPosition.column - pivotPosition.column : endPosition.column);

                return new TokenTree(tree,
                        new TreeToken(tokens, positionRange, IToken.getTokenKind(tree.production), tree.tree));
            } else
                return new TokenTree(tree,
                        tree.tree == null ? null : new TreeToken(tokens, EMPTY_RANGE, TK_NO_TOKEN_KIND, tree.tree));
        } else {
            List<TokenTree> children = new ArrayList<>(tree.children.size());
            TreeToken leftToken = null;
            TreeToken rightToken = null;
            List<TreeImploder.SubTree<IStrategoTerm>> subTrees = tree.children;
            for (TreeImploder.SubTree<IStrategoTerm> imploderSubTree : subTrees) {
                TokenTree subTree = tokenizeInternalFromSuper(tokens, imploderSubTree, pivotPosition);// TODO: from super ?
                children.add(subTree);

                // If tree ast == null, that means it's layout or literal lexical;
                // that means it needs to be bound to the current tree
                if (subTree.tree == null) {
                    if (subTree.token != null)
                        subTree.token.setAstNode(tree.tree);
                }

                // The left-most token of this tree is the first non-null leftToken of a subTree
                if (leftToken == null)
                    leftToken = subTree.leftToken;

                // The right-most token of this tree is the last non-null rightToken of a subTree
                if (subTree.rightToken != null)
                    rightToken = subTree.rightToken;

                // In the case when we're dealing with an ambiguous tree node, position is not advanced
                if (!tree.isAmbiguous) {
                    pivotPosition = TreeTokens.addPosition(pivotPosition, subTree.positionRange);
                }
            }

            // If there is no token, this means that this AST has no characters in the input.
            // In this case, create an empty token to associate with this AST node.
            if (leftToken == null) {
                assert rightToken == null;
                return new TokenTree(tree, new TreeToken(tokens, EMPTY_RANGE, TK_NO_TOKEN_KIND, tree.tree));
            }

            Position positionRange = tree.isAmbiguous ? children.get(0).positionRange
                    : children.stream().map(child -> child.positionRange).reduce(EMPTY_RANGE, TreeTokens::addPosition);
            TokenTree res = new TokenTree(tree, children, leftToken, rightToken, positionRange);
            for (TokenTree child : children) {
                child.parent = res;
            }
            return res;
        }
    }

//    protected final void finalize(TreeImploder.SubTree<IStrategoTerm> tree, TreeTokens tokens, TokenTree tokenTree) {
//        TokenTree res = new TokenTree(null,
//                Arrays.asList(new TokenTree(null, tokens.startToken), tokenTree, new TokenTree(null, tokens.endToken)),
//                tokens.startToken, tokens.endToken, tokenTree.positionRange);
//        for(TokenTree child : res.children) {
//            child.parent = res;
//        }
//        tokens.startToken.setAstNode(tree.tree);
//        tokens.endToken.setAstNode(tree.tree);
//
//        tokens.startToken.tree = res.children.get(0);
//        tokens.endToken.tree = res.children.get(2);
//        tokens.tree = res;
//    }

//    private static Position positionRange(Position beginPosition, Position endPosition) {
//        return new Position(endPosition.offset - beginPosition.offset, endPosition.line - beginPosition.line + 1,
//                beginPosition.line == endPosition.line ? endPosition.column - beginPosition.column : endPosition.column);
//    }

}
