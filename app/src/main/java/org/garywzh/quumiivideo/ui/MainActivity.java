package org.garywzh.quumiivideo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.garywzh.quumiivideo.R;
import org.garywzh.quumiivideo.model.Item;
import org.garywzh.quumiivideo.ui.adapter.ItemAdapter;
import org.garywzh.quumiivideo.ui.loader.AsyncTaskLoader;
import org.garywzh.quumiivideo.ui.loader.ItemListLoader;
import org.garywzh.quumiivideo.util.LogUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<AsyncTaskLoader.LoaderResult<List<Item>>>, SwipeRefreshLayout.OnRefreshListener, ItemAdapter.OnItemActionListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ItemAdapter mAdapter;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private boolean onLoading;
    private int mCount;
    private List<Item> mItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefreshlayout);
        recyclerView = (RecyclerView) findViewById(R.id.recycle_view);

        mSwipeRefreshLayout.setOnRefreshListener(this);
        linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        mAdapter = new ItemAdapter(this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (!onLoading) {
                    int visibleItemCount = linearLayoutManager.getChildCount();
                    int totalItemCount = linearLayoutManager.getItemCount();
                    int pastItems = linearLayoutManager.findFirstVisibleItemPosition();
                    if ((pastItems + visibleItemCount) >= (totalItemCount - 6)) {

                        LogUtils.d(TAG, "scrolled to bottom, loading more");
                        onLoading = true;
                        mSwipeRefreshLayout.setRefreshing(true);

                        final ItemListLoader loader = getLoader();
                        if (loader == null) {
                            return;
                        }
                        loader.setPage(mCount + 1);
                    }
                }
            }
        });

        mItems = new ArrayList<>();
        onLoading = true;
        mCount = 0;
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<AsyncTaskLoader.LoaderResult<List<Item>>> onCreateLoader(int id, Bundle args) {
        return new ItemListLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<AsyncTaskLoader.LoaderResult<List<Item>>> loader, AsyncTaskLoader.LoaderResult<List<Item>> result) {
        if (result.hasException()) {
            return;
        }

        mCount++;
        mItems.addAll(result.mResult);
        mAdapter.setDataSource(mItems);
        mSwipeRefreshLayout.setRefreshing(false);
        onLoading = false;
    }

    @Override
    public void onLoaderReset(Loader<AsyncTaskLoader.LoaderResult<List<Item>>> loader) {
        mAdapter.setDataSource(null);
        LogUtils.d(TAG, "onLoaderReset called");
    }

    private ItemListLoader getLoader() {
        return (ItemListLoader) getSupportLoaderManager().<AsyncTaskLoader.LoaderResult<List<Item>>>getLoader(0);
    }


    @Override
    public void onRefresh() {
        if (!onLoading) {
            final ItemListLoader loader = getLoader();
            if (loader == null) {
                return;
            }
            mCount = 0;
            mItems.clear();
            loader.setPage(mCount + 1);
        }
    }

    @Override
    public boolean onItemOpen(View view, Item item) {
        final Intent intent = new Intent(this, VideoActivity.class);

        intent.putExtra("id", String.valueOf(item.getId()));
        intent.putExtra("title", item.getTitle());

        startActivity(intent);

        return false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            mSwipeRefreshLayout.setRefreshing(true);
            onRefresh();
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
