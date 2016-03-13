package corp.katet.evernote.util;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class BitmapCache {

    private static BitmapCache mInstance;
    private HashMap<String, Bitmap> mMemoryCache;

    private BitmapCache(FragmentManager fm, int maxCacheSize) {
        RetainFragment retainFragment = RetainFragment.findOrCreateRetainFragment(fm);
        mMemoryCache = retainFragment.getRetainedCache();

        if (mMemoryCache == null) {
            // Get max available VM memory, exceeding this amount will throw an
            // OutOfMemory exception. Stored in kilobytes as LruCache takes an
            // int in its constructor
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

            // Use 1/8 of the available memory for this memory cache, with a maximum of 4096
            final int cacheSize = Math.min(maxMemory / 8, maxCacheSize) / 1024;

            mMemoryCache = new LinkedHashMap<>(cacheSize);
            retainFragment.setRetainedCache(mMemoryCache);
        }
    }

    public static BitmapCache getInstance(FragmentManager fm, int maxCacheSize) {
        if (mInstance == null) {
            mInstance = new BitmapCache(fm, maxCacheSize);
        }
        return mInstance;
    }

    public synchronized void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public synchronized Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key) != null ? mMemoryCache.get(key) : null;
    }

    public static class RetainFragment extends Fragment {
        private static final String TAG = "RetainFragment";
        private static HashMap<String, Bitmap> mRetainedCache;

        public RetainFragment() {
        }

        public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
            RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
            if (fragment == null) {
                fragment = new RetainFragment();
                fm.beginTransaction().add(fragment, TAG).commit();
            }
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        public HashMap<String, Bitmap> getRetainedCache() {
            return mRetainedCache;
        }

        public void setRetainedCache(HashMap<String, Bitmap> retainedCache) {
            mRetainedCache = retainedCache;
        }
    }
}
