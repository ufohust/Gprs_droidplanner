package com.playuav.android.dialogs.openfile;

import com.playuav.android.utils.file.IO.MissionReader;

public abstract class OpenMissionDialog extends OpenFileDialog {
	public abstract void waypointFileLoaded(MissionReader reader);

	@Override
	protected FileReader createReader() {
		return new MissionReader();
	}

	@Override
	protected void onDataLoaded(FileReader reader) {
		waypointFileLoaded((MissionReader) reader);
	}
}
