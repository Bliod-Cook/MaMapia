package mamapia;

import mamapia.content.MaMapiaBlocks;
import mindustry.mod.Mod;

public class MaMapiaMod extends Mod{

    @Override
    public void loadContent(){
        MaMapiaBlocks.load();
    }
}

