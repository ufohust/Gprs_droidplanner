package com.geeksville.apiproxy;

import java.io.*;

/**
 * A utility that scans through all suitable files in a src directory, uploading
 * them and then moving them to a 'sent' directory.
 */
public class DirectoryUploader {

	private File srcDir;
	private File destDir;
	private IUploadListener callback;
	private String userId, userPass, vehicleId;
	private String apiKey;
	private String privacy;

	public DirectoryUploader(File srcDir, File destDir,
			IUploadListener callback, String userId, String userPass,
			String vehicleId, String apiKey, String privacy) {
		this.srcDir = srcDir;
		this.destDir = destDir;
		this.callback = callback;
		this.userId = userId;
		this.userPass = userPass;
		this.vehicleId = vehicleId;
		this.apiKey = apiKey;
		this.privacy = privacy;
	}

	public void run() {

		File[] files = srcDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".tlog");
			}
		});

        File processing = null;
		try {
			for (File f : files) {
                processing = f;
				callback.onUploadStart(f);
				String url = RESTClient.doUpload(f, userId, userPass,
						vehicleId, apiKey, privacy);

				destDir.mkdirs();
				File newName = new File(destDir, f.getName());
				f.renameTo(newName);

				callback.onUploadSuccess(f, url);
			}
		} catch (IOException ex) {
			// If the server returns any IO or HTTP exceptions, report _one_
			// failure to the application
			// but then stop scanning until asked to scan again.

			callback.onUploadFailure(processing, ex);
		}
	}
}
