package net.gnisio.server;

import static org.jboss.netty.channel.Channels.pipeline;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import net.gnisio.server.PacketsProcessor.ServerContext;
import net.gnisio.server.clients.ClientsStorage;
import net.gnisio.server.impl.DefaultPacketsProcessor;
import net.gnisio.server.impl.DefaultRequestProcessorsCollection;
import net.gnisio.server.impl.DefaultServerContext;
import net.gnisio.server.impl.MemoryClientsStorage;
import net.gnisio.server.impl.MemorySessionsStorage;
import net.gnisio.server.processors.RequestProcessorsCollection;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is main abstraction for creating your own server server.
 * 
 * @author c58
 */
public abstract class AbstractGnisioServer {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractGnisioServer.class);

	// for stopping the server
	private ServerBootstrap bootstrap;
	private Channel serverChannel;
	private SSLContext sslContext;
	private ServerContext servContext;

	public void start(int port) throws Exception {
		start("localhost", port);
	}

	public void start(final String host, int port) throws Exception {
		// Make storages
		final ClientsStorage clientsStorage = createClientsStorage();
		final SessionsStorage sessionsStorage = createSessionsStorage();

		// Make request processors collection
		final RequestProcessorsCollection requestProcessors = createRequestProcessorsCollection(sessionsStorage,
				clientsStorage);

		// Make remote service
		final AbstractRemoteService remoteService = createRemoteService(sessionsStorage, clientsStorage);
		remoteService.init(sessionsStorage);

		// Create server context
		servContext = createServerContext(sessionsStorage, clientsStorage, requestProcessors, remoteService, host,
				port, sslContext != null);

		// Make packet processor
		final PacketsProcessor packetsProcessor = createPacketsProcessor(servContext);

		// Create bootstrap
		ExecutorService bossExec = new OrderedMemoryAwareThreadPoolExecutor(1, 400000000, 2000000000, 60,
				TimeUnit.SECONDS);
		ExecutorService ioExec = new OrderedMemoryAwareThreadPoolExecutor(4, 400000000, 2000000000, 60,
				TimeUnit.SECONDS);
		bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(bossExec, ioExec, 4));

		// Set up the event pipeline factory.
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = pipeline();

				// If SSL enabled
				if (sslContext != null) {
					SSLEngine engine = sslContext.createSSLEngine();
					engine.setUseClientMode(false);
					pipeline.addLast("ssl", new SslHandler(engine));
				}

				// Set handlers
				pipeline.addLast("decoder", new HttpRequestDecoder());
				pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
				pipeline.addLast("encoder", new HttpResponseEncoder());
				pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
				pipeline.addLast("handler", new GnisioPipelineHandler(packetsProcessor));
				return pipeline;
			}
		});

		// Other options
		bootstrap.setOption("backlog", 500);
		bootstrap.setOption("connectTimeoutMillis", 10000);

		// Prepare request processors
		createRequestProcessors(requestProcessors);

		// Bind and start to accept incoming connections.
		this.serverChannel = bootstrap.bind(new InetSocketAddress(host, port));

		LOG.info("Server Started at host [" + host + "] and port [" + port + "]");
	}

	/**
	 * Override this method for making your own server context
	 * 
	 * @param sessionsStorage
	 * @param clientsStorage
	 * @param requestProcessors
	 * @param remoteService
	 * @param host
	 * @param port
	 * @param useSSL
	 * @return
	 */
	protected ServerContext createServerContext(SessionsStorage sessionsStorage, ClientsStorage clientsStorage,
			RequestProcessorsCollection requestProcessors, AbstractRemoteService remoteService, String host, int port,
			boolean useSSL) {
		return new DefaultServerContext(sessionsStorage, clientsStorage, remoteService, requestProcessors, useSSL,
				host, port);
	}

	/**
	 * Override this method for creating some other packets processor. Default
	 * packets processor works in 8 threads. This number of threads used because
	 * any client in general use 2 connection (client-server and server-client),
	 * and one of this thread can block other one. For example when one thread
	 * send frame and in this moment other thread set client as connected.
	 * 
	 * @param sessionsStorage
	 * @param clientsStorage
	 * @param requestProcessors
	 * @param remoteService
	 * @param host
	 * @return
	 */
	protected PacketsProcessor createPacketsProcessor(ServerContext servContext) {
		return new DefaultPacketsProcessor(servContext, 8);
	}

	/**
	 * Stop the server
	 */
	public void stop() {
		serverChannel.close();
	}

	/**
	 * This method adding some request processors
	 * 
	 * @param requestProcessors
	 * @throws Exception
	 */
	protected abstract void createRequestProcessors(RequestProcessorsCollection requestProcessors) throws Exception;

	/**
	 * This method create remote service
	 * 
	 * @param sessionsStorage
	 * @param clientsStorage
	 * @return
	 */
	protected abstract AbstractRemoteService createRemoteService(SessionsStorage sessionsStorage,
			ClientsStorage clientsStorage);

	/**
	 * Override this method for using some specific SessionStorage. By default
	 * this method return {@link MemorySessionStorage}
	 * 
	 * @return
	 */
	protected SessionsStorage createSessionsStorage() {
		return new MemorySessionsStorage();
	}

	/**
	 * Override this method for using some specific ClientsStorage. By default
	 * this method return {@link MemoryClientStorage}
	 * 
	 * @return
	 */
	protected ClientsStorage createClientsStorage() {
		return new MemoryClientsStorage();
	}

	/**
	 * Override this method for using some specific RequestProcessorsCollection.
	 * By default this method return {@link DefaultRequestProcessorsCollection}
	 * 
	 * @param clientsStorage
	 * @param sessionsStorage
	 * @return
	 */
	protected RequestProcessorsCollection createRequestProcessorsCollection(SessionsStorage sessionsStorage,
			ClientsStorage clientsStorage) {
		return new DefaultRequestProcessorsCollection(sessionsStorage, clientsStorage);
	}

	protected void setSSLEnabled(String certPath, String certPass) {
		setSSLEnabled(certPath, certPass, "SunX509");
	}

	/**
	 * Enable SSL support
	 * 
	 * @param certPath
	 * @param certPass
	 * @param algorithm
	 */
	protected void setSSLEnabled(String certPath, String certPass, String algorithm) {
		try {
			SSLContext serverContext;
			try {
				KeyStore ks = KeyStore.getInstance("JKS");
				FileInputStream fin = new FileInputStream(certPath);
				ks.load(fin, certPass.toCharArray());

				// Set up key manager factory to use our key store
				// Assume key password is the same as the key store file
				// password
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
				kmf.init(ks, certPass.toCharArray());

				// Initialise the SSLContext to work with our key managers.
				serverContext = SSLContext.getInstance("TLS");
				serverContext.init(kmf.getKeyManagers(), null, null);
			} catch (Exception e) {
				throw new Error("Failed to initialize the server-side SSLContext", e);
			}

			sslContext = serverContext;
		} catch (Exception ex) {
			LOG.error("Error initializing SslContextManager. " + ex.getMessage(), ex);
			System.exit(1);
		}
	}
}
