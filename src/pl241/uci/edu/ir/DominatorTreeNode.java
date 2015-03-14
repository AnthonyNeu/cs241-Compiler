package pl241.uci.edu.ir;


import java.util.ArrayList;
/*
Date:2015/03/03
This is the class to store the dominator tree node in control flow graph.
 */
public class DominatorTreeNode {
    public BasicBlock block;
    public ArrayList<DominatorTreeNode> children;

    public DominatorTreeNode(BasicBlock block){
        this.block = block;
        this.children = new ArrayList<DominatorTreeNode>();
    }

    public BasicBlock getBasicBlock() {
        return block;
    }

    public void setBasicBlock(BasicBlock block) {
        this.block = block;
    }

    public ArrayList<DominatorTreeNode> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<DominatorTreeNode> children) {
        this.children = children;
    }
}
