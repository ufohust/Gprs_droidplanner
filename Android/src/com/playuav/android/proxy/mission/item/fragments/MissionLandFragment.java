package com.playuav.android.proxy.mission.item.fragments;

import com.playuav.android.R;

import com.o3dr.services.android.lib.drone.mission.MissionItemType;

public class MissionLandFragment extends MissionDetailFragment {

	@Override
	protected int getResource() {
		return R.layout.fragment_editor_detail_land;
	}

	@Override
	public void onApiConnected(){
        super.onApiConnected();
        typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.LAND));
    }

}
