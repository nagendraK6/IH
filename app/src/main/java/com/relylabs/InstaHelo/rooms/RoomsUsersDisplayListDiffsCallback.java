package com.relylabs.InstaHelo.rooms;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.relylabs.InstaHelo.models.UsersInRoom;

import java.util.ArrayList;

public class RoomsUsersDisplayListDiffsCallback extends DiffUtil.Callback {
    private final ArrayList<UsersInRoom> mOldPostingDetailsList;
    private final ArrayList<UsersInRoom> mNewPostingDetailsList;

    public RoomsUsersDisplayListDiffsCallback(ArrayList<UsersInRoom> mOldPostingDetailsList, ArrayList<UsersInRoom> mNewPostingDetailsList) {
        this.mOldPostingDetailsList = mOldPostingDetailsList;
        this.mNewPostingDetailsList = mNewPostingDetailsList;
    }
    @Override
    public int getOldListSize() {
        return mOldPostingDetailsList.size();
    }
    @Override
    public int getNewListSize() {
        return mNewPostingDetailsList.size();
    }
    // for matching the items data type (usually same therefore true)
    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return mNewPostingDetailsList.get(newItemPosition).UserId == mOldPostingDetailsList.get(oldItemPosition).UserId;
    }

    // checking the data of the model (exactly same or not)
    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        final UsersInRoom oldPostingDetail = mOldPostingDetailsList.get(oldItemPosition);
        final UsersInRoom newPostingDetail = mNewPostingDetailsList.get(newItemPosition);
        return oldPostingDetail.UserId.equals(newPostingDetail.UserId) &&
                oldPostingDetail.Name.equals(newPostingDetail.Name)  &&
                oldPostingDetail.IsMuted.equals(newPostingDetail.IsMuted) &&
                oldPostingDetail.profileImageURL.equals(newPostingDetail.profileImageURL) &&
                oldPostingDetail.IsSpeaker.equals(newPostingDetail.IsSpeaker) &&
                oldPostingDetail.IsdataFetchRequired.equals(newPostingDetail.IsdataFetchRequired);
    }
    // WE will be comparing every element of model and putting the //element with change in a bundle
    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        UsersInRoom oldItem = mOldPostingDetailsList.get(oldItemPosition);
        UsersInRoom newItem = mNewPostingDetailsList.get(newItemPosition);
        Bundle diff = new Bundle();
        if (!newItem.Name.equals(oldItem.Name)) {
            diff.putString("Name", newItem.Name);
        }
        if (!newItem.IsMuted.equals(oldItem.IsMuted)) {
            diff.putBoolean("IsMuted", newItem.IsMuted);
        }

        if (!newItem.UserId.equals(oldItem.UserId)) {
            diff.putInt("UserId", newItem.UserId);
        }

        if (!newItem.profileImageURL.equals(oldItem.profileImageURL)) {
            diff.putString("profileImageURL", newItem.profileImageURL);
        }

        if (!newItem.IsdataFetchRequired.equals(oldItem.IsdataFetchRequired)) {
            diff.putBoolean("IsdataFetchRequired", newItem.IsdataFetchRequired);
        }

        if (!newItem.IsSpeaker.equals(oldItem.IsSpeaker)) {
            diff.putBoolean("IsSpeaker", newItem.IsSpeaker);
        }

        if (diff.size() == 0) {
            return null;
        }
        return diff;
    }
}