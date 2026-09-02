package me.friendly.exeter.module.impl.toggle.client;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;

public class TestModule extends ToggleableModule {

  private final Property<Boolean> testInfo = new Property<Boolean>(false, "Send Info Message");
  private final Property<Boolean> testWarning =
      new Property<Boolean>(false, "Send Warning Message");
  private final Property<Boolean> testError = new Property<Boolean>(false, "Send Error Message");
  private final Property<Boolean> testMultiLine =
      new Property<Boolean>(false, "Send Multi-line Message");
  private final Property<Boolean> testAllModules =
      new Property<Boolean>(false, "Send To All Modules");
  private final Property<Boolean> clearLog = new Property<Boolean>(false, "Clear Log File");

  public TestModule() {
    super("TestModule", new String[] {"testmodule", "test"}, 0xFFAA00, ModuleType.CLIENT);
    setDescription("Sends test debug messages for verifying the Debug system.");
    offerProperties(testInfo, testWarning, testError, testMultiLine, testAllModules, clearLog);

    this.listeners.add(
        new Listener<TickEvent>("test_module_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            TestModule.this.onTick();
          }
        });
  }

  private void onTick() {
    if (testInfo.getValue()) {
      testInfo.setValue(false);
      sendTestChat("This is an info-level test message.");
      DebugLogger.get().log("TestModule", "This is an info-level test message.");
    }

    if (testWarning.getValue()) {
      testWarning.setValue(false);
      sendTestChat("\u00a7e[WARNING] This is a warning test message.");
      DebugLogger.get().log("TestModule", "[WARNING] This is a warning test message.");
    }

    if (testError.getValue()) {
      testError.setValue(false);
      sendTestChat("\u00a7c[ERROR] This is an error test message.");
      DebugLogger.get().log("TestModule", "[ERROR] This is an error test message.");
    }

    if (testMultiLine.getValue()) {
      testMultiLine.setValue(false);
      sendTestChat("Line 1 of multi-line message.");
      sendTestChat("Line 2 of multi-line message.");
      sendTestChat("Line 3 of multi-line message.");
      DebugLogger.get().log("TestModule", "Line 1 of multi-line message.");
      DebugLogger.get().log("TestModule", "Line 2 of multi-line message.");
      DebugLogger.get().log("TestModule", "Line 3 of multi-line message.");
    }

    if (testAllModules.getValue()) {
      testAllModules.setValue(false);
      for (String label :
          new String[] {
            "AutoCart", "BedAura", "AutoPot", "Speed", "Velocity", "SelfBed", "Config"
          }) {
        sendTestChat("Ping from TestModule for " + label);
        DebugLogger.get().log(label, "Ping from TestModule for " + label);
      }
    }

    if (clearLog.getValue()) {
      clearLog.setValue(false);
      DebugLogger.get().clearLog();
      sendTestChat("Log file cleared.");
      DebugLogger.get().log("TestModule", "Log file cleared.");
    }
  }

  private static void sendTestChat(String message) {
    try {
      net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
      if (mc.player != null) {
        mc.player.sendSystemMessage(
            net.minecraft.network.chat.Component.literal(
                "\u00a77[\u00a76Exeter Test\u00a77] \u00a7f" + message));
      }
    } catch (Exception e) {
      // player not available
    }
  }
}
