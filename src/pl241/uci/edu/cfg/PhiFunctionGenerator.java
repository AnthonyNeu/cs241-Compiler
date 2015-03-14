package pl241.uci.edu.cfg;

import pl241.uci.edu.middleend.Instruction;
import pl241.uci.edu.middleend.InstructionType;
import pl241.uci.edu.middleend.SSAValue;
import pl241.uci.edu.frontend.Scanner;

import java.util.HashMap;

/*
Date:2015/03/04
This class is used to generate phi function in control flow graph.
 */
public class PhiFunctionGenerator {
    //hash the address of a variable to the phi function
    private HashMap<Integer,Instruction> phiInstructionMap;

    public PhiFunctionGenerator()
    {
        this.phiInstructionMap = new HashMap<Integer,Instruction>();
    }

    public Instruction addPhiFunction(int address,SSAValue SSA)
    {
        if(this.phiInstructionMap.containsKey(address))
            return this.phiInstructionMap.get(address);
        else
        {
            Instruction returnIns = new Instruction(InstructionType.PHI,address,SSA,SSA);
            phiInstructionMap.put(address,returnIns);

            //update the control flow graph
            ControlFlowGraph.delUseChain.updateXDefUseChains(SSA,returnIns);
            ControlFlowGraph.delUseChain.updateYDefUseChains(SSA,returnIns);

            return returnIns;
        }
    }

    public void updatePhiFunction(int address,SSAValue SSA,PhiFunctionUpdateType type)
    {
        if(!this.phiInstructionMap.containsKey(address))
            Error("cannot find phi function for variable " + Scanner.ident.get(address));
        else
        {
            Instruction ins = this.phiInstructionMap.get(address);
            if(type == PhiFunctionUpdateType.LEFT)
            {
                ins.setLeftLatestUpdated(true);
                ins.setLeftSSA(SSA);

                ControlFlowGraph.delUseChain.updateXDefUseChains(SSA, ins);
            }
            else
            {
                ins.setLeftLatestUpdated(false);
                ins.setRightSSA(SSA);

                ControlFlowGraph.delUseChain.updateYDefUseChains(SSA,ins);
            }
        }
    }

    public HashMap<Integer,Instruction> getPhiInstructionMap(){
        return phiInstructionMap;
    }

    private void Error(String msg)
    {
        System.out.println("PhiFunctionGenerator Error! " + msg);
    }

}
