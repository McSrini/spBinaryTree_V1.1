/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spbinarytree;

import static ca.mcmaster.spbinarytree.BinaryTree.ONE;
import static ca.mcmaster.spbinarytree.BinaryTree.*;
import java.util.*;

/**
 *
 * @author tamvadss
 */
public class BranchingInstruction {
      
    //variables used to create this child by branching the parent
    public List<String>  varNames = new ArrayList< String> (); 
    //is this an upper bound ?
    public List< Boolean > isBranchDirectionDown = new ArrayList< Boolean >(); 
    public List< Double>  varBounds = new ArrayList< Double >();
    
    public     BranchingInstruction( ) {
        
    }
    
    public int size () {
        return varNames.size();
    }
    
    public     BranchingInstruction(BranchingInstruction anotherInstruction ) {
        for (int index = ZERO; index < anotherInstruction.size(); index ++) {
            this.varNames.addAll(anotherInstruction.varNames);
            this.varBounds.addAll( anotherInstruction.varBounds);
            this.isBranchDirectionDown.addAll(anotherInstruction.isBranchDirectionDown);
        }
    }
    
    public     BranchingInstruction(String[] names, Boolean[] dirs, double[] bounds) {
        for (String name : names) {
            varNames.add(name);
        }
        for (Boolean dir  : dirs){
            isBranchDirectionDown.add(dir);
        }
        for (Double bound : bounds)   {
            varBounds.add(bound);
        }
    }
    
    public void merge (BranchingInstruction instructionOne) {
        varNames.addAll(instructionOne.varNames);
        
        isBranchDirectionDown.addAll( instructionOne.isBranchDirectionDown);
        
        varBounds.addAll(instructionOne.varBounds );
        
    }
    
    public void merge (BranchingInstruction instructionOne, BranchingInstruction instructionTwo) {
        varNames.addAll(instructionOne.varNames);
        varNames.addAll(instructionTwo.varNames);
        
        isBranchDirectionDown.addAll( instructionOne.isBranchDirectionDown);
        isBranchDirectionDown.addAll( instructionTwo.isBranchDirectionDown);
        
        varBounds.addAll(instructionOne.varBounds );
        varBounds.addAll(instructionTwo.varBounds );
    }
    
    public BranchingInstruction subtract (  BranchingInstruction minusInstruction ) {
        BranchingInstruction bi = new BranchingInstruction ();
               
        for (int index = ZERO; index < this.varBounds.size(); index ++){
            if (! minusInstruction.varNames.contains(this.varNames.get(index)))  {
                bi.varNames.add( this.varNames.get(index));
                bi.varBounds.add(this.varBounds.get(index));
                bi.isBranchDirectionDown.add(this.isBranchDirectionDown.get(index));
            }
        }
        
        return bi;
    }
    
    public String toString (){
        String result =EMPTY_STRING;
        
        for (int index = ZERO; index < varNames.size(); index ++) {
            String varname = varNames.get(index);
            Double varbound = this.varBounds.get(index);
            int isDown = this.isBranchDirectionDown.get(index) ? ONE: ZERO;
            result += "("+varname + "," +varbound+ ","+isDown +") ";
        }
        
        
        return result ;
    }
    
}
