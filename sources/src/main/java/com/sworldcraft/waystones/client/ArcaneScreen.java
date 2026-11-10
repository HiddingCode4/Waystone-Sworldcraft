package com.sworldcraft.waystones.client;

import com.sworldcraft.waystones.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.entity.player.Inventory;
import java.util.function.BooleanSupplier;

/** Deliberately dim, stone-and-gold UI; all geometry is native GUI drawing. */
public final class ArcaneScreen extends AbstractContainerScreen<ArcaneMenu> {
    private static final int GOLD=0xFFB69A62, CYAN=0xFF73D8D5, INK=0xFF10151D, MUTED=0xFF8C9AA6;
    private EditBox field;
    private ArcaneButton confirm;
    private int selected=-1;
    public ArcaneScreen(ArcaneMenu menu,Inventory inv,Component title) { super(menu,inv,title,320,236); }
    private ArcaneMenu.Snapshot data() { return menu.snapshot; }
    @Override protected void init() {
        super.init();
        addRenderableWidget(new ArcaneButton(leftPos+286,topPos+10,22,18,"X",b->onClose()));
        if(data().naming()) { initName();return; }
        field=new EditBox(font,leftPos+17,topPos+49,241,12,Component.literal("Rechercher une destination"));
        field.setBordered(false);field.setTextColor(0xD4E1E4);field.setMaxLength(64);field.setHint(Component.literal("Rechercher une destination..."));
        field.setValue(data().query());addRenderableWidget(field);
        addRenderableWidget(new ArcaneButton(leftPos+268,topPos+44,40,20,"OK",b->search(0)));
        for(int i=0;i<data().destinations().size();i++) {
            final int index=i;var d=data().destinations().get(i);
            var row=new ArcaneButton(leftPos+12,topPos+72+i*20,296,18,d.name(),b->{selected=index;confirm.active=true;},()->selected==index,true);
            row.setTooltip(Tooltip.create(Component.literal(d.name()+"\n"+dimension(d.dimension())+" · "+d.x()+", "+d.y()+", "+d.z())));
            addRenderableWidget(row);
        }
        confirm=addRenderableWidget(new ArcaneButton(leftPos+12,topPos+196,120,22,"Se téléporter",b->{if(selected>=0)button(selected);}));
        confirm.active=selected>=0;
        var rename=addRenderableWidget(new ArcaneButton(leftPos+136,topPos+196,78,22,"Renommer",b->button(-1)));
        rename.active=data().owner();rename.setTooltip(Tooltip.create(Component.literal(data().owner()?"Renommer cette pierre":"Seul le propriétaire peut renommer cette pierre")));
        var refresh=addRenderableWidget(new ArcaneButton(leftPos+218,topPos+196,24,22,"R",b->button(-2)));
        refresh.setTooltip(Tooltip.create(Component.literal("Actualiser les destinations")));
        var prev=addRenderableWidget(new ArcaneButton(leftPos+246,topPos+196,24,22,"<",b->search(data().page()-1)));prev.active=data().page()>0;
        var next=addRenderableWidget(new ArcaneButton(leftPos+274,topPos+196,24,22,">",b->search(data().page()+1)));next.active=data().page()+1<data().pages();
    }
    private void initName() {
        field=new EditBox(font,leftPos+24,topPos+102,272,14,Component.literal("Nom de la Waystone"));
        field.setBordered(false);field.setTextColor(0xD4E1E4);field.setMaxLength(32);field.setValue(data().sourceName());
        addRenderableWidget(field);
        confirm=addRenderableWidget(new ArcaneButton(leftPos+24,topPos+148,170,24,"Graver le nom",b->saveName()));
        confirm.active=!field.getValue().isBlank();field.setResponder(s->confirm.active=!s.isBlank());
        addRenderableWidget(new ArcaneButton(leftPos+202,topPos+148,94,24,"Retour",b->button(-2)));
        setInitialFocus(field);field.setHighlightPos(0);
    }
    private void button(int id) { if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id); }
    private void action(int type,String text,int page) {
        if(minecraft.player!=null)minecraft.player.connection.send(new ServerboundCustomPayloadPacket(new ArcaneAction(menu.containerId,type,text,page)));
    }
    private void search(int page) { action(0,field.getValue(),page); }
    private void saveName() { if(!field.getValue().isBlank())action(1,field.getValue(),0); }
    @Override public boolean keyPressed(KeyEvent event) {
        if(field!=null && field.isFocused()) {
            if(event.key()==257 || event.key()==335) { if(data().naming())saveName();else search(0);return true; }
            // Prevent inventory hotkeys from closing the menu while typing a name/search.
            if(!event.isEscape())return field.keyPressed(event);
        }
        return super.keyPressed(event);
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy) {
        if(!data().naming() && sy!=0 && x>=leftPos+12 && x<=leftPos+308 && y>=topPos+72 && y<=topPos+172) {
            int page=data().page()+(sy<0?1:-1);
            if(page>=0 && page<data().pages())search(page);
            return true;
        }
        return super.mouseScrolled(x,y,sx,sy);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my) { /* No inventory or chest labels. */ }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial) {
        super.extractBackground(g,mx,my,partial);
        int x=leftPos,y=topPos;
        g.fill(x-3,y-3,x+323,y+239,0xAA05080D);
        g.fill(x,y,x+320,y+236,INK);g.fillGradient(x+2,y+2,x+318,y+234,0xFF252B35,0xFF10151C);
        // Stone joints and restrained violet inlays.
        for(int row=0;row<12;row++) {
            int yy=y+4+row*20;g.fill(x+4,yy,x+316,yy+1,0xFF2B303A);
            for(int col=0;col<4;col++){int xx=x+20+col*83+(row%2)*25;if(xx<x+315)g.fill(xx,yy,xx+1,Math.min(yy+20,y+232),0xFF171C25);}
        }
        g.outline(x,y,320,236,GOLD);g.outline(x+3,y+3,314,230,0xFF554C3B);
        for(int xx:new int[]{x+7,x+306})for(int yy:new int[]{y+7,y+220})rune(g,xx,yy,0xFF9B82B5);
        g.text(font,"SANCTUAIRE DES PASSAGES",x+22,y+12,GOLD);
        g.text(font,font.plainSubstrByWidth("Depuis : "+data().sourceName(),266),x+22,y+29,MUTED);
        if(data().naming()) {
            g.centeredText(font,"NOMMER LA WAYSTONE",x+160,y+68,CYAN);
            frame(g,x+20,y+94,280,30,0xFF53666A);
            g.text(font,"32 caractères maximum",x+24,y+130,MUTED);
            g.centeredText(font,"Ce nom sera visible par ses visiteurs.",x+160,y+187,MUTED);
            return;
        }
        frame(g,x+12,y+44,250,20,0xFF53666A);
        if(data().destinations().isEmpty()) {
            g.centeredText(font,data().knownCount()==0?"Aucune autre pierre découverte":"Aucun résultat pour cette recherche",x+160,y+108,MUTED);
            g.centeredText(font,"Découvre une pierre en cliquant dessus.",x+160,y+126,0xFF74818E);
        }
        String info="Choisis une destination découverte";
        if(selected>=0 && selected<data().destinations().size()) {
            var d=data().destinations().get(selected);info=dimension(d.dimension())+" · "+d.x()+", "+d.y()+", "+d.z();
        }
        g.text(font,font.plainSubstrByWidth(info,292),x+14,y+178,selected>=0?CYAN:MUTED);
        g.centeredText(font,data().knownCount()+" destinations · Page "+(data().page()+1)+" / "+data().pages(),x+160,y+223,MUTED);
    }
    private static String dimension(String id) {
        return switch(id) { case "minecraft:overworld"->"Surface";case "minecraft:the_nether"->"Nether";case "minecraft:the_end"->"End";default->id; };
    }
    private static void frame(GuiGraphicsExtractor g,int x,int y,int w,int h,int border) {
        g.fill(x,y,x+w,y+h,0xFF0D131A);g.outline(x,y,w,h,border);
    }
    private static void rune(GuiGraphicsExtractor g,int x,int y,int color) {
        g.fill(x+2,y,x+3,y+10,color);g.fill(x,y+2,x+6,y+3,color);g.fill(x+2,y+6,x+6,y+7,color);g.fill(x+5,y+4,x+6,y+9,color);
    }
    private static final class ArcaneButton extends Button {
        private final BooleanSupplier selected;
        private final boolean row;
        ArcaneButton(int x,int y,int w,int h,String text,OnPress action) { this(x,y,w,h,text,action,()->false,false); }
        ArcaneButton(int x,int y,int w,int h,String text,OnPress action,BooleanSupplier selected,boolean row) {
            super(x,y,w,h,Component.literal(text),action,DEFAULT_NARRATION);this.selected=selected;this.row=row;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float partial) {
            boolean lit=active && (isHoveredOrFocused()||selected.getAsBoolean());
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();
            g.fillGradient(x,y,x+w,y+h,lit?0xFF2A4248:0xFF292F38,lit?0xFF172A33:0xFF181D26);
            g.outline(x,y,w,h,lit?CYAN:(active?0xFF80704F:0xFF3E4249));
            g.fill(x+2,y+1,x+w-2,y+2,lit?0xFF62928D:0xFF3C424A);
            var font=Minecraft.getInstance().font;
            int color=!active?0xFF616C76:(lit?0xFFB6F4EA:0xFFD7C9AB);
            String text=font.plainSubstrByWidth(getMessage().getString(),w-(row?30:8));
            if(row){rune(g,x+8,y+4,selected.getAsBoolean()?CYAN:0xFF83709F);g.text(font,text,x+23,y+(h-8)/2,color);}
            else g.centeredText(font,text,x+w/2,y+(h-8)/2,color);
        }
    }
}
