/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.gnisio.server;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Socket.IO packet for versions 0.8.x
 * @author c58
 */
public class SocketIOFrame {
	public static final char SEPERATOR_CHAR = ':';

	/**
	 * Frame error
	 * 
	 * @author c58
	 */
	public enum FrameErrorReason {
		UNKNOWN(-1), TRANSPORT_NOT_SUPPORTED(0), CLIENT_NOT_HANDSHAKEN(1), UNAUTHORIZED(2);

		private int value;

		FrameErrorReason(int value) {
			this.value = value;
		}

		public int value() {
			return value;
		}

		public static FrameErrorReason fromInt(int val) {
			switch (val) {
			case 0:
				return TRANSPORT_NOT_SUPPORTED;
			case 1:
				return CLIENT_NOT_HANDSHAKEN;
			case 2:
				return UNAUTHORIZED;
			default:
				return UNKNOWN;
			}
		}
	}

	/**
	 * Advice on error
	 * 
	 * @author c58
	 */
	public enum FrameErrorAdvice {
		UNKNOWN(-1), RECONNECT(0);

		private int value;

		FrameErrorAdvice(int value) {
			this.value = value;
		}

		public int value() {
			return value;
		}

		public static FrameErrorAdvice fromInt(int val) {
			if (val == 0)
				return RECONNECT;
			else
				return UNKNOWN;
		}
	}

	/**
	 * Frame type representation
	 * 
	 * @author c58
	 */
	public enum FrameType {
		UNKNOWN(-1), DISCONNECT(0), CONNECT(1), HEARTBEAT(2), MESSAGE(3), JSON(4), EVENT(5), ACK(6), ERROR(
				7), NOOP(8);

		private int value;

		FrameType(int value) {
			this.value = value;
		}

		public int value() {
			return value;
		}

		public static FrameType fromInt(int val) {
			switch (val) {
			case 0:
				return DISCONNECT;
			case 1:
				return CONNECT;
			case 2:
				return HEARTBEAT;
			case 3:
				return MESSAGE;
			case 4:
				return JSON;
			case 5:
				return EVENT;
			case 6:
				return ACK;
			case 7:
				return ERROR;
			case 8:
				return NOOP;

			default:
				return UNKNOWN;
			}
		}
	}

	// Pattern for getting pieces of packet
	private static final Pattern decodePat = Pattern
			.compile("([^:]+):([0-9]+)?(\\+)?:([^:]+)?:?([\\s\\S]*)?");

	/**
	 * Decode single packet and return SocketIOFrame instance
	 * 
	 * @return
	 */
	public static SocketIOFrame decodePacket(String raw) {
		// Create pattern matcher
		Matcher matcher = decodePat.matcher(raw);

		// If our data not in given format return null
		if (!matcher.matches())
			return null;

		// Get id, data and endPoint
		String data = matcher.group(5);
		String endPoint = matcher.group(4);
		String packetData = "";
		boolean isJson = false;
		String qs = "";

		// Get message type
		FrameType type = FrameType.fromInt(Integer.parseInt(matcher.group(1)));
		FrameErrorReason reason = null;
		FrameErrorAdvice advice = null;

		switch (type) {
		case MESSAGE:
			packetData = data;
			break;
		case JSON:
			packetData = data;
			isJson = true;
			break;
		case CONNECT:
			qs = data;
			break;
		case ERROR:
			String[] pieces = data.split("+");
			reason = FrameErrorReason.fromInt(Integer.parseInt(pieces[0]));
			advice = FrameErrorAdvice.fromInt(Integer.parseInt(pieces[1]));
		}

		return new SocketIOFrame(type, packetData, isJson, endPoint, qs, reason, advice);
	}

	/**
	 * Decode multiple frames
	 * 
	 * @param raw
	 * @return
	 */
	public static List<SocketIOFrame> decodePayload(String raw) {
		List<SocketIOFrame> result = new ArrayList<SocketIOFrame>();

		// If raw data is empty return empty result
		if (raw == null || raw.isEmpty())
			return result;

		// If start symbol is what we need
		if (raw.charAt(0) == '\ufffd') {
			String lengthBuffer = "";

			for (int i = 1; i < raw.length(); i++) {
				if (raw.charAt(i) == '\ufffd') {
					// Get length from buffer
					int intLength = Integer.parseInt(lengthBuffer);

					// Parse message
					result.add(decodePacket(raw.substring(i + 1, i + 1 + intLength)));

					// Clear tmp data
					i += intLength + 1;
					lengthBuffer = "";
				} else {
					lengthBuffer += raw.charAt(i);
				}
			}
		} else
			result.add(decodePacket(raw));

		return result;
	}

	/**
	 * Encode one packet
	 * 
	 * @param frame
	 * @return
	 */
	public static String encodePacket( SocketIOFrame frame ) {
		// Data to send
		String data = null;
		
		// Set data to send
		switch(frame.getFrameType()) {
		case MESSAGE:
		case JSON:
			data = frame.getData();
			break;
	    case CONNECT:
	    	data = frame.getQs();
	    	break;
		case ERROR:
			data = frame.getErrorReason().value() + "+" + (frame.getErrorAdvice() != null ? frame.getErrorAdvice().value() : "");
		    break;
		}
		  
		// Construct packet with required fragments
		String encoded = frame.getFrameType().value() + "::";

		// Data fragment is optional
		if (data != null && !data.isEmpty())
			encoded += ":" + data;

		return encoded;
	}

	/**
	 * Encode payload of packets
	 * 
	 * @param frames
	 * @return
	 */
	public static String encodePayload(List<SocketIOFrame> frames) {
		// For single request
		if( frames.size() == 1 )
			return encodePacket( frames.get(0) );
		
		// For multiple requests
		StringBuilder builder = new StringBuilder();
		for(SocketIOFrame f : frames){
			String encodedPacket = encodePacket(f);
			builder.append('\ufffd');
			builder.append( encodedPacket.length() );
			builder.append('\ufffd');
			builder.append( encodePacket(f) );
		}

		return builder.toString();
	}
	
	/**
	 * Message makers
	 */
	public static SocketIOFrame makeHeartbeat() {
		return new SocketIOFrame(FrameType.HEARTBEAT, null, null, null, null, null, null);
	}
	
	public static SocketIOFrame makeMessage(String data) {
		return new SocketIOFrame(FrameType.MESSAGE, data, false, "", "", null, null);
	}
	
	public static SocketIOFrame makeConnect() {
		return new SocketIOFrame(FrameType.CONNECT, null, null, null, null, null, null);
	}

	// Data containers
	private final FrameType frameType;
	private final FrameErrorReason errorReason;
	private final FrameErrorAdvice errorAdvice;
	private final String data;
	private final String endPoint;
	private final String qs;
	private final boolean isJson;

	/**
	 * Constructor for creating new socket.io frame
	 * 
	 * @param type
	 * @param packetData
	 * @param endPoint2
	 * @param qs
	 * @param reason
	 * @param advice
	 */
	public SocketIOFrame(FrameType type, String packetData, Boolean isJson, String endPoint2, String qs,
			FrameErrorReason reason, FrameErrorAdvice advice) {
		this.isJson = isJson == null ? false : isJson;
		this.frameType = type;
		this.data = packetData != null ? packetData : "";
		this.endPoint = endPoint2;
		this.qs = qs;
		this.errorReason = reason;
		this.errorAdvice = advice;
	}

	public FrameType getFrameType() {
		return frameType;
	}

	public String getData() {
		return data;
	}

	public String getEndPoint() {
		return endPoint;
	}

	public String getQs() {
		return qs;
	}
	
	public boolean isJson() {
		return isJson;
	}

	public FrameErrorReason getErrorReason() {
		return errorReason;
	}

	public FrameErrorAdvice getErrorAdvice() {
		return errorAdvice;
	}
	
	public String encode() {
		return encodePacket( this ); 
	}
	
	@Override
	public String toString() {
		return " ["+frameType+":"+data+"] ";
	}

}
