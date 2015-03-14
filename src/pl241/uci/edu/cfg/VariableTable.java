package pl241.uci.edu.cfg;

import pl241.uci.edu.middleend.Result;
import pl241.uci.edu.middleend.SSAValue;
import pl241.uci.edu.frontend.Scanner;

import java.util.*;

/*
Date:2015/03/03
This class is used to store the hash map of global variable and SSA reference.
*/
public class VariableTable {
    //store the address of global variables
    private static HashSet<Integer> GlobalVariableIdent;

    //store the SSA form variables which have the same address
    private static HashMap<Integer,ArrayList<SSAValue>> SSAUseChain;

    //store the definition of array
    public static HashMap<Integer,Result> ArrayDefinition;

    public VariableTable()
    {
        GlobalVariableIdent = new HashSet<Integer>();

        SSAUseChain = new HashMap<Integer,ArrayList<SSAValue>>();

        ArrayDefinition = new HashMap<Integer,Result>();
    }

    public static void addGlobalVariable(int varIdent)
    {
        GlobalVariableIdent.add(varIdent);
    }

    public static SSAValue getLatestVersion(int varIdent)
    {
        if(!SSAUseChain.containsKey(varIdent))
        {
            System.out.println(Scanner.ident.get(varIdent));
            Error("Cannot find address " + varIdent + " ! get latest version of SSA failed!");
            return null;
        }
        else
            return SSAUseChain.get(varIdent).get(SSAUseChain.get(varIdent).size()-1);
    }

    public static void addSSAUseChain(int varIdent,int version)
    {
        if(!SSAUseChain.containsKey(varIdent))
            SSAUseChain.put(varIdent,new ArrayList<SSAValue>());
        SSAUseChain.get(varIdent).add(new SSAValue(version));
    }

    public static void addSSAUseChain(int varIdent,SSAValue ssa)
    {
        if(!SSAUseChain.containsKey(varIdent))
            SSAUseChain.put(varIdent, new ArrayList<SSAValue>());
        SSAUseChain.get(varIdent).add(ssa);
    }

    public static HashMap<Integer,ArrayList<SSAValue>> cloneSSAUseChain()
    {
        HashMap<Integer,ArrayList<SSAValue>> clone = new HashMap<Integer,ArrayList<SSAValue>>();
        for(Map.Entry<Integer, ArrayList<SSAValue>> entry : SSAUseChain.entrySet()){
            clone.put(entry.getKey(), new ArrayList<SSAValue>(entry.getValue()));
        }
        return clone;
    }

    public static HashSet<Integer> cloneGlobalVariableAddress()
    {
        Iterator iter = GlobalVariableIdent.iterator();
        HashSet<Integer> clone = new HashSet<Integer>();
        while(iter.hasNext())
            clone.add((Integer)iter.next());
        return clone;
    }

    public static HashSet<Integer> getGlobalVariableIdent()
    {
        return GlobalVariableIdent;
    }

    public static void setGlobalVariableIdent(HashSet<Integer> set)
    {
        GlobalVariableIdent = set;
    }

    public static HashMap<Integer,ArrayList<SSAValue>> getSSAUseChain()
    {
        return SSAUseChain;
    }

    public static void setSSAUseChain(HashMap<Integer,ArrayList<SSAValue>> set)
    {
        SSAUseChain = set;
    }

    private static void Error(String msg)
    {
        System.out.println("VariableTable Error! " + msg);
    }

}
