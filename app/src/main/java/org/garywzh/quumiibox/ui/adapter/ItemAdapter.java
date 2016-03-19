package org.garywzh.quumiibox.ui.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.garywzh.quumiibox.R;
import org.garywzh.quumiibox.model.Item;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final OnItemActionListener mListener;
    private List<Item> mData;

    public ItemAdapter(@NonNull OnItemActionListener listener) {
        mListener = listener;
        setHasStableIds(true);
    }

    public void setDataSource(List<Item> data) {
        mData = data;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == Item.TYPE_VIDEO) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_video, parent, false);
            return new VideoViewHolder(mListener, view);
        } else if (viewType == Item.TYPE_PIC | viewType == Item.TYPE_LONGPIC | viewType == Item.TYPE_GIF) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_pic, parent, false);
            return new ImageViewHolder(mListener, view);
        } else if (viewType == Item.TYPE_LINK | viewType == Item.TYPE_TUJI | viewType == Item.TYPE_DUANZI) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_link, parent, false);
            return new TopicViewHolder(mListener, view);
        }

        throw new RuntimeException("there is no type that matches the type " + viewType + " + make sure your using types correctly");
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof VideoViewHolder) {
            ((VideoViewHolder) holder).fillData(mData.get(position));
        } else if (holder instanceof ImageViewHolder) {
            ((ImageViewHolder) holder).fillData(mData.get(position));
        } else if (holder instanceof TopicViewHolder) {
            ((TopicViewHolder) holder).fillData(mData.get(position));
        }
    }

    @Override
    public long getItemId(int position) {
        return mData == null ? RecyclerView.NO_ID : Integer.parseInt(mData.get(position).blogid);
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    @Override
    public int getItemViewType(int position) {
        int type;

        switch (mData.get(position).type) {
            case "video":
                type = Item.TYPE_VIDEO;
                break;
            case "pic":
                type = Item.TYPE_PIC;
                break;
            case "longpic":
                type = Item.TYPE_LONGPIC;
                break;
            case "gif":
                type = Item.TYPE_GIF;
                break;
            case "link":
                type = Item.TYPE_LINK;
                break;
            case "duanzi":
                type = Item.TYPE_DUANZI;
                break;
            case "tuji":
                type = Item.TYPE_TUJI;
                break;
            default:
                throw new RuntimeException("unknown type");
        }

        return type;
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final ImageView mCoverPic;
        public final TextView mTitle;
        public final TextView mTime;
        public final TextView mReplyCount;
        public final TextView mThumbUpCount;

        private final OnItemActionListener mListener;
        private Item mItem;

        public VideoViewHolder(View view) {
            this(null, view);
        }

        public VideoViewHolder(OnItemActionListener listener, View view) {
            super(view);
            mListener = listener;

            view.setOnClickListener(this);

            mCoverPic = ((ImageView) view.findViewById(R.id.cover_img));
            mTitle = ((TextView) view.findViewById(R.id.tv_title));
            mTime = ((TextView) view.findViewById(R.id.tv_time));
            mThumbUpCount = (TextView) view.findViewById(R.id.tv_thumbupcount);
            mReplyCount = (TextView) view.findViewById(R.id.tv_replycount);
        }

        public void fillData(Item item) {
            if (item.equals(mItem)) {
                return;
            }
            mItem = item;

            mTitle.setText(item.subject);
            mTime.setText(item.dateline);
            mThumbUpCount.setText(item.like);
            mReplyCount.setText(item.replynum);

            setCoverPic(item);
        }

        private void setCoverPic(Item item) {
            final String url = item.img;
            Glide.with(mCoverPic.getContext()).load(url)
                    .placeholder(R.drawable.coverpic_default).crossFade()
                    .into(mCoverPic);
        }

        @Override
        public void onClick(View v) {
            if (mListener == null) {
                return;
            }

            mListener.onItemOpen(v, mItem);
        }
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final ImageView mCoverPic;
        public final TextView mTitle;
        public final TextView mTime;
        public final TextView mReplyCount;
        public final TextView mThumbUpCount;

        private final OnItemActionListener mListener;
        private Item mItem;

        public ImageViewHolder(View view) {
            this(null, view);
        }

        public ImageViewHolder(OnItemActionListener listener, View view) {
            super(view);
            mListener = listener;

            view.setOnClickListener(this);

            mTitle = ((TextView) view.findViewById(R.id.tv_title));
            mCoverPic = ((ImageView) view.findViewById(R.id.cover_img));
            mTime = ((TextView) view.findViewById(R.id.tv_time));
            mThumbUpCount = (TextView) view.findViewById(R.id.tv_thumbupcount);
            mReplyCount = (TextView) view.findViewById(R.id.tv_replycount);
        }

        public void fillData(Item item) {
            if (item.equals(mItem)) {
                return;
            }
            mItem = item;

            mTitle.setText(item.subject);
            mTime.setText(item.dateline);
            mThumbUpCount.setText(item.like);
            mReplyCount.setText(item.replynum);

            setCoverPic(item);
        }

        private void setCoverPic(Item item) {
            final String url = item.img;
            Glide.with(mCoverPic.getContext()).load(url)
                    .placeholder(R.drawable.coverpic_default).crossFade()
                    .into(mCoverPic);
        }

        @Override
        public void onClick(View v) {
            if (mListener == null) {
                return;
            }

            mListener.onItemOpen(v, mItem);
        }
    }

    public static class TopicViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final TextView mTitle;
        public final TextView mTime;
        public final TextView mReplyCount;
        public final TextView mThumbUpCount;

        private final OnItemActionListener mListener;
        private Item mItem;

        public TopicViewHolder(View view) {
            this(null, view);
        }

        public TopicViewHolder(OnItemActionListener listener, View view) {
            super(view);
            mListener = listener;

            view.setOnClickListener(this);

            mTitle = ((TextView) view.findViewById(R.id.tv_title));
            mTime = ((TextView) view.findViewById(R.id.tv_time));
            mThumbUpCount = (TextView) view.findViewById(R.id.tv_thumbupcount);
            mReplyCount = (TextView) view.findViewById(R.id.tv_replycount);
        }

        public void fillData(Item item) {
            if (item.equals(mItem)) {
                return;
            }
            mItem = item;

            mTitle.setText(item.subject);
            mTime.setText(item.dateline);
            mThumbUpCount.setText(item.like);
            mReplyCount.setText(item.replynum);
        }

        @Override
        public void onClick(View v) {
            if (mListener == null) {
                return;
            }

            mListener.onItemOpen(v, mItem);
        }
    }

    public interface OnItemActionListener {

        boolean onItemOpen(View view, Item item);
    }
}
