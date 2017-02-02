package com.globalbin.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(Servlet.class)
@Properties({ @Property(name = "service.description", value = "Page Creator Servlet"),
        @Property(name = "service.vendor", value = "The GlobalBin"),
        @Property(name = "sling.servlet.extensions", value = "json"),
        @Property(name = "sling.servlet.paths", value = "/bin/api/page") })
public class PageCreatorServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = 9837495234L;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Reference
    Repository repository;
    
    Session session;
    
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        
        StringBuffer jb = new StringBuffer();
        String line = null;
        try {
          BufferedReader reader = request.getReader();
          while ((line = reader.readLine()) != null)
            jb.append(line);
        } catch (Exception e) { 
            
        }
        
        JSONObject jsonObject = null;
        try {
          jsonObject = new JSONObject(jb.toString());
          
          JSONObject jcrJsonObject = getChildJson("jcr", jsonObject);
          String path = jsonObject.getString("path").trim();          
          createNode(path, jcrJsonObject);         
          response.setContentType("application/json");
          PrintWriter out = response.getWriter();
          out.print(jcrJsonObject);
          out.flush();          
        } catch (JSONException e) {
            throw new IOException("Error parsing JSON request string");
        }
    }    
    
    private void createNode(String path, JSONObject jsonObject) {
        try {
            session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            Node root = session.getRootNode();
            Node context = root.getNode(path);
            if (context!=null) {
                log.info("Creating nodes under " + context.getPath());
                traverseJSON(context, jsonObject);                
            }
            session.save();
            session.logout();
        } catch (RepositoryException e) {
        }
    }
    
    private JSONObject getChildJson(String key, JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        try {
            if (jsonObject.get(key) instanceof JSONObject) {
                return jsonObject.getJSONObject(key);
            }
        } catch (JSONException e) {
        }
        return null;
    }

    
    private void traverseJSON(Node parent, JSONObject jsonObject) {
        Iterator<?> keys = jsonObject.keys();        
        while(keys.hasNext()) {
            String key = (String) keys.next();
            key = key.trim();            
            try {             
                if (jsonObject.get(key) instanceof JSONObject) {          
                    JSONObject childJson = getChildJson(key, jsonObject);                   
                    String primaryType = childJson.getString("jcr:primaryType");                    
                    Node childNode = null;
                    if (primaryType!=null) {
                        childNode = parent.addNode(key, primaryType);                        
                    } else {
                        childNode = parent.addNode(key);
                    }
                    session.save();                      
                    // depth first traversal
                    traverseJSON(childNode, childJson);
                    
                } else if (jsonObject.get(key) instanceof String) {
                    
                    String value = jsonObject.getString(key);
                    if (!key.equals("jcr:primaryType")) {
                        log.info(parent.getName()+": property >>> " + key+ " : " + value);
                        parent.setProperty(key, value);
                        session.save();
                    }
                }
            } catch (JSONException e) {
            } catch (RepositoryException e) {                
            }
        }
    }
}
