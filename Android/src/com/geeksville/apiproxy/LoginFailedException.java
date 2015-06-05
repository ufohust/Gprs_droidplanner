package com.geeksville.apiproxy;

import com.geeksville.dapi.Webapi.ShowMsg;

public class LoginFailedException extends APIException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8014892179413821461L;

	public LoginFailedException(ShowMsg message) {
		super("LoginFailed:" + message.getText());
	}

}
