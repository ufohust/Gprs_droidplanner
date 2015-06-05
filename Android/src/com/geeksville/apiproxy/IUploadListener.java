package com.geeksville.apiproxy;

import java.io.File;

/**
 * A set of callbacks for tracking DirectoryUploader progress.
 */
public interface IUploadListener {

	void onUploadStart(File f);

	/**
	 * Called to inform client of upload success
	 * 
	 * @param viewURL
	 *            the URL the user should be shown to view this flight (CAN BE
	 *            NULL - if null the server thought the flight was boring, do
	 *            not show to user)
	 */
	void onUploadSuccess(File f, String viewURL);

	void onUploadFailure(File f, Exception ex);
}
