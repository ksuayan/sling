package com.globalbin.servlets;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello World Servlet registered by path
 * 
 * Annotations below are short version of:
 * 
 * @Component
 * @Service(Servlet.class)
 * @Properties({
 *     @Property(name="service.description", value="Hello World Path Servlet"),
 *     @Property(name="service.vendor", value="The Global Bin"),
 *     @Property(name="sling.servlet.paths", value="/hello-world-servlet")
 * })
 */
@SlingServlet(paths="/hello-world-servlet")
@Properties({
    @Property(name="service.description", value="Hello World Path Servlet"),
    @Property(name="service.vendor", value="The Global Bin")
})
@SuppressWarnings("serial")
public class ByPathServlet extends SlingSafeMethodsServlet {
    
    private final Logger log = LoggerFactory.getLogger(ByPathServlet.class);

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws ServletException,
            IOException {
    	
        response.setContentType("text/html");
        Writer w = response.getWriter();
        w.write("<!DOCTYPE html PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
        w.write("<html>");
        w.write("<head>");
        w.write("<title>Hello World Servlet</title>");
        w.write("</head>");
        w.write("<body>");
        w.write("<h1>Hello World!</h1>");
        w.write("</body>");
        w.write("</html>");
        
        log.info("Hello World Servlet");
        
    }

}

