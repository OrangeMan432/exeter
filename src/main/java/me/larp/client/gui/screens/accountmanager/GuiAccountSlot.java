// package me.larp.client.gui.screens.accountmanager;
//
// import me.larp.client.core.Larp;
// import me.larp.client.gui.screens.accountmanager.Account;
// import me.larp.client.gui.screens.accountmanager.AccountException;
// import me.larp.client.gui.screens.accountmanager.GuiAccountScreen;
// import net.minecraft.client.Minecraft;
// import net.minecraft.client.gui.GuiGraphicsSlot;
//
// public class GuiAccountSlot
// extends GuiSlot {
//    private GuiAccountScreen guiAccountScreen;
//    int selected;
//
//    public GuiAccountSlot(GuiAccountScreen aList) {
//        super(Minecraft.getInstance(), aList.width, aList.height, 32, aList.height - 60, 27);
//        this.guiAccountScreen = aList;
//        this.selected = 0;
//    }
//
//    @Override
//    protected int getContentHeight() {
//        return this.getSize() * 40;
//    }
//
//    @Override
//    protected int getSize() {
//        return Larp.getInstance().getAccountManager().getRegistry().size();
//    }
//
//    @Override
//    protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
//        this.selected = slotIndex;
//        if (isDoubleClick) {
//            Account account =
// (Account)Larp.getInstance().getAccountManager().getRegistry().get(slotIndex);
//            try {
//                Minecraft.getInstance().processLogin(account.getLabel(), account.getPassword());
//            }
//            catch (AccountException exception) {
//                exception.printStackTrace();
//            }
//        }
//    }
//
//    @Override
//    protected boolean isSelected(int slotIndex) {
//        return this.selected == slotIndex;
//    }
//
//    protected int getSelected() {
//        return this.selected;
//    }
//
//    @Override
//    protected void drawBackground() {
//        this.guiAccountScreen.drawDefaultBackground();
//    }
//
//    @Override
//    protected void drawSlot(int selectedIndex, int x, int y, int var5, int var6, int var7) {
//        try {
//            Account account =
// (Account)Larp.getInstance().getAccountManager().getRegistry().get(selectedIndex);
//
// this.mc.fontRenderer.drawCenteredString(((Account)Larp.getInstance().getAccountManager().getRegistry().get(selectedIndex)).getLabel(), this.mc.displayWidth / 2, y + 2, -5592406, true);
//            this.mc.fontRenderer.drawCenteredString(account.isPremium() ?
// account.getPassword().replaceAll("(?s).", "*") : "Not Available", this.mc.displayWidth / 2, y +
// 15, -5592406, true);
//        }
//        catch (AccountException exception) {
//            exception.printStackTrace();
//        }
//    }
// }
//
