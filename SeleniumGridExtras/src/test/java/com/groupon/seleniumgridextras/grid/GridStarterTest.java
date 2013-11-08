package com.groupon.seleniumgridextras.grid;

import com.groupon.seleniumgridextras.config.Config;
import com.groupon.seleniumgridextras.config.GridNode;
import com.groupon.seleniumgridextras.config.RuntimeConfig;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.*;

public class GridStarterTest {

  private static final String START_HUB_BAT = "start_hub.bat";
  private static final String GRID_HUB_LOG = "grid_hub.log";
  private static final String TEST_COMMAND = "command is here";
  private final String nodeOneConfig = "node1.json";
  private final String nodeTwoConfig = "node2.json";


  //COMPILED WITH USE OF http://gskinner.com/RegExr/
  private final
  Pattern
      startHubCommandPattern =
      Pattern.compile(
          "(java -cp)\\s*([/\\\\\\w-]*)([:;])([/\\\\\\w-^]*)(1.1.1.jar)\\s*(org.openqa.grid.selenium.GridLauncher)\\s*-role\\s(\\w*)\\s-port\\s(\\d{4})\\s-host\\s([\\d\\.]*)\\s-servlets\\s([\\w\\.]*),([\\w\\.]*)");


  @Before
  public void setUp() throws Exception {
    RuntimeConfig.setConfigFile("grid_start_test.json");
    Config config = new Config();
    config.getWebdriver().setVersion("1.1.1");

    GridNode node1 = new GridNode();
    GridNode node2 = new GridNode();

    node1.writeToFile(nodeOneConfig);
    node2.writeToFile(nodeTwoConfig);

    config.addNodeConfigFile(nodeOneConfig);
    config.addNodeConfigFile(nodeTwoConfig);

    config.writeToDisk(RuntimeConfig.getConfigFile());

    RuntimeConfig.load();
  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void testGetNodesStartCommand() throws Exception{


  }

  @Test
  public void testGetOsSpecificHubStartCommandForWindows() throws Exception {

    Matcher
        matcher =
        startHubCommandPattern.matcher(GridStarter.getOsSpecificHubStartCommand(true));

    assertTrue(matcher.find()); //Make sure the matchers are met
    assertEquals(11, matcher.groupCount()); //We have 11 total matches
    assertEquals("java -cp", matcher.group(1)); //start with java command
    assertEquals(";", matcher.group(3)); //OS specific class delimeter
    assertEquals("\\tmp\\webdriver\\", matcher.group(4)); //Location of the WD jar file
    assertEquals("1.1.1.jar", matcher.group(5)); //name of jar file
    assertEquals("org.openqa.grid.selenium.GridLauncher",
                 matcher.group(6)); //Calling the Grid launcher class
    assertEquals("hub", matcher.group(7)); //check role of the start command
    assertEquals("4444", matcher.group(8)); //Check port used
    assertEquals(RuntimeConfig.getCurrentHostIP(), matcher.group(9)); //Host name
    assertEquals("com.groupon.seleniumgridextras.grid.servlets.SeleniumGridExtrasServlet",
                 matcher.group(10)); //Using the servlete to pretty print html
    assertEquals("com.groupon.seleniumgridextras.grid.servlets.ProxyStatusJsonServlet",
                 matcher.group(11)); //JSON current status proxy


  }

  @Test
  public void testGetOsSpecificHubStartCommandForLinux() throws Exception {

    Matcher
        matcher =
        startHubCommandPattern.matcher(GridStarter.getOsSpecificHubStartCommand(false));

    assertTrue(matcher.find()); //Make sure the matchers are met
    assertEquals(11, matcher.groupCount()); //We have 11 total matches
    assertEquals("java -cp", matcher.group(1)); //start with java command
    assertEquals(":", matcher.group(3)); //OS specific class delimeter
    assertEquals("/tmp/webdriver/", matcher.group(4)); //Location of the WD jar file
    assertEquals("1.1.1.jar", matcher.group(5)); //name of jar file
    assertEquals("org.openqa.grid.selenium.GridLauncher",
                 matcher.group(6)); //Calling the Grid launcher class
    assertEquals("hub", matcher.group(7)); //check role of the start command
    assertEquals("4444", matcher.group(8)); //Check port used
    assertEquals(RuntimeConfig.getCurrentHostIP(), matcher.group(9)); //Host name
    assertEquals("com.groupon.seleniumgridextras.grid.servlets.SeleniumGridExtrasServlet",
                 matcher.group(10)); //Using the servlete to pretty print html
    assertEquals("com.groupon.seleniumgridextras.grid.servlets.ProxyStatusJsonServlet",
                 matcher.group(11)); //JSON current status proxy


  }

  @Test
  public void testBuildBackgroundStartCommand() throws Exception {
    assertEquals(TEST_COMMAND + " & 2>&1 > " + GRID_HUB_LOG,
                 GridStarter.buildBackgroundStartCommand(TEST_COMMAND, false));

    assertEquals(
        "powershell.exe /c \"Start-Process " + START_HUB_BAT + "\" | Out-File " + GRID_HUB_LOG,
        GridStarter.buildBackgroundStartCommand(TEST_COMMAND, true));

    assertEquals(TEST_COMMAND, readFile(START_HUB_BAT));

  }


  private String readFile(String filePath) {
    String returnString = "";
    try {
      BufferedReader reader = new BufferedReader(new FileReader(filePath));
      String line = null;
      while ((line = reader.readLine()) != null) {
        returnString = returnString + line;
      }
    } catch (FileNotFoundException error) {
      System.out.println("File " + filePath + " does not exist, going to use default configs");
    } catch (IOException error) {
      System.out.println("Error reading" + filePath + ". Going with default configs");
    }
    return returnString;
  }

}
