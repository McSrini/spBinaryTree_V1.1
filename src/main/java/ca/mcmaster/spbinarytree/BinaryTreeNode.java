/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spbinarytree;
 
import static ca.mcmaster.spbinarytree.BinaryTree.*;
import java.io.Serializable;
import java.util.*;

import static ca.mcmaster.spbinarytree.BinaryTree.*;

/**
 *
 * @author tamvadss
 */
public class BinaryTreeNode {
    
       
    public String nodeID;
    public BinaryTreeNode        parent;
    
    //child 0 and 1
    public List<BinaryTreeNode> childList = new ArrayList<BinaryTreeNode>();
    //branching needed to create each child
    public  List<BranchingInstruction> childBranchingInstructionList  = new  ArrayList<BranchingInstruction>  ();
    
    //Cumulative variables branched on,  to create this node
    public BranchingInstruction cumulativeBranchingInstructions = new BranchingInstruction()  ; 
            
    //descendant lists are populated just before generating farming instructions
    //These contain node IDs of descendants on left and right which have been chosen for farming
    public List <String> leftDescendantList = new ArrayList <String> ();
    public List <String> rightDescendantList = new ArrayList <String> ();
            
    //how many nodes on each side we can skip over
    //Note that we can have skippable nodes on both left and right sides of this node, however a skippable node can have skippable nodes on only 1 side
    //At each node, we can decide which direction to skip by checking the direction which has a non zero desecndant count
    public int leftSideSkipCount=ZERO, rightSideSkipCount= ZERO;
        
    public BinaryTreeNode (String nodeId, BinaryTreeNode  parentNode,   BranchingInstruction cumulativeBranchingInstructions) {
        this.nodeID=nodeId;
        this.parent = parentNode;     
        this.cumulativeBranchingInstructions=cumulativeBranchingInstructions ;
    }
    
    //remove child with nodeID and return   branching Instructions For Removed Child
    //NOTE : we do not retunr the removed node. It is expected that you have its reference, since you are passing in its node id
    public BranchingInstruction removeChild (String nodeID ) {
        int removalIndex  = MINUS_ONE;
        for (int index = ZERO; index < childList.size(); index ++) {
            if (childList.get(index).nodeID.equals(nodeID )) removalIndex = index;
        }
        
        childList.remove(removalIndex );
        return childBranchingInstructionList.remove(removalIndex );         
    }
    
    public void addChild(BinaryTreeNode child, BranchingInstruction childCreationInstruction) {
        childList.add(child );
        childBranchingInstructionList.add(childCreationInstruction);
    }
     
    public String toString() {
        String result =EMPTY_STRING;
        
        result += "Nid = "+this.nodeID + "\n";
        
        BinaryTreeNode parentNode = this.parent;
        while (parentNode!=null){
            result += " \nPid =" + parentNode.nodeID+"\n";
            
            int numLeftKids = parentNode.childList.size() >= 1 ? 1: 0;
            int numRightKids = parentNode.childList.size() >= 2 ? 1: 0;
            result += " LC="+numLeftKids+ " RC="+numRightKids+"\n";
            
            result += " LDES="+parentNode.leftDescendantList.size()+ " RDES="+parentNode.rightDescendantList.size()+"\n";
            if (parentNode.rightSideSkipCount  +  parentNode.leftSideSkipCount > ZERO)  result += " ****************  ";
            result += " LSK =" + parentNode.leftSideSkipCount + " RSK = "+parentNode.rightSideSkipCount+"\n\n";
            
            parentNode = parentNode.parent;
        }
        
        result +=  cumulativeBranchingInstructions.toString();
        
        return result+"--------------------------------------------------------------------------------------------\n";
    }
}

