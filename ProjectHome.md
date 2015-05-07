# MOVED TO BitBucket: https://bitbucket.org/c58/gnisio #
## Description ##
Lightweight implementation of Socket.IO protocol applied to GWT RPC. As result - server-push in GWT!
Also this library provide lightweight request processors (something like servlets) for any kind of thing that you may imagine :) It also provides SSL.

Enjoy! And bug report please.

P.S - If you want using it in prodaction - be careful, it's not tested well!.

## Futures ##
  * Netty HTTP server for GWT RPC
  * GWT RPC requests works through Socket.IO protocol
  * Server push. Integrated to general GWT RPC
  * Authority levels for GWT RPC methods
  * Fully SSL support
  * Very simple to use

### Implemented Socket.IO transports ###
_websocket, htmlfile, xhr-polling, jsonp-polling_

I remove flashsocket (with any client-side code applied to flash) because i hate it (flash) :)

## Example of usage ##
In separated repository you can find simple chat.

## How to use ##
It's very simple.
### Client side ###
Create regular "**...Service**" and "**...ServiceAsync**" but implementing SocketIOService and SocketIOServiceAsync.
```
@RemoteServiceRelativePath("socket.io")
public interface GreetingService extends SocketIOService {
	
	public enum PushEvent implements PushEventType {
		@PushClass(String.class)  TEST_EVENT,
		@PushClass(Integer.class) TEST_EVENT_1
	}
	
        @AuthorityLevel(0)
	String greetServer(String name) throws IllegalArgumentException;
}

public interface GreetingServiceAsync extends SocketIOServiceAsync {
	
	void greetServer(String input, AsyncCallback<String> callback)
			throws IllegalArgumentException;
}
```

As you can see, _GreetingService_ interface contains some Enum implementing _PushEventType_. This enum is events, that you want push on server side and handle on client side. This enum **MUST** be named _PushEvent_ and **MUST** implements _PushEventType_. Any event **MUST** be annotated with _@PushClass_ with any serializable Class value. It needs for validating received data from server.

For handling any event you need create Service as you do it with general GWT services
```
private final GreetingServiceAsync greetingService = GWT
			.create(GreetingService.class);
	
public void onModuleLoad() {
	greetingService.handleEvent(PushEvent.TEST_EVENT, new AsyncCallback<String>() {
		@Override
		public void onFailure(Throwable caught) {	
		}

		@Override
		public void onSuccess(String result) {
			Window.alert(result);
		}
	});
}
```

... and for handling event, just add handler by invoking _handleEvent_ method of created service. Very simple!

### Server side ###
First of all you need create class extending _AbstractGnisioServer_, Something like this...
```
public class ExampleChat extends AbstractGnisioServer {

	public static void main(String[] args) {
		try {
			new ExampleChat().start(3001);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void createRequestProcessors(RequestProcessorsCollection requestProcessors) throws Exception {
		requestProcessors.addProcessor(new StaticContentProcessor("./war"), "/*");
		requestProcessors.addProcessor(new SocketIOScriptProcessor(), "/socket.io/socket.io.js");
	}

	@Override
	protected AbstractRemoteService createRemoteService(SessionsStorage sessionsStorage, ClientsStorage clientsStorage) {
		return new GreetingServiceImpl("./war");
	}

}
```

It's entry point of your server. You must implement  _createRequestProcessors_ and _createRemoteService_ methods. First method is similar as you adding servlets for Jetty for different URI patterns. Second method must return your GWT RPC service. This service is...

```
public class GreetingServiceImpl extends AbstractRemoteService implements
		GreetingService {

	public GreetingServiceImpl(String gwtAppLocation) {
		super(gwtAppLocation);
	}

	public String greetServer(String input) throws IllegalArgumentException {
		// Verify that the input is valid. 
		if (!FieldVerifier.isValidName(input)) {
			// If the input is not valid, throw an IllegalArgumentException back to
			// the client.
			throw new IllegalArgumentException(
					"Name must be at least 4 characters long");
		}
		
		pushEvent(PushEvent.TEST_EVENT, "Yahooo!!! It works!", "/");
		getSession().setAuthorityLevel(1);
		return "Hello, " + input + "!<br><br>I am running ";
	}

	@Override
	public void onClientConnected() {
		addSubscriber("/");
	}

	@Override
	public void onClientDisconnected() {
		removeSubscriber();
	}
	
}
```

Your service must extends _AbstractRemoteService_ and implement your service interface, as you do it in general GWT RPC remote service. But you must implement two methods:  _onClientConnected_ and  _onClientDisconnected_. This methods invoked when client connected to server and disconnected from server. In this methods you can subscribe connected client for any _nodes_ of events. For pushing some event for all (expect current client) connected clients and subscribed for some node, you just need invoke _pushEvent_ with some event type, result object and _node_. Simple again!