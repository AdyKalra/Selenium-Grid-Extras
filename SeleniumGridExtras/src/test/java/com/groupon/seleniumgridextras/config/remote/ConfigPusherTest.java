package com.groupon.seleniumgridextras.config.remote;

import com.groupon.seleniumgridextras.config.RuntimeConfig;
import com.groupon.seleniumgridextras.config.remote.ConfigPusher;
import com.groupon.seleniumgridextras.utilities.FileIOUtility;

import org.apache.http.HttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ConfigPusherTest {

  private ConfigPusher config;
  File testFile = new File("test.txt");

  @Before
  public void setUp() throws Exception {
    config = new ConfigPusher();
    config.setHubHost("host");
    config.addConfigFile(testFile.getName());

  }

  @After
  public void tearDown() throws Exception {
    if (testFile.exists()) {
      testFile.delete();
    }
  }

  @Test
  public void testBasicParam() throws Exception {

    Assert.assertEquals("http://host:3000/update_node_config?node=" +
                        RuntimeConfig.getOS().getHostName()
                        + "&filename=file&content=content",
                        config.buildUrl("file", "content").toString());
  }


  @Test
  public void testConfigFileReader() throws Exception {
    final String content = "This is a test";
    Map<String, String> expected = new HashMap<String, String>();
    expected.put(testFile.getName(), "VGhpcyBpcyBhIHRlc3Q=");

    FileIOUtility.writeToFile(testFile, content);

    assertEquals(expected, config.getConfigFiles());


  }

}
