package net.gnisio.rebind;

import java.util.HashMap;
import java.util.Map;

import net.gnisio.client.wrapper.Push;
import net.gnisio.client.wrapper.SocketServiceProxy;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.generator.NameFactory;
import com.google.gwt.http.client.Request;
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

	private static final Map<JPrimitiveType, ResponseReader> JPRIMITIVETYPE_TO_RESPONSEREADER = new HashMap<JPrimitiveType, ResponseReader>();

	{
		JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.BOOLEAN, ResponseReader.BOOLEAN);
		JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.BYTE, ResponseReader.BYTE);
		JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.CHAR, ResponseReader.CHAR);
		JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.DOUBLE, ResponseReader.DOUBLE);
		JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.FLOAT, ResponseReader.FLOAT);
		JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.INT, ResponseReader.INT);
		JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.LONG, ResponseReader.LONG);
		JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.SHORT, ResponseReader.SHORT);
		JPRIMITIVETYPE_TO_RESPONSEREADER.put(JPrimitiveType.VOID, ResponseReader.VOID);
	}

	public CustomProxyCreator(JClassType serviceIntf) {
		super(serviceIntf);
	}

	@Override
	protected void generateProxyMethod(SourceWriter w, SerializableTypeOracle serializableTypeOracle,
			TypeOracle typeOracle, JMethod syncMethod, JMethod asyncMethod) {
		if (syncMethod.getAnnotation(Push.class) != null) {
			generateProxyPushMethod(w, serializableTypeOracle, typeOracle, syncMethod, asyncMethod);
		} else
			super.generateProxyMethod(w, serializableTypeOracle, typeOracle, syncMethod, asyncMethod);
	}

	@Override
	protected Class<? extends RemoteServiceProxy> getProxySupertype() {
		return SocketServiceProxy.class;
	}

	private void generateProxyPushMethod(SourceWriter w, SerializableTypeOracle serializableTypeOracle,
			TypeOracle typeOracle, JMethod syncMethod, JMethod asyncMethod) {

		w.println();

		// Write the method signature
		JType asyncReturnType = asyncMethod.getReturnType().getErasedType();
		w.print("public ");
		w.print(asyncReturnType.getQualifiedSourceName());
		w.print(" ");
		w.print(asyncMethod.getName() + "(");

		boolean needsComma = false;
		NameFactory nameFactory = new NameFactory();
		JParameter[] asyncParams = asyncMethod.getParameters();
		for (int i = 0; i < asyncParams.length; ++i) {
			JParameter param = asyncParams[i];

			if (needsComma) {
				w.print(", ");
			} else {
				needsComma = true;
			}

			/*
			 * Ignoring the AsyncCallback parameter, if any method requires a
			 * call to SerializationStreamWriter.writeObject we need a try catch
			 * block
			 */
			JType paramType = param.getType();
			paramType = paramType.getErasedType();

			w.print(paramType.getQualifiedSourceName());
			w.print(" ");

			String paramName = param.getName();
			nameFactory.addName(paramName);
			w.print(paramName);
		}

		w.println(") {");
		w.indent();

		String statsContextName = nameFactory.createName("statsContext");
		generateRpcStatsContext(w, syncMethod, asyncMethod, statsContextName);

		/*
		 * Depending on the return type for the async method, return a Request
		 * or nothing at all.
		 */
		if (asyncReturnType == JPrimitiveType.VOID) {
			w.print("doRegisterPushMethod(");
		} else if (asyncReturnType.getQualifiedSourceName().equals(Request.class.getName())) {
			w.print("return doRegisterPushMethod(");
		} else {
			// This method should have been caught by
			// RemoteServiceAsyncValidator
			throw new RuntimeException("Unhandled return type " + asyncReturnType.getQualifiedSourceName());
		}

		JParameter callbackParam = asyncParams[asyncParams.length - 1];
		String callbackName = callbackParam.getName();
		JType returnType = syncMethod.getReturnType();
		w.print("ResponseReader." + getResponseReaderFor(returnType).name());
		w.println(", \"" + getProxySimpleName() + "." + syncMethod.getName() + "\", " + statsContextName + ", "
				+ callbackName + ");");

		w.outdent();
		w.println("}");
	}

	private ResponseReader getResponseReaderFor(JType returnType) {
		if (returnType.isPrimitive() != null) {
			return JPRIMITIVETYPE_TO_RESPONSEREADER.get(returnType.isPrimitive());
		}

		if (returnType.getQualifiedSourceName().equals(String.class.getCanonicalName())) {
			return ResponseReader.STRING;
		}

		return ResponseReader.OBJECT;
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
