package com.globalbin.servlets;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import javax.imageio.ImageIO;
import javax.jcr.Node;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Hello World Servlet registered by resource type
 *
 */
@SlingServlet(resourceTypes="sling/servlet/default", selectors="show", extensions="jpg")
@Properties({
    @Property(name="service.description", value="Image Passthrough Servlet"),
    @Property(name="service.vendor", value="The Global Bin")
})
public class ImageServlet extends SlingSafeMethodsServlet {
	private static final long serialVersionUID = 7187392393675387755L;
	private final Logger log = LoggerFactory.getLogger(ImageServlet.class);

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException,
            IOException {

    	String resourcePath = request.getResource().getPath();
    	ResourceResolver resourceResolver = request.getResourceResolver();
    	
    	String contentPath = resourcePath.replaceAll(".show", "") + "/jcr:content";
		Resource resource = resourceResolver.getResource(contentPath);	
    	log.info(">>> contentPath: " + contentPath);
    	try {
        	Node resourceNode = resource.adaptTo(Node.class);
        	if (resourceNode!=null) {
        		log.info(">>> node found. " + resourceNode.getPath());
    			response.setContentType("image/jpg");
    			response.setStatus(HttpServletResponse.SC_OK);
    			
    			InputStream inputStream = resourceNode.getProperty("jcr:data").getBinary().getStream();
    			OutputStream outputStream = response.getOutputStream();    			
    			BufferedImage sourceImage = ImageIO.read(inputStream);
    			
    			int width = sourceImage.getWidth();
    			int height = sourceImage.getHeight();    			
    			log.info("Image WxH: " + width + "x" + height);
    			
    			
    			Image thumbnail = sourceImage.getScaledInstance(200, -1, Image.SCALE_SMOOTH);
    			BufferedImage bufferedThumbnail = new BufferedImage(thumbnail.getWidth(null),
    			                                                    thumbnail.getHeight(null),
    			                                                    BufferedImage.TYPE_INT_RGB);
    			bufferedThumbnail.getGraphics().drawImage(thumbnail, 0, 0, null);
    			ImageIO.write(bufferedThumbnail, "jpeg", outputStream);

    			// IOUtils.copy(inputStream, outputStream);
    			
    			inputStream.close();
    			outputStream.close();
        	} else {
        		log.info(">>> adapTo failed! -----------");
        	}
		} catch (Exception e) {
			e.printStackTrace();			
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	        Writer w = response.getWriter();
	        w.write("<!DOCTYPE html PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
	        w.write("<html>");
	        w.write("<head>");
	        w.write("<title>Error</title>");
	        w.write("</head>");
	        w.write("<body>");
	        w.write("<h1>Error Retrieving ");
	        w.write(resource.getPath());
	        w.write("</h1>");
	        w.write("</body>");
	        w.write("</html>");			
		}
		resourceResolver.close();
    }	
}
