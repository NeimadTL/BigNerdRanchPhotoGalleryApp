package com.bignerdranch.android.photogallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Neimad on 10/04/2017.
 */

public class PhotoGalleryFragment extends VisibleFragment
{



    private static final  String TAG = "PhotoGalleryFragment";



    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;



    public static PhotoGalleryFragment newInstance()
    {
        return new PhotoGalleryFragment();
    }



    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItems();

        /*Intent intent = PollService.newIntent(getActivity());
        getActivity().startService(intent);*/

        /*PollService.setServiceAlarm(getActivity(), true);*/

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloaderListener(new ThumbnailDownloader.ThumbnailDownloaderListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap)
            {
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                photoHolder.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");

    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));

        setupAdapter();

        return view;
    }



    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }



    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater)
    {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                Log.d(TAG, "QueryTextSubmit : " + query);
                QueryPreferences.setStoredQuery(getActivity(),query);
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                Log.d(TAG, "QueryTextSubmit : " + newText);
                return false;
            }
        });


        searchView.setOnSearchClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity()))
        {
            toggleItem.setTitle(R.string.stop_polling);
        }
        else
        {
            toggleItem.setTitle(R.string.start_polling);
        }
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;

            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }



    private void updateItems()
    {
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }



    private void setupAdapter()
    {
        if (isAdded())
        {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }



    private class PhotoHolder extends RecyclerView.ViewHolder implements  View.OnClickListener
    {
        private ImageView mItemImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView)
        {
            super(itemView);
            mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
            itemView.setOnClickListener(this);
        }

        public void bindDrawable(Drawable drawable)
        {
            mItemImageView.setImageDrawable(drawable);
        }

        public void bindGalleryItem(GalleryItem galleryItem)
        {
            mGalleryItem = galleryItem;
        }

        @Override
        public void onClick(View v)
        {
            //Intent intent = new Intent(Intent.ACTION_VIEW, mGalleryItem.getPhotoPageUri());
            Intent intent = PhotoPageActivity.newIntent(getActivity(), mGalleryItem.getPhotoPageUri());
            startActivity(intent);
        }
    }



    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>
    {
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems)
        {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType)
        {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, viewGroup, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position)
        {
            GalleryItem galleryItem = mGalleryItems.get(position);
            photoHolder.bindGalleryItem(galleryItem);
            //Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
            Drawable placeholder = ContextCompat.getDrawable(getActivity(), R.drawable.bill_up_close);
            photoHolder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
        }


        @Override
        public int getItemCount()
        {
            return mGalleryItems.size();
        }
    }



    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>>
    {
        private String mQuery;

        public FetchItemsTask(String query)
        {
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void...params)
        {


            if (mQuery == null)
            {
                return new FlickrFetchr().fetchRecentPhotos();
            }
            else
            {
                return new FlickrFetchr().searchPhotos(mQuery);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items)
        {
            mItems = items;
            setupAdapter();
        }
    }
}
