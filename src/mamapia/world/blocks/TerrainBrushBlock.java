package mamapia.world.blocks;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mamapia.ui.ContentSelect;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Planets;
import mindustry.core.World;
import mindustry.game.Team;
import mindustry.game.Teams.TeamData;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.environment.OverlayFloor;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

import static mindustry.Vars.*;

public class TerrainBrushBlock extends Block{
    public static final byte modeFloor = 0, modeWall = 1, modeOre = 2;
    public static final int maxRadius = 6;
    public static final int range = 20;

    private static final ObjectSet<Item> allowedItems = ObjectSet.with(
        Items.copper, Items.lead, Items.coal, Items.titanium, Items.thorium, Items.scrap, Items.sporePod, Items.sand
    );

    private final Seq<Block> floorOptions = new Seq<>();
    private final Seq<Block> wallOptions = new Seq<>();
    private final Seq<Block> oreOptions = new Seq<>();

    private byte[] lastPackedConfig;

    public TerrainBrushBlock(String name){
        super(name);

        configurable = true;
        saveConfig = true;

        buildOptionLists();
        lastPackedConfig = pack(modeFloor, 0, Blocks.stone);

        config(String.class, (TerrainBrushBuild build, String value) -> {
            if(value == null) return;
            build.setMode(parseMode(value));
            updateLastPacked(build);
        });

        config(Integer.class, (TerrainBrushBuild build, Integer value) -> {
            if(value == null) return;
            build.setBrushRadius(value);
            updateLastPacked(build);
        });

        config(Block.class, (TerrainBrushBuild build, Block value) -> {
            if(value == null) return;
            build.setTarget(value);
            updateLastPacked(build);
        });

        config(byte[].class, (TerrainBrushBuild build, byte[] value) -> {
            if(value == null) return;
            build.applyPacked(value);
            updateLastPacked(build);
        });

        config(Point2.class, (TerrainBrushBuild build, Point2 value) -> {
            if(net.client() || value == null) return;
            build.applyAt(value.x, value.y);
        });
    }

    @Override
    public Object nextConfig(){
        return saveConfig ? lastPackedConfig : null;
    }

    @Override
    public void onPicked(Tile tile){
        if(tile != null && tile.build instanceof TerrainBrushBuild){
            lastPackedConfig = ((TerrainBrushBuild)tile.build).pack();
        }
    }

    private void updateLastPacked(TerrainBrushBuild build){
        lastPackedConfig = build.pack();
    }

    private void buildOptionLists(){
        floorOptions.addAll(
            Blocks.deepwater, Blocks.water, Blocks.taintedWater, Blocks.deepTaintedWater, Blocks.sandWater, Blocks.darksandWater, Blocks.darksandTaintedWater, Blocks.tar,
            Blocks.stone, Blocks.craters, Blocks.charr, Blocks.sand, Blocks.darksand, Blocks.dirt, Blocks.mud, Blocks.grass, Blocks.salt, Blocks.shale, Blocks.moss, Blocks.sporeMoss,
            Blocks.snow, Blocks.iceSnow, Blocks.ice, Blocks.basalt, Blocks.hotrock, Blocks.magmarock,
            Blocks.metalFloor, Blocks.metalFloorDamaged, Blocks.metalFloor2, Blocks.metalFloor3, Blocks.metalFloor4, Blocks.metalFloor5,
            Blocks.darkPanel1, Blocks.darkPanel2, Blocks.darkPanel3, Blocks.darkPanel4, Blocks.darkPanel5, Blocks.darkPanel6,
            Blocks.metalTiles1, Blocks.metalTiles2, Blocks.metalTiles3, Blocks.metalTiles4, Blocks.metalTiles5, Blocks.metalTiles6, Blocks.metalTiles7,
            Blocks.metalTiles8, Blocks.metalTiles9, Blocks.metalTiles10, Blocks.metalTiles11, Blocks.metalTiles12, Blocks.metalTiles13
        );

        wallOptions.addAll(
            Blocks.air,
            Blocks.stoneWall, Blocks.dirtWall, Blocks.sporeWall, Blocks.iceWall, Blocks.snowWall, Blocks.duneWall, Blocks.sandWall, Blocks.saltWall, Blocks.shrubs, Blocks.shaleWall,
            Blocks.sporePine, Blocks.snowPine, Blocks.pine, Blocks.whiteTree, Blocks.whiteTreeDead,
            Blocks.darkMetal, Blocks.metalWall1, Blocks.metalWall2, Blocks.metalWall3
        );

        oreOptions.addAll(
            Blocks.air,
            Blocks.oreCopper, Blocks.oreLead, Blocks.oreCoal, Blocks.oreTitanium, Blocks.oreThorium, Blocks.oreScrap
        );
    }

    private byte parseMode(String value){
        if(value == null) return modeFloor;
        String lower = value.toLowerCase();
        if("wall".equals(lower)) return modeWall;
        if("ore".equals(lower)) return modeOre;
        return modeFloor;
    }

    private Seq<Block> optionsFor(byte mode){
        if(mode == modeWall) return wallOptions;
        if(mode == modeOre) return oreOptions;
        return floorOptions;
    }

    private Block defaultTargetFor(byte mode){
        if(mode == modeWall) return Blocks.stoneWall;
        if(mode == modeOre) return Blocks.oreCopper;
        return Blocks.stone;
    }

    private boolean allowedTarget(byte mode, Block target){
        if(target == null) return false;
        return optionsFor(mode).contains(target);
    }

    private static Item costFor(byte mode, Block target){
        if(mode == modeOre){
            if(target instanceof OverlayFloor){
                OverlayFloor overlay = (OverlayFloor)target;
                if(overlay.itemDrop != null){
                    return overlay.itemDrop;
                }
            }
            return Items.copper;
        }

        String name = target == null ? "" : target.name;
        if(name.contains("metal") || name.contains("panel")) return Items.scrap;
        if(name.contains("spore") || name.contains("moss")) return Items.sporePod;
        if(name.contains("ice") || name.contains("snow")) return Items.titanium;
        if(name.contains("sand") || name.contains("salt") || name.contains("dune")) return Items.sand;
        return Items.copper;
    }

    private static byte[] pack(byte mode, int radius, Block target){
        int clamped = Mathf.clamp(radius, 0, maxRadius);
        short id = (short)(target == null ? 0 : target.id);
        return new byte[]{
            1,
            mode,
            (byte)clamped,
            (byte)((id >>> 8) & 0xFF),
            (byte)(id & 0xFF)
        };
    }

    public class TerrainBrushBuild extends Building{
        public byte mode = modeFloor;
        public int brushRadius = 0;
        public Block target = Blocks.stone;

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(mode);
            write.b((byte)brushRadius);
            write.s((short)(target == null ? 0 : target.id));
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            mode = read.b();
            brushRadius = Mathf.clamp(read.b(), 0, maxRadius);
            short id = read.s();
            target = Vars.content.block(id);
            sanitize();
        }

        @Override
        public Object config(){
            return pack();
        }

        public byte[] pack(){
            return TerrainBrushBlock.pack(mode, brushRadius, target);
        }

        public void applyPacked(byte[] value){
            if(value.length < 5 || value[0] != 1) return;
            byte newMode = value[1];
            int newRadius = value[2];
            int id = ((value[3] & 0xFF) << 8) | (value[4] & 0xFF);
            Block newTarget = Vars.content.block(id);

            mode = newMode;
            brushRadius = Mathf.clamp(newRadius, 0, maxRadius);
            target = newTarget;
            sanitize();
        }

        public void setMode(byte newMode){
            mode = newMode;
            sanitize();
        }

        public void setBrushRadius(int value){
            brushRadius = Mathf.clamp(value, 0, maxRadius);
        }

        public void setTarget(Block value){
            target = value;
            sanitize();
        }

        private void sanitize(){
            if(mode != modeFloor && mode != modeWall && mode != modeOre){
                mode = modeFloor;
            }

            if(!allowedTarget(mode, target)){
                target = defaultTargetFor(mode);
            }

            if(mode == modeFloor){
                if(!(target instanceof Floor) || target instanceof OverlayFloor){
                    target = defaultTargetFor(modeFloor);
                }
            }else if(mode == modeOre){
                if(target != Blocks.air && !(target instanceof OverlayFloor)){
                    target = defaultTargetFor(modeOre);
                }
            }else if(mode == modeWall){
                if(target != Blocks.air && target instanceof Floor){
                    target = defaultTargetFor(modeWall);
                }
            }
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return allowedItems.contains(item) && items.get(item) < itemCapacity;
        }

        @Override
        public void buildConfiguration(Table table){
            table.background(Styles.black6);
            table.margin(6f);
            table.defaults().left();

            table.table(top -> {
                top.right();
                top.add().growX();
                top.button(mindustry.gen.Icon.cancel, Styles.cleari, () -> Vars.control.input.config.hideConfig()).size(32f).right();
            }).growX().padBottom(4f).row();

            Table targetTable = new Table();
            Runnable[] rebuildTargets = {() -> {}};
            byte[] uiMode = {mode};

            table.table(modes -> {
                modes.left();
                modes.defaults().height(40f).minWidth(70f).padRight(6f);
                modes.button("Floor", Styles.clearTogglet, () -> {
                    setMode(modeFloor);
                    updateLastPacked(this);
                    configure("floor");
                    rebuildTargets[0].run();
                }).update(b -> b.setChecked(mode == modeFloor));

                modes.button("Wall", Styles.clearTogglet, () -> {
                    setMode(modeWall);
                    updateLastPacked(this);
                    configure("wall");
                    rebuildTargets[0].run();
                }).update(b -> b.setChecked(mode == modeWall));

                modes.button("Ore", Styles.clearTogglet, () -> {
                    setMode(modeOre);
                    updateLastPacked(this);
                    configure("ore");
                    rebuildTargets[0].run();
                }).update(b -> b.setChecked(mode == modeOre));
            }).growX().left().row();

            table.row();
            table.add(targetTable).growX().row();

            rebuildTargets[0] = () -> {
                targetTable.clear();
                targetTable.add("Target:").left().row();

                ContentSelect.buildTable(
                    targetTable,
                    optionsFor(mode),
                    () -> target,
                    value -> {
                        if(value != null){
                            setTarget(value);
                            updateLastPacked(this);
                            configure(value);
                        }
                    },
                    false,
                    10,
                    8
                );
            };

            rebuildTargets[0].run();

            table.row();
            table.label(() -> "Radius: " + brushRadius).left().row();
            Slider slider = new Slider(0, maxRadius, 1, false);
            slider.setValue(brushRadius);
            slider.changed(() -> {
                int val = (int)slider.getValue();
                if(val != brushRadius){
                    setBrushRadius(val);
                    updateLastPacked(this);
                    configure(val);
                }
            });
            table.add(slider).growX().row();

            table.row();
            table.add("Range: " + range).color(mindustry.graphics.Pal.accent).left().row();
            float infoWidth = Math.min(40f * 8f, Core.graphics.getWidth() * 0.9f);
            table.add("Tip: hover to preview; click on the map to apply.").wrap().width(infoWidth).left().padTop(4f).row();
            table.add("Preview: green=place, red=remove, dim=not enough items.").wrap().width(infoWidth).left().row();

            table.update(() -> {
                if(Core.input.keyTap(KeyCode.mouseRight)){
                    Vars.control.input.config.hideConfig();
                    return;
                }

                if(uiMode[0] != mode){
                    uiMode[0] = mode;
                    rebuildTargets[0].run();
                }

                if(!allowedTarget(mode, target) || (mode == modeFloor && (!(target instanceof Floor) || target instanceof OverlayFloor))){
                    sanitize();
                    rebuildTargets[0].run();
                }
            });
        }

        @Override
        public boolean onConfigureTapped(float x, float y){
            int tx = World.toTile(x);
            int ty = World.toTile(y);

            if(Mathf.dst2(tileX(), tileY(), tx, ty) > range * range){
                return false;
            }

            Tile tapped = world == null ? null : world.tile(tx, ty);
            //allow switching/closing by tapping other buildings; close when tapping the brush itself
            if(tapped != null && tapped.build != null){
                if(tapped.build == this){
                    Vars.control.input.config.hideConfig();
                    return true;
                }
                return false;
            }

            //do not use configure() here; it would overwrite lastConfig with a Point2
            Call.tileConfig(player, this, new Point2(tx, ty));
            return true;
        }

        @Override
        public void drawConfigure(){
            super.drawConfigure();

            if(world == null || state == null) return;
            if(control == null || control.input == null || control.input.config == null) return;
            if(!control.input.config.isShown() || control.input.config.getSelected() != this) return;

            //keep behavior consistent with applyAt()
            if(state.getPlanet() != null && state.getPlanet() != Planets.serpulo) return;

            float prevZ = Draw.z();
            Draw.z(Layer.overlayUI);

            Drawf.dashCircle(x, y, range * tilesize, Pal.accent);

            Tmp.v1.set(Core.input.mouseWorld());
            int tx = World.toTile(Tmp.v1.x);
            int ty = World.toTile(Tmp.v1.y);

            if(Mathf.dst2(tileX(), tileY(), tx, ty) > range * range){
                Draw.z(prevZ);
                return;
            }

            int r = Mathf.clamp(brushRadius, 0, maxRadius);
            Item cost = costFor(mode, target);
            if(cost == null) cost = Items.copper;

            int available = items.get(cost);
            int used = 0;

            for(int dx = -r; dx <= r; dx++){
                for(int dy = -r; dy <= r; dy++){
                    Tile t = world.tile(tx + dx, ty + dy);
                    if(t == null) continue;

                    //avoid previewing inside undiscovered static fog
                    if(state.rules.staticFog && state.rules.fog && !fogControl.isDiscovered(team, t.x, t.y)){
                        continue;
                    }

                    boolean editable = canEdit(t, team);

                    boolean change;
                    boolean placeGhost;
                    boolean placeMark;
                    boolean removeMark;

                    if(mode == modeFloor){
                        if(!(target instanceof Floor) || target instanceof OverlayFloor) continue;
                        Floor floor = (Floor)target;

                        boolean needsOverlayClear = !floor.supportsOverlay && t.overlay() != Blocks.air;
                        boolean floorChange = t.floor() != floor;

                        change = floorChange || needsOverlayClear;
                        placeGhost = floorChange;
                        placeMark = floorChange;
                        removeMark = needsOverlayClear;
                    }else if(mode == modeOre){
                        if(target != Blocks.air && !(target instanceof OverlayFloor)) continue;

                        if(target != Blocks.air && !t.floor().hasSurface()){
                            change = false;
                        }else{
                            change = t.overlay() != target;
                        }

                        placeGhost = change && target != Blocks.air;
                        placeMark = change && target != Blocks.air;
                        removeMark = change && target == Blocks.air;
                    }else if(mode == modeWall){
                        //avoid wiping player blocks; only place on empty, or allow removal
                        if(target != Blocks.air && t.block() != Blocks.air){
                            change = false;
                        }else{
                            change = t.block() != target;
                        }

                        placeGhost = change && target != Blocks.air;
                        placeMark = change && target != Blocks.air;
                        removeMark = change && target == Blocks.air;
                    }else{
                        continue;
                    }

                    if(!change) continue;

                    float ghostAlpha;
                    float lineAlpha;
                    Color placeColor;
                    Color removeColor;

                    if(!editable){
                        ghostAlpha = 0.20f;
                        lineAlpha = 0.60f;
                        placeColor = Pal.noplace;
                        removeColor = Pal.noplace;
                    }else{
                        boolean willApply = used < available;
                        used++;

                        if(willApply){
                            ghostAlpha = 0.65f;
                            lineAlpha = 1f;
                        }else{
                            ghostAlpha = 0.20f;
                            lineAlpha = 0.25f;
                        }

                        placeColor = Pal.place;
                        removeColor = Pal.remove;
                    }

                    if(placeGhost){
                        Draw.z(Layer.overlayUI);
                        Draw.color(Color.white);
                        Draw.alpha(ghostAlpha);
                        target.drawBase(t);
                        Draw.reset();
                    }

                    float markerSize = tilesize / 2f;
                    if(placeMark){
                        Drawf.square(t.worldx(), t.worldy(), markerSize, Tmp.c1.set(placeColor).a(lineAlpha));
                    }

                    if(removeMark){
                        Drawf.cross(t.worldx(), t.worldy(), markerSize, Tmp.c1.set(removeColor).a(lineAlpha));
                    }
                }
            }

            Draw.z(prevZ);
        }

        private void applyAt(int tx, int ty){
            if(state == null || world == null) return;

            //planet gate
            if(state.getPlanet() != null && state.getPlanet() != Planets.serpulo) return;

            if(Mathf.dst2(tileX(), tileY(), tx, ty) > range * range){
                return;
            }

            sanitize();

            Item cost = costFor(mode, target);
            if(cost == null) cost = Items.copper;

            int r = Mathf.clamp(brushRadius, 0, maxRadius);

            applyLoop:
            for(int dx = -r; dx <= r; dx++){
                for(int dy = -r; dy <= r; dy++){
                    int x = tx + dx;
                    int y = ty + dy;

                    Tile t = world.tile(x, y);
                    if(t == null) continue;

                    if(!canEdit(t, team)) continue;

                    if(mode == modeFloor){
                        if(!(target instanceof Floor) || target instanceof OverlayFloor) continue;
                        Floor floor = (Floor)target;

                        boolean needsOverlayClear = !floor.supportsOverlay && t.overlay() != Blocks.air;
                        boolean already = t.floor() == floor && !needsOverlayClear;
                        if(already) continue;

                        if(!items.has(cost, 1)) break applyLoop;
                        items.remove(cost, 1);

                        Block overlayToSet = floor.supportsOverlay ? t.overlay() : Blocks.air;
                        Call.setFloor(t, floor, overlayToSet);
                    }else if(mode == modeOre){
                        if(target != Blocks.air && !(target instanceof OverlayFloor)) continue;

                        if(target != Blocks.air && !t.floor().hasSurface()) continue;

                        if(t.overlay() == target) continue;

                        if(!items.has(cost, 1)) break applyLoop;
                        items.remove(cost, 1);

                        Call.setOverlay(t, target);
                    }else if(mode == modeWall){
                        //avoid wiping player blocks; only place on empty, or allow removal
                        if(target != Blocks.air && t.block() != Blocks.air) continue;

                        if(t.block() == target) continue;

                        if(!items.has(cost, 1)) break applyLoop;
                        items.remove(cost, 1);

                        Call.setTile(t, target, Team.derelict, 0);
                    }
                }
            }
        }

        private boolean canEdit(Tile t, Team team){
            if(t.build != null) return false;

            if(state.rules.staticFog && state.rules.fog){
                if(!fogControl.isDiscovered(team, t.x, t.y)){
                    return false;
                }
            }

            if(!t.interactable(team)){
                return false;
            }

            //core protection
            if(!state.rules.editor){
                float wx = t.x * tilesize;
                float wy = t.y * tilesize;

                if(state.rules.polygonCoreProtection){
                    float mindst = Float.MAX_VALUE;
                    CoreBuild closest = null;
                    for(TeamData data : state.teams.active){
                        for(CoreBuild core : data.cores){
                            float dst = core.dst2(wx, wy);
                            if(dst < mindst){
                                mindst = dst;
                                closest = core;
                            }
                        }
                    }
                    if(closest != null && closest.team != team){
                        return false;
                    }
                }else{
                    if(state.teams.anyEnemyCoresWithinBuildRadius(team, wx, wy)){
                        return false;
                    }
                }
            }

            return true;
        }
    }
}
