package com.playuav.android.activities.interfaces;

import com.o3dr.services.android.lib.coordinate.LatLong;

import com.playuav.android.proxy.mission.item.MissionItemProxy;

public interface OnEditorInteraction {
	public boolean onItemLongClick(MissionItemProxy item);

	public void onItemClick(MissionItemProxy item, boolean zoomToFit);

	public void onMapClick(LatLong coord);

	public void onListVisibilityChanged();
}
