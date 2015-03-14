package pl241.uci.edu.cfg;

import pl241.uci.edu.middleend.Instruction;
import pl241.uci.edu.middleend.Result;
import pl241.uci.edu.middleend.SSAValue;

import java.util.ArrayList;
import java.util.HashMap;

public class DelUseChain {

    //this store all the use chains of left result of pre-SSA
    public HashMap<Instruction, ArrayList<Instruction>> xDefUseChains;

    //this store all the use chains of right result of pre-SSA
    public HashMap<Instruction, ArrayList<Instruction>> yDefUseChains;

    public DelUseChain()
    {
        xDefUseChains = new HashMap<Instruction,ArrayList<Instruction>>();
        yDefUseChains = new HashMap<Instruction,ArrayList<Instruction>>();
    }

    //update use chain of a pre-SSA instruction

    /**
     * @param left ,left Result of a instruction
     * @param right,right Result of a instruction
     */
    public void updateDefUseChain(Result left, Result right) {
        ArrayList<Instruction> useInstructions = null;
        Instruction curInstr = null;
        if (left.type == Result.ResultType.variable) {
            curInstr = ControlFlowGraph.allInstructions.get(left.instrRef);
            Instruction leftLastUse = ControlFlowGraph.allInstructions.get(left.ssaVersion.getVersion());
            if (xDefUseChains.containsKey(leftLastUse)) {
                useInstructions = xDefUseChains.get(leftLastUse);
            } else {
                useInstructions = new ArrayList<Instruction>();
                xDefUseChains.put(leftLastUse, useInstructions);
            }
            useInstructions.add(curInstr);
        }
        if (right.type == Result.ResultType.variable) {
            Instruction rightLastUse = ControlFlowGraph.allInstructions.get(right.ssaVersion.getVersion());

            if (yDefUseChains.containsKey(rightLastUse)) {
                useInstructions = yDefUseChains.get(rightLastUse);
            } else {
                useInstructions = new ArrayList<Instruction>();
                yDefUseChains.put(rightLastUse, useInstructions);
            }
            useInstructions.add(curInstr);
        }
    }

    /**
     * @param  ssaDef,reference SSA of a instruction
     * @param  use, instruction refers to a SSA value
     **/
    public void updateXDefUseChains(SSAValue ssaDef, Instruction use) {
        updateXDefUseChains(ControlFlowGraph.allInstructions.get(ssaDef.getVersion()), use);
    }

    /**
     * @param ssaDef,reference SSA of a instruction
     * @param  use, instruction refers to a SSA value
     **/
    public void updateYDefUseChains(SSAValue ssaDef, Instruction use) {
        updateYDefUseChains(ControlFlowGraph.allInstructions.get(ssaDef.getVersion()), use);
    }

    public void updateXDefUseChains(Instruction def, Instruction use) {
        ArrayList<Instruction> useInstructions = null;
        if (xDefUseChains.containsKey(def)) {
            useInstructions = xDefUseChains.get(def);
        } else {
            useInstructions = new ArrayList<Instruction>();
            xDefUseChains.put(def, useInstructions);
        }
        useInstructions.add(use);
    }

    public void updateYDefUseChains(Instruction def, Instruction use) {
        ArrayList<Instruction> useInstructions = null;
        if (yDefUseChains.containsKey(def)) {
            useInstructions = yDefUseChains.get(def);
        } else {
            useInstructions = new ArrayList<Instruction>();
            yDefUseChains.put(def, useInstructions);
        }
        useInstructions.add(use);
    }
}
