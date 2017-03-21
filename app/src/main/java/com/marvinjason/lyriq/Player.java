package com.marvinjason.lyriq;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Player extends Service {
    private IBinder mBinder = new MyBinder();
    private MediaPlayer mediaPlayer;
    private SeekBar mSeekBar;
    private TextView mDurationLength;
    private TextView mDurationCurrent;
    private ImageView mPrev;
    private ImageView mPlay;
    private ImageView mNext;
    private ImageView mPlayCollapsed;
    private Handler mHandler;
    private String mPath;
    private SimpleDateFormat sdf;
    private boolean isSet;
    private String title;
    private String artist;
    private Bitmap bmp;

    public Player() {
        mediaPlayer = new MediaPlayer();
        mHandler = new Handler();
        sdf = new SimpleDateFormat("mm:ss");
        isSet = false;

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stop();
                prepare(mPath);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() == "notification") {
            Intent intentNotif = new Intent(Player.this, MainActivity.class);
            intentNotif.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intentNotif.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intentNotif.putExtra("notification", true);
            startActivity(intentNotif);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setSeekBar(SeekBar mSeekBar) {
        this.mSeekBar = mSeekBar;
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    mediaPlayer.seekTo(i);
                    seekBar.setProgress(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void setDurationLength(TextView mDurationLength) {
        this.mDurationLength = mDurationLength;
    }

    public void setDurationCurrent(TextView mDurationCurrent) {
        this.mDurationCurrent = mDurationCurrent;
    }

    public void setPrev(ImageView mPrev) {
        this.mPrev = mPrev;
    }

    public void setPlay(ImageView mPlay) {
        this.mPlay = mPlay;
    }

    public void setNext(ImageView mNext) {
        this.mNext = mNext;
    }

    public void setPlayCollapsed(ImageView mPlayCollapsed) {
        this.mPlayCollapsed = mPlayCollapsed;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setBitmap(Bitmap bmp) {
        this.bmp = bmp;
    }

    public void prepare(String path) {
        mPath = path;

        try {
            stop();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mSeekBar.setMax(mediaPlayer.getDuration());
            mSeekBar.setProgress(0);
            mDurationCurrent.setText("00:00");
            mDurationLength.setText("/" + sdf.format(new Date(mediaPlayer.getDuration())));

            mPlay.setImageResource(R.mipmap.ic_play_arrow_white_24dp);
            mPlayCollapsed.setImageResource(R.mipmap.ic_play_arrow_white_24dp);

        } catch (Exception e) {
            Log.d("Error", e.toString());
        }
    }

    public void start() {
        mediaPlayer.start();
        isSet = true;
        mPlay.setImageResource(R.mipmap.ic_pause_white_24dp);
        mPlayCollapsed.setImageResource(R.mipmap.ic_pause_white_24dp);

        //mPlay.animate().rotation(180).scaleX(0).scaleY(0).setDuration(200).setListener(new Animator.AnimatorListener() {
//        mPlay.animate().rotation(180).setDuration(200).withEndAction(new Runnable() {
//            @Override
//            public void run() {
//                mPlay.setImageResource(R.mipmap.ic_pause_white_24dp);
//                mPlay.setRotation(-90);
//                mPlay.animate().rotation(90).setDuration(200);
//            }
//        });

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                int mCurrentPosition = mediaPlayer.getCurrentPosition();
                mSeekBar.setProgress(mCurrentPosition);
                mDurationCurrent.setText(sdf.format(new Date(mCurrentPosition)));

                if (mediaPlayer.getDuration() - mCurrentPosition >= 100) {
                    mHandler.postDelayed(this, 100);
                }
            }
        });

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(Player.this);
        mBuilder.setSmallIcon(android.R.drawable.ic_dialog_info);
        mBuilder.setLargeIcon(bmp);
        mBuilder.setContentTitle(title + " - " + artist);
        mBuilder.setContentText("Now Playing");

        Intent intent = new Intent(Player.this, Player.class);
        intent.setAction("notification");

        mBuilder.setContentIntent(PendingIntent.getService(Player.this, 0, intent, 0));

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, mBuilder.build());
    }

    public void resume() {
        if (isSet && !mediaPlayer.isPlaying()) {
//            mediaPlayer.start();
//            mPlay.setImageResource(R.mipmap.ic_pause_white_24dp);
//            mPlayCollapsed.setImageResource(R.mipmap.ic_pause_white_24dp);

            mediaPlayer.start();
            mPlay.animate().rotation(90).scaleX(0.5f).scaleY(0.5f).setDuration(100).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mPlay.setImageResource(R.mipmap.ic_pause_white_24dp);
                    mPlay.animate().rotation(0).scaleX(1).scaleY(1).setDuration(100);
                }
            });

            mPlayCollapsed.animate().rotation(90).scaleX(0.5f).scaleY(0.5f).setDuration(100).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mPlayCollapsed.setImageResource(R.mipmap.ic_pause_white_24dp);
                    mPlayCollapsed.animate().rotation(0).scaleX(1).scaleY(1).setDuration(100);
                }
            });
        }
    }

    public void pause() {
        if (isSet && mediaPlayer.isPlaying()) {
//            mediaPlayer.pause();
//            mPlay.setImageResource(R.mipmap.ic_play_arrow_white_24dp);
//            mPlayCollapsed.setImageResource(R.mipmap.ic_play_arrow_white_24dp);

            mediaPlayer.pause();
            mPlay.animate().rotation(90).scaleX(0.5f).scaleY(0.5f).setDuration(100).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mPlay.setImageDrawable(getRotateDrawable(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_play_arrow_white_24dp), 180));
                    mPlay.animate().rotation(180).scaleX(1).scaleY(1).setDuration(100);
                }
            });

            mPlayCollapsed.animate().rotation(90).scaleX(0.5f).scaleY(0.5f).setDuration(100).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mPlayCollapsed.setImageDrawable(getRotateDrawable(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_play_arrow_white_24dp), 180));
                    mPlayCollapsed.animate().rotation(180).scaleX(1).scaleY(1).setDuration(100);
                }
            });
        }
    }

    public void toggle() {
        if (isSet) {
            if (mediaPlayer.isPlaying()) {
                pause();
            } else {
                resume();
            }
        }
    }

    public void stop() {
        if (isSet) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            isSet = false;
        }
    }

    public String getCurrentPath() {
        return mPath;
    }

    public void prepareFromNotification() {
        mSeekBar.setMax(mediaPlayer.getDuration());
        mSeekBar.setProgress(0);
        mDurationLength.setText("/" + sdf.format(new Date(mediaPlayer.getDuration())));

        if (mediaPlayer.isPlaying()) {
            mPlay.setImageResource(R.mipmap.ic_pause_white_24dp);
            mPlayCollapsed.setImageResource(R.mipmap.ic_pause_white_24dp);
        } else {
            mPlay.setImageResource(R.mipmap.ic_play_arrow_white_24dp);
            mPlayCollapsed.setImageResource(R.mipmap.ic_play_arrow_white_24dp);
        }
    }

    Drawable getRotateDrawable(final Bitmap b, final float angle) {
        final BitmapDrawable drawable = new BitmapDrawable(getResources(), b) {
            @Override
            public void draw(final Canvas canvas) {
                canvas.save();
                canvas.rotate(angle, b.getWidth() / 2, b.getHeight() / 2);
                super.draw(canvas);
                canvas.restore();
            }
        };
        return drawable;
    }

    public class MyBinder extends Binder {
        public Player getService() {
            return Player.this;
        }
    }
}
