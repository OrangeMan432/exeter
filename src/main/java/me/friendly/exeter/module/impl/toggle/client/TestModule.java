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
    testInfo.setDescription("Sends a sample info message to test debug output.");
    testWarning.setDescription("Sends a sample warning message to test debug output.");
    testError.setDescription("Sends a sample error message to test debug output.");
    testMultiLine.setDescription("Sends a three-line sample message to test debug output.");
    testAllModules.setDescription("Sends sample messages from several module names.");
    clearLog.setDescription("Clears the saved debug log file.");

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
      DebugLogger.get()
          .log("TestModule", DebugLogger.Level.INFO, "This is an info-level test message.");
    }

    if (testWarning.getValue()) {
      testWarning.setValue(false);
      DebugLogger.get()
          .log("TestModule", DebugLogger.Level.WARN, "This is a warning test message.");
    }

    if (testError.getValue()) {
      testError.setValue(false);
      DebugLogger.get()
          .log("TestModule", DebugLogger.Level.ERROR, "This is an error test message.");
    }

    if (testMultiLine.getValue()) {
      testMultiLine.setValue(false);
      DebugLogger.get().log("TestModule", DebugLogger.Level.INFO, "Line 1 of multi-line message.");
      DebugLogger.get().log("TestModule", DebugLogger.Level.INFO, "Line 2 of multi-line message.");
      DebugLogger.get().log("TestModule", DebugLogger.Level.INFO, "Line 3 of multi-line message.");
    }

    if (testAllModules.getValue()) {
      testAllModules.setValue(false);
      for (String label :
          new String[] {
            "AutoCart", "BedAura", "AutoPot", "Speed", "Velocity", "SelfBed", "Config"
          }) {
        DebugLogger.get().log(label, DebugLogger.Level.INFO, "Ping from TestModule for " + label);
      }
    }

    if (clearLog.getValue()) {
      clearLog.setValue(false);
      DebugLogger.get().clearLog();
      DebugLogger.get().log("TestModule", DebugLogger.Level.INFO, "Log file cleared.");
    }
  }
}
