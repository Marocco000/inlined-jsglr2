package org.spoofax.jsglr2.inlinedIncremental;

import java.util.List;

import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.parser.observing.IParserObserver;

public class IncrementalParseForestManager2 {
//    protected final ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observing;
public final List<IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>> observers;

    public IncrementalParseForestManager2(
//            ParserObserving<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observing
            List<IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>> observers) {
//        this.observing = observing;
        this.observers = observers;
    }

//    public IncrementalParseNode createParseNode(IncrementalParseState2 parseState, IStackNode stack,
//                                                IProduction production, IncrementalDerivation firstDerivation) {
//
//        // create parse node
//        IState state = parseState.newParseNodesAreReusable() ? stack.state() : NO_STATE;
//        IncrementalParseNode parseNode = new IncrementalParseNode(production, firstDerivation, state);
//
//        // notify parse node creation
//        if(!observers.isEmpty())
//            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers) {
//                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createParseNode(parseNode, production)).notify(observer1);
//                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.addDerivation(parseNode, firstDerivation)).notify(observer1);
//            }
//
//        return parseNode;
//    }

//    public IncrementalParseNode createChangedParseNode(IncrementalParseForest... children) {
//        // create changed parse node
//        IncrementalParseNode parseNode = new IncrementalParseNode(children);
//
//        // notify observers on new changed parse node creation
//        if(!observers.isEmpty()) {
//            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer3 : observers) {
//                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createDerivation(parseNode.getFirstDerivation(), null, children)).notify(observer3);
//            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer2 : observers)
//                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createParseNode(parseNode, null)).notify(observer2);
//            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.addDerivation(parseNode, parseNode.getFirstDerivation())).notify(observer1);
//            }
//        }
//
//        return parseNode;
//    }

//    public IncrementalDerivation createDerivation(IncrementalParseState2 parseState, IStackNode stack,
//                                                  IProduction production, ProductionType productionType, IncrementalParseForest[] parseForests) {
//
//        // create derivation (from parseForestManager)
//        IncrementalDerivation derivation = new IncrementalDerivation(production, productionType, parseForests);
//
//        // notify observers on created derivation
//        if(!observers.isEmpty()) {
//            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createDerivation(derivation, production, derivation.parseForests)).notify(observer1);
//        }
//
//        return derivation;
//    }

//    public void addDerivation(IncrementalParseState2 parseState, IncrementalParseNode parseNode,
//                              IncrementalDerivation derivation) {
//
//        // notify observers on added derivation
//        if(!observers.isEmpty()) {
//            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.addDerivation(parseNode, derivation)).notify(observer1);
//        }
//
//        // add derivation
//        parseNode.addDerivation(derivation);
//
//    }

//    public IncrementalSkippedNode createSkippedNode(IncrementalParseState2 parseState, IProduction production,
//                                                    IncrementalParseForest[] parseForests) {
//        // create skipped node
//        return new IncrementalSkippedNode(production, parseForests);
//    }

//    public IncrementalParseForest createCharacterNode(int currentChar) {
//        // create character node
//        IncrementalCharacterNode characterNode = new IncrementalCharacterNode(currentChar);
//
//        // notify observers on character node creation
//        if(!observers.isEmpty()) {
//            for (IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2> observer1 : observers)
//                ((IParserNotification<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode2, IncrementalParseState2>) observer -> observer.createCharacterNode(characterNode, characterNode.character)).notify(observer1);
//        }
//
//        return characterNode;
//    }

//    public IncrementalParseForest[] parseForestsArray(int length) {
//        // parse forest array
//        return new IncrementalParseForest[length];
//    }

//    public IncrementalParseForest filterStartSymbol(IncrementalParseForest parseForest, String startSymbol, IncrementalParseState2 parseState) {
//        // Filter start symbol
//        IncrementalParseNode topNode = (IncrementalParseNode) parseForest;
//        List<IncrementalDerivation> derivationsWithStartSymbol = new ArrayList<>();
//
//        for (IncrementalDerivation derivation : topNode.getDerivations()) {
//            String derivationStartSymbol = derivation.production().startSymbolSort();
//
//            if (derivationStartSymbol != null && derivationStartSymbol.equals(startSymbol))
//                derivationsWithStartSymbol.add(derivation);
//        }
//
//        if (derivationsWithStartSymbol.isEmpty())
//            return null;
//        else {
//            IncrementalParseNode topParseNode =
//                    new IncrementalParseNode(topNode.production(), derivationsWithStartSymbol.get(0), NO_STATE);
//
//            for (int i = 1; i < derivationsWithStartSymbol.size(); i++)
//                topParseNode.addDerivation(derivationsWithStartSymbol.get(i));
//
//            return topParseNode;
//        }
//    }

//    // from parseNodeVisited
//    public void visit(JSGLR2Request request, IncrementalParseForest root,
//                      ParseNodeVisitor<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode> visitor) {
//        Stack<Position> positionStack = new Stack<>(); // Start positions of parse nodes
//        Stack<Object> inputStack = new Stack<>(); // Pending parse nodes and derivations
//        Stack<IncrementalVisit> outputStack = new Stack<>(); // Parse node and derivations with remaining children
//
//        Position pivotPosition = Position.START_POSITION;
//        positionStack.push(Position.START_POSITION);
//        inputStack.push(root);
//        outputStack.push(new IncrementalVisit());
//
//        while (!inputStack.isEmpty() || !outputStack.isEmpty()) {
//            if (!outputStack.isEmpty() && outputStack.peek().done()) { // Finish derivation
//                outputStack.pop();
//
//                if (outputStack.isEmpty())
//                    break;
//
//                outputStack.peek().remainingChildren--;
//
//                if (!outputStack.isEmpty() && outputStack.peek().done()) { // Visit parse node
//                    IncrementalParseNode parseNode = outputStack.pop().parseNode;
//
//                    visitor.postVisit(parseNode, positionStack.pop(), pivotPosition);
//
//                    outputStack.peek().remainingChildren--;
//                }
//            } else if (inputStack.peek() instanceof IDerivation) { // Consume derivation
//                IncrementalDerivation derivation = (IncrementalDerivation) inputStack.pop();
//
//                outputStack.push(new IncrementalVisit(derivation));
//
//                IncrementalParseForest[] children = derivation.parseForests();
//
//                for (int i = children.length - 1; i >= 0; i--)
//                    inputStack.push(children[i]);
//
//                pivotPosition = positionStack.peek();
//            } else if (inputStack.peek() instanceof ICharacterNode) { // Consume character node
//                pivotPosition = pivotPosition.step(request.input, ((ICharacterNode) inputStack.pop()).width());
//
//                outputStack.peek().remainingChildren--;
//            } else if (inputStack.peek() instanceof IParseNode) { // Consume (skipped) parse node
//                IncrementalParseNode parseNode = (IncrementalParseNode) inputStack.pop();
//                positionStack.push(pivotPosition);
//
//                boolean visitChildren = visitor.preVisit(parseNode, pivotPosition);
//
//                if (visitChildren && parseNode.hasDerivations()) { // Parse node with derivation(s)
//                    int derivations = 0;
//
//                    for (IncrementalDerivation derivation : parseNode.getDerivations()) {
//                        inputStack.push(derivation);
//                        derivations++;
//
//                        if (derivations >= 1 && !visitor.visitAmbiguousDerivations())
//                            break;
//                    }
//
//                    outputStack.push(new IncrementalVisit(derivations, parseNode));
//                } else { // Skipped parse node (without derivations)
//                    pivotPosition = pivotPosition.step(request.input, parseNode.width());
//
//                    visitor.postVisit(parseNode, positionStack.pop(), pivotPosition);
//
//                    outputStack.peek().remainingChildren--;
//                }
//            }
//        }
//    }
//
//    class IncrementalVisit {
//        int remainingChildren;
//        IncrementalParseNode parseNode;
//
//        IncrementalVisit() {
//            this.remainingChildren = 1;
//            this.parseNode = null;
//        }
//
//        IncrementalVisit(IDerivation<?> derivation) {
//            this.remainingChildren = derivation.parseForests().length;
//            this.parseNode = null;
//        }
//
//        IncrementalVisit(int remainingChildren, IncrementalParseNode parseNode) {
//            this.remainingChildren = remainingChildren;
//            this.parseNode = parseNode;
//        }
//
//        boolean done() {
//            return remainingChildren == 0;
//        }
//    }
//
//    public static SourceRegion visitRegion(String inputString, Position startPosition, Position endPosition) {
//        if (endPosition.offset > startPosition.offset)
//            endPosition = endPosition.previous(inputString);
//
//        return new SourceRegion(startPosition, endPosition);
//    }

}
