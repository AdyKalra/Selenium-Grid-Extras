/**
 * Copyright (c) 2013, Groupon, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * Neither the name of GROUPON nor the names of its contributors may be
 * used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 * Created with IntelliJ IDEA.
 * User: Dima Kovalenko (@dimacus) && Darko Marinov
 * Date: 5/10/13
 * Time: 4:06 PM
 */
package com.groupon.seleniumgridextras.config;


import com.groupon.seleniumgridextras.browser.BrowserVersionDetector;
import com.groupon.seleniumgridextras.config.capabilities.Capability;
import com.groupon.seleniumgridextras.config.remote.ConfigPusher;
import com.groupon.seleniumgridextras.downloader.webdriverreleasemanager.WebDriverReleaseManager;
import com.groupon.seleniumgridextras.os.GridPlatform;
import com.groupon.seleniumgridextras.utilities.FileIOUtility;
import com.groupon.seleniumgridextras.OS;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FirstTimeRunConfig {

  private static Logger logger = Logger.getLogger(FirstTimeRunConfig.class);

  public static Config customiseConfig(Config defaultConfig) {
    final
    String
        message =
        "We noticed this is a first time running, we will ask some configuration settings";
    logger.info(message);
    System.out.println("\n\n\n\n" + message + "\n\n");

    setDefaultService(defaultConfig);

    String hubHost = getGridHubHost();
    String hubPort = getGridHubPort();

    if (defaultConfig.getDefaultRole().equals("hub")) {
      configureHub(hubHost, hubPort, defaultConfig);
    }

    List<Capability> caps = getCapabilitiesFromUser(defaultConfig);

    if (defaultConfig.getAutoStartNode()) {
      configureNodes(caps, hubHost, hubPort, defaultConfig);

      List<Capability> appiumCaps = getAppiumCapabilitiesFromUser(defaultConfig);

      if (appiumCaps.size() > 0) {
        String appiumStartCommand = getAppiumStartCommand();

        configureAppiumNodes(appiumCaps, hubHost, hubPort, appiumStartCommand, defaultConfig);
      }
    }

    setRebootAfterSessionLimit(defaultConfig);

    setDriverAutoUpdater(defaultConfig);

    if (defaultConfig.getAutoStartNode()) {
      askToRecordVideo(defaultConfig);
    }

    final
    String
        thankYouMessage =
        "Thank you, your answers were recorded to '" + RuntimeConfig.getConfigFile() + "'\n\n"
        + "You can modify this file directly to tweak more options";
    logger.info(thankYouMessage);
    System.out.println(thankYouMessage);

    defaultConfig.writeToDisk(RuntimeConfig.getConfigFile());
    if (!defaultConfig.getAutoStartHub()) { //For now let's not store hub's config in central repo
      askToStoreConfigsOnHub(defaultConfig, hubHost);
    }

    return defaultConfig;
  }

  private static void askToRecordVideo(Config defaultConfig) {
    String answer = askQuestion("Should this Node record test runs? (1-yes/0-no)", "1");

    if (answer.equals("1")) {
      logger.info("Setting node to record videos");

      answer =
          askQuestion("How many videos should node keep in threads?",
                      String.valueOf(DefaultConfig.VIDEOS_TO_KEEP));

      logger.info("Will keep " + answer + " videos");
      if (!answer.equals("10")) {
        //10 is default, so if we are going with that, no reason to keep it explicitly
        defaultConfig.initializeVideoRecorder();
        defaultConfig.getVideoRecording().setVideosToKeep(Integer.valueOf(answer));
      }
    } else {
      logger.info("This node will not record test videos");
      defaultConfig.initializeVideoRecorder();
      defaultConfig.getVideoRecording().setRecordTestVideos(false);
    }

  }

  private static void askToStoreConfigsOnHub(Config defaultConfig, String hubHost) {
    String
        answer =
        askQuestion(
            "Should we store all of these configs in central location on the HUB node and update from there? (1-yes/0-no)",
            "1");

    if (answer.equals("1")) {

      saveCentralStorageUrl("http://" + hubHost + ":3000/");

      ConfigPusher pusher = new ConfigPusher();
      pusher.setHubHost(hubHost);
      pusher.addConfigFile("selenium_grid_extras_config.json");

      logger.info("Sending config files to " + hubHost);
      for (String file : defaultConfig.getNodeConfigFiles()) {
        pusher.addConfigFile(file);
      }

      logger.info("Open transfer");
      Map<String, Integer> results = pusher.sendAllConfigsToHub();
      logger.info("Checking status of transfered files");
      Boolean failure = false;
      for (String file : results.keySet()) {
        logger.info(file + " - " + results.get(file));
        if (!results.get(file).equals(200)) {
          failure = true;
        }
      }

      if (failure) {
        System.out.println(
            "Not all files were successfully sent to the HUB, please check log for more info");
      } else {
        System.out.println(
            "All files sent to hub, check the 'configs" + RuntimeConfig.getOS().getFileSeparator()
            + RuntimeConfig.getOS().getHostName()
            + "' directory to modify the configs for this node in the future");
      }

    }
  }

  private static void saveCentralStorageUrl(String url) {
    File configsDirectory = RuntimeConfig.getConfig().getConfigsDirectory();
    if (!configsDirectory.exists()) {
      configsDirectory.mkdir();
    }

    File
        storageUrlFile =
        new File(configsDirectory.getAbsoluteFile() + RuntimeConfig.getOS().getFileSeparator()
                 + RuntimeConfig.getConfig().getCentralConfigFileName());

    try {
      logger.info("Saving the central config url '" + url + "' to file " + storageUrlFile
          .getAbsolutePath());
      FileIOUtility.writeToFile(storageUrlFile, url);
    } catch (IOException error) {
      String
          message =
          "Unable to save the central config repository URL to '" + storageUrlFile
          + "' please update that file to allow the nodes to automatically self update in the future";
      System.out.println(message);
      logger.warn(message);
      logger.warn(error);
    }

  }

  private static void setRebootAfterSessionLimit(Config defaultConfig) {

    if (!defaultConfig.getAutoStartHub()) { // If this is a HUB, we never want to restart it
      String
          answer =
          askQuestion("Restart after how many tests (0-never restart)", "10");

      defaultConfig.setRebootAfterSessions(answer);
    }

  }

  private static void setDriverAutoUpdater(Config defaultConfig) {
    String
        answer =
        askQuestion(
            "Would you like WebDriver, IEDriver and ChromeDriver to auto update (1-yes/0-no)", "1");

    WebDriverReleaseManager manager = RuntimeConfig.getReleaseManager();
    String versionOfChrome = manager.getChromeDriverLatestVersion().getPrettyPrintVersion(".");
    String versionOfWebDriver = manager.getWedriverLatestVersion().getPrettyPrintVersion(".");
    String versionOfIEDriver = manager.getIeDriverLatestVersion().getPrettyPrintVersion(".");

    if (answer.equals("1")) {
      defaultConfig.setAutoUpdateDrivers("1");

    } else {
      defaultConfig.setAutoUpdateDrivers("0");
      System.out.println(
          "Drivers will not be automatically updated.\n You can change the versions of each driver later in the config");

      versionOfWebDriver =
          askQuestion("What version of WebDriver Jar should we use?", versionOfWebDriver);
      versionOfChrome =
          askQuestion("What version of Chrome Driver should we use?", versionOfChrome);
      versionOfIEDriver =
          askQuestion("What version of IE Driver should we use?", versionOfIEDriver);
    }

    defaultConfig.getWebdriver().setVersion(versionOfWebDriver);
    defaultConfig.getIEdriver().setVersion(versionOfIEDriver);
    defaultConfig.getChromeDriver().setVersion(versionOfChrome);

    System.out
        .println("Current Selenium Driver Version: " + defaultConfig.getWebdriver().getVersion());
    System.out.println("Current IE Driver Version: " + defaultConfig.getIEdriver().getVersion());
    System.out
        .println("Current Chrome Driver Version: " + defaultConfig.getChromeDriver().getVersion());

  }

  private static void configureNodes(List<Capability> capabilities, String hubHost,
                                     String hubPort, Config defaultConfig) {
    GridNode node = new GridNode();
    int nodePort = 5555;

    node.getConfiguration().setHubHost(hubHost);
    node.getConfiguration().setHubPort(Integer.parseInt(hubPort));
    node.getConfiguration().setPort(nodePort);

    for (Capability cap : capabilities) {
      node.getCapabilities().add(cap);
    }

    String configFileName = "node_" + nodePort + ".json";

    node.writeToFile(configFileName);
    defaultConfig.addNode(node, configFileName);
  }

  private static void configureAppiumNodes(List<Capability> capabilities, String hubHost,
                                           String hubPort, String appiumStartCommand, Config defaultConfig) {
    GridNode node = new GridNode();
    int nodePort = 4723;
    String nodeIp = new OS().getHostIp();
    String nodeUrl = "http://" + nodeIp + ":" + nodePort + "/wd/hub";
    int registerCycle = 5000;

    node.getConfiguration().setMaxSession(1);
    node.getConfiguration().setHubHost(hubHost);
    node.getConfiguration().setHubPort(Integer.parseInt(hubPort));
    node.getConfiguration().setPort(nodePort);
    node.getConfiguration().setHost(nodeIp);
    node.getConfiguration().setUrl(nodeUrl);
    node.getConfiguration().setRegisterCycle(registerCycle);
    node.getConfiguration().setAppiumStartCommand(appiumStartCommand);

    for (Capability cap : capabilities) {
      node.getCapabilities().add(cap);
    }

    String configFileName = "appium_node_" + nodePort + ".json";

    node.writeToFile(configFileName);
    defaultConfig.addNode(node, configFileName);
  }

  private static void configureHub(String host, String port,
                                   Config defaultConfig) {
    GridHub hub = new GridHub();

//    hub.getConfiguration().setHost(host); // Should this always be null ?
    hub.getConfiguration().setPort(Integer.parseInt(port));

    String configFileName = "hub_" + port + ".json";

    hub.writeToFile(configFileName);
    defaultConfig.addHub(hub, configFileName);
  }


  private static List<Capability> getCapabilitiesFromUser(Config defaultConfig) {

    List<Capability> chosenCapabilities = new LinkedList<Capability>();

    if (defaultConfig.getAutoStartNode()) {

      String guessedPlatform = guessPlatform();
      System.out.println("What is node Platform? (WINDOWS|XP|VISTA|WIN8|WIN8_1|MAC|LINUX|UNIX|ANDROID)");
      if (guessedPlatform.equals("WINDOWS")){
        System.out.println("WARNING: We had a hard time guessing your platform accurately so will default to 'WINDOWS' pleas update this to be more accurate, or the grid capability matcher might not function properly");
      }
      String platform = askQuestion(
          "",
          guessedPlatform);


      /* If we can't detect the correct browser version, default to No for auto updating the 
       * browser version automatically on node startup */
      String ableToAutoDetectBrowserVersions = "1";
      for (Class currentCapabilityClass : Capability.getSupportedWebCapabilities().keySet()) {
        String
            value =
            askQuestion(
                "Will this node run '" + currentCapabilityClass.getSimpleName()
                + "' (1-yes/0-no)", "0");

        if (value.equals("1")) {
          Capability capability;
          try {
            capability =
                (Capability) Class.forName(currentCapabilityClass.getCanonicalName()).newInstance();
            capability.setPlatform(platform.toUpperCase());
            String guessedBrowserVersion = BrowserVersionDetector.guessBrowserVersion(currentCapabilityClass.getSimpleName());
            String realBrowserVersion = askQuestion(
                "What version of '" + currentCapabilityClass.getSimpleName() + "' is installed?", guessedBrowserVersion);
            capability.setBrowserVersion(realBrowserVersion);
            if ((guessedBrowserVersion != realBrowserVersion) && ableToAutoDetectBrowserVersions.equals("1")) {
              ableToAutoDetectBrowserVersions = "0";
            }

            chosenCapabilities.add(capability);
          } catch (Exception e) {
            logger.warn("Warning: Had an issue creating capability for " + currentCapabilityClass
                .getSimpleName());
            logger.warn(e.toString());
          }
        }

      }


      String answer = askQuestion("Would you like this Node to auto update browser versions?? (1-yes/0-no)", ableToAutoDetectBrowserVersions);
      if (answer.equals("1")) {
        defaultConfig.setAutoUpdateBrowserVersions("1");
      } else {
        defaultConfig.setAutoUpdateBrowserVersions("0");
      }
    }

    return chosenCapabilities;
  }

  private static List<Capability> getAppiumCapabilitiesFromUser(Config defaultConfig) {

    List<Capability> chosenCapabilities = new LinkedList<Capability>();

    if (defaultConfig.getAutoStartNode()) {

      String appium = askQuestion("Will this node run 'Appium' (1-yes/0-no)", "0");
      if (appium.equals("1")) {

        for (Class currentCapabilityClass : Capability.getSupportedAppiumCapabilities().keySet()) {
          String
              value =
              askQuestion(
                  "Will this Appium node run '" + currentCapabilityClass.getSimpleName()
                  + "' (1-yes/0-no)", "0");

          if (value.equals("1")) {
            Capability capability;
            try {
              capability =
                  (Capability) Class.forName(currentCapabilityClass.getCanonicalName()).newInstance();
              String capabilityName = currentCapabilityClass.getSimpleName();
              if (capabilityName.equals("IPhone") || capabilityName.equals("IPad")
                      || capabilityName.equals("Safari")) {
                capability.setPlatform("MAC");
              } else {
                capability.setPlatform("ANDROID");
              }
              capability.setMaxInstances(1);
              capability.setBrowserVersion(askQuestion(
                "What version of '" + currentCapabilityClass.getSimpleName() + "' is installed?", ""));

              chosenCapabilities.add(capability);
            } catch (Exception e) {
              logger.warn("Warning: Had an issue creating capability for " + currentCapabilityClass
                  .getSimpleName());
              logger.warn(e.toString());
            }
          }
        }
      }
    }
    return chosenCapabilities;
  }

  private static String guessPlatform() {
    if (RuntimeConfig.getOS().isWindows()) {
      String osFamily = new GridPlatform().getWindowsFamily(System.getProperty("os.name"));
      return osFamily;
    } else if (RuntimeConfig.getOS().isMac()) {
      return "MAC";
    } else {
      return "LINUX";
    }
  }


  private static void setGridHubAutostart(Config defaultConfig, String value) {
    defaultConfig.setAutoStartHub(value);
  }

  private static void setGridNodeAutostart(Config defaultConfig, String value) {
    defaultConfig.setAutoStartNode(value);
  }

  private static String getGridHubHost() {
    String
        host =
        askQuestion("What is the HOST for the Selenium Grid Hub?",
                    "127.0.0.1");
    return host;
  }


  private static String getGridHubPort() {
    String port = askQuestion("What is the PORT for the Selenium Grid Hub?", "4444");
    return port;
  }

  private static String getAppiumStartCommand() {
    String command = askQuestion("What is the command to start Appium?", "appium");
    return command;
  }

  private static void setDefaultService(Config defaultConfig) {
    String
        role =
        askQuestion(
            "What is the default Role of this computer? (1 - node | 2 - hub | 3 - hub & node) ",
            "1");

    if (role.equals("1")) {
      setGridHubAutostart(defaultConfig, "0");
      setGridNodeAutostart(defaultConfig, "1");
      defaultConfig.setDefaultRole("node");
    } else if (role.equals("2")) {
      setGridHubAutostart(defaultConfig, "1");
      setGridNodeAutostart(defaultConfig, "0");
      defaultConfig.setDefaultRole("hub");
    } else {
      setGridHubAutostart(defaultConfig, "1");
      setGridNodeAutostart(defaultConfig, "1");
      defaultConfig.setDefaultRole("hub");
    }
  }

  private static String askQuestion(String question, String defaultValue) {

    System.out.println("\n\n" + question);
    System.out.println("Default Value: " + defaultValue);

    String answer = readLine();

    if (answer.equals("")) {
      answer = defaultValue;
    }

    final String printOutAswer = "'" + answer + "' was set as your value";
    System.out.println(printOutAswer);
    logger.info(printOutAswer);

    return answer;

  }

  private static String askQuestion(String question) {
    System.out.println("\n\n" + question);
    System.out.println("(No Default Value)");
    String answer = readLine();

    return answer;
  }

  private static String readLine() {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String line = null;

    try {
      line = br.readLine();
    } catch (IOException ioe) {
      logger.fatal("IO error trying to read your input.");
      System.exit(1);
    }

    return line;
  }

}
