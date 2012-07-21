package net.gnisio.server.processors;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import net.gnisio.server.PacketsProcessor.Packet;
import net.gnisio.server.exceptions.ForceCloseConnection;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.FileRegion;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static content processor with NOT_MODIFIED response future. Also it disable
 * caching of any file that contain in path word "nocache"
 * 
 * @author c58
 */
public class StaticContentProcessor extends AbstractRequestProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(StaticContentProcessor.class);

	// Add base mime types
	private static final MimetypesFileTypeMap mimeTypesMap;
	static {
		mimeTypesMap = new MimetypesFileTypeMap();
		
		mimeTypesMap.addMimeTypes("text/plain txt text");
		mimeTypesMap.addMimeTypes("text/css css");
		mimeTypesMap.addMimeTypes("text/javascript js");
		mimeTypesMap.addMimeTypes("text/html html htm");
	}
	
	public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
	public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
	public static final int HTTP_CACHE_SECONDS = 60;

	private final String basePath;
	private final String defaultPage;

	public StaticContentProcessor(String path) {
		this(path, "index.html");
	}

	public StaticContentProcessor(String path, String defaultPage) {
		this.basePath = !path.endsWith(File.separatorChar + "") ? path + File.separatorChar : path;
		this.defaultPage = defaultPage;
	}

	@Override
	public void processRequest(HttpRequest request, HttpResponse resp, Packet packet) throws Exception {
		if (request.getMethod() != GET)
			sendError(resp, METHOD_NOT_ALLOWED);

		final String path = sanitizeUri(request.getUri());
		if (path == null)
			sendError(resp, FORBIDDEN);

		File file = new File(path);
		if (!file.isFile())
			if (defaultPage == null)
				sendError(resp, FORBIDDEN);
			else
				file = new File(path + defaultPage);

		if (file.isHidden() || !file.exists())
			sendError(resp, NOT_FOUND);

		// Disable caching
		if (path.contains("nocache")) {
			setNoCacheHeaders(resp);
		} else {
			// Validate cache
			checkLastModification(request, resp, file.lastModified());

			// Set headers for caching
			setDateAndCacheHeaders(resp, file.lastModified());
		}

		RandomAccessFile raf;
		try {
			raf = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException fnfe) {
			sendError(resp, NOT_FOUND);
			return;
		}
		long fileLength = raf.length();

		// Set other headers
		setContentLength(resp, fileLength);
		setContentTypeHeader(resp, file);

		Channel ch = packet.getCtx().getChannel();

		// Write the initial line and the header.
		ch.write(resp);

		ChannelFuture writeFuture;
		if (ch.getPipeline().get(SslHandler.class) != null) {
			// Cannot use zero-copy with HTTPS.
			writeFuture = ch.write(new ChunkedFile(raf, 0, fileLength, 8192));
		} else {
			// No encryption - use zero-copy.
			final FileRegion region = new DefaultFileRegion(raf.getChannel(), 0, fileLength);
			writeFuture = ch.write(region);
		}

		// Decide whether to close the connection or not.
		if (!isKeepAlive(request)) {
			// Close the connection when the whole content is written out.
			writeFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}

	public static void checkLastModification(HttpRequest request, HttpResponse resp, long lastModified)
			throws ParseException, ForceCloseConnection {
		// Cache Validation
		String ifModifiedSince = request.getHeader(HttpHeaders.Names.IF_MODIFIED_SINCE);
		if (ifModifiedSince != null && !ifModifiedSince.equals("")) {
			SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
			Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

			// Only compare up to the second because the datetime format we
			// send
			// to the client does
			// not have milliseconds
			long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
			long fileLastModifiedSeconds = lastModified / 1000;

			if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds)
				sendNotModified(resp);
		}
	}

	private String sanitizeUri(String uri) {
		// Decode the path.
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			try {
				uri = URLDecoder.decode(uri, "ISO-8859-1");
			} catch (UnsupportedEncodingException e1) {
				throw new Error();
			}
		}

		// Remove query
		int pos = -1;
		if ((pos = uri.indexOf("?")) > 0)
			uri = uri.substring(0, pos);

		// Convert file separators.
		uri = uri.replace('/', File.separatorChar);
		uri = uri.startsWith("/") ? uri.substring(1) : uri;

		// Simplistic dumb security check.
		// You will have to do something serious in the production environment.
		if (uri.contains(File.separator + ".") || uri.contains("." + File.separator) || uri.startsWith(".")
				|| uri.endsWith(".")) {
			return null;
		}

		// Convert to absolute path.
		return basePath + uri;
	}

	protected static void sendError(HttpResponse response, HttpResponseStatus status) throws ForceCloseConnection {
		LOG.info("Error in static processor: "+status);
		response.setStatus(status);
		response.setContent(ChannelBuffers.copiedBuffer(status.toString() + "\r\n", CharsetUtil.UTF_8));

		throw new ForceCloseConnection();
	}

	/**
	 * When file timestamp is the same as what the browser is sending up, send a
	 * "304 Not Modified"
	 * 
	 * @param ctx
	 *            Context
	 * @throws ForceCloseConnection
	 */
	private static void sendNotModified(HttpResponse response) throws ForceCloseConnection {
		response.setStatus(HttpResponseStatus.NOT_MODIFIED);
		setDateHeader(response);

		// Close the connection as soon as the error message is sent.
		throw new ForceCloseConnection();
	}

	/**
	 * Sets the Date header for the HTTP response
	 * 
	 * @param response
	 *            HTTP response
	 */
	public static void setDateHeader(HttpResponse response) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
		dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

		Calendar time = new GregorianCalendar();
		response.setHeader(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()));
	}

	/**
	 * Disable caching of content
	 * 
	 * @param response
	 */
	public static void setNoCacheHeaders(HttpResponse response) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
		dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));
		Date now = new Date();

		// Disable caching
		response.setHeader(HttpHeaders.Names.DATE, dateFormatter.format(now.getTime()));
		response.setHeader(HttpHeaders.Names.EXPIRES, dateFormatter.format(now.getTime() - 86400000L));
		response.setHeader(HttpHeaders.Names.PRAGMA, "no-cache");
		response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
	}

	/**
	 * Sets the Date and Cache headers for the HTTP Response
	 * 
	 * @param response
	 *            HTTP response
	 * @param fileToCache
	 *            file to extract content type
	 */
	public static void setDateAndCacheHeaders(HttpResponse response, long lastModified) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
		dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

		// Date header
		Calendar time = new GregorianCalendar();
		response.setHeader(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()));

		// Add cache headers
		time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
		response.setHeader(HttpHeaders.Names.EXPIRES, dateFormatter.format(time.getTime()));
		response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
		response.setHeader(HttpHeaders.Names.LAST_MODIFIED, dateFormatter.format(new Date(lastModified)));
	}

	/**
	 * Sets the content type header for the HTTP Response
	 * 
	 * @param response
	 *            HTTP response
	 * @param file
	 *            file to extract content type
	 */
	protected static void setContentTypeHeader(HttpResponse response, File file) {
		response.setHeader(HttpHeaders.Names.CONTENT_TYPE, mimeTypesMap.getContentType(file.getName())
				+ "; charset=UTF-8");
	}

}
