/*******************************************************************************
 * Copyright 2011 Towee.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package net.gnisio.client.wrapper;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;

public class SocketRpcBuilder extends RpcRequestBuilder {

	/**
	 * @author c58
	 */
	protected static class SocketRequestBuilder extends RequestBuilder {
		private int requestId;

		public SocketRequestBuilder(String url) {
			super(RequestBuilder.POST.toString(), url);
		}

		public void setRequestId(int id) {
			requestId = id;
		}

		@Override
		public Request send() throws RequestException {
			return doSend(getRequestData(), getCallback());
		}

		@Override
		public Request sendRequest(String requestData, RequestCallback callback) throws RequestException {
			return doSend(requestData, callback);
		}

		protected Request doSend(final String data, final RequestCallback callback) {
			SocketRPCController.getInstance().sendRPCRequest(requestId, data, callback);
			return null;
		}
	}

	/**
	 * Create our WS request builder
	 */
	@Override
	protected RequestBuilder doCreate(String serviceEntryPoint) {
		return new SocketRequestBuilder(serviceEntryPoint);
	}

	protected void doSetRequestId(RequestBuilder rb, int id) {
		((SocketRequestBuilder) rb).setRequestId(id);
	}
}