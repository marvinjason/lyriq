package com.marvinjason.lyriq;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import jp.wasabeef.blurry.Blurry;
import wseemann.media.FFmpegMediaMetadataRetriever;

public class MainActivity extends Activity {

    private SlidingUpPanelLayout mSlidingUpPanelLayout;
    private ListView mTracks;
    private ImageView mCover;
    private Toolbar mToolbar;
    private ImageView mBack;
    private LinearLayout mToolbarCollapsed;
    private LinearLayout mToolbarExpanded;
    private TextView mTitleCollapsed;
    private TextView mTitleExpanded;
    private TextView mArtistExpanded;
    private SeekBar mSeekBar;
    private TextView mDurationLength;
    private TextView mDurationCurrent;
    private ImageView mPrev;
    private ImageView mPlay;
    private ImageView mNext;
    private ImageView mPlayCollapsed;
    private Utility mUtility;
    private List<Utility.Track> mTrackList;
    private FFmpegMediaMetadataRetriever fFmpegMediaMetadataRetriever;
    private ServiceConnection mServiceConnection;
    private Player mPlayer;
    private DatabaseHelper databaseHelper;
    private TextView mLyrics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSlidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.supl);
        mTracks = (ListView) findViewById(R.id.lv_tracks);
        mCover = (ImageView) findViewById(R.id.iv_cover);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mBack = (ImageView) findViewById(R.id.iv_back);
        mToolbarCollapsed = (LinearLayout) findViewById(R.id.toolbar_collapsed);
        mToolbarExpanded = (LinearLayout) findViewById(R.id.toolbar_expanded);
        mTitleCollapsed = (TextView) findViewById(R.id.tv_title_collapsed);
        mTitleExpanded = (TextView) findViewById(R.id.tv_title_expanded);
        mArtistExpanded = (TextView) findViewById(R.id.tv_artist_expanded);
        mSeekBar = (SeekBar) findViewById(R.id.sb);
        mDurationLength = (TextView) findViewById(R.id.tv_duration_length);
        mDurationCurrent = (TextView) findViewById(R.id.tv_duration_current);
        mPrev = (ImageView) findViewById(R.id.iv_prev);
        mPlay = (ImageView) findViewById(R.id.iv_play);
        mNext = (ImageView) findViewById(R.id.iv_next);
        mPlayCollapsed = (ImageView) findViewById(R.id.iv_play_collapsed);
        mUtility = new Utility(MainActivity.this);
        mTrackList = mUtility.fetchTracks();
        fFmpegMediaMetadataRetriever = new FFmpegMediaMetadataRetriever();
        databaseHelper = new DatabaseHelper(MainActivity.this);
        mLyrics = (TextView) findViewById(R.id.tv_lyrics);
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                mPlayer = ((Player.MyBinder) iBinder).getService();
                mPlayer.setSeekBar(mSeekBar);
                mPlayer.setDurationLength(mDurationLength);
                mPlayer.setDurationCurrent(mDurationCurrent);
                mPlayer.setPrev(mPrev);
                mPlayer.setPlay(mPlay);
                mPlayer.setNext(mNext);
                mPlayer.setPlayCollapsed(mPlayCollapsed);

                if (getIntent().hasExtra("notification") && getIntent().getExtras().getBoolean("notification") == true) {
                    mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);

                    for (Utility.Track track : mTrackList) {
                        if (track.path.equals(mPlayer.getCurrentPath())) {
                            mTitleCollapsed.setText(track.title);
                            mTitleExpanded.setText(track.title);
                            mArtistExpanded.setText(track.artist);

                            fFmpegMediaMetadataRetriever.setDataSource(track.path);
                            byte[] image = fFmpegMediaMetadataRetriever.getEmbeddedPicture();
                            Bitmap bmp;

                            if (image != null) {
                                bmp = BitmapFactory.decodeByteArray(image, 0, image.length);
                            } else {
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.no_cover);
                            }

                            Blurry.with(MainActivity.this).from(bmp).into(mCover);
                            mCover.setImageAlpha(255);
                            mBack.setRotation(180);
                        }
                    }

                    mPlayer.prepareFromNotification();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };

        bindService(new Intent(MainActivity.this, Player.class), mServiceConnection, BIND_AUTO_CREATE);

        mTracks.setAdapter(new CustomListAdapter(MainActivity.this, mTrackList));
        mTracks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                Utility.Track track = (Utility.Track) adapterView.getAdapter().getItem(i);
                mTitleCollapsed.setText(track.title);
                mTitleExpanded.setText(track.title);
                mArtistExpanded.setText(track.artist);
                mLyrics.setText(databaseHelper.getLyrics(track.title));

                fFmpegMediaMetadataRetriever.setDataSource(track.path);
                byte[] image = fFmpegMediaMetadataRetriever.getEmbeddedPicture();
                Bitmap bmp;

                if (image != null) {
                    bmp = BitmapFactory.decodeByteArray(image, 0, image.length);
                } else {
                    bmp = BitmapFactory.decodeResource(getResources(), R.drawable.no_cover);
                }

                Blurry.with(MainActivity.this).from(bmp).into(mCover);

                mPlayer.setTitle(track.title);
                mPlayer.setArtist(track.artist);
                mPlayer.setBitmap(bmp);
                mPlayer.prepare(track.path);
                mPlayer.start();
            }
        });

        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.no_cover);;
        Blurry.with(MainActivity.this).from(bmp).into(mCover);
        mCover.setImageAlpha(0);
        mToolbarExpanded.setAlpha(0);

        mSlidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                mCover.setImageAlpha((int) (255 * slideOffset));
                mBack.setRotation(180 * slideOffset);
                mToolbarExpanded.setAlpha(slideOffset);
                mToolbarCollapsed.setAlpha(1 - slideOffset);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {

            }
        });

        mSeekBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        mSeekBar.getThumb().setColorFilter(Color.rgb(45, 142, 196), PorterDuff.Mode.SRC_IN);

        mPlayCollapsed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlayer.toggle();
            }
        });

        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlayer.toggle();
            }
        });

        mPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int index = -1;

                for (int i = 0; i < mTrackList.size(); i++) {
                    Utility.Track track = mTrackList.get(i);

                    if (track.path.equals(mPlayer.getCurrentPath())) {
                        index = i;
                        break;
                    }
                }

                if (index - 1 >= 0) {
                    Utility.Track track = mTrackList.get(index - 1);
                    mTitleCollapsed.setText(track.title);
                    mTitleExpanded.setText(track.title);
                    mArtistExpanded.setText(track.artist);
                    mLyrics.setText(databaseHelper.getLyrics(track.title));

                    fFmpegMediaMetadataRetriever.setDataSource(track.path);
                    byte[] image = fFmpegMediaMetadataRetriever.getEmbeddedPicture();
                    Bitmap bmp;

                    if (image != null) {
                        bmp = BitmapFactory.decodeByteArray(image, 0, image.length);
                    } else {
                        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.no_cover);
                    }

                    Blurry.with(MainActivity.this).from(bmp).into(mCover);

                    mPlayer.setTitle(track.title);
                    mPlayer.setArtist(track.artist);
                    mPlayer.setBitmap(bmp);
                    mPlayer.prepare(track.path);
                    mPlayer.start();
                }

                mPrev.animate().translationX(-5).setDuration(10).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mPrev.animate().translationX(5).setDuration(10);
                    }
                });
            }
        });

        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int index = -1;

                for (int i = 0; i < mTrackList.size(); i++) {
                    Utility.Track track = mTrackList.get(i);

                    if (track.path.equals(mPlayer.getCurrentPath())) {
                        index = i;
                        break;
                    }
                }

                if (index + 1 < mTrackList.size()) {
                    Utility.Track track = mTrackList.get(index + 1);
                    mTitleCollapsed.setText(track.title);
                    mTitleExpanded.setText(track.title);
                    mArtistExpanded.setText(track.artist);
                    mLyrics.setText(databaseHelper.getLyrics(track.title));

                    fFmpegMediaMetadataRetriever.setDataSource(track.path);
                    byte[] image = fFmpegMediaMetadataRetriever.getEmbeddedPicture();
                    Bitmap bmp;

                    if (image != null) {
                        bmp = BitmapFactory.decodeByteArray(image, 0, image.length);
                    } else {
                        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.no_cover);
                    }

                    Blurry.with(MainActivity.this).from(bmp).into(mCover);

                    mPlayer.setTitle(track.title);
                    mPlayer.setArtist(track.artist);
                    mPlayer.setBitmap(bmp);
                    mPlayer.prepare(track.path);
                    mPlayer.start();
                }

                mNext.animate().translationX(5).setDuration(10).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mNext.animate().translationX(-5).setDuration(10);
                    }
                });
            }
        });

        new AsyncTask(){
            @Override
            protected Object doInBackground(Object[] objects) {
                databaseHelper.update(mTrackList);
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                Toast.makeText(MainActivity.this, "Lyrics database updated!", Toast.LENGTH_LONG).show();
            }
        }.execute();
    }

    private class CustomListAdapter extends BaseAdapter {

        private Context context;
        private List<Utility.Track> list;

        public CustomListAdapter(Context context, List<Utility.Track> list) {
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int i) {
            return list.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View v = LayoutInflater.from(context).inflate(R.layout.track_list_item, null);
            ((TextView) v.findViewById(R.id.tv_title)).setText(list.get(i).title);
            ((TextView) v.findViewById(R.id.tv_artist)).setText(list.get(i).artist);

            SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
            Date date = new Date(list.get(i).duration);
            ((TextView) v.findViewById(R.id.tv_duration)).setText(sdf.format(date));
            return v;
        }
    }
}
