package es.carview.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/** Audio files explicitly chosen by the user using Android's document picker. */
public final class LocalAudioLibrary {
    private static final String PREFS = "carview_prefs";
    private static final String KEY = "local_audio_items";

    private LocalAudioLibrary() { }

    public static final class Track {
        public final String uri;
        public final String title;
        public Track(String uri, String title) {
            this.uri = uri;
            this.title = title;
        }
    }

    public static List<Track> load(Context context) {
        List<Track> tracks = new ArrayList<>();
        String stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, "[]");
        try {
            JSONArray items = new JSONArray(stored);
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                String uri = item.optString("uri", "");
                String title = item.optString("title", "Audio local");
                if (uri.startsWith("content://")) tracks.add(new Track(uri, title));
            }
        } catch (JSONException ignored) {
            // Corrupt preference data is equivalent to an empty library.
        }
        return tracks;
    }

    public static void add(Context context, Uri uri, String title) {
        if (uri == null || !"content".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Se requiere un archivo local");
        }
        List<Track> tracks = load(context);
        JSONArray next = new JSONArray();
        for (Track item : tracks) {
            if (!item.uri.equals(uri.toString())) next.put(asJson(item));
        }
        next.put(asJson(new Track(uri.toString(), title)));
        save(context, next);
    }

    public static void remove(Context context, String trackUri) {
        JSONArray next = new JSONArray();
        for (Track item : load(context)) {
            if (!item.uri.equals(trackUri)) next.put(asJson(item));
        }
        save(context, next);
    }

    public static Track find(Context context, String trackUri) {
        for (Track track : load(context)) {
            if (track.uri.equals(trackUri)) return track;
        }
        return null;
    }

    private static JSONObject asJson(Track track) {
        JSONObject out = new JSONObject();
        try {
            out.put("uri", track.uri);
            out.put("title", track.title);
        } catch (JSONException ignored) { }
        return out;
    }

    private static void save(Context context, JSONArray items) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY, items.toString()).apply();
    }
}
