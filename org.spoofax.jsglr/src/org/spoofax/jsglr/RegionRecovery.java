//TODO: samenwerking met recovery connector eenduidiger
package org.spoofax.jsglr;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Supports error recovery by selecting the region containing the error.
 * This region can be discarded (parsed as layout) or can be inspected by a refined recover method
 */
public class RegionRecovery {

    private SGLR myParser;    
    private StructureSkipSuggestion erroneousRegion;    
    private int errorDetectionLocation;
    private static int NR_OF_LINES_TILL_SUCCESS=3;

    /**
     * Supports error recovery by selecting the region containing the error
     */
    public RegionRecovery(SGLR sglr){
        myParser=sglr;        
    }
    
    private ParserHistory getHistory() {
        return myParser.getHistory();
    }
    
    /** *
     *  Returns info about the parser configuration at the start of the erroneous region 
     */
    public IndentInfo getStartLineErrorFragment() {
        return erroneousRegion.getStartSkip();
    }
    
    /**
     * returns the location of the first non-erroneous character
     */
    public int getEndPositionErrorFragment() {
        return erroneousRegion.getEndSkip().getTokensSeen();
    }

    /**
     *  Returns error fragment including the left margin (needed for bridge-parsing)
     */
    public String getErrorFragmentWithLeftMargin() {
        int tokIndexLine=getHistory().getTokensSeenStartLine(getStartLineErrorFragment().getTokensSeen());
        return getHistory().getFragment(tokIndexLine, getEndPositionErrorFragment()-1);
    }
    
    public String getErrorFragment() {
        int tokIndexLine=erroneousRegion.getStartSkip().getTokensSeen();//+erroneousRegion.getAdditionalTokens().length; 
        return getHistory().getFragment(tokIndexLine, getEndPositionErrorFragment()-1);
    }

    /**
     * Returns location where erroneous region starts, including left margin
     */
    public int getStartPositionErrorFragment_InclLeftMargin() {
        int tokIndexLine=getHistory().getTokensSeenStartLine(getStartLineErrorFragment().getTokensSeen());
        return tokIndexLine;
    }

    public ArrayList<IndentInfo> getSkippedLines() {        
        return getHistory().getLinesFromTo(erroneousRegion.getIndexHistoryStart(), getEndPositionErrorFragment());
    }      

    /**
     * Selects erroneous region based on layout 
     */
    public boolean selectErroneousFragment() throws IOException { 
        NewStructureSkipper newRegionSelector=new NewStructureSkipper(myParser);
        int failureIndex=getHistory().getIndexLastLine();
        errorDetectionLocation=getHistory().getIndexLastToken();
        ArrayList<StructureSkipSuggestion> prevRegions=newRegionSelector.getPreviousSkipSuggestions(failureIndex);
        //System.out.println("PREVIOUS REGION");        
        if(trySetErroneousRegion(prevRegions)){
            ArrayList<StructureSkipSuggestion> decomposedRegions=newRegionSelector.getZoomOnPreviousSuggestions(erroneousRegion);
            trySetErroneousRegion(decomposedRegions);
            return true;
        }        
        ArrayList<StructureSkipSuggestion> currentRegions=newRegionSelector.getCurrentSkipSuggestions(failureIndex);
        //System.out.println("CURRENT REGION");
        if(trySetErroneousRegion(currentRegions)){            
            return true;
        }
        //System.out.println("PRIOR REGIONS");
        ArrayList<StructureSkipSuggestion> priorRegions=newRegionSelector.getPriorSkipSuggestions(failureIndex);
        if(trySetErroneousRegion(priorRegions)){
            ArrayList<StructureSkipSuggestion> decomposedRegions=newRegionSelector.getZoomOnPreviousSuggestions(erroneousRegion);
            trySetErroneousRegion(decomposedRegions);
            return true;
        }
        //System.out.println("FW-SIB REGIONS");
        ArrayList<StructureSkipSuggestion> siblingForWardRegions=newRegionSelector.getSibblingForwardSuggestions(failureIndex);
        if(trySetErroneousRegion(siblingForWardRegions)){            
            return true;
        }
        //System.out.println("BW-SIB REGIONS");
        ArrayList<StructureSkipSuggestion> siblingBackWardRegions=newRegionSelector.getSibblingBackwardSuggestions(failureIndex);
        if(trySetErroneousRegion(siblingBackWardRegions)){            
            return true;
        }
        //System.out.println("SURROUNDING-SIB REGIONS");        
        ArrayList<StructureSkipSuggestion> siblingSurroundingRegions=newRegionSelector.getSibblingSurroundingSuggestions(failureIndex);
        if(trySetErroneousRegion(siblingSurroundingRegions)){            
            return true;
        }
        //System.out.println("PARENT REGION");
        ArrayList<StructureSkipSuggestion> parentRegion=newRegionSelector.getParentSkipSuggestions(failureIndex);
        if(trySetErroneousRegion(parentRegion)){            
            return true;
        }
        erroneousRegion=newRegionSelector.getErroneousPrefix();
        ArrayList<StructureSkipSuggestion> decomposedRegions=newRegionSelector.getZoomOnPreviousSuggestions(erroneousRegion);
        boolean findSmallerPart=trySetErroneousRegion(decomposedRegions);
        return findSmallerPart; 
    }

    private boolean trySetErroneousRegion(ArrayList<StructureSkipSuggestion> regions) throws IOException {
        StructureSkipSuggestion aSkip=new StructureSkipSuggestion();
        int indexSkips=0;
        myParser.acceptingStack=null; 
        myParser.activeStacks.clear(); //undo success
        boolean hasFoundErroneousRegion=false;
        while (indexSkips < regions.size() && !hasFoundErroneousRegion) {
            aSkip = regions.get(indexSkips);            
            hasFoundErroneousRegion=testRegion(aSkip);
            indexSkips++;            
        }        
        if(hasFoundErroneousRegion){
            erroneousRegion=aSkip;   
            //System.out.println("Erroneous region set ");
        }
        return hasFoundErroneousRegion;
    }

    private boolean testRegion(StructureSkipSuggestion aSkip) throws IOException {
        //System.out.println("%%%%%%%%%%% TEST REGION %%%%%%%%%%%");
        //System.out.println(getInputFragment(aSkip));
        //System.out.printlnBlock(getInputFragment(aSkip));           
        IndentInfo endPos=aSkip.getEndSkip();
        getHistory().setTokenIndex(endPos.getTokensSeen());
        myParser.activeStacks.clear();
        myParser.acceptingStack=null;
        myParser.activeStacks.addAll(endPos.getStackNodes());        
        parseAdditionalTokens(aSkip);
        int nrOfParsedLines=0; 
        //System.out.println("%%%%%%%%%%% CONTINUE PARSING %%%%%%%%%%%");
        IndentationHandler indentHandler = new IndentationHandler();
        indentHandler.setInLeftMargin(false);
        while((myParser.activeStacks.size() > 0 && nrOfParsedLines<NR_OF_LINES_TILL_SUCCESS)) {//|| !getHistory().hasFinishedRecoverTokens() 
            getHistory().readRecoverToken(myParser); 
            indentHandler.updateIndentation(myParser.currentToken);           
            //System.out.println((char)myParser.currentToken); 
            //System.out.print((char)myParser.currentToken);
            myParser.doParseStep();
            if(getHistory().getTokenIndex()>errorDetectionLocation && indentHandler.lineMarginEnded())
                nrOfParsedLines++;
        }
        return successCriterion();
    }

    private void parseAdditionalTokens(
            StructureSkipSuggestion aSkip) throws IOException {
        for (char aChar : aSkip.getAdditionalTokens()) {
            myParser.currentToken=aChar;           
            myParser.doParseStep();
        }
        if(aSkip.getAdditionalTokens().length>0){            
            aSkip.getStartSkip().fillStackNodes(myParser.activeStacks);
            aSkip.getEndSkip().fillStackNodes(myParser.activeStacks);
            aSkip.getStartSkip().setTokensSeen(aSkip.getStartSkip().getTokensSeen() + aSkip.getAdditionalTokens().length);
            aSkip.setAdditionalTokens(new char[0]);
        }
    }

    private String getInputFragment(StructureSkipSuggestion aSkip) {
        return getHistory().getFragment(aSkip.getStartSkip().getTokensSeen(), aSkip.getEndSkip().getTokensSeen()-1);
    }

    private boolean successCriterion() {
        return myParser.activeStacks.size() > 0 || myParser.acceptingStack!=null;
    }

}
