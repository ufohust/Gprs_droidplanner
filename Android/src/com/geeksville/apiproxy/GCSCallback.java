package com.geeksville.apiproxy;

/**
 * Operations the GCS must implement to support the API proxy
 * 
 * @author kevinh
 * 
 */
public interface GCSCallback {
	/**
	 * The APIAdapter would like the GCS to send the indicated packet (GCS
	 * should perform processing very similar to if it created the packet
	 * itself)
	 * 
	 * @param packet
	 */
	void sendMavlink(byte[] packet);
}
