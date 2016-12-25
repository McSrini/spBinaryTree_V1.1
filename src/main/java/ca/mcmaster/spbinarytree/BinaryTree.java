/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spbinarytree;

import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class BinaryTree {
    
    private static Logger logger=Logger.getLogger(BinaryTree.class);
        
    public static final int ZERO = 0;    
    public static final int ONE = 1;    
    public static final int TWO = 2;  
    public static final int MINUS_ONE=-1;
    public static final String MINUS_ONE_STRING = ""+-1;
    public static final String EMPTY_STRING = "";
    public static final String LOG_FILE_EXTENSION = ".log";
    public static String LOG_FOLDER="F:\\temporary files here\\logs\\testing\\";
    
    
    public BinaryTreeNode        rootNode;
    
    //node chosen for solution by CPLEX, this will be one of the active leafs
    public BinaryTreeNode        currentlySelectedNode;
    //branching instructions from parent, for this currently chosen leaf node
    public BranchingInstruction branchingInstructionsForCurrentlySelectedNode;
    
    //maintain an index by Node ID
    public Map<String, BinaryTreeNode> activeLeafs = new HashMap<String, BinaryTreeNode>();
    
    static {
        logger.setLevel(Level.OFF);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+BinaryTree.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
        }
          
    }
    
    public BinaryTree ( ) {
        //starting tree has  only the root node, which has no branch instructions, no parent, and invalid nodeID
        rootNode = new BinaryTreeNode(MINUS_ONE_STRING, null, new BranchingInstruction());
        activeLeafs.put(MINUS_ONE_STRING, rootNode);
    }
     
    //invoke this method when CPLEX node handler chooses a node for solving
    public void recordNodeSelectedForSolution(String nodeID){
        //remove it from leaf map
        currentlySelectedNode = activeLeafs.remove(nodeID );
        //remove it from tree
        if (!isRootNode(currentlySelectedNode)) branchingInstructionsForCurrentlySelectedNode = currentlySelectedNode.parent.removeChild(nodeID );
        
        //note that a parent node with both kids solved will be a hanging node in our binary tree. Such hanging nodes ar eno longer in the
        //CPLEX tree, but appear as leafs in our mirror tree. 
        //This is not a problem since such nodes are never chosen as a leaf for migration by CPLEX, so contribute nothing to any operation on the mirror tree.
        //In effect , such nodes are like parent nodes with kids that are never selected for farming.
        
    }
    
    //invoke this method when CPLEX branch handler creates kids
    public void recordChildCreation (List<String> childNodeIDList,     List<BranchingInstruction> branchingInstructions ) {
        
        //first restore currentlySelectedNode in tree
        if (!isRootNode(currentlySelectedNode)) currentlySelectedNode.parent.addChild(currentlySelectedNode , branchingInstructionsForCurrentlySelectedNode);
                
        //create its 2 kids and update currentSelectedNode accordingly.  
        //Add the kids to active leafs map
        for (int index = ZERO; index < childNodeIDList.size(); index ++){
           
            BranchingInstruction branchingInstructionsForThisChild =branchingInstructions.get(index);
            
            BranchingInstruction cumulativeBranchingInstructionsForThisChild = new BranchingInstruction();
            cumulativeBranchingInstructionsForThisChild.merge(currentlySelectedNode.cumulativeBranchingInstructions , branchingInstructionsForThisChild );
             
            BinaryTreeNode childNode = new BinaryTreeNode(childNodeIDList.get(index), currentlySelectedNode   ,   cumulativeBranchingInstructionsForThisChild) ;
            
            currentlySelectedNode.addChild(childNode, branchingInstructionsForThisChild);
            
            activeLeafs.put(childNode.nodeID, childNode);
        }
        
    }
    
    public boolean isRootNode (BinaryTreeNode  node) {
        return node.parent==null;
    }
    
    //User can select some leafs for farming. When user does this, we have to take care of 2 things
    //1) prune chosen leafs, and update our binary tree including its active leaf map
    //2) return a branching instruction tree, which allows you to recreate the leafs on another machine
    
    public BranchingInstructionTree getInstructionForFarmingNodes(List<String> nodesChosenForfarming) {
        
        //prepare ref counts and skip counts
        updateDescendantRefCounts(nodesChosenForfarming, false);
        logger.error("AFTER UPDATING REF COUNTS"   + this.toString());
        
        updateSkipCounts(nodesChosenForfarming, false);
        
        logger.error("AFTER UPDATING SKIP COUNTS"   + this.toString());
         
        BranchingInstructionNode rootNode = new BranchingInstructionNode (MINUS_ONE_STRING);
        BranchingInstructionTree instructionTree = new BranchingInstructionTree(rootNode);
        getBranchingInstructionTree(this.rootNode, instructionTree);
        
        logger.error("COMPLETED  GENARTING INSTRUCTION TREE"   );
        
        //reset binary tree ref counts and skip counts
        updateDescendantRefCounts(nodesChosenForfarming, true);
         logger.error("RESET REF COUNTS"   );
         
        updateSkipCounts(nodesChosenForfarming, true);
         logger.error("RESET SKIP COUNTS"   );
        
        pruneTreeAfterFarming(nodesChosenForfarming);
         logger.error("PRUNED TREE AFTER GENERATING INSTRUCTIONS"   );
         
        return instructionTree;
    }
    
    //use pre-order traversal of this binary tree to generate the instruction tree
    //Method has some code duplication and should be cleaned up
    private void getBranchingInstructionTree(BinaryTreeNode  subtreeRoot, BranchingInstructionTree instructionTree){
        int leftSideSize = subtreeRoot.leftDescendantList.size() ;
        int rightSideSize = subtreeRoot.rightDescendantList.size() ;
        
        logger.info("getBranchingInstructionTree " + subtreeRoot.nodeID);
        
        //check left of subtree root
        if (leftSideSize!=ZERO) {
            
            if (leftSideSize==ONE) {
                //create the kid, no recursion
                String descendantNodeID = subtreeRoot.leftDescendantList.get(ZERO);
                BranchingInstruction cumulativeBranchingInstructions= this.activeLeafs.get(descendantNodeID ).cumulativeBranchingInstructions;
                BranchingInstruction instruction = cumulativeBranchingInstructions.subtract( subtreeRoot.cumulativeBranchingInstructions);
                instructionTree.createChild( subtreeRoot.nodeID, new BranchingInstructionNode(descendantNodeID),   instruction);
            
            } else if (leftSideSize==TWO && rightSideSize==ZERO) {
                
                //create both kids, no recursion
                
                String descendantNodeID = subtreeRoot.leftDescendantList.get(ZERO);
                BranchingInstruction cumulativeBranchingInstructions= this.activeLeafs.get(descendantNodeID ).cumulativeBranchingInstructions;
                BranchingInstruction instructionLeft = cumulativeBranchingInstructions.subtract( subtreeRoot.cumulativeBranchingInstructions);
                instructionTree.createChild( subtreeRoot.nodeID, new BranchingInstructionNode(descendantNodeID),   instructionLeft);
                
                descendantNodeID = subtreeRoot.leftDescendantList.get(ONE);
                cumulativeBranchingInstructions= this.activeLeafs.get(descendantNodeID ).cumulativeBranchingInstructions;
                BranchingInstruction instructionRight = cumulativeBranchingInstructions.subtract( subtreeRoot.cumulativeBranchingInstructions);
                instructionTree.createChild( subtreeRoot.nodeID, new BranchingInstructionNode(descendantNodeID),   instructionRight);
                
            } else {
                
                //create left kid and make recursive call
                 
                BranchingInstruction instruction = subtreeRoot. childBranchingInstructionList.get(ZERO);
                
                BinaryTreeNode thisChild= subtreeRoot.childList.get(ZERO);
                BranchingInstruction compoundInstr = new BranchingInstruction();
                compoundInstr.merge(instruction);
                for (int index = ZERO;  index < subtreeRoot.leftSideSkipCount; index++){
                     
                    boolean isSkipDirectionLeft =  thisChild.leftDescendantList.size()>ZERO;
                    if (isSkipDirectionLeft) {
                        //skip one node to the left
                        compoundInstr .merge(thisChild.childBranchingInstructionList.get(ZERO));
                        thisChild = thisChild .childList.get(ZERO)  ;
                    }else {
                        //skip to the right
                        compoundInstr .merge(thisChild.childBranchingInstructionList.get(ONE));
                        thisChild = thisChild .childList.get(ONE)  ;
                    }                   
                }
                
                BranchingInstructionNode childNode = new BranchingInstructionNode ( thisChild.nodeID);
                instructionTree.createChild( subtreeRoot.nodeID, childNode,   compoundInstr);

                //recursive call to left side
                getBranchingInstructionTree(   thisChild,   instructionTree);
            } 
        }
        
       
        //check right of subtree root
        if (rightSideSize!=ZERO) {
            
            if (rightSideSize==ONE) {
                //create the kid, no recursion
                String descendantNodeID = subtreeRoot.rightDescendantList.get(ZERO);
                BranchingInstruction cumulativeBranchingInstructions= this.activeLeafs.get(descendantNodeID ).cumulativeBranchingInstructions;
                BranchingInstruction instruction = cumulativeBranchingInstructions.subtract( subtreeRoot.cumulativeBranchingInstructions);
                instructionTree.createChild( subtreeRoot.nodeID, new BranchingInstructionNode(descendantNodeID),   instruction);
            
            } else if (leftSideSize==ZERO && rightSideSize==TWO) {
                
                //create both kids, no recursion
                
                String descendantNodeID = subtreeRoot.rightDescendantList.get(ZERO);
                BranchingInstruction cumulativeBranchingInstructions= this.activeLeafs.get(descendantNodeID ).cumulativeBranchingInstructions;
                BranchingInstruction instructionZero = cumulativeBranchingInstructions.subtract( subtreeRoot.cumulativeBranchingInstructions);
                instructionTree.createChild( subtreeRoot.nodeID, new BranchingInstructionNode(descendantNodeID),   instructionZero);
                
                descendantNodeID = subtreeRoot.rightDescendantList.get(ONE);
                cumulativeBranchingInstructions= this.activeLeafs.get(descendantNodeID ).cumulativeBranchingInstructions;
                BranchingInstruction instructionONE = cumulativeBranchingInstructions.subtract( subtreeRoot.cumulativeBranchingInstructions);
                instructionTree.createChild( subtreeRoot.nodeID, new BranchingInstructionNode(descendantNodeID),   instructionONE);
                
            } else   {
                
                //create right side kid and make recursive call, may skip over few descendants
                       
                //get  instruction for creating right side child
                BranchingInstruction instruction = subtreeRoot. childBranchingInstructionList.get(ONE);
                //get right side kid
                BinaryTreeNode thisChild= subtreeRoot.childList.get(ONE);
                
                //prepare compund instruction in case we are skipping over some desecndants
                BranchingInstruction compoundInstr = new BranchingInstruction();
                compoundInstr.merge(instruction);
                for (int index = ZERO;  index < subtreeRoot.rightSideSkipCount; index++){
                      
                    boolean isSkipDirectionLeft =  thisChild.leftDescendantList.size()>ZERO;
                    if (isSkipDirectionLeft) {
                        //skip one node to the left
                        compoundInstr .merge(thisChild.childBranchingInstructionList.get(ZERO));
                        thisChild = thisChild .childList.get(ZERO)  ;
                    }else {
                        //skip to the right
                        compoundInstr .merge(thisChild.childBranchingInstructionList.get(ONE));
                        thisChild = thisChild .childList.get(ONE)  ;
                    }                   
                }
                
                BranchingInstructionNode childNode = new BranchingInstructionNode ( thisChild.nodeID);
                instructionTree.createChild( subtreeRoot.nodeID, childNode,   compoundInstr);

                //recursive call to right side
                getBranchingInstructionTree(   thisChild,   instructionTree);
                   
            } 
        }
        
        
    }
    
    
    
    //return number of active leafs left
    private void pruneTreeAfterFarming (List<String> nodesChosenForfarming) {
        //remove the leafs migrated, by updating their parent links
        for (String nodeID :nodesChosenForfarming ){
            
            //get the node              
            BinaryTreeNode node = activeLeafs.get( nodeID);
            BinaryTreeNode parentNode = node.parent;
            parentNode.removeChild(nodeID);
            
            this.activeLeafs.remove(nodeID);
        }
    }
    
    //if reset , set all skip counts back to 0
    //else assign every node its left and right skip count
    private void updateSkipCounts(List<String> nodesChosenForfarming, boolean isReset){
        //
        for (String nodeIDofFarmedNode :nodesChosenForfarming ){            
            //get the node              
            BinaryTreeNode currentNode = this.activeLeafs.get( nodeIDofFarmedNode);           
            BinaryTreeNode parentNode = currentNode.parent;
            
            logger.info("updateSkipCounts with node chosen for farming " + currentNode.nodeID + " having parent "+ parentNode.nodeID);
                        
            //this node, and each of its parents, must do the following 
            //   check if self can be skipped over, if self's refcounts are like (N>2, 0) or (0, N>2)
            //if yes, inform parent of direction and cumulative skip count
            
            while (parentNode !=null){
                
                if (isReset){
                        parentNode.leftSideSkipCount=ZERO;
                        parentNode.rightSideSkipCount=ZERO;
                } else {
                    
                    String currentNodeID = currentNode.nodeID;
                    
                    boolean canSelfBeSkippedOver = currentNode.leftDescendantList.size() ==ZERO && currentNode.rightDescendantList.size() > TWO;
                    canSelfBeSkippedOver = canSelfBeSkippedOver || (currentNode.rightDescendantList.size() ==ZERO && currentNode. leftDescendantList .size() > TWO);

                    Boolean amITheLeftChild = parentNode.childList.get(ZERO).nodeID.equals(currentNodeID );
                    
                    if (canSelfBeSkippedOver) {
                        
                        //check if I have a skip count that I recieved from either of my 2 kids
                        //Recall that , since I am skippable, at most one kid could have sent me a skip count
                        int mySkipCount = Math.max( currentNode.leftSideSkipCount , currentNode.rightSideSkipCount );
                         
                        //now send the parent the cumulative skip count 
                        if (amITheLeftChild) {
                            parentNode.leftSideSkipCount= ONE + mySkipCount;
                        }  else {
                            parentNode.rightSideSkipCount  = ONE + mySkipCount;
                        }
                        
                    } else {
                        //send 0 skip count to parent
                        if (amITheLeftChild) parentNode.leftSideSkipCount=ZERO; else parentNode.rightSideSkipCount=ZERO;
                    }                    
                 
                } //end if reset
               
                currentNode = parentNode;
                parentNode=currentNode.parent;                 
            }
           
        }
    }
    
    //for every leaf chosen for migration, send its node ID up
    //Add the node id when constrcting the refcounts, delete it to reset the refcounts back to 0
    private void updateDescendantRefCounts (List<String> nodesChosenForfarming, boolean isReset){
        for (String nodeID :nodesChosenForfarming ){
            
            //get the node              
            BinaryTreeNode node = activeLeafs.get( nodeID);
           
            BinaryTreeNode parentNode = node.parent;
           
            //this node, and each of its parents, must send up the node ID , and 
            //wheteher they are on the left or right side of their parent
            while (parentNode !=null){
                
                String currentNodeID = node.nodeID;
                                
                //send up the node id sum
                if (parentNode.childList.get(ZERO).nodeID.equals(currentNodeID )) {
                    //treat as left child    
                    if (isReset )parentNode.leftDescendantList.remove(nodeID); else parentNode.leftDescendantList.add(nodeID);
                }else {
                    //treat as right child   
                    if (isReset ) parentNode.rightDescendantList.remove(nodeID); else parentNode.rightDescendantList.add(nodeID);
                }
               
                node = parentNode;
                parentNode=node.parent;                 
            }
           
        }
    }
     
    public String toString (){
        String result =EMPTY_STRING;
        
        for (BinaryTreeNode leaf : this.activeLeafs.values()) {
            result+="\n"+leaf.toString();
        }
        
        return result;
    }
    
   
    
}
