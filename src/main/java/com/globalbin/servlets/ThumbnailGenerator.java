package com.globalbin.servlets;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observe /content/images for changes, and generate thumbnails when images are
 * added.
 *
 */
@Component(immediate = true)
@Property(name = "service.description", value = "GlobalBin Thumbnail Generator")
public class ThumbnailGenerator implements EventListener {

	private Session session;
	private ObservationManager observationManager;

	@Reference
	private ResourceResolverFactory factory;

	@Reference
	private SlingRepository repository;

	@Property(value = "/content/images")
	private static final String CONTENT_PATH_PROPERTY = "content.path";

	private static final Logger log = LoggerFactory.getLogger(ThumbnailGenerator.class);

	private Map<String, String> supportedMimeTypes = new HashMap<String, String>();

	protected void activate(ComponentContext context) throws Exception {
		log.info("Activated: ThumbnailGenerator");
		supportedMimeTypes.put("image/jpeg", ".jpg");
		supportedMimeTypes.put("image/png", ".png");
		String contentPath = (String) context.getProperties().get(CONTENT_PATH_PROPERTY);

		session = repository.loginService("datawrite", repository.getDefaultWorkspace());
		if (repository.getDescriptor(Repository.OPTION_OBSERVATION_SUPPORTED).equals("true")) {
			log.info("ThumbnailGenerator supports Observation");
			observationManager = session.getWorkspace().getObservationManager();
			String[] types = { "nt:file" };
			observationManager.addEventListener(this, Event.NODE_ADDED | Event.NODE_MOVED, contentPath, true, null,
					types, false);
		} else {
			log.info("ThumbnailGenerator does not support observation.");
		}
	}

	protected void deactivate(ComponentContext componentContext) throws RepositoryException {
		if (observationManager != null) {
			observationManager.removeEventListener(this);
		}
		if (session != null) {
			session.logout();
			session = null;
		}
	}

	public void onEvent(EventIterator it) {
		while (it.hasNext()) {
			Event event = it.nextEvent();
			try {
				if (event.getType() == Event.NODE_ADDED && !(event.getPath().contains("thumbnails"))) {
					log.info("new upload: {}", event.getPath());
					Node addedNode = session.getRootNode().getNode(event.getPath().substring(1));
					processNewNode(addedNode);
					log.info("finished processing of {}", event.getPath());
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	private String getMimeType(Node n) throws RepositoryException {
		String result = null;
		final String mimeType = n.getProperty("jcr:mimeType").getString();

		for (String key : supportedMimeTypes.keySet()) {
			log.info("Supported mimeType: {}", key);
			if (mimeType != null && mimeType.startsWith(key)) {
				result = key;
				break;
			}
		}

		if (result == null) {
			log.info("Node {} rejected, unsupported mime-type {}", n.getPath(), mimeType);
		}

		if (n.getName().startsWith(".")) {
			log.info("Node {} rejected, name starts with '.'", n.getPath(), mimeType);
			result = null;
		}

		return result;
	}

	private void processNewNode(Node addedNode) throws Exception {
		final String mimeType = getMimeType(addedNode);
		if (mimeType == null) {
			return;
		}
		final String suffix = supportedMimeTypes.get(mimeType);

		// Scale to a temp file for simplicity
		log.info("Creating thumbnails for node {}", addedNode.getPath());
		final int[] widths = { 50, 100, 250 };
		for (int width : widths) {
			createThumbnail(addedNode, width, mimeType, suffix);
		}
	}

	private void createThumbnail(Node image, int scalePercent, String mimeType, String suffix) throws Exception {

		log.info("createThumbnail: {}, {}, {}", image.getPath(), mimeType, suffix);

		final File tmp = File.createTempFile(getClass().getSimpleName(), suffix);
		try {
			scale(image.getProperty("jcr:data").getBinary().getStream(), scalePercent, new FileOutputStream(tmp),
					suffix);

			// Create thumbnail node and set the mandatory properties
			Node thumbnailFolder = getThumbnailFolder(image);
			Node thumbnail = thumbnailFolder.addNode(image.getParent().getName() + "_" + scalePercent + suffix,
					"nt:file");
			Node contentNode = thumbnail.addNode("jcr:content", "nt:resource");

			FileInputStream is = new FileInputStream(tmp);
			ValueFactory valueFactory = session.getValueFactory();
			Binary contentValue = valueFactory.createBinary(is);
			contentNode.setProperty("jcr:data", contentValue);

			Calendar lastModified = Calendar.getInstance();
			lastModified.setTimeInMillis(lastModified.getTimeInMillis());
			contentNode.setProperty("jcr:lastModified", lastModified);
			contentNode.setProperty("jcr:mimeType", mimeType);

			session.save();

			log.info("Created thumbnail " + contentNode.getPath());
		} finally {
			if (tmp != null) {
				tmp.delete();
			}
		}

	}

	private Node getThumbnailFolder(Node addedNode) throws Exception {
		Node post = addedNode.getParent().getParent().getParent();
		if (post.hasNode("thumbnails")) {
			log.info("thumbnails node exists already at " + post.getPath());
			return post.getNode("thumbnails");
		} else {
			Node t = post.addNode("thumbnails", "nt:folder");
			session.save();
			return t;
		}
	}

	public void scale(InputStream inputStream, int width, OutputStream outputStream, String suffix) throws IOException {
		if (inputStream == null) {
			throw new IOException("InputStream is null");
		}

		final BufferedImage src = ImageIO.read(inputStream);
		if (src == null) {
			final StringBuffer sb = new StringBuffer();
			for (String fmt : ImageIO.getReaderFormatNames()) {
				sb.append(fmt);
				sb.append(' ');
			}
			throw new IOException("Unable to read image, registered formats: " + sb);
		}

		final double scale = (double) width / src.getWidth();

		int destWidth = width;
		int destHeight = new Double(src.getHeight() * scale).intValue();

		log.debug("Generating thumbnail, w={}, h={}", destWidth, destHeight);
		BufferedImage dest = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = dest.createGraphics();
		AffineTransform at = AffineTransform.getScaleInstance((double) destWidth / src.getWidth(),
				(double) destHeight / src.getHeight());
		g.drawRenderedImage(src, at);
		ImageIO.write(dest, suffix.substring(1), outputStream);
	}
}