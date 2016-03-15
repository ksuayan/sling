package com.globalbin.servlets;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Simple Filter
 * 
 * Annotations below are short version of:
 * 
 * @Component
 * @Service(Filter.class)
 * @Properties({
 *     @Property(name="service.description", value="A Simple Filter"),
 *     @Property(name="service.vendor", value="The Global Bin"),
 *     @Property(name="sling.filter.scope", value="REQUEST"),
 *     @Property(name="service.ranking", intValue=1)
 * })
 */
@SlingFilter(order=1, description="A Simple Filter")
@Property(name="service.vendor", value="The Global Bin")
public class SimpleFilter implements Filter {
    
    private final Logger log = LoggerFactory.getLogger(SimpleFilter.class);

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        log.info("filter invoked - start");
        chain.doFilter(request, response);
        log.info("filter invoked - end");
    }

    public void destroy() {
    }

}
