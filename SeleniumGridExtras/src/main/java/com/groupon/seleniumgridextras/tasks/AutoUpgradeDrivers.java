package com.groupon.seleniumgridextras.tasks;


import com.google.gson.JsonObject;

import com.groupon.seleniumgridextras.utilities.json.JsonResponseBuilder;
import com.groupon.seleniumgridextras.config.ConfigFileReader;
import com.groupon.seleniumgridextras.config.RuntimeConfig;

import org.apache.log4j.Logger;

import java.util.Map;

public class AutoUpgradeDrivers extends ExecuteOSTask {

  private static final String OLD_WEB_DRIVER_JAR = "old_web_driver_jar";
  private static final String OLD_CHROME_DRIVER = "old_chrome_driver";
  private static final String OLD_IE_DRIVER = "old_ie_driver";
  private static final String NEW_WEB_DRIVER_JAR = "new_web_driver_jar";
  private static final String NEW_CHROME_DRIVER = "new_chrome_driver";
  private static final String NEW_IE_DRIVER = "new_ie_driver";
  private static Logger logger = Logger.getLogger(AutoUpgradeDrivers.class);

  private boolean updateWebDriver = false;
  private boolean updateIEDriver = false;
  private boolean updateChromeDriver = false;


  public AutoUpgradeDrivers() {
    setEndpoint("/auto_upgrade_webdriver");
    setDescription(
        "Automatically checks the latest versions of all drivers and upgrades the current config to use them");
    setRequestType("GET");
    setResponseType("json");
    setClassname(this.getClass().getCanonicalName().toString());

    addResponseDescription(OLD_WEB_DRIVER_JAR, "Old version of WebDriver Jar");
    addResponseDescription(OLD_CHROME_DRIVER, "Old version of Chrome Driver");
    addResponseDescription(OLD_IE_DRIVER, "Old version of IE Driver");

    addResponseDescription(NEW_WEB_DRIVER_JAR, "New versions of WebDriver Jar");
    addResponseDescription(NEW_CHROME_DRIVER, "New version of Chrome Driver");
    addResponseDescription(NEW_IE_DRIVER, "New version of IE Driver");

    getJsonResponse()
        .addKeyValues(OLD_WEB_DRIVER_JAR, RuntimeConfig.getConfig().getWebdriver().getVersion());
    getJsonResponse().addKeyValues(OLD_CHROME_DRIVER,
                                   RuntimeConfig.getConfig().getChromeDriver().getVersion());
    getJsonResponse()
        .addKeyValues(OLD_IE_DRIVER, RuntimeConfig.getConfig().getIEdriver().getVersion());

    getJsonResponse()
        .addKeyValues(NEW_WEB_DRIVER_JAR, RuntimeConfig.getConfig().getWebdriver().getVersion());
    getJsonResponse().addKeyValues(NEW_CHROME_DRIVER,
                                   RuntimeConfig.getConfig().getChromeDriver().getVersion());
    getJsonResponse()
        .addKeyValues(NEW_IE_DRIVER, RuntimeConfig.getConfig().getIEdriver().getVersion());

  }


  @Override
  public JsonObject execute() {
    checkWhoNeedsUpdates();

    String genericUpdate = " needs to be updated to latest version of ";
    ConfigFileReader configOnDisk = new ConfigFileReader(RuntimeConfig.getConfigFile());
    Map configHash = configOnDisk.toHashMap();

    if (updateChromeDriver) {
      String
          newChromeDriverVersion =
          RuntimeConfig.getReleaseManager().getChromeDriverLatestVersion().getPrettyPrintVersion(
              ".");
      logger.info("Chrome Driver " + genericUpdate + " " + newChromeDriverVersion);
      RuntimeConfig.getConfig().getChromeDriver().setVersion(newChromeDriverVersion);

      updateVersionFor(configHash, "chromedriver", newChromeDriverVersion);
      getJsonResponse().addKeyValues(NEW_CHROME_DRIVER, newChromeDriverVersion);
    }


    if (updateWebDriver) {
      String
          newWebDriverVersion =
          RuntimeConfig.getReleaseManager().getWedriverLatestVersion().getPrettyPrintVersion(".");
      logger.info("WebDriver JAR " + genericUpdate + " " + newWebDriverVersion);
      RuntimeConfig.getConfig().getWebdriver().setVersion(newWebDriverVersion);
      updateVersionFor(configHash, "webdriver", newWebDriverVersion);
      getJsonResponse().addKeyValues(NEW_WEB_DRIVER_JAR, newWebDriverVersion);
    }

    if (updateIEDriver) {
      String
          newIEDriverVersion =
          RuntimeConfig.getReleaseManager().getIeDriverLatestVersion().getPrettyPrintVersion(".");
      logger.info("IE Driver " + genericUpdate + " " + newIEDriverVersion);
      RuntimeConfig.getConfig().getIEdriver().setVersion(newIEDriverVersion);
      updateVersionFor(configHash, "iedriver", newIEDriverVersion);
      getJsonResponse().addKeyValues(NEW_IE_DRIVER, newIEDriverVersion);
    }

    if (updateChromeDriver || updateIEDriver || updateWebDriver) {
      String
          message =
          "Update was detected for one or more versions of the drivers. You may need to restart Grid Extras for new versions to work";

      systemAndLog(message);
      getJsonResponse().addKeyValues(JsonResponseBuilder.OUT, message);

      logger.info(configOnDisk.toHashMap());
      configOnDisk.overwriteExistingConfig(configHash);
      logger.info(configOnDisk.toHashMap());

    }

    return getJsonResponse().getJson();
  }

  protected void updateVersionFor(Map inputMap, String driver, String version) {
    ((Map) ((Map) inputMap.get("theConfigMap")).get(driver)).put("version", version);
  }

  @Override
  public boolean initialize() {

    if (RuntimeConfig.getConfig().getAutoUpdateDrivers()) {
      try {
        execute();
      } catch (Exception e) {
        printInitilizedFailure();
        logger.error(e.toString());
        return false;
      }
    }

    printInitilizedSuccessAndRegisterWithAPI();
    return true;
  }

  private void checkWhoNeedsUpdates() {

    int
        currentChromeVersion =
        getComparableVersion(RuntimeConfig.getConfig().getChromeDriver().getVersion());
    int
        newestChromeVersion =
        RuntimeConfig.getReleaseManager().getChromeDriverLatestVersion().getComparableVersion();

    updateChromeDriver = currentChromeVersion < newestChromeVersion;

    int
        currentIEDriverVersion =
        getComparableVersion(RuntimeConfig.getConfig().getIEdriver().getVersion());
    int
        newestIEDriverVersion =
        RuntimeConfig.getReleaseManager().getIeDriverLatestVersion().getComparableVersion();

    updateIEDriver = currentIEDriverVersion < newestIEDriverVersion;

    int
        currentWebDriverJarVersion =
        getComparableVersion(RuntimeConfig.getConfig().getWebdriver().getVersion());
    int
        newestWebDriverJarVersion =
        RuntimeConfig.getReleaseManager().getWedriverLatestVersion().getComparableVersion();

    updateWebDriver = currentWebDriverJarVersion < newestWebDriverJarVersion;

  }

  private Integer getComparableVersion(String version) {
    return Integer.valueOf(version.replace(".", "0"));
  }


}
