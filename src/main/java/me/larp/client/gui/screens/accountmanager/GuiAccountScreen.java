// package me.larp.client.gui.screens.accountmanager;
//
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
// import java.io.BufferedWriter;
// import java.io.FileNotFoundException;
// import java.io.FileReader;
// import java.io.FileWriter;
// import java.io.IOException;
// import java.util.HashMap;
// import java.util.Random;
// import java.util.Scanner;
// import javax.swing.JFileChooser;
// import javax.swing.JFrame;
// import javax.swing.filechooser.FileNameExtensionFilter;
// import me.larp.client.core.Larp;
// import me.larp.client.gui.screens.accountmanager.Account;
// import me.larp.client.gui.screens.accountmanager.AccountException;
// import me.larp.client.gui.screens.accountmanager.GuiAccountAdd;
// import me.larp.client.gui.screens.accountmanager.GuiAccountSlot;
// import me.larp.client.gui.screens.accountmanager.GuiDirectLogin;
// import net.minecraft.client.Minecraft;
// import net.minecraft.client.gui.GuiGraphicsButton;
// import net.minecraft.client.gui.GuiGraphicsMainMenu;
// import net.minecraft.client.gui.GuiGraphicsScreen;
// import net.minecraft.client.gui.GuiGraphicsYesNo;
// import net.minecraft.client.gui.GuiGraphicsYesNoCallback;
// import net.minecraft.client.resources.ResourcePackRepository;
//
// public class GuiAccountScreen
// extends GuiScreen
// implements GuiYesNoCallback {
//    private String dispErrorString = "";
//    private boolean deleteMenuOpen = false;
//    private GuiAccountSlot accountSlot;
//    private int timer = 0;
//    private Minecraft mc = Minecraft.getInstance();
//
//    @Override
//    public void handleMouseInput() throws IOException {
//        super.handleMouseInput();
//        this.accountSlot.func_178039_p();
//    }
//
//    @Override
//    public void initGui() {
//        this.buttonList.clear();
//        this.buttonList.add(new GuiButton(2, this.width / 2 - 76, this.height - 48, 73, 20,
// "Login"));
//        this.buttonList.add(new GuiButton(5, this.width / 2, this.height - 48, 73, 20, "Direct
// Login"));
//        this.buttonList.add(new GuiButton(1, this.width / 2 - 154, this.height - 48, 73, 20,
// "Add"));
//        this.buttonList.add(new GuiButton(3, this.width / 2 + 78, this.height - 48, 73, 20,
// "Remove"));
//        this.buttonList.add(new GuiButton(4, this.width / 2 - 76, this.height - 26, 149, 20,
// "Back"));
//        this.buttonList.add(new GuiButton(6, this.width / 2 - 154, this.height - 26, 73, 20,
// "Random"));
//        this.buttonList.add(new GuiButton(7, this.width / 2 + 78, this.height - 26, 73, 20,
// "Import"));
//        this.accountSlot = new GuiAccountSlot(this);
//    }
//
//    @Override
//    public void confirmClicked(boolean result, int id) {
//        super.confirmClicked(result, id);
//        if (this.deleteMenuOpen) {
//            this.deleteMenuOpen = false;
//            if (result) {
//                Larp.getInstance().getAccountManager().getRegistry().remove(id);
//            }
//            this.mc.displayGuiScreen(this);
//        }
//    }
//
//    @Override
//    protected void keyTyped(char typedChar, int keyCode) {
//        if (keyCode == 200) {
//            --this.accountSlot.selected;
//        }
//        if (keyCode == 208) {
//            ++this.accountSlot.selected;
//        }
//        if (keyCode == 28) {
//            Account account =
// (Account)Larp.getInstance().getAccountManager().getRegistry().get(this.accountSlot.selected);
//            try {
//                Minecraft.getInstance().processLogin(account.getLabel(), account.getPassword());
//            }
//            catch (AccountException exception) {
//                exception.printStackTrace();
//                ResourcePackRepository
//            }
//        }
//    }
//
//    @Override
//    public void actionPerformed(GuiButton guiButton) {
//        block21: {
//            try {
//                super.actionPerformed(guiButton);
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//            if (guiButton.id == 1) {
//                GuiAccountAdd gaa = new GuiAccountAdd();
//                this.mc.displayGuiScreen(gaa);
//            }
//            if (guiButton.id == 2) {
//                try {
//                    Account account =
// (Account)Larp.getInstance().getAccountManager().getRegistry().get(this.accountSlot.getSelected());
//                    if (account.isPremium()) {
//                        try {
//                            HashMap<String, Object> map = new HashMap<String, Object>(3, 1.0f);
//                            map.put("user", account.getLabel());
//                            map.put("password", account.getPassword());
//                            map.put("version", 13);
//                            Minecraft.getInstance().processLogin(account.getLabel(),
// account.getPassword());
//                        }
//                        catch (Exception exception) {
//                            Larp.getInstance().getAccountManager().unregister(account);
//                        }
//                        break block21;
//                    }
//                    return;
//                }
//                catch (Exception exception) {
//                    exception.printStackTrace();
//                }
//            }
//        }
//        if (guiButton.id == 3) {
//            try {
//                String s1 = "Are you sure you want to delete the alt \"" +
// ((Account)Larp.getInstance().getAccountManager().getRegistry().get(this.accountSlot.getSelected())).getLabel() + "\"" + "?";
//                String s3 = "Delete";
//                String s4 = "Cancel";
//                GuiYesNo guiyesno = new GuiYesNo(this, s1, "", s3, s4,
// this.accountSlot.getSelected());
//                this.deleteMenuOpen = true;
//                this.mc.displayGuiScreen(guiyesno);
//            }
//            catch (Exception exception) {
//                exception.printStackTrace();
//            }
//        }
//        if (guiButton.id == 4) {
//            this.mc.displayGuiScreen(new GuiMainMenu());
//        }
//        if (guiButton.id == 5) {
//            GuiDirectLogin gdl = new GuiDirectLogin(this);
//            this.mc.displayGuiScreen(gdl);
//        }
//        if (guiButton.id == 6) {
//            Random random = new Random();
//            Account a1 =
// (Account)Larp.getInstance().getAccountManager().getRegistry().get(random.nextInt(Larp.getInstance().getAccountManager().getRegistry().size()));
//            try {
//                if (a1.isPremium()) {
//                    try {
//                        HashMap<String, Object> map = new HashMap<String, Object>(3, 1.0f);
//                        map.put("user", a1.getLabel());
//                        map.put("password", a1.getPassword());
//                        map.put("version", 13);
//                        Minecraft.getInstance().processLogin(a1.getLabel(), a1.getPassword());
//                    }
//                    catch (Exception exception) {
//                        exception.printStackTrace();
//                    }
//                }
//            }
//            catch (Exception exception) {
//                exception.printStackTrace();
//            }
//        }
//        if (guiButton.id == 7) {
//            this.importAlts();
//        }
//    }
//
//    private void importAlts() {
//        final JFileChooser chooser = new JFileChooser();
//        chooser.setVisible(true);
//        chooser.setSize(500, 400);
//        chooser.setAcceptAllFileFilterUsed(false);
//        chooser.setFileFilter(new FileNameExtensionFilter("File", "txt"));
//        final JFrame frame = new JFrame("Select a file");
//        chooser.addActionListener(new ActionListener(){
//
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                if (e.getActionCommand().equals("ApproveSelection") && chooser.getSelectedFile()
// != null) {
//                    try {
//                        Scanner scanner = new Scanner(new FileReader(chooser.getSelectedFile()));
//                        scanner.useDelimiter("\n");
//                        while (scanner.hasNext()) {
//                            String[] split = scanner.next().trim().split(":");
//                            Larp.getInstance().getAccountManager().getRegistry().add(new
// Account(split[0], split[1]));
//                        }
//                        scanner.close();
//                    }
//                    catch (FileNotFoundException e1) {
//                        e1.printStackTrace();
//                    }
//                    try {
//                        StringBuilder data = new StringBuilder();
//                        for (Account alt : Larp.getInstance().getAccountManager().getRegistry())
// {
//                            data.append(alt.getFileLine() + "\n");
//                        }
//                        BufferedWriter writer = new BufferedWriter(new
// FileWriter(Larp.getInstance().getDirectory() + "/accounts.txt"));
//                        writer.write(data.toString());
//                        writer.close();
//                    }
//                    catch (Exception localException) {
//                        localException.printStackTrace();
//                    }
//                    frame.setVisible(false);
//                    frame.dispose();
//                }
//                if (e.getActionCommand().equals("CancelSelection")) {
//                    frame.setVisible(false);
//                    frame.dispose();
//                }
//            }
//        });
//        frame.setAlwaysOnTop(true);
//        frame.add(chooser);
//        frame.setVisible(true);
//        frame.setSize(750, 600);
//    }
//
//    @Override
//    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
//        this.accountSlot.drawScreen(mouseX, mouseY, partialTicks);
//
// this.mc.fontRenderer.drawStringWithShadow(Minecraft.getInstance().getSession().getUsername(),
// this.width -
// this.mc.fontRenderer.getStringWidth(Minecraft.getInstance().getSession().getUsername()) - 2,
// 2.0f, 0xA0A0A0);
//        this.mc.fontRenderer.drawStringWithShadow("Accounts: " +
// Larp.getInstance().getAccountManager().getRegistry().size(), 2.0f, 2.0f, 0xA0A0A0);
//        if (this.dispErrorString.length() > 1) {
//            ++this.timer;
//            if (this.timer > 100) {
//                this.dispErrorString = "";
//                this.timer = 0;
//            }
//        }
//        super.drawScreen(mouseX, mouseY, partialTicks);
//    }
// }
//
