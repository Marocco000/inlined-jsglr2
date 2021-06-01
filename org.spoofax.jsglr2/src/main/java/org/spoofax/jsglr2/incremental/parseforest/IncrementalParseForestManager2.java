package org.spoofax.jsglr2.incremental.parseforest;

import static org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode.NO_STATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.metaborg.parsetable.productions.IProduction;
import org.metaborg.parsetable.productions.ProductionType;
import org.metaborg.parsetable.states.IState;
import org.spoofax.jsglr2.JSGLR2Request;
import org.spoofax.jsglr2.incremental.IncrementalParseState;
import org.spoofax.jsglr2.messages.SourceRegion;
import org.spoofax.jsglr2.parseforest.*;
import org.spoofax.jsglr2.parser.Position;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.hybrid.HybridStackNode;
import org.spoofax.jsglr2.stack.hybrid.HybridStackNode2;

public class IncrementalParseForestManager2 {
    protected final ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState<HybridStackNode2>> observing;

    public IncrementalParseForestManager2(
            ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState<HybridStackNode2>> observing) {
        this.observing = observing;
    }

    public IncrementalParseNode createParseNode(IncrementalParseState<HybridStackNode2> parseState, IStackNode stack,
                                                IProduction production, IncrementalDerivation firstDerivation) {

        IState state = parseState.newParseNodesAreReusable() ? stack.state() : NO_STATE;
        IncrementalParseNode parseNode = new IncrementalParseNode(production, firstDerivation, state);

        observing.notify(observer -> observer.createParseNode(parseNode, production));
        observing.notify(observer -> observer.addDerivation(parseNode, firstDerivation));

        return parseNode;
    }

    public IncrementalParseNode createChangedParseNode(IncrementalParseForest... children) {
        IncrementalParseNode parseNode = new IncrementalParseNode(children);

        observing.notify(observer -> observer.createDerivation(parseNode.getFirstDerivation(), null, children));
        observing.notify(observer -> observer.createParseNode(parseNode, null));
        observing.notify(observer -> observer.addDerivation(parseNode, parseNode.getFirstDerivation()));

        return parseNode;
    }

    public IncrementalDerivation createDerivation(IncrementalParseState<HybridStackNode2> parseState, IStackNode stack,
                                                  IProduction production, ProductionType productionType, IncrementalParseForest[] parseForests) {

        IncrementalDerivation derivation = new IncrementalDerivation(production, productionType, parseForests);

        observing.notify(observer -> observer.createDerivation(derivation, production, derivation.parseForests));

        return derivation;
    }

    public void addDerivation(IncrementalParseState<HybridStackNode2> parseState, IncrementalParseNode parseNode,
                              IncrementalDerivation derivation) {

        observing.notify(observer -> observer.addDerivation(parseNode, derivation));

        parseNode.addDerivation(derivation);

    }

    public IncrementalSkippedNode createSkippedNode(IncrementalParseState<HybridStackNode2> parseState, IProduction production,
                                                    IncrementalParseForest[] parseForests) {
        return new IncrementalSkippedNode(production, parseForests);
    }

    public IncrementalParseForest createCharacterNode(int currentChar) {
        IncrementalCharacterNode characterNode = new IncrementalCharacterNode(currentChar);

        observing.notify(observer -> observer.createCharacterNode(characterNode, characterNode.character));

        return characterNode;
    }

    public IncrementalParseForest[] parseForestsArray(int length) {
        return new IncrementalParseForest[length];
    }

    public IncrementalParseForest filterStartSymbol(IncrementalParseForest parseForest, String startSymbol, IncrementalParseState<HybridStackNode2> parseState) {
        IncrementalParseNode topNode = (IncrementalParseNode) parseForest;
        List<IncrementalDerivation> derivationsWithStartSymbol = new ArrayList<>();

        for (IncrementalDerivation derivation : topNode.getDerivations()) {
            String derivationStartSymbol = derivation.production().startSymbolSort();

            if (derivationStartSymbol != null && derivationStartSymbol.equals(startSymbol))
                derivationsWithStartSymbol.add(derivation);
        }

        if (derivationsWithStartSymbol.isEmpty())
            return null;
        else {
            IncrementalParseNode topParseNode =
                    new IncrementalParseNode(topNode.production(), derivationsWithStartSymbol.get(0), NO_STATE);

            for (int i = 1; i < derivationsWithStartSymbol.size(); i++)
                topParseNode.addDerivation(derivationsWithStartSymbol.get(i));

            return topParseNode;
        }
    }

    // from parseNodeVisited
    public void visit(JSGLR2Request request, IncrementalParseForest root,
                      ParseNodeVisitor<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode> visitor) {
        Stack<Position> positionStack = new Stack<>(); // Start positions of parse nodes
        Stack<Object> inputStack = new Stack<>(); // Pending parse nodes and derivations
        Stack<IncrementalVisit> outputStack = new Stack<>(); // Parse node and derivations with remaining children

        Position pivotPosition = Position.START_POSITION;
        positionStack.push(Position.START_POSITION);
        inputStack.push(root);
        outputStack.push(new IncrementalVisit());

        while (!inputStack.isEmpty() || !outputStack.isEmpty()) {
            if (!outputStack.isEmpty() && outputStack.peek().done()) { // Finish derivation
                outputStack.pop();

                if (outputStack.isEmpty())
                    break;

                outputStack.peek().remainingChildren--;

                if (!outputStack.isEmpty() && outputStack.peek().done()) { // Visit parse node
                    IncrementalParseNode parseNode = outputStack.pop().parseNode;

                    visitor.postVisit(parseNode, positionStack.pop(), pivotPosition);

                    outputStack.peek().remainingChildren--;
                }
            } else if (inputStack.peek() instanceof IDerivation) { // Consume derivation
                IncrementalDerivation derivation = (IncrementalDerivation) inputStack.pop();

                outputStack.push(new IncrementalVisit(derivation));

                IncrementalParseForest[] children = derivation.parseForests();

                for (int i = children.length - 1; i >= 0; i--)
                    inputStack.push(children[i]);

                pivotPosition = positionStack.peek();
            } else if (inputStack.peek() instanceof ICharacterNode) { // Consume character node
                pivotPosition = pivotPosition.step(request.input, ((ICharacterNode) inputStack.pop()).width());

                outputStack.peek().remainingChildren--;
            } else if (inputStack.peek() instanceof IParseNode) { // Consume (skipped) parse node
                IncrementalParseNode parseNode = (IncrementalParseNode) inputStack.pop();
                positionStack.push(pivotPosition);

                boolean visitChildren = visitor.preVisit(parseNode, pivotPosition);

                if (visitChildren && parseNode.hasDerivations()) { // Parse node with derivation(s)
                    int derivations = 0;

                    for (IncrementalDerivation derivation : parseNode.getDerivations()) {
                        inputStack.push(derivation);
                        derivations++;

                        if (derivations >= 1 && !visitor.visitAmbiguousDerivations())
                            break;
                    }

                    outputStack.push(new IncrementalVisit(derivations, parseNode));
                } else { // Skipped parse node (without derivations)
                    pivotPosition = pivotPosition.step(request.input, parseNode.width());

                    visitor.postVisit(parseNode, positionStack.pop(), pivotPosition);

                    outputStack.peek().remainingChildren--;
                }
            }
        }
    }

    class IncrementalVisit {
        int remainingChildren;
        IncrementalParseNode parseNode;

        IncrementalVisit() {
            this.remainingChildren = 1;
            this.parseNode = null;
        }

        IncrementalVisit(IDerivation<?> derivation) {
            this.remainingChildren = derivation.parseForests().length;
            this.parseNode = null;
        }

        IncrementalVisit(int remainingChildren, IncrementalParseNode parseNode) {
            this.remainingChildren = remainingChildren;
            this.parseNode = parseNode;
        }

        boolean done() {
            return remainingChildren == 0;
        }
    }

    public static SourceRegion visitRegion(String inputString, Position startPosition, Position endPosition) {
        if(endPosition.offset > startPosition.offset)
            endPosition = endPosition.previous(inputString);

        return new SourceRegion(startPosition, endPosition);
    }

}
