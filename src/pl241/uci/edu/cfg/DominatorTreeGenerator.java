package pl241.uci.edu.cfg;

import pl241.uci.edu.ir.BasicBlock;
import pl241.uci.edu.ir.DominatorTreeNode;
import java.util.*;

/*
Date:2015/03/04
This class is used to generate the dominator tree.
 */
public class DominatorTreeGenerator {
    public static DominatorTreeNode root;

    private static HashSet<BasicBlock> visited;

    public DominatorTreeGenerator()
    {
        root = new DominatorTreeNode(ControlFlowGraph.getFirstBlock());
        visited = new HashSet<BasicBlock>();
        visited.add(root.block);
    }

    /**
     *  This is used to generate the dominator tree successor block of root.
     */
    public void buildDominatorTree(DominatorTreeNode start)
    {
        if(start != null)
        {
            if(start.getBasicBlock().getFollowBlock() != null && !(visited.contains(start.getBasicBlock().getFollowBlock())))
            {
                DominatorTreeNode child = new DominatorTreeNode(start.getBasicBlock().getFollowBlock());
                start.getChildren().add(child);
                visited.add(start.getBasicBlock().getFollowBlock());
            }
            if(start.getBasicBlock().getJoinBlock() != null && !(visited.contains(start.getBasicBlock().getJoinBlock())))
            {
                DominatorTreeNode child = new DominatorTreeNode(start.getBasicBlock().getJoinBlock());
                start.getChildren().add(child);
                visited.add(start.getBasicBlock().getJoinBlock());
            }
            if(start.getBasicBlock().getElseBlock() != null && !(visited.contains(start.getBasicBlock().getElseBlock())))
            {
                DominatorTreeNode child = new DominatorTreeNode(start.getBasicBlock().getElseBlock());
                start.getChildren().add(child);
                visited.add(start.getBasicBlock().getElseBlock());
            }

            for(int i = 0;i < start.getChildren().size();i++)
                buildDominatorTree(start.getChildren().get(i));
        }
        else
            Error("Root node is null!");
    }

    private void Error(String msg)
    {
        System.out.println("DominatorTreeGenerator failed! " + msg);
    }
}
