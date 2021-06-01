package org.spoofax.jsglr2.stack.hybrid;

import org.metaborg.parsetable.states.IState;
import org.spoofax.jsglr2.incremental.IncrementalParseState;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForestManager2;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.StackLink2;
import org.spoofax.jsglr2.stack.paths.*;

import java.util.ArrayList;
import java.util.List;

public class HybridStackManager2 {
    protected final ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState<HybridStackNode2>> observing;


    public HybridStackManager2(
            ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState<HybridStackNode2>> observing) {
        this.observing = observing;
    }

//    protected HybridStackNode<IncrementalParseForest> createStackNode(IState state, boolean isRoot) {
//        return new HybridStackNode<>(state);
//    }

//    public HybridStackNode<IncrementalParseForest> createInitialStackNode(IState state) {
//        HybridStackNode<IncrementalParseForest> newStackNode = new HybridStackNode<>(state);
//
//        observing.notify(observer -> observer.createStackNode(newStackNode));
//
//        return newStackNode;
//    }

    /*
    Create stack node. Could also be a  initial stack node.
     */
    public HybridStackNode2 createStackNode(IState state) {
        HybridStackNode2 newStackNode = new HybridStackNode2(state);

        observing.notify(observer -> observer.createStackNode(newStackNode));

        return newStackNode;
    }

    public StackLink2 createStackLink(IncrementalParseState<HybridStackNode2> parseState,
                                      HybridStackNode2 from,
                                      HybridStackNode2 to,
                                      IncrementalParseForest parseForest) {
        // add link
        StackLink2 link = new StackLink2( from, to, parseForest);

        from.addLink(link);

        //StackLink<IncrementalParseForest, HybridStackNode2> link = from.addLink(to, parseForest);

        observing.notify(observer -> observer.createStackLink(link));

        return link;
    }

    protected Iterable<StackLink2> stackLinksOut(HybridStackNode2 stack) {
        return stack.getLinks();
    }


    public void rejectStackLink(StackLink2 link) {
        link.reject();

        observing.notify(observer -> observer.rejectStackLink(link));
    }

    public StackLink2 findDirectLink(HybridStackNode2 from, HybridStackNode2 to) {
        for (StackLink2 link : stackLinksOut(from)) {
            if (link.to == to)
                return link;
        }

        return null;
    }

    public List<StackPath2> findAllPathsOfLength(HybridStackNode2 stack, int length) {
        List<StackPath2> paths = new ArrayList<>();

        StackPath2 pathsOrigin = new EmptyStackPath2(stack);

        findAllPathsOfLength(pathsOrigin, length, paths);

        return paths;
    }


    private void findAllPathsOfLength(StackPath2 path, int length,
                                      List<StackPath2> paths) {
        if (length == 0)
            paths.add(path);
        else {
            HybridStackNode2 lastStackNode = path.head();

            for (StackLink2 linkOut : stackLinksOut(lastStackNode)) {
                if (!linkOut.isRejected()) {
                    StackPath2 extendedPath = new NonEmptyStackPath2(linkOut, path);

                    findAllPathsOfLength(extendedPath, length - 1, paths);
                }
            }
        }
    }

    public IncrementalParseForest[] getParseForests( IncrementalParseForestManager2 parseForestManager,
//            ParseForestManager<IncrementalParseForest, ?, ?, ?, ?> parseForestManager,
                                                    StackPath2 pathBegin) {
        IncrementalParseForest[] res = parseForestManager.parseForestsArray(pathBegin.length);

        if (res != null) {
            StackPath2 path = pathBegin;

            for (int i = 0; i < pathBegin.length; i++) {
                NonEmptyStackPath2 nonEmptyPath =
                        (NonEmptyStackPath2) path;

                res[i] = nonEmptyPath.link.parseForest;

                path = nonEmptyPath.tail;
            }

            return res;
        }
        return null;
    }

}