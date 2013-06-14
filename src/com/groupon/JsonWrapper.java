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

package com.groupon;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JsonWrapper {


  public static String taskResultToJson(int result, String output, String error) {

    //TODO: Move all of these out to each object's toJson() because this is getting to be too much.

    JSONObject resultsHash = new JSONObject();
    JSONArray standardOut = new JSONArray();
    JSONArray standardError = new JSONArray();

    String stdOutLines[] = output.split("\n");
    for(String line: stdOutLines) {
      standardOut.add(line);
    }

    String stdErrorLines[] = error.split("\n");
    for(String line: stdErrorLines) {
      standardError.add(line);
    }



    resultsHash.put("exit_code", result);
    resultsHash.put("standard_out", standardOut);
    resultsHash.put("standard_error", standardError);

    return resultsHash.toString();
  }

  public static Map parseJson(String inputString) {
    Map returnHash = new HashMap();

    JSONParser parser = new JSONParser();
    ContainerFactory containerFactory = new ContainerFactory() {
      public List creatArrayContainer() {
        return new LinkedList();
      }

      public Map createObjectContainer() {
        return new LinkedHashMap();
      }

    };

    try {
      Map json = (Map) parser.parse(inputString, containerFactory);
      Iterator iter = json.entrySet().iterator();
      while (iter.hasNext()) {
        Map.Entry entry = (Map.Entry) iter.next();
        returnHash.put(entry.getKey(), entry.getValue());
      }

    } catch (ParseException error) {
      System.out.println("position: " + error.getPosition());
      System.out.println(error);
    }

    return returnHash;
  }

  public static String fileArrayToJson(File[] inputArray){
    JSONArray fileList = new JSONArray();

    JSONObject wrapper = new JSONObject();

    for(File f : inputArray){
      fileList.add(f.toString());
    }

    wrapper.put("files", fileList);

    return wrapper.toString();
  }

  public static String screenshotToJson(String encodedImage, String file, String type){
    JSONObject screenshotInfo = new JSONObject();

    screenshotInfo.put("file_type", type);
    screenshotInfo.put("file", file);
    screenshotInfo.put("image", encodedImage);

    return screenshotInfo.toString();
  }

  public static String getDefaultConfigs(){
    JSONObject config = new JSONObject();
    JSONArray activeModules = new JSONArray();
    JSONArray setupTask = new JSONArray();
    JSONArray teardownTask = new JSONArray();
    JSONObject webdriverConfig = new JSONObject();

    //Webdriver Config
    webdriverConfig.put("directory", "webdriver");
    webdriverConfig.put("version", "2.33.0");
    config.put("webdriver", webdriverConfig);

    //Activated Modules
    activeModules.add("com.groupon.DownloadWebdriver");
    activeModules.add("com.groupon.UpgradeWebdriver");
    activeModules.add("com.groupon.Setup");
    activeModules.add("com.groupon.Teardown");
    activeModules.add("com.groupon.MoveMouse");
    activeModules.add("com.groupon.RebootNode");
    activeModules.add("com.groupon.KillAllIE");
    activeModules.add("com.groupon.KillAllFirefox");
    activeModules.add("com.groupon.KillAllChrome");
    activeModules.add("com.groupon.GetProcesses");
    activeModules.add("com.groupon.KillPid");
    activeModules.add("com.groupon.Netstat");
    activeModules.add("com.groupon.Screenshot");
    activeModules.add("com.groupon.ExposeDirectory");
    activeModules.add("com.groupon.GetFile");
    config.put("activated_modules", activeModules);

    //Setup Task Modules
    setupTask.add("com.groupon.KillAllIE");
    setupTask.add("com.groupon.MoveMouse");
    config.put("setup", setupTask);

    //Teardown Task Modules
    teardownTask.add("com.groupon.KillAllIE");
    config.put("teardown", teardownTask);


    config.put("expose_directory", "shared");


    return config.toString();
  }

  public static String downloadResultsToJson(Integer result, String rootDir, String fileName, String sourceUrl, String error){
    JSONObject returnResults = new JSONObject();

    returnResults.put("exit_code", result);
    returnResults.put("root_dir", rootDir);
    returnResults.put("file", fileName);
    returnResults.put("source_url", sourceUrl);
    returnResults.put("standard_error", error);

    return returnResults.toString();
  }

  public static String upgradeWebdriverToJson(Integer result, String oldVersion, String newVersion, String error){
    JSONObject returnResults = new JSONObject();

    returnResults.put("exit_code", result);
    returnResults.put("old_version", oldVersion);
    returnResults.put("new_version", newVersion);
    returnResults.put("error", error);

    return returnResults.toString();
  }



}
