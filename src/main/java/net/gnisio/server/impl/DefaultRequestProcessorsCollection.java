package net.gnisio.server.impl;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import net.gnisio.server.SessionsStorage;
import net.gnisio.server.clients.ClientsStorage;
import net.gnisio.server.exceptions.StopRequestProcessing;
import net.gnisio.server.processors.RequestProcessor;
import net.gnisio.server.processors.RequestProcessorsCollection;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple implementation of processors collection using patterns and TreeMap for
 * finding appropriate processor by given URI
 * 
 * @author c58
 */
public class DefaultRequestProcessorsCollection implements RequestProcessorsCollection {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultRequestProcessorsCollection.class);

	// Comparator for processor patterns
	// More slashes in begin of map
	// Less slashes in end of map
	private final Comparator<Pattern> pattsComparator = new Comparator<Pattern>() {
		@Override
		public int compare(Pattern a, Pattern b) {
			int sa = numberOfSlashes(a);
			int sb = numberOfSlashes(b);

			return (sa < sb) ? 1 : -1;
		}

		private int numberOfSlashes(Pattern pat) {
			String str = pat.pattern();

			int n = 0, pos = 0;
			while ((pos = str.indexOf("/", pos)) >= 0) {
				n++;
				pos++;
			}

			return n;
		}
	};

	// Processors maps
	private final SortedMap<Pattern, RequestProcessor> processors;
	private final SortedMap<Pattern, RequestProcessor> preprocessors;
	private final SortedMap<Pattern, RequestProcessor> postprocessors;

	private final SessionsStorage sessionsStorage;
	private final ClientsStorage clientStorage;

	public DefaultRequestProcessorsCollection(SessionsStorage sessionsStorage, ClientsStorage clientsStorage) {
		this.processors = new TreeMap<Pattern, RequestProcessor>(pattsComparator);
		this.preprocessors = new TreeMap<Pattern, RequestProcessor>(pattsComparator);
		this.postprocessors = new TreeMap<Pattern, RequestProcessor>(pattsComparator);

		this.clientStorage = clientsStorage;
		this.sessionsStorage = sessionsStorage;
	}

	@Override
	public void invokeRequestPreProcessor(HttpRequest req, HttpResponse resp, ChannelHandlerContext ctx,
			String sessionId) throws Exception {
		RequestProcessor proc = getAppropriateProcessor(preprocessors, req.getUri());

		try {
			if (proc != null)
				proc.processRequest(req, resp, ctx);
		} catch (Exception e) {
			throw new StopRequestProcessing();
		}
	}

	@Override
	public void invokeRequestProcessor(HttpRequest req, HttpResponse resp, ChannelHandlerContext ctx, String sessionId)
			throws Exception {
		RequestProcessor proc = getAppropriateProcessor(processors, req.getUri());

		if (proc != null)
			proc.processRequest(req, resp, ctx);
	}

	@Override
	public void invokeRequestPostProcessor(HttpRequest req, HttpResponse resp, ChannelHandlerContext ctx,
			String sessionId) throws Exception {
		RequestProcessor proc = getAppropriateProcessor(postprocessors, req.getUri());

		if (proc != null)
			proc.processRequest(req, resp, ctx);
	}

	@Override
	public void addProcessor(RequestProcessor processor, String regexp) {
		addToProcessors(processors, processor, regexp);
	}

	@Override
	public void addPreProcessor(RequestProcessor processor, String regexp) {
		addToProcessors(preprocessors, processor, regexp);
	}

	@Override
	public void addPostProcessor(RequestProcessor processor, String regexp) {
		addToProcessors(postprocessors, processor, regexp);
	}

	/**
	 * Return request processor by given processors map and URI.
	 * 
	 * @param map
	 * @param uri
	 * @return
	 */
	protected RequestProcessor getAppropriateProcessor(SortedMap<Pattern, RequestProcessor> map, String uri) {
		LOG.debug("Start matching URI: " + uri);

		// Get iterator
		Iterator<Entry<Pattern, RequestProcessor>> it = map.entrySet().iterator();

		while (it.hasNext()) {
			Entry<Pattern, RequestProcessor> ent = it.next();

			LOG.debug("Try match pattern: " + ent.getKey().pattern() + "  with  " + uri);

			if (ent.getKey().matcher(uri.trim()).matches())
				return ent.getValue();
		}

		return null;
	}

	/**
	 * Add processor to given processors map
	 * 
	 * @param map
	 * @param proc
	 * @param pat
	 */
	protected void addToProcessors(SortedMap<Pattern, RequestProcessor> map, RequestProcessor proc, String pat) {
		// Create RegExp pattern by GLOB
		String regexp = convertGlobToRegEx(pat);
		Pattern patc = Pattern.compile(regexp);

		// Init processor
		proc.init(sessionsStorage, clientStorage);

		LOG.debug("Add processor with pattern: " + patc.pattern());

		// Put to map
		map.put(patc, proc);
	}

	/**
	 * Convert GLOB pattern to RegExp
	 * 
	 * @param line
	 * @return
	 */
	private String convertGlobToRegEx(String glob) {
		String out = "^";
		for (int i = 0; i < glob.length(); ++i) {
			final char c = glob.charAt(i);
			switch (c) {
			case '*':
				out += ".*";
				break;
			case '?':
				out += '.';
				break;
			case '.':
				out += "\\.";
				break;
			case '\\':
				out += "\\\\";
				break;
			default:
				out += c;
			}
		}
		out += '$';
		return out;
	}
}
