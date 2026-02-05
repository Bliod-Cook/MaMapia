package mamapia.content;

import arc.Events;
import arc.util.Log;
import mamapia.world.blocks.TerrainBrushBlock;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Planets;
import mindustry.content.TechTree;
import mindustry.game.EventType.ContentInitEvent;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.meta.BuildVisibility;

public class MaMapiaBlocks{
    public static TerrainBrushBlock terrainBrush;

    public static void load(){
        terrainBrush = new TerrainBrushBlock("mamapia-terrain-brush");
        terrainBrush.buildVisibility = BuildVisibility.shown;
        terrainBrush.category = Category.effect;
        terrainBrush.size = 2;
        terrainBrush.update = true;
        terrainBrush.destructible = true;
        terrainBrush.hasItems = true;
        terrainBrush.itemCapacity = 64000;
        terrainBrush.configurable = true;
        terrainBrush.saveConfig = true;
        terrainBrush.requirements = ItemStack.with(
            Items.copper, 150,
            Items.lead, 150,
            Items.graphite, 100,
            Items.silicon, 100
        );
        terrainBrush.shownPlanets.add(Planets.serpulo);

        Events.on(ContentInitEvent.class, event -> {
            TechTree.TechNode parent = Blocks.siliconSmelter == null ? null : Blocks.siliconSmelter.techNode;
            if(parent == null){
                Log.warn("[MaMapia] Tech tree parent not found; terrain brush will not appear in tech tree.");
                return;
            }

            new TechTree.TechNode(parent, terrainBrush, terrainBrush.researchRequirements());
        });
    }
}
