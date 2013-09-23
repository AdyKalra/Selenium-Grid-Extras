package com.groupon.seleniumgridextras.grid.servlets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.grid.common.exception.GridException;
import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.internal.TestSlot;
import org.openqa.grid.web.servlet.RegistryBasedServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

public class ProxyStatusJsonServlet extends RegistryBasedServlet {

  private static final long serialVersionUID = -1975392591408983229L;

  public ProxyStatusJsonServlet() {
    this(null);
  }

  public ProxyStatusJsonServlet(Registry registry) {
    super(registry);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doPost(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    process(req, resp);

  }

  protected void process(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setStatus(200);
    JSONObject res;
    try {
      res = getResponse();
      response.getWriter().print(res.toString(4));
      response.getWriter().close();
    } catch (JSONException e) {
      throw new GridException(e.getMessage());
    }

  }

  private JSONObject getResponse() throws IOException, JSONException {
    JSONObject requestJSON = new JSONObject();
    ProxySet proxies = this.getRegistry().getAllProxies();
    Iterator<RemoteProxy> iterator = proxies.iterator();
    JSONArray Proxies = new JSONArray();
    JSONArray ProxyStatus = new JSONArray();
    while (iterator.hasNext()) {
      RemoteProxy eachProxy = iterator.next();
      Iterator<TestSlot> proxyIterator = eachProxy.getTestSlots().iterator();
      while (proxyIterator.hasNext()) {
        JSONObject TestMachine = new JSONObject();
        TestSlot eachSlot = proxyIterator.next();
        TestMachine.put("browserName", eachSlot.getCapabilities().get("browserName").toString());

        String version = "";
        if (eachSlot.getCapabilities().containsKey("version")) {
          version = eachSlot.getCapabilities().get("version").toString();
        }

        TestMachine.put("version", version);

        TestMachine.put("session", "");

        if (eachSlot.getSession() != null) {
          TestMachine.put("session", eachSlot.getSession().getExternalKey().getKey());
        }

        URL host = eachProxy.getRemoteHost();
        String hostName = host.getHost();
        TestMachine.put("host", hostName);

        ProxyStatus.put(TestMachine);
      }

      Proxies.put(eachProxy.getOriginalRegistrationRequest().getAssociatedJSON());
    }
    requestJSON.put("Proxies", ProxyStatus);
    requestJSON.put("TotalProxies", Proxies);

    return requestJSON;
  }

}

