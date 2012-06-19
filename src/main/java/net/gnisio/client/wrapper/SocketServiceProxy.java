package net.gnisio.client.wrapper;

import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter.ResponseReader;
import com.google.gwt.user.client.rpc.impl.RpcStatsContext;
import com.google.gwt.user.client.rpc.impl.Serializer;

public abstract class SocketServiceProxy extends RemoteServiceProxy {

	public SocketServiceProxy(String moduleBaseURL, String remoteServiceRelativePath, String serializationPolicyName,
			Serializer serializer) {
		super(moduleBaseURL, remoteServiceRelativePath, serializationPolicyName, serializer);

		// Set custom RPC request builder
		setRpcRequestBuilder(new SocketRpcBuilder());

		// Set serialization policy
		SocketRPCController.getInstance().setSerializationPolicy(getSerializationPolicy());
	}
	
	protected <T> void doCreatePushEventHandler(ResponseReader responseReader, String eventName, AsyncCallback<T> callback){
		// Create request callback
		RequestCallback reqCallback = new RequestCallbackAdapter<T>(this, eventName, new RpcStatsContext(),
		        callback, getRpcTokenExceptionHandler(), responseReader);
		
		// Register it in socket controller
		SocketRPCController.getInstance().registerPushEvent(eventName, reqCallback);
	}

	public abstract String getSerializationPolicy();

}
