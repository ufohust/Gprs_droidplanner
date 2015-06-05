package com.geeksville.apiproxy;

import java.io.IOException;

public class APIException extends IOException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8067504823120048443L;

    APIException(String reason) {
        super(reason);
    }
}
