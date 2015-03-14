package pl241.uci.edu.cfg;

import java.util.HashMap;
import pl241.uci.edu.ir.BlockType;

/*
Date:2015/03/05
This is a enumerate class for the update type of phi function.
In SSA, we can only update the left reference ID and the right reference ID of the phi function.
 */
public enum PhiFunctionUpdateType {
    LEFT,
    RIGHT;

    private static HashMap<BlockType,PhiFunctionUpdateType> BlockPhiMap;

    public static void setBlockPhiMap()
    {
        BlockPhiMap = new HashMap<BlockType,PhiFunctionUpdateType>();
        BlockPhiMap.put(BlockType.IF, LEFT);
        BlockPhiMap.put(BlockType.ELSE, RIGHT);
        BlockPhiMap.put(BlockType.DO, RIGHT);
    }

    public static HashMap<BlockType,PhiFunctionUpdateType> getBlockPhiMap()
    {
        if(BlockPhiMap.size() == 0)
            setBlockPhiMap();
        return BlockPhiMap;
    }
}
