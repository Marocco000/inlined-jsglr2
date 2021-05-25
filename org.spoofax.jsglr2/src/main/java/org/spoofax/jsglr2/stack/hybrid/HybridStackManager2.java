package org.spoofax.jsglr2.stack.hybrid;

import org.metaborg.parsetable.states.IState;
import org.spoofax.jsglr2.incremental.IncrementalParseState;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForestManager2;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.StackLink;
import org.spoofax.jsglr2.stack.paths.EmptyStackPath;
import org.spoofax.jsglr2.stack.paths.NonEmptyStackPath;
import org.spoofax.jsglr2.stack.paths.StackPath;

import java.util.ArrayList;
import java.util.List;

public class HybridStackManager2 {
    protected final ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>> observing;


    public HybridStackManager2(
            ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, IncrementalParseState<HybridStackNode<IncrementalParseForest>>> observing) {
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
    public HybridStackNode<IncrementalParseForest> createStackNode(IState state) {
        HybridStackNode<IncrementalParseForest> newStackNode = new HybridStackNode<>(state);

        observing.notify(observer -> observer.createStackNode(newStackNode));

        return newStackNode;
    }

    public StackLink<IncrementalParseForest, HybridStackNode<IncrementalParseForest>> createStackLink(IncrementalParseState<HybridStackNode<IncrementalParseForest>> parseState,
                                                                                                      HybridStackNode<IncrementalParseForest> from,
                                                                                                      HybridStackNode<IncrementalParseForest> to,
                                                                                                      IncrementalParseForest parseForest) {
        StackLink<IncrementalParseForest, HybridStackNode<IncrementalParseForest>> link = from.addLink(to, parseForest);

        observing.notify(observer -> observer.createStackLink(link));

        return link;
    }

    protected Iterable<StackLink<IncrementalParseForest, HybridStackNode<IncrementalParseForest>>> stackLinksOut(HybridStackNode<IncrementalParseForest> stack) {
        return stack.getLinks();
    }


    public void rejectStackLink(StackLink<IncrementalParseForest, HybridStackNode<IncrementalParseForest>> link) {
        link.reject();

        observing.notify(observer -> observer.rejectStackLink(link));
    }

    public StackLink<IncrementalParseForest, HybridStackNode<IncrementalParseForest>> findDirectLink(HybridStackNode<IncrementalParseForest> from, HybridStackNode<IncrementalParseForest> to) {
        for (StackLink<IncrementalParseForest, HybridStackNode<IncrementalParseForest>> link : stackLinksOut(from)) {
            if (link.to == to)
                return link;
        }

        return null;
    }

    public List<StackPath<IncrementalParseForest, HybridStackNode<IncrementalParseForest>>> findAllPathsOfLength(HybridStackNode<IncrementalParseForest> stack, int length) {
        List<StackPath<IncrementalParseForest, HybridStackNode<IncrementalParseForest>>> paths = new ArrayList<>();

        StackPath<IncrementalParseForest, HybridStackNode<IncrementalParseForest>> pathsOrigin = new EmptyStackPath<>(stack);

        findAllPathsOfLength(pathsOrigin, length, paths);

        return paths;
    }


    private void findAllPathsOfLength(StackPath<IncrementalParseForest, HybridStackNode<IncrementalParseForest>> path, int length,
                                      List<StackPath<IncrementalParseForest, HybridStackNode<IncrementalParseForest>>> paths) {
        if (length == 0)
            paths.add(path);
        else {
            HybridStackNode<IncrementalParseForest> lastStackNode = path.head();

            for (StackLink<IncrementalParseForest, HybridStackNode<IncrementalParseForest>> linkOut : stackLinksOut(lastStackNode)) {
                if (!linkOut.isRejected()) {
                    StackPath<IncrementalParseForest, HybridStackNode<IncrementalParseForest>> extendedPath = new NonEmptyStackPath<>(linkOut, path);

                    findAllPathsOfLength(extendedPath, length - 1, paths);
                }
            }
        }
    }

    public IncrementalParseForest[] getParseForests( IncrementalParseForestManager2 parseForestManager,
//            ParseForestManager<IncrementalParseForest, ?, ?, ?, ?> parseForestManager,
                                                    StackPath<IncrementalParseForest, HybridStackNode<IncrementalParseForest>> pathBegin) {
        IncrementalParseForest[] res = parseForestManager.parseForestsArray(pathBegin.length);

        if (res != null) {
            StackPath<IncrementalParseForest, HybridStackNode<IncrementalParseForest>> path = pathBegin;

            for (int i = 0; i < pathBegin.length; i++) {
                NonEmptyStackPath<IncrementalParseForest, HybridStackNode<IncrementalParseForest>> nonEmptyPath =
                        (NonEmptyStackPath<IncrementalParseForest, HybridStackNode<IncrementalParseForest>>) path;

                res[i] = nonEmptyPath.link.parseForest;

                path = nonEmptyPath.tail;
            }

            return res;
        }
        return null;
    }

}