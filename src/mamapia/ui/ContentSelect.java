package mamapia.ui;

import arc.Core;
import arc.func.Cons;
import arc.func.Prov;
import arc.math.Mathf;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.ImageButton;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.ui.Styles;
import mindustry.world.Block;

public class ContentSelect{
    private static TextField search;
    private static int rowCount;

    public static void buildTable(Table table, Seq<Block> options, Prov<Block> holder, Cons<Block> consumer, boolean closeSelect, int rows, int columns){
        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        group.setMinCheckCount(0);

        Table cont = new Table().top();
        cont.defaults().size(40f);

        if(search != null) search.clearText();

        Runnable rebuild = () -> {
            group.clear();
            cont.clearChildren();

            String text = search != null ? search.getText() : "";
            int i = 0;
            rowCount = 0;

            Seq<Block> list = options;
            if(text != null && !text.isEmpty()){
                String lower = text.toLowerCase();
                list = options.select(b -> b.localizedName != null && b.localizedName.toLowerCase().contains(lower));
            }

            for(Block option : list){
                ImageButton button = cont.button(mindustry.gen.Tex.whiteui, Styles.clearNoneTogglei, Mathf.clamp(option.selectionSize, 0f, 40f), () -> {
                    if(closeSelect) mindustry.Vars.control.input.config.hideConfig();
                }).tooltip(option.localizedName).group(group).get();
                button.changed(() -> consumer.get(button.isChecked() ? option : null));
                button.getStyle().imageUp = new TextureRegionDrawable(option.uiIcon);
                button.update(() -> button.setChecked(holder.get() == option));

                if(i++ % columns == (columns - 1)){
                    cont.row();
                    rowCount++;
                }
            }
        };

        rebuild.run();

        Table main = new Table().background(Styles.black6);
        if(rowCount > rows * 1.5f){
            main.table(s -> {
                s.image(mindustry.gen.Icon.zoom).padLeft(4f);
                search = s.field(null, text -> rebuild.run()).padBottom(4f).left().growX().get();
                search.setMessageText(Core.bundle.get("players.search"));
            }).fillX().row();
        }

        ScrollPane pane = new ScrollPane(cont, Styles.smallPane);
        pane.setScrollingDisabled(true, false);
        pane.exited(() -> {
            if(pane.hasScroll()){
                Core.scene.setScrollFocus(null);
            }
        });
        pane.setOverscroll(false, false);
        main.add(pane).maxHeight(40f * rows);

        table.top().add(main);
    }
}

