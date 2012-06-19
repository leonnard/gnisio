package net.gnisio.server;

import static org.jboss.netty.channel.Channels.pipeline;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import net.gnisio.server.clients.ClientsStorage;
import net.gnisio.server.impl.DefaultRequestProcessorsCollection;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGnisioServer {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractGnisioServer.class);

	private ServerBootstrap bootstrap;
	private Channel serverChannel;

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
		remoteService.init( sessionsStorage );

		// Create bootstrap
		bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));

		// Set up the event pipeline factory.
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = pipeline();
				pipeline.addLast("decoder", new HttpRequestDecoder());
				pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
				pipeline.addLast("encoder", new HttpResponseEncoder());
				pipeline.addLast("handler", new GnisioPipelineHandler(sessionsStorage, clientsStorage,
						requestProcessors, remoteService, host));
				return pipeline;
			}
		});

		// Prepare request processors
		createRequestProcessors(requestProcessors);

		// Bind and start to accept incoming connections.
		this.serverChannel = bootstrap.bind(new InetSocketAddress(host, port));

		LOG.info("Server Started at port [" + port + "]");
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
}
