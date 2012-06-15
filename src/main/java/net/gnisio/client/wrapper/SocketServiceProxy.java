package net.gnisio.client.wrapper;



import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter.ResponseReader;
import com.google.gwt.user.client.rpc.impl.RpcStatsContext;
import com.google.gwt.user.client.rpc.impl.Serializer;

public abstract class SocketServiceProxy extends RemoteServiceProxy {

	public SocketServiceProxy(String moduleBaseURL, String remoteServiceRelativePath,
			String serializationPolicyName, Serializer serializer) {
		super(moduleBaseURL, remoteServiceRelativePath, serializationPolicyName, serializer);
		
		// Set custom RPC request builder
		setRpcRequestBuilder(new SocketRpcBuilder());
		
		// Set serialization policy
		SocketRPCController.getInstance().setSerializationPolicy( getSerializationPolicy() );
	}

	protected <T> Request doRegisterPushMethod(ResponseReader responseReader, String methodName,
			RpcStatsContext statsContext, AsyncCallback<T> callback) {

		// Create request callback
		RequestCallback reqCallback = doCreateRequestCallback(responseReader, methodName, statsContext,
				callback);

		// Register it in socket controller
		SocketRPCController.getInstance().registerPushMethod(methodName, reqCallback);
		return null;
	}
	
	public abstract String getSerializationPolicy();
}
