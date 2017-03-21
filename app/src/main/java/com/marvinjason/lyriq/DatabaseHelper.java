package com.marvinjason.lyriq;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class DatabaseHelper {
    private Context context;
    private SQLiteDatabase sqLiteDatabase;

    public DatabaseHelper(Context context) {
        this.context = context;
        sqLiteDatabase = context.openOrCreateDatabase("lyriq", Context.MODE_PRIVATE, null);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS tracks (title TEXT, lyrics TEXT);");
    }

    public void update(List<Utility.Track> trackList) {
        try {
            for (Utility.Track track : trackList) {
                String q = "http://api.musixmatch.com/ws/1.1/matcher.track.get?apikey=" + context.getString(R.string.api_key);

                if (!track.title.equals("<unknown>")) {
                    q += "&q_track=" + track.title;
                }

                if (!track.artist.equals("<unknown>")) {
                    q += "&q_artist=" + track.artist;
                }

                URL url = new URL(q);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                InputStream is = urlConnection.getInputStream();
                JSONObject jsonObject = new JSONObject(Utility.convertStreamToString(is));
                JSONObject obj = jsonObject.getJSONObject("message");

                if (!obj.getString("body").isEmpty()){
                    obj = obj.getJSONObject("body").getJSONObject("track");
                    String trackId = obj.getString("track_id");
                    urlConnection.disconnect();

                    q = "http://api.musixmatch.com/ws/1.1/track.lyrics.get?apikey=" + context.getString(R.string.api_key) + "&track_id=" + trackId;
                    url = new URL(q);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    is = urlConnection.getInputStream();
                    jsonObject = new JSONObject(Utility.convertStreamToString(is));
                    String lyrics = jsonObject.getJSONObject("message").getJSONObject("body").getJSONObject("lyrics").getString("lyrics_body");
                    urlConnection.disconnect();

                    q = "INSERT INTO tracks VALUES (?, ?)";
                    SQLiteStatement sqLiteStatement = sqLiteDatabase.compileStatement(q);
                    sqLiteStatement.bindString(1, track.title);
                    sqLiteStatement.bindString(2, lyrics);
                    sqLiteStatement.executeInsert();
                }
            }
        } catch (Exception e) {
            Log.d("Error", e.toString());
        }
    }

    public String getLyrics(String title) {
        String table = "tracks";
        String[] columnsToReturn = { "lyrics" };
        String selection = "title = ?";
        String[] selectionArgs = { title };
        Cursor cursor = sqLiteDatabase.query(table, columnsToReturn, selection, selectionArgs, null, null, null);
        String lyrics = "Lyrics are not available for this song at the moment.";

        while (cursor.moveToNext()) {
            lyrics = cursor.getString(0);
        }

        return lyrics;
    }
}
