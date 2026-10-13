package dev.nononsense.nononsense

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.ryanheise.audioservice.AudioServicePlugin
import es.antonborri.home_widget.HomeWidgetPlugin
import es.antonborri.home_widget.HomeWidgetProvider
import io.flutter.embedding.engine.FlutterEngineCache
import org.json.JSONObject

/**
 * The home-screen widget: the song, previous/play/next and like; at the large size also the last three plays and
 * four quick-start playlists. It draws what the app stored (HomeScreenWidget in lib/core/home_screen_widget.dart)
 * and has no timer of its own: the app redraws it when something changes.
 */
class NowPlayingWidget : HomeWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray, widgetData: SharedPreferences) {
        val state = WidgetState.read(widgetData)
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, views(context, state, appWidgetManager.getAppWidgetOptions(id)))
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        appWidgetManager.updateAppWidget(appWidgetId, views(context, WidgetState.read(HomeWidgetPlugin.getData(context)), newOptions))
    }

    private fun views(context: Context, state: WidgetState, options: Bundle): RemoteViews {
        if (!state.signedIn) return signedOut(context)
        night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val cover = state.cover?.let { decodeCover(it) }
        val small = build(context, state, R.layout.widget_small, cover)
        val medium = build(context, state, R.layout.widget_medium, cover)
        val large = build(context, state, R.layout.widget_large, cover)
        // Android 12 picks the layout for each size the launcher shows the widget at.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return RemoteViews(mapOf(SizeF(110f, 110f) to small, SizeF(250f, 110f) to medium, SizeF(250f, 260f) to large))
        }
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        return when {
            width < 250 -> small
            height < 260 -> medium
            else -> large
        }
    }

    private fun build(context: Context, s: WidgetState, layout: Int, cover: Bitmap?): RemoteViews {
        val v = RemoteViews(context.packageName, layout)
        val press = Presses(context)
        v.setOnClickPendingIntent(R.id.widget_root, WidgetActionReceiver.openApp(context))
        if (cover != null) v.setImageViewBitmap(R.id.cover, cover) else v.setImageViewResource(R.id.cover, R.drawable.widget_no_cover)
        v.setOnClickPendingIntent(R.id.cover, WidgetActionReceiver.openApp(context))

        v.setTextViewText(R.id.title, s.title ?: context.getString(R.string.widget_nothing_played))
        v.setOnClickPendingIntent(R.id.play, press("toggle", 1))
        v.setImageViewResource(R.id.play, if (s.playing) R.drawable.widget_pause else R.drawable.widget_play)
        // The play disc takes the song's accent, as the app's Filled buttons do; its icon is black or white on it.
        val accent = s.accent ?: Accent.fallback(context)
        tint(v, R.id.play_bg, accent.light, accent.dark)
        tint(v, R.id.play, accent.onLight, accent.onDark)
        v.setContentDescription(R.id.play, context.getString(if (s.playing) R.string.widget_pause else R.string.widget_play))
        v.setBoolean(R.id.play, "setEnabled", s.title != null)
        if (layout == R.layout.widget_small) return v

        v.setTextViewText(R.id.artist, if (s.resume) context.getString(R.string.widget_resume, s.artist ?: "") else s.artist ?: "")
        v.setOnClickPendingIntent(R.id.prev, press("previous", 2))
        v.setOnClickPendingIntent(R.id.next, press("next", 3))
        // Like is for the song that is loaded; the song offered to resume is not loaded yet.
        val loaded = s.title != null && !s.resume
        v.setViewVisibility(R.id.prev, if (loaded) View.VISIBLE else View.INVISIBLE)
        v.setViewVisibility(R.id.next, if (loaded) View.VISIBLE else View.INVISIBLE)
        v.setViewVisibility(R.id.like, if (loaded) View.VISIBLE else View.GONE)
        v.setImageViewResource(R.id.like, if (s.liked) R.drawable.widget_heart else R.drawable.widget_heart_outline)
        // a liked heart in the accent, an empty one in the ink; set each time, since a launcher may reuse the view
        if (s.liked) tint(v, R.id.like, accent.light, accent.dark) else tint(v, R.id.like, Accent.ink(context, false), Accent.ink(context, true))
        v.setContentDescription(R.id.like, context.getString(if (s.liked) R.string.widget_unlike else R.string.widget_like))
        v.setOnClickPendingIntent(R.id.like, press("like", 4))
        if (layout == R.layout.widget_medium) return v

        val rows = listOf(R.id.recent0 to (R.id.recent0_title to R.id.recent0_artist), R.id.recent1 to (R.id.recent1_title to R.id.recent1_artist), R.id.recent2 to (R.id.recent2_title to R.id.recent2_artist))
        v.setViewVisibility(R.id.recent_header, if (s.recent.isEmpty()) View.GONE else View.VISIBLE)
        rows.forEachIndexed { i, (row, text) ->
            val song = s.recent.getOrNull(i)
            v.setViewVisibility(row, if (song == null) View.GONE else View.VISIBLE)
            if (song == null) return@forEachIndexed
            v.setTextViewText(text.first, song.first)
            v.setTextViewText(text.second, song.second)
            v.setOnClickPendingIntent(row, press("recent:$i", 10 + i))
        }
        val tiles = listOf(R.id.tile0, R.id.tile1, R.id.tile2, R.id.tile3)
        v.setViewVisibility(R.id.tiles_header, if (s.tiles.isEmpty()) View.GONE else View.VISIBLE)
        v.setViewVisibility(R.id.tiles_top, if (s.tiles.isEmpty()) View.GONE else View.VISIBLE)
        v.setViewVisibility(R.id.tiles_bottom, if (s.tiles.size <= 2) View.GONE else View.VISIBLE)
        tiles.forEachIndexed { i, tile ->
            val playlist = s.tiles.getOrNull(i)
            v.setViewVisibility(tile, if (playlist == null) View.INVISIBLE else View.VISIBLE)
            if (playlist == null) return@forEachIndexed
            v.setTextViewText(tile, playlist.second)
            v.setOnClickPendingIntent(tile, press("tile:${playlist.first}", 20 + i))
        }
        return v
    }

    /**
     * Colours an image: [light] or [dark] after the phone's theme. Android 12 lets the launcher choose as the theme
     * changes; before it, the colour is picked now, and the next redraw follows a change of theme.
     */
    private fun tint(v: RemoteViews, id: Int, light: Int, dark: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            v.setColorInt(id, "setColorFilter", light, dark)
        } else {
            v.setInt(id, "setColorFilter", if (night) dark else light)
        }
    }

    private var night = false

    private fun signedOut(context: Context) = RemoteViews(context.packageName, R.layout.widget_signed_out).apply {
        setOnClickPendingIntent(R.id.widget_root, WidgetActionReceiver.openApp(context))
    }

    /**
     * Each button's intent. While the app runs, a press is a broadcast that the app handles without coming to the
     * front. Otherwise it opens the app with the press: Android may refuse an activity start from a broadcast while
     * the app is in the background, so the widget asks for the activity directly. The app redraws the widget once it
     * runs, which switches the buttons back to broadcasts.
     */
    private class Presses(private val context: Context) {
        private val running = FlutterEngineCache.getInstance().get(AudioServicePlugin.getFlutterEngineId()) != null

        operator fun invoke(press: String, code: Int): PendingIntent = if (running) {
            WidgetActionReceiver.pressIntent(context, press, code)
        } else {
            PendingIntent.getActivity(context, code, WidgetActionReceiver.openIntent(context, press), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }

    /** The cover, shrunk to at most [COVER_PX]: a widget's views travel to the launcher, which limits their size. */
    private fun decodeCover(path: String): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= COVER_PX && bounds.outHeight / (sample * 2) >= COVER_PX) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val COVER_PX = 256

        /** Redraws every placed widget from the stored state. */
        fun redraw(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NowPlayingWidget::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(Intent(context, NowPlayingWidget::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
        }
    }
}

/** The accent on each theme, and the black or white that reads on it (the app's Accent.readable, buildTheme). */
data class Accent(val light: Int, val dark: Int, val onLight: Int, val onDark: Int) {
    companion object {
        /** Windows 11's default accent from colors.xml, for a song with a grey cover or none. */
        fun fallback(context: Context) = Accent(
            themed(context, false, R.color.widget_accent), themed(context, true, R.color.widget_accent),
            themed(context, false, R.color.widget_on_accent), themed(context, true, R.color.widget_on_accent),
        )

        fun ink(context: Context, night: Boolean) = themed(context, night, R.color.widget_ink)

        /** A colour from the light or the dark resources, whichever theme the phone has now. */
        private fun themed(context: Context, night: Boolean, id: Int): Int {
            val config = Configuration(context.resources.configuration)
            config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                (if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
            return context.createConfigurationContext(config).getColor(id)
        }
    }
}

/** The state the app stored under `state`: see HomeWidgetState.of in lib/core/home_screen_widget.dart. */
data class WidgetState(
    val signedIn: Boolean,
    val title: String?,
    val artist: String?,
    val cover: String?,
    val playing: Boolean,
    val liked: Boolean,
    val resume: Boolean,
    val recent: List<Pair<String, String>>, // title, artist
    val tiles: List<Pair<String, String>>, // id, name
    val accent: Accent?, // null: the default blue
) {
    companion object {
        private val SIGNED_OUT = WidgetState(false, null, null, null, false, false, false, emptyList(), emptyList(), null)

        fun read(prefs: SharedPreferences): WidgetState {
            val json = prefs.getString("state", null) ?: return SIGNED_OUT
            return try {
                val j = JSONObject(json)
                if (!j.optBoolean("signedIn")) return SIGNED_OUT
                fun text(o: JSONObject, k: String) = if (o.isNull(k)) null else o.optString(k)
                val recent = j.optJSONArray("recent")
                val tiles = j.optJSONArray("tiles")
                WidgetState(
                    signedIn = true,
                    title = text(j, "title"),
                    artist = text(j, "artist"),
                    cover = text(j, "cover"),
                    playing = j.optBoolean("playing"),
                    liked = j.optBoolean("liked"),
                    resume = j.optBoolean("resume"),
                    recent = (0 until (recent?.length() ?: 0)).map { recent!!.getJSONObject(it).let { r -> r.optString("title") to r.optString("artist") } },
                    tiles = (0 until (tiles?.length() ?: 0)).map { tiles!!.getJSONObject(it).let { t -> t.optString("id") to t.optString("name") } },
                    // ARGB as Dart's toARGB32: a JSON number past Int's range, read as Long and cut to its 32 bits
                    accent = j.optJSONObject("accent")?.let { a ->
                        Accent(a.getLong("light").toInt(), a.getLong("dark").toInt(), a.getLong("onLight").toInt(), a.getLong("onDark").toInt())
                    },
                )
            } catch (_: Exception) {
                SIGNED_OUT
            }
        }
    }
}
