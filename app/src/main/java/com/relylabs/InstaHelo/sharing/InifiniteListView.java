package com.relylabs.InstaHelo.sharing;

import android.util.Log;
import android.widget.AbsListView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public abstract class InifiniteListView extends RecyclerView.OnScrollListener {
    // The minimum number of items to have below your current scroll position
    // before loading more.
    private int visibleThreshold = 3;
    // The current offset index of data you have loaded
    private int currentPage = 0;
    // The total number of items in the dataset after the last load
    private int previousTotalItemCount1 = 0;
    private int itemCountInList = 50;
    private int totalItemCount = 100;
    // True if we are still waiting for the last set of data to load.
    private boolean loading = true;
    // Sets the starting page index
    private int startingPageIndex = 0;
    RecyclerView.LayoutManager mlayoutManager;

    public InifiniteListView(LinearLayoutManager layoutManager) {
        this.mlayoutManager = layoutManager;
    }

    // This happens many times a second during a scroll, so be wary of the code you place here.
    // We are given a few useful parameters to help us work out if we need to load some more data,
    // but first we check if we are waiting for the previous load to finish.
    @Override
    public void onScrolled(RecyclerView view, int dx, int dy) {
        Log.d("Mqtt", "method on scrolled " +dx + " " + dy);
        int lastVisibleItemPosition = ((LinearLayoutManager)mlayoutManager).findLastVisibleItemPosition();

        // If the total item count is zero and the previous isn't, assume the
        // list is invalidated and should be reset back to initial state
        Log.d("Mqtt", "totalitemcount " +totalItemCount);
        //Log.d("Mqtt", "lastvisibleitemposition " + lastVisibleItemPosition);
        //Log.d("Mqtt", "previoystotalitemcount " +previousTotalItemCount1);
        Log.d("Mqtt", "loading " +loading);
        //loading = true;
        if (totalItemCount < previousTotalItemCount1) {
            this.currentPage = this.startingPageIndex;
            //this.previousTotalItemCount1 = totalItemCount;
            if (totalItemCount == 0) {
                this.loading = true;
            }
        }
        // If it’s still loading, we check to see if the dataset count has
        // changed, if so we conclude it has finished loading and update the current page
        // number and total item count.
        if (loading && (totalItemCount > previousTotalItemCount1)) {
            Log.d("Mqtt", "itemcount > previoustotalitemcount" + loading);
            loading = false;
            //previousTotalItemCount = previousTotalItemCount + itemCountInList;
        }

        // If it isn’t currently loading, we check to see if we have breached
        // the visibleThreshold and need to reload more data.
        // If we do need to reload some more data, we execute onLoadMore to fetch the data.
        // threshold should reflect how many total columns there are too
        if (!loading && (lastVisibleItemPosition + visibleThreshold) > itemCountInList) {
            currentPage++;
            previousTotalItemCount1 = (currentPage)*itemCountInList;
            onLoadMore(previousTotalItemCount1, totalItemCount, view);
            loading = true;
        }
    }
    public void updateTotalItemCount(int totalItemCount) {
        this.totalItemCount = totalItemCount;
    }

    // Call this method whenever performing new searches
    public void resetState() {
        this.currentPage = this.startingPageIndex;
        this.previousTotalItemCount1 = 0;
        this.loading = true;
    }

    // Defines the process for actually loading more data based on page
    public abstract void onLoadMore(int page, int totalItemsCount, RecyclerView view);

}