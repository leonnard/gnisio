package net.gnisio.rebind;

import java.util.HashMap;
import java.util.Map;

import net.gnisio.client.wrapper.PushClass;
import net.gnisio.client.wrapper.SocketServiceProxy;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter.ResponseReader;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.rpc.ProxyCreator;
import com.google.gwt.user.rebind.rpc.SerializableTypeOracle;

/**
 * Creates a client-side proxy for a
 * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService} interface
 * as well as the necessary type and field serializers.
 */
public class CustomProxyCreator extends ProxyCreator {

	private static final Map<Class<?>, ResponseReader> TYPE_TO_RESPONSEREADER = new HashMap<Class<?>, ResponseReader>();

	{
		TYPE_TO_RESPONSEREADER.put(Boolean.class, ResponseReader.BOOLEAN);
		TYPE_TO_RESPONSEREADER.put(Byte.class, ResponseReader.BYTE);
		TYPE_TO_RESPONSEREADER.put(Character.class, ResponseReader.CHAR);
		TYPE_TO_RESPONSEREADER.put(Double.class, ResponseReader.DOUBLE);
		TYPE_TO_RESPONSEREADER.put(Float.class, ResponseReader.FLOAT);
		TYPE_TO_RESPONSEREADER.put(Integer.class, ResponseReader.INT);
		TYPE_TO_RESPONSEREADER.put(Long.class, ResponseReader.LONG);
		TYPE_TO_RESPONSEREADER.put(Short.class, ResponseReader.SHORT);
		TYPE_TO_RESPONSEREADER.put(Void.class, ResponseReader.VOID);
		TYPE_TO_RESPONSEREADER.put(String.class, ResponseReader.STRING);
	}

	public CustomProxyCreator(JClassType serviceIntf) {
		super(serviceIntf);
	}

	@Override
	protected Class<? extends RemoteServiceProxy> getProxySupertype() {
		return SocketServiceProxy.class;
	}

	/**
	 * Override for getting push events
	 */
	protected void generateProxyMethods(SourceWriter w, SerializableTypeOracle serializableTypeOracle,
			TypeOracle typeOracle, Map<JMethod, JMethod> syncMethToAsyncMethMap) {
		
		// Generate methods with skipping handleEvent method
	    JMethod[] syncMethods = serviceIntf.getOverridableMethods();
	    for (JMethod syncMethod : syncMethods) {

	      JMethod asyncMethod = syncMethToAsyncMethMap.get(syncMethod);
	      assert (asyncMethod != null);
	      
	      if(asyncMethod.getName().equals("handleEvent"))
	    	  continue;

	      JClassType enclosingType = syncMethod.getEnclosingType();
	      JParameterizedType isParameterizedType = enclosingType.isParameterized();
	      if (isParameterizedType != null) {
	        JMethod[] methods = isParameterizedType.getMethods();
	        for (int i = 0; i < methods.length; ++i) {
	          if (methods[i] == syncMethod) {
	            /*
	             * Use the generic version of the method to ensure that the server
	             * can find the method using the erasure of the generic signature.
	             */
	            syncMethod = isParameterizedType.getBaseType().getMethods()[i];
	          }
	        }
	      }

	      generateProxyMethod(w, serializableTypeOracle, typeOracle, syncMethod, asyncMethod);
	    }
		
		String pushEventName = serviceIntf.getQualifiedSourceName()+".PushEvent";
		JClassType pushEventType = serviceIntf.getOracle().findType( pushEventName );
		
		w.println();

		// Write the method signature
		w.println("@Override");
		w.print("public <T> void handleEvent(");
		w.print("net.gnisio.shared.PushEventType event, ");
		w.println("com.google.gwt.user.client.rpc.AsyncCallback<T> callback) {");
		w.println(pushEventName+" eventCasted = ("+pushEventName+")event;");
		w.println("switch( eventCasted ) {");

		for(JField event : pushEventType.getFields()) {
			w.println("case "+event.getName()+": ");
			w.println("doCreatePushEventHandler( ");
			w.print("ResponseReader." + getResponseReaderFor(event.getAnnotation(PushClass.class).value()).name());
			w.println(", \""+ pushEventName+"."+event.getName() +"\", callback );");
			w.println("break;");
		}
		
		w.println("}");
		w.println("}");
	}

	private ResponseReader getResponseReaderFor(Class<?> returnType) {
		ResponseReader reader = TYPE_TO_RESPONSEREADER.get( returnType );
		return reader != null ? reader : ResponseReader.OBJECT;
	}

	/**
	 * Generate any fields required by the proxy.
	 * 
	 * @param serializableTypeOracle
	 *            the type oracle
	 */
	protected void generateProxyFields(SourceWriter srcWriter, SerializableTypeOracle serializableTypeOracle,
			String serializationPolicyStrongName, String remoteServiceInterfaceName) {

		// Write fields
		super.generateProxyFields(srcWriter, serializableTypeOracle, serializationPolicyStrongName,
				remoteServiceInterfaceName);

		// Write abstract method for wrapping serialization policy
		srcWriter.println("public String getSerializationPolicy() {");
		srcWriter.indent();
		srcWriter.println("return \"" + serializationPolicyStrongName + "\";");
		srcWriter.outdent();
		srcWriter.println("}");
		srcWriter.println();
	}
}
