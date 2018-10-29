package org.mozilla.vrbrowser.ui.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.mozilla.vrbrowser.R;
import org.mozilla.vrbrowser.audio.AudioEngine;
import org.mozilla.vrbrowser.utils.UrlUtils;

import java.util.ArrayList;
import java.util.List;

public class URLBarPopupWidget extends UIWidget implements WidgetManagerDelegate.FocusChangeListener {

    private ListView mList;
    private URLBarItemAdapter mAdapter;
    private List<URLBarItem> mListItems;
    private Animation mScaleUpAnimation;
    private Animation mScaleDownAnimation;
    private URLBarPopupDelegate mURLBarDelegate;
    private String mHighlightedText;
    private AudioEngine mAudio;

    public interface URLBarPopupDelegate {
        void OnItemClicked(URLBarPopupWidget.URLBarItem item);
        void OnItemDeleted(URLBarPopupWidget.URLBarItem item);
    }

    public URLBarPopupWidget(Context aContext) {
        super(aContext);
        initialize(aContext);
    }

    public URLBarPopupWidget(Context aContext, AttributeSet aAttrs) {
        super(aContext, aAttrs);
        initialize(aContext);
    }

    public URLBarPopupWidget(Context aContext, AttributeSet aAttrs, int aDefStyle) {
        super(aContext, aAttrs, aDefStyle);
        initialize(aContext);
    }

    private void initialize(Context aContext) {
        inflate(aContext, R.layout.list_popup_window, this);

        mWidgetManager.addFocusChangeListener(this);

        mList = findViewById(R.id.list);
        mList.setSoundEffectsEnabled(false);

        mScaleUpAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.popup_scaleup);
        mScaleDownAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.popup_scaledown);
        mScaleDownAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                URLBarPopupWidget.super.hide();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mListItems = new ArrayList<>();

        mAudio = AudioEngine.fromContext(aContext);

        mHighlightedText = "";
    }

    @Override
    public void releaseWidget() {
        mWidgetManager.removeFocusChangeListener(this);

        super.releaseWidget();
    }

    @Override
    protected void initializeWidgetPlacement(WidgetPlacement aPlacement) {
        aPlacement.visible = false;
        aPlacement.worldWidth = WidgetPlacement.floatDimension(getContext(), R.dimen.url_bar_popup_world_width);
        aPlacement.height =  WidgetPlacement.dpDimension(getContext(), R.dimen.url_bar_popup_height);
        aPlacement.parentAnchorX = 0.0f;
        aPlacement.parentAnchorY = 1.0f;
        aPlacement.anchorX = 0.0f;
        aPlacement.anchorY = 0.0f;
        aPlacement.translationX = WidgetPlacement.unitFromMeters(getContext(), R.dimen.url_bar_popup_world_x);
        aPlacement.translationZ = WidgetPlacement.unitFromMeters(getContext(), R.dimen.url_bar_popup_world_z);
        aPlacement.translationY = WidgetPlacement.unitFromMeters(getContext(), R.dimen.url_bar_popup_world_y);
    }

    @Override
    public void show() {
        super.show(false);
        mList.startAnimation(mScaleUpAnimation);
    }

    @Override
    public void hide() {
        mList.startAnimation(mScaleDownAnimation);
    }

    @Override
        public void handleResizeEvent(float aWorldWidth, float aWorldHeight) {
        mWidgetPlacement.worldWidth = aWorldWidth * (mWidgetPlacement.width/getWorldWidth());
        mWidgetManager.updateWidget(this);
    }

    // FocusChangeListener

    @Override
    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
        if (oldFocus != null && isVisible()) {
            onDismiss();
        }
    }

    public void setURLBarPopupDelegate(URLBarPopupDelegate aDelegate) {
        mURLBarDelegate = aDelegate;
    }

    public void setHighlightedText(String text) {
        mHighlightedText = text;
    }

    public void setItems(List<URLBarItem> items) {
        mListItems = items;
        mAdapter = new URLBarItemAdapter(getContext(), R.layout.list_popup_window_item, mListItems);
        mList.setAdapter(mAdapter);
    }

    public void updatePlacement() {
        float worldWidth = WidgetPlacement.floatDimension(getContext(), R.dimen.browser_world_width);
        float aspect = mWidgetPlacement.width / mWidgetPlacement.height;
        float worldHeight = worldWidth / aspect;
        float area = worldWidth * worldHeight;

        float targetWidth = (float) Math.sqrt(area * aspect);
        float targetHeight = (float) Math.sqrt(area / aspect);

        handleResizeEvent(targetWidth, targetHeight);
    }

    public static class URLBarItem {

        public enum Type {
            BOOKMARK,
            FAVORITE,
            HISTORY,
            SUGGESTION,
            COMPLETION
        }

        public String faviconURL;
        public String text;
        public String url;
        public Type type = Type.SUGGESTION;

        public static URLBarItem create(@NonNull String text, String url, String faviconURL, Type type) {
            URLBarItem item = new URLBarItem();
            item.text = text;
            item.url = url;
            item.faviconURL = faviconURL;
            item.type = type;

            return item;
        }
    }

    public class URLBarItemAdapter extends ArrayAdapter<URLBarItem> {

        private class ItemViewHolder {
            ViewGroup layout;
            ImageView favicon;
            TextView title;
            TextView url;
            ImageButton delete;
            View divider;
        }

        private LayoutInflater mInflater;

        public URLBarItemAdapter(@NonNull Context context, int resource, @NonNull List<URLBarItem> objects) {
            super(context, resource, objects);

            mInflater = LayoutInflater.from(getContext());
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View listItem = convertView;

            ItemViewHolder itemViewHolder;
            if(listItem == null) {
                listItem = mInflater.inflate(R.layout.list_popup_window_item, parent, false);

                itemViewHolder = new ItemViewHolder();

                itemViewHolder.layout = listItem.findViewById(R.id.layout);
                itemViewHolder.layout.setTag(R.string.position_tag, position);
                itemViewHolder.layout.setOnClickListener(mRowListener);
                itemViewHolder.layout.setSoundEffectsEnabled(false);
                itemViewHolder.favicon = listItem.findViewById(R.id.favicon);
                itemViewHolder.title = listItem.findViewById(R.id.title);
                itemViewHolder.url = listItem.findViewById(R.id.url);
                itemViewHolder.delete = listItem.findViewById(R.id.delete);
                itemViewHolder.delete.setTag(R.string.position_tag, position);
                itemViewHolder.delete.setSoundEffectsEnabled(false);
                itemViewHolder.delete.setOnClickListener(mDeleteButtonListener);
                itemViewHolder.divider = listItem.findViewById(R.id.divider);

                listItem.setTag(R.string.list_item_view_tag, itemViewHolder);

            } else {
                itemViewHolder = (ItemViewHolder) listItem.getTag(R.string.list_item_view_tag);
                itemViewHolder.layout.setTag(R.string.position_tag, position);
                itemViewHolder.delete.setTag(R.string.position_tag, position);
            }

            URLBarItem selectedItem = getItem(position);

            // Make search substring as bold
            final SpannableStringBuilder sb = new SpannableStringBuilder(selectedItem.text);
            final StyleSpan bold = new StyleSpan(Typeface.BOLD);
            final StyleSpan normal = new StyleSpan(Typeface.NORMAL);
            int start = selectedItem.text.indexOf(mHighlightedText);
            if (start >= 0) {
                int end = start + mHighlightedText.length();
                sb.setSpan(normal, 0, start, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                sb.setSpan(bold, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                sb.setSpan(normal, end, selectedItem.text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            itemViewHolder.title.setText(sb);

            // Set the URL text
            if (selectedItem.url == null) {
                itemViewHolder.url.setVisibility(GONE);

            } else {
                itemViewHolder.url.setVisibility(VISIBLE);
                itemViewHolder.url.setText(selectedItem.url);
            }

            // Set the description
            if (selectedItem.faviconURL == null) {
                itemViewHolder.favicon.setVisibility(GONE);

            } else {
                // TODO: Load favicon
                itemViewHolder.favicon.setVisibility(VISIBLE);
            }

            // Type related
            if (selectedItem.type == URLBarItem.Type.SUGGESTION) {
                itemViewHolder.delete.setVisibility(GONE);
                itemViewHolder.divider.setVisibility(GONE);
                itemViewHolder.favicon.setVisibility(VISIBLE);
                itemViewHolder.favicon.setImageResource(R.drawable.ic_icon_search);

            } else  if (selectedItem.type == URLBarItem.Type.COMPLETION) {
                itemViewHolder.delete.setVisibility(GONE);
                itemViewHolder.divider.setVisibility(VISIBLE);
                itemViewHolder.favicon.setVisibility(VISIBLE);
                itemViewHolder.favicon.setImageResource(R.drawable.ic_icon_browser);
            }

            return listItem;
        }

        OnClickListener mDeleteButtonListener = v -> {
            if (mAudio != null) {
                mAudio.playSound(AudioEngine.Sound.CLICK);
            }

            int position = (Integer)v.getTag(R.string.position_tag);
            URLBarPopupWidget.URLBarItem item = getItem(position);
            mAdapter.remove(item);
            mAdapter.notifyDataSetChanged();

            if (mURLBarDelegate != null) {
                mURLBarDelegate.OnItemDeleted(item);
            }
        };

        OnClickListener mRowListener = v -> {
            if (mAudio != null) {
                mAudio.playSound(AudioEngine.Sound.CLICK);
            }

            hide();

            requestFocus();
            requestFocusFromTouch();

            if (mURLBarDelegate != null) {
                int position = (Integer)v.getTag(R.string.position_tag);
                URLBarPopupWidget.URLBarItem item = getItem(position);
                mURLBarDelegate.OnItemClicked(item);
            }
        };
    }
}
