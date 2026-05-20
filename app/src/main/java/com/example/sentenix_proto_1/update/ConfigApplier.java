package com.example.sentenix_proto_1.update;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.sentenix_proto_1.R;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

public final class ConfigApplier {

    private static final String TAG = "ConfigApplier";

    public static final class Config {
        public boolean sosVisible;
        @Nullable public Integer colorPrimary;
        @Nullable public Integer colorBackground;
        @Nullable public Integer colorSosButton;
        @Nullable public Integer colorUploadButton;
        @Nullable public String imageHero;
        @Nullable public String textAppTitle;
        @Nullable public String textTagline;
        @Nullable public String textUploadLabel;
    }

    private ConfigApplier() {}

    /**
     * Extracts the inner XML string from the server JSON envelope:
     * {"version": N, "xml": "<config>..."}
     */
    @NonNull
    public static String extractXmlFromEnvelope(@NonNull String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        return obj.getString("xml");
    }

    @NonNull
    public static Config parse(@NonNull String xml) throws XmlPullParserException, java.io.IOException {
        Config cfg = new Config();
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xml));

        int event = parser.getEventType();
        String currentTag = null;
        while (event != XmlPullParser.END_DOCUMENT) {
            switch (event) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    currentTag = name;
                    if ("sos".equals(name)) {
                        String v = parser.getAttributeValue(null, "visible");
                        cfg.sosVisible = "true".equalsIgnoreCase(v);
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentTag != null && !parser.isWhitespace()) {
                        String text = parser.getText().trim();
                        if (text.isEmpty()) break;
                        switch (currentTag) {
                            case "primary":      cfg.colorPrimary       = parseColor(text); break;
                            case "background":   cfg.colorBackground    = parseColor(text); break;
                            case "sosButton":    cfg.colorSosButton     = parseColor(text); break;
                            case "uploadButton": cfg.colorUploadButton  = parseColor(text); break;
                            case "hero":         cfg.imageHero          = text;             break;
                            case "appTitle":     cfg.textAppTitle       = text;             break;
                            case "tagline":      cfg.textTagline        = text;             break;
                            case "uploadLabel":  cfg.textUploadLabel    = text;             break;
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    currentTag = null;
                    break;
            }
            event = parser.next();
        }
        return cfg;
    }

    @Nullable
    private static Integer parseColor(@NonNull String hex) {
        try {
            return Color.parseColor(hex);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "ignoring bad color: " + hex);
            return null;
        }
    }

    public static void apply(@NonNull View root, @NonNull Config cfg) {
        View bgTarget = root.findViewById(R.id.homeRoot);
        if (bgTarget == null) bgTarget = root;
        if (cfg.colorBackground != null) {
            bgTarget.setBackgroundColor(cfg.colorBackground);
        }

        View sos = root.findViewById(R.id.send_sos_button);
        if (sos != null) {
            sos.setVisibility(cfg.sosVisible ? View.VISIBLE : View.GONE);
            if (cfg.colorSosButton != null) {
                tintBackground(sos, cfg.colorSosButton);
            }
        }

        TextView title = root.findViewById(R.id.welcomeTextView);
        if (title != null) {
            if (cfg.textAppTitle != null) title.setText(cfg.textAppTitle);
            if (cfg.colorPrimary != null) title.setTextColor(cfg.colorPrimary);
        }

        TextView tagline = root.findViewById(R.id.SCYNC);
        if (tagline != null) {
            if (cfg.textTagline != null) tagline.setText(cfg.textTagline);
            if (cfg.colorPrimary != null) tagline.setTextColor(cfg.colorPrimary);
        }

        Button upload = root.findViewById(R.id.uplbtn);
        if (upload != null) {
            if (cfg.textUploadLabel != null) upload.setText(cfg.textUploadLabel);
            if (cfg.colorUploadButton != null) tintBackground(upload, cfg.colorUploadButton);
        }

        ImageView hero = root.findViewById(R.id.imageView);
        if (hero != null && cfg.imageHero != null) {
            int resId = resolveDrawable(root, cfg.imageHero);
            if (resId != 0) hero.setImageResource(resId);
        }
    }

    private static int resolveDrawable(@NonNull View root, @NonNull String name) {
        Resources res = root.getResources();
        String pkg = root.getContext().getPackageName();
        int id = res.getIdentifier(name, "drawable", pkg);
        if (id == 0) {
            Log.w(TAG, "drawable not found: " + name);
        }
        return id;
    }

    private static void tintBackground(@NonNull View v, int color) {
        Drawable bg = v.getBackground();
        if (bg == null) return;
        bg.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }
}
