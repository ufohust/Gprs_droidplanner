package com.geeksville.apiproxy;

import com.geeksville.dapi.Webapi.ShowMsg;

public class CallbackLaterException extends APIException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8595682932382088086L;

	public CallbackLaterException(ShowMsg message, int callbackDelay) {
		super("Callback later:" + message.getText());
	}

}
