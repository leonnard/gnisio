package net.gnisio.shared;

public class RPCUtils {
	public static String hexToLength(String hex, int needLen) {
		while (hex.length() < needLen) {
			hex = "0" + hex;
		}

		return hex;
	}
}
