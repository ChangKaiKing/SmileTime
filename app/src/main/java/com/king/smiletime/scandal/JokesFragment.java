package com.king.smiletime.scandal;


import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.handmark.pulltorefresh.library.extras.SoundPullEventListener;
import com.king.entity.JokesEntity;
import com.king.smiletime.R;
import com.king.utils.HttpUtils;
import com.squareup.picasso.Picasso;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.media.UMImage;
import com.umeng.socialize.media.UMVideo;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * Created by Administrator on 2016/11/17.
 */
public class JokesFragment extends Fragment {

    private MediaPlayer mediaPlayer = new MediaPlayer();

    private PullToRefreshListView mLv;
    private Myadapter adapter;
    private String url;
    private List<JokesEntity.ItemsBean> items;
    private HashSet<JokesEntity.ItemsBean> hs;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.jokesfragment, null);
        mLv = (PullToRefreshListView) view.findViewById(R.id.jokeslv_id);
        TextView mTv = (TextView) view.findViewById(R.id.jokestv_id);
        mLv.setEmptyView(mTv);
        Bundle bundle = getArguments();
        url = bundle.getString("url");
        hs = new LinkedHashSet<>();
        new MyAsynctask().execute(url);

        mLv.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                String label = DateUtils.formatDateTime(getContext(), System.currentTimeMillis(),
                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);

                // Update the LastUpdatedLabel
                refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);

                // Do work to refresh the list here.
                new GetDataTask().execute(url);
            }
        });
        return view;
    }

    private class GetDataTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            // Simulates a background job.

            return HttpUtils.getJsonData(params[0]);

        }

        @Override
        protected void onPostExecute(String result) {
            JokesEntity entity = JSON.parseObject(result, JokesEntity.class);
            List<JokesEntity.ItemsBean> additems = entity.getItems();
            hs.clear();
            hs.addAll(items);
            hs.addAll(additems);
            items.clear();
            items.addAll(hs);
            adapter.notifyDataSetChanged();
            aboutlistview(items);


            // Call onRefreshComplete when the list has been refreshed.


            super.onPostExecute(result);
        }
    }


    private void aboutlistview(List<JokesEntity.ItemsBean> list) {
        //数据源

        //添加头部布局
        // View view1 = View.inflate(getContext(), R.layout.jokesfristitem, null);
        //mLv.addHeaderView(view1);

        mLv.onRefreshComplete();
        //适配器
        adapter = new Myadapter(list);
        SoundPullEventListener<ListView> soundListener = new SoundPullEventListener(getContext());
        soundListener.addSoundEvent(PullToRefreshBase.State.PULL_TO_REFRESH, R.raw.pull_event);
        soundListener.addSoundEvent(PullToRefreshBase.State.RESET, R.raw.reset_sound);
        soundListener.addSoundEvent(PullToRefreshBase.State.REFRESHING, R.raw.refreshing_sound);
        mLv.setOnPullEventListener(soundListener);
        //绑定适配器

        mLv.setAdapter(adapter);
    }


    public class MyAsynctask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            return HttpUtils.getJsonData(params[0]);
        }

        @Override
        protected void onPostExecute(String s) {

            JokesEntity entity = JSON.parseObject(s, JokesEntity.class);
            items = entity.getItems();
            aboutlistview(items);

        }
    }

    public class Myadapter extends BaseAdapter implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
        private final int TYPE_TEXT = 0;// 只包含TextView的布局类型
        private final int TYPE_IMG = 1;// 都包含TextView，ImageView的布局类型
        private final int TYPE_VIDO = 2;
        private int curPosition = -1;
        private List<JokesEntity.ItemsBean> items;


        public Myadapter(List<JokesEntity.ItemsBean> items) {
            this.items = items;
        }

        public Myadapter() {
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {

            return position;


        }

        @Override
        public int getItemViewType(int position) {

            if (items.get(position).getFormat().equals("word")) {
                return TYPE_TEXT;
            } else if (items.get(position).getFormat().equals("image")) {
                return TYPE_IMG;
            } else if (items.get(position).getFormat().equals("video")) {
                return TYPE_VIDO;
            }
            return 0;


        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHoldertext holdertext = null;
            ViewHolderimg holderimg = null;
            ViewHolderVideo holdervido = null;
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            JokesEntity.ItemsBean itemsBean = null;
            Resources resources = getResources();
            int type = getItemViewType(position);
            //如果缓存convertView为空，则需要创建View
            if (convertView == null) {
                //根据自定义的Item布局加载布局

//
                switch (type) {

                    case TYPE_TEXT:
                        itemsBean = items.get(position);
                        convertView = View.inflate(getContext(), R.layout.jokesitem, null);
                        holdertext = new ViewHoldertext(convertView, itemsBean);
                        convertView.setTag(holdertext);
                        break;
                    case TYPE_IMG:
                        itemsBean = items.get(position);
                        convertView = View.inflate(getContext(), R.layout.jokesitemimg, null);
                        holderimg = new ViewHolderimg(convertView, itemsBean);
                        convertView.setTag(holderimg);
                        break;
                    case TYPE_VIDO:
                        itemsBean = items.get(position);
                        convertView = View.inflate(getContext(), R.layout.jokes_itemvideo, null);
                        holdervido = new ViewHolderVideo(convertView, itemsBean);
                        convertView.setTag(holdervido);
                        break;
                    default:
                        break;


                }
            }
            //将设置好的布局保存到缓存中，并将其设置在Tag里，以便后面方便取出Tag

            else {

                switch (type) {
                    case TYPE_TEXT:
                        holdertext = (ViewHoldertext) convertView.getTag();
                        break;
                    case TYPE_IMG:
                        holderimg = (ViewHolderimg) convertView.getTag();
                        break;
                    case TYPE_VIDO:
                        holdervido = (ViewHolderVideo) convertView.getTag();
                        break;
                    default:
                        break;


                }
            }

            switch (type) {

                case TYPE_TEXT:

                    itemsBean = items.get(position);
                    if (itemsBean.getUser() == null || "".equals(itemsBean.getUser())) {
                        holdertext.headimg.setImageResource(resources.getIdentifier("default_users_avatar", "mipmap", getContext().getPackageName()));
                        holdertext.username.setText("匿名用户");
                    } else {
                        String s = itemsBean.getUser().getUid() + "";
                        String s1 = s.substring(0, s.length() - 4) + "/";
                        StringBuffer textbf = new StringBuffer();
                        textbf.append("http://pic.qiushibaike.com/system/avtnew/").append(s1);
                        textbf.append(s + "/").append("thumb/").append(itemsBean.getUser().getIcon());

                        Picasso.with(getContext()).load(textbf.toString()).into(holdertext.headimg);

                        holdertext.username.setText(itemsBean.getUser().getLogin());
                    }
                    String jokeType = itemsBean.getType();

                    if ("hot".equals(jokeType)) {
                        holdertext.typeimg.setImageResource(resources.getIdentifier("fire", "mipmap", getContext().getPackageName()));
                        holdertext.typetext.setText("火热");
                    } else if ("fresh".equals(jokeType)) {
                        holdertext.typeimg.setImageResource(resources.getIdentifier("leaf", "mipmap", getContext().getPackageName()));
                        holdertext.typetext.setText("新鲜");
                    } else if ("".equals(jokeType) || jokeType == null) {

                    }
                    holdertext.itemtext.setText(itemsBean.getContent());
                    holdertext.itemsmilenumb.setText(itemsBean.getVotes().getUp() + "");
                    holdertext.itemcommentnumb.setText(itemsBean.getComments_count() + "");
                    holdertext.itemsharenumb.setText(itemsBean.getShare_count() + "");
                    break;
                case TYPE_IMG:
                    itemsBean = items.get(position);
                    if (itemsBean.getUser() == null || "".equals(itemsBean.getUser())) {
                        holderimg.headimg.setImageResource(resources.getIdentifier("default_users_avatar", "mipmap", getContext().getPackageName()));
                        holderimg.username.setText("匿名用户");
                    } else {
                        String imgtext = itemsBean.getUser().getUid() + "";
                        String imgtext1 = imgtext.substring(0, imgtext.length() - 4);
                        StringBuffer imgbftitile = new StringBuffer();
                        imgbftitile.append("http://pic.qiushibaike.com/system/avtnew/").append(imgtext1);
                        imgbftitile.append("/" + imgtext).append("/medium/").append(itemsBean.getUser().getIcon());
                        Picasso.with(getContext()).load(imgbftitile.toString()).into(holderimg.headimg);
                        holderimg.username.setText(itemsBean.getUser().getLogin());
                    }
                    String jokeimgType = itemsBean.getType();
                    if ("hot".equals(jokeimgType)) {
                        holderimg.typeimg.setImageResource(resources.getIdentifier("fire", "mipmap", getContext().getPackageName()));
                        holderimg.typetext.setText("火热");
                    } else if ("fresh".equals(jokeimgType)) {
                        holderimg.typeimg.setImageResource(resources.getIdentifier("leaf", "mipmap", getContext().getPackageName()));
                        holderimg.typetext.setText("新鲜");
                    } else if ("".equals(jokeimgType) || jokeimgType == null) {

                    }
                    holderimg.itemtext.setText(itemsBean.getContent());
                    String img = itemsBean.getId() + "";
                    String img1 = img.substring(0, img.length() - 4) + "/";
                    StringBuffer imgbf = new StringBuffer();
                    imgbf.append("http://pic.qiushibaike.com/system/pictures/").append(img1);
                    imgbf.append(img + "/").append("medium/").append(itemsBean.getImage());
                    Picasso.with(getContext()).load(imgbf.toString()).into(holderimg.itemimg);
                    holderimg.itemsmilenumb.setText(itemsBean.getVotes().getUp() + "");
                    holderimg.itemcommentnumb.setText(itemsBean.getComments_count() + "");
                    holderimg.itemsharenumb.setText(itemsBean.getShare_count() + "");
                    break;
                case TYPE_VIDO:
                    itemsBean = items.get(position);
                    if (itemsBean.getUser() == null || "".equals(itemsBean.getUser())) {
                        holdervido.headimg.setImageResource(resources.getIdentifier("default_users_avatar", "mipmap", getContext().getPackageName()));
                        holdervido.username.setText("匿名用户");
                    } else {
                        String imgtext = itemsBean.getUser().getUid() + "";
                        String imgtext1 = imgtext.substring(0, imgtext.length() - 4);
                        StringBuffer videobftitile = new StringBuffer();
                        videobftitile.append("http://pic.qiushibaike.com/system/avtnew/").append(imgtext1);
                        videobftitile.append("/" + imgtext).append("/medium/").append(itemsBean.getUser().getIcon());
                        Picasso.with(getContext()).load(videobftitile.toString()).into(holdervido.headimg);
                        holdervido.username.setText(itemsBean.getUser().getLogin());
                    }
                    String jokevideoType = itemsBean.getType();
                    if ("hot".equals(jokevideoType)) {
                        holdervido.typeimg.setImageResource(resources.getIdentifier("fire", "mipmap", getContext().getPackageName()));
                        holdervido.typetext.setText("火热");
                    } else if ("fresh".equals(jokevideoType)) {
                        holdervido.typeimg.setImageResource(resources.getIdentifier("leaf", "mipmap", getContext().getPackageName()));
                        holdervido.typetext.setText("新鲜");
                    } else if ("".equals(jokevideoType) || jokevideoType == null) {

                    }
                    if (itemsBean.getContent() == null || "".equals(itemsBean.getContent())) {

                    } else {
                        holdervido.itemtext.setText(itemsBean.getContent());
                    }
                    if (itemsBean.getPic_url() != null) {
                        Picasso.with(getContext()).load(itemsBean.getPic_url()).into(holdervido.itemvideoimg);
                    }
                    if (holdervido.itemvideoimg.getTag() != null) {
                        int pos = (int) holdervido.itemvideoimg.getTag();
                        if (pos == curPosition && pos != position)//pos==curPosition表示：如果复用item出现了，则停止被复用的item的播放，这种其实不太好，如果离复用还有好几个item，就不会在第一时间停止播放
                        {
                            if (mediaPlayer.isPlaying()) {
                                mediaPlayer.stop();
                                curPosition = -1;
                            }
                        }
                    }
                    holdervido.itemvideoimg.setTag(position);

                    if (curPosition == position) {
                        holdervido.itemvideoimg.setVisibility(View.INVISIBLE);
                        holdervido.itemsurface.setVisibility(View.VISIBLE);
                        mediaPlayer.reset();
                        try {
                            mediaPlayer.setDisplay(holdervido.itemsurface.getHolder());
                            if (itemsBean.getHigh_url() != null) {
                                mediaPlayer.setDataSource(itemsBean.getHigh_url());
                            } else {
                                mediaPlayer.setDataSource(itemsBean.getLow_url());
                            }
                            mediaPlayer.prepareAsync();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        holdervido.itemvideoimg.setVisibility(View.VISIBLE);
                        holdervido.itemsurface.setVisibility(View.INVISIBLE);
                    }
                    holdervido.itemsmilenumb.setText(itemsBean.getVotes().getUp() + "");
                    holdervido.itemcommentnumb.setText(itemsBean.getComments_count() + "");
                    holdervido.itemsharenumb.setText(itemsBean.getShare_count() + "");
                    break;
                default:
                    break;


            }

            return convertView;
        }

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            curPosition = -1;
        }

        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            mediaPlayer.start();
        }


        class ViewHoldertext implements View.OnClickListener {

            public ImageView headimg;
            public TextView username;
            public ImageView typeimg;
            public TextView typetext;
            public TextView itemtext;
            public TextView itemsmilenumb;
            public TextView itemcommentnumb;
            public TextView itemsharenumb;
            public ImageView itemsmile;
            public ImageView itemsad;
            public ImageView comment;
            public ImageView share;
            public JokesEntity.ItemsBean itemsBean;
            private UMShareListener umShareListener = new UMShareListener() {
                @Override
                public void onResult(SHARE_MEDIA share_media) {

                }

                @Override
                public void onError(SHARE_MEDIA share_media, Throwable throwable) {
                    Toast.makeText(getContext(), "分享失败", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCancel(SHARE_MEDIA share_media) {
                    Toast.makeText(getContext(), "取消分享", Toast.LENGTH_LONG).show();
                }
            };

            public ViewHoldertext(View convertView, JokesEntity.ItemsBean itemsBean) {
                this.itemsBean = itemsBean;
                this.headimg = (ImageView) convertView.findViewById(R.id.jokesitmeheadimg_id);
                this.username = (TextView) convertView.findViewById(R.id.jokesitemusername_id);
                this.typeimg = (ImageView) convertView.findViewById(R.id.jokesitemtypeimg_id);
                this.typetext = (TextView) convertView.findViewById(R.id.jokesitemtypetext_id);
                this.itemtext = (TextView) convertView.findViewById(R.id.jokesitemtext_id);
                this.itemsmilenumb = (TextView) convertView.findViewById(R.id.jokesitemsmilenumb_id);
                this.itemcommentnumb = (TextView) convertView.findViewById(R.id.jokesitemcommentnumb_id);
                this.itemsharenumb = (TextView) convertView.findViewById(R.id.jokesitemsharenumb_id);
                this.itemsmile = (ImageView) convertView.findViewById(R.id.jokesitemsmileimg_id);
                this.itemsad = (ImageView) convertView.findViewById(R.id.jokesitemsadimg_id);
                this.comment = (ImageView) convertView.findViewById(R.id.jokesitemcommentimg_id);
                this.share = (ImageView) convertView.findViewById(R.id.jokesitemshareimg_id);
                itemsmile.setOnClickListener(this);
                itemsad.setOnClickListener(this);
                comment.setOnClickListener(this);
                share.setOnClickListener(this);
            }


            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.jokesitemsmileimg_id:
                        String r = itemsmilenumb.getText().toString();
                        int a = Integer.parseInt(r);
                        itemsmilenumb.setText(a + 1 + "");
                        itemsmile.setEnabled(false);
                        itemsad.setEnabled(true);
                        break;
                    case R.id.jokesitemsadimg_id:
                        String s = itemsmilenumb.getText().toString();
                        int b = Integer.parseInt(s);
                        itemsmilenumb.setText(b - 1 + "");
                        itemsmile.setEnabled(true);
                        itemsad.setEnabled(false);
                        break;
                    case R.id.jokesitemcommentimg_id:
                        break;
                    case R.id.jokesitemshareimg_id:
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        //把布局文件先填充成View对象
                        View inflate = View.inflate(getActivity(), R.layout.alertdialogmenu, null);
                        ImageView weibo = (ImageView) inflate.findViewById(R.id.weibo_id);
                        ImageView weixin = (ImageView) inflate.findViewById(R.id.weixin_id);
                        ImageView qq = (ImageView) inflate.findViewById(R.id.qq_id);
                        ImageView copy = (ImageView) inflate.findViewById(R.id.copy_id);
                        ImageView collection = (ImageView) inflate.findViewById(R.id.collection_id);
                        ImageView report = (ImageView) inflate.findViewById(R.id.report_id);
                        weibo.setOnClickListener(this);
                        weixin.setOnClickListener(this);
                        qq.setOnClickListener(this);
                        copy.setOnClickListener(this);
                        collection.setOnClickListener(this);
                        report.setOnClickListener(this);
                        //把填充得来的view对象设置为对话框显示内容
                        builder.setView(inflate);
                        builder.show();
                        break;
                    case R.id.weibo_id:
                        ShareAction actionsina = new ShareAction(getActivity());
                        actionsina.withTitle("给你看一条好笑糗事");
                        //shareAction.withText(itemsBean.getContent());
                        actionsina.withText(itemsBean.getContent());
                        actionsina.withTargetUrl("http://www.qiushibaike.com/share/"+itemsBean.getId()+"?source=qqapp");
                        actionsina.setPlatform(SHARE_MEDIA.SINA).setCallback(umShareListener).share();
                        break;
                    case R.id.weixin_id:
                        ShareAction action = new ShareAction(getActivity());
                        action.withTitle("给你看一条好笑糗事");
                        //shareAction.withText(itemsBean.getContent());
                        action.withText(itemsBean.getContent());
                        action.withTargetUrl("http://www.qiushibaike.com/share/"+itemsBean.getId()+"?source=qqapp");
                        action.setPlatform(SHARE_MEDIA.WEIXIN).setCallback(umShareListener).share();
                        break;
                    case R.id.qq_id:
                        ShareAction actionqq = new ShareAction(getActivity());
                        actionqq.withTitle("给你看一条好笑糗事");
                        //shareAction.withText(itemsBean.getContent());
                        actionqq.withText(itemsBean.getContent());
                        actionqq.withTargetUrl("http://www.qiushibaike.com/share/"+itemsBean.getId()+"?source=qqapp");
                        actionqq.setPlatform(SHARE_MEDIA.QQ).setCallback(umShareListener).share();
                        break;
                    case R.id.copy_id:
                        break;
                    case R.id.collection_id:
                        break;
                    case R.id.report_id:
                        break;
                    default:
                        break;
                }
            }
        }

        private final class ViewHolderimg implements View.OnClickListener {
            public ImageView headimg;
            public TextView username;
            public ImageView typeimg;
            public TextView typetext;
            public TextView itemtext;
            public ImageView itemimg;
            public TextView itemsmilenumb;
            public TextView itemcommentnumb;
            public TextView itemsharenumb;
            public ImageView itemsmile;
            public ImageView itemsad;
            public ImageView comment;
            public ImageView share;
            public JokesEntity.ItemsBean itemsBean;
            private UMShareListener umShareListener = new UMShareListener() {
                @Override
                public void onResult(SHARE_MEDIA share_media) {

                }

                @Override
                public void onError(SHARE_MEDIA share_media, Throwable throwable) {
                    Toast.makeText(getContext(), "分享失败", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCancel(SHARE_MEDIA share_media) {
                    Toast.makeText(getContext(), "取消分享", Toast.LENGTH_LONG).show();
                }
            };

            public ViewHolderimg(View convertView, JokesEntity.ItemsBean itemsBean) {
                this.itemsBean = itemsBean;
                this.headimg = (ImageView) convertView.findViewById(R.id.jokesitmeimgheadimg_id);
                this.username = (TextView) convertView.findViewById(R.id.jokesitemimguesrname_id);
                this.typeimg = (ImageView) convertView.findViewById(R.id.jokesitemimgtypeimg_id);
                this.typetext = (TextView) convertView.findViewById(R.id.jokesitemimgtypetext_id);
                this.itemtext = (TextView) convertView.findViewById(R.id.jokesitemimgtext_id);
                this.itemimg = (ImageView) convertView.findViewById(R.id.jokesitemimg_id);
                this.itemsmilenumb = (TextView) convertView.findViewById(R.id.jokesitemimgsmilenumb_id);
                this.itemcommentnumb = (TextView) convertView.findViewById(R.id.jokesitemimgcommentnumb_id);
                this.itemsharenumb = (TextView) convertView.findViewById(R.id.jokesitemimgsharenumb_id);
                this.itemsmile = (ImageView) convertView.findViewById(R.id.jokesitemimgsmileimg_id);
                this.itemsad = (ImageView) convertView.findViewById(R.id.jokesitemimgsadimg_id);
                this.comment = (ImageView) convertView.findViewById(R.id.jokesitemimgcommentimg_id);
                this.share = (ImageView) convertView.findViewById(R.id.jokesitemimgshareimg_id);
                itemsmile.setOnClickListener(this);
                itemsad.setOnClickListener(this);
                comment.setOnClickListener(this);
                share.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.jokesitemimgsmileimg_id:
                        String r = itemsmilenumb.getText().toString();
                        int a = Integer.parseInt(r);
                        itemsmilenumb.setText(a + 1 + "");
                        itemsmile.setEnabled(false);
                        itemsad.setEnabled(true);
                        break;
                    case R.id.jokesitemimgsadimg_id:
                        String s = itemsmilenumb.getText().toString();
                        int b = Integer.parseInt(s);
                        itemsmilenumb.setText(b - 1 + "");
                        itemsmile.setEnabled(true);
                        itemsad.setEnabled(false);
                        break;
                    case R.id.jokesitemimgcommentimg_id:
                        break;
                    case R.id.jokesitemimgshareimg_id:
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle("分享给朋友");
                        //把布局文件先填充成View对象
                        View inflate = View.inflate(getActivity(), R.layout.alertdialogmenu, null);
                        ImageView weibo = (ImageView) inflate.findViewById(R.id.weibo_id);
                        ImageView weixin = (ImageView) inflate.findViewById(R.id.weixin_id);
                        ImageView qq = (ImageView) inflate.findViewById(R.id.qq_id);
                        ImageView copy = (ImageView) inflate.findViewById(R.id.copy_id);
                        ImageView collection = (ImageView) inflate.findViewById(R.id.collection_id);
                        ImageView report = (ImageView) inflate.findViewById(R.id.report_id);
                        weibo.setOnClickListener(this);
                        weixin.setOnClickListener(this);
                        qq.setOnClickListener(this);
                        copy.setOnClickListener(this);
                        collection.setOnClickListener(this);
                        report.setOnClickListener(this);
                        //把填充得来的view对象设置为对话框显示内容
                        builder.setView(inflate);
                        builder.show();
                        break;
                    case R.id.weibo_id:
                        ShareAction actionsina = new ShareAction(getActivity());
                        actionsina.withTitle("给你看一张好笑糗图");
                        //shareAction.withText(itemsBean.getContent());
                        actionsina.withText(itemsBean.getContent());
                        String img = itemsBean.getId() + "";
                        String img1 = img.substring(0, img.length() - 4) + "/";
                        StringBuffer imgbf = new StringBuffer();
                        imgbf.append("http://pic.qiushibaike.com/system/pictures/").append(img1);
                        imgbf.append(img + "/").append("medium/").append(itemsBean.getImage());
                        //UMImage umImage = new UMImage(activity,itemsBean.getLow_url());
                        UMImage umImage = new UMImage(getActivity(), imgbf.toString());
                        actionsina.withMedia(umImage);
                        actionsina.withTargetUrl("http://www.qiushibaike.com/share/"+itemsBean.getId()+"?source=qqapp");
                        actionsina.setPlatform(SHARE_MEDIA.SINA).setCallback(umShareListener).share();
                        break;
                    case R.id.weixin_id:
                        ShareAction action = new ShareAction(getActivity());
                        action.withTitle("给你看一张好笑糗图");
                        //shareAction.withText(itemsBean.getContent());
                        action.withText(itemsBean.getContent());
                        img = itemsBean.getId() + "";
                        img1 = img.substring(0, img.length() - 4) + "/";
                        imgbf = new StringBuffer();
                        imgbf.append("http://pic.qiushibaike.com/system/pictures/").append(img1);
                        imgbf.append(img + "/").append("medium/").append(itemsBean.getImage());
                        //UMImage umImage = new UMImage(activity,itemsBean.getLow_url());
                        umImage = new UMImage(getActivity(), imgbf.toString());
                        action.withMedia(umImage);
                        action.withTargetUrl("http://www.qiushibaike.com/share/"+itemsBean.getId()+"?source=qqapp");
                        action.setPlatform(SHARE_MEDIA.WEIXIN).setCallback(umShareListener).share();
                        break;
                    case R.id.qq_id:
                        ShareAction actionqq = new ShareAction(getActivity());
                        actionqq.withTitle("给你看一张好笑糗图");
                        //shareAction.withText(itemsBean.getContent());
                        actionqq.withText(itemsBean.getContent());
                        img = itemsBean.getId() + "";
                        img1 = img.substring(0, img.length() - 4) + "/";
                        imgbf = new StringBuffer();
                        imgbf.append("http://pic.qiushibaike.com/system/pictures/").append(img1);
                        imgbf.append(img + "/").append("medium/").append(itemsBean.getImage());
                        //UMImage umImage = new UMImage(activity,itemsBean.getLow_url());
                        umImage = new UMImage(getActivity(), imgbf.toString());
                        actionqq.withMedia(umImage);
                        actionqq.withTargetUrl("http://www.qiushibaike.com/share/"+itemsBean.getId()+"?source=qqapp");

                        actionqq.setPlatform(SHARE_MEDIA.QQ).setCallback(umShareListener).share();
                        break;
                    case R.id.copy_id:
                        break;
                    case R.id.collection_id:
                        break;
                    case R.id.report_id:
                        break;
                    default:
                        break;
                }
            }
        }

        private final class ViewHolderVideo implements View.OnClickListener {
            public ImageView headimg;
            public TextView username;
            public ImageView typeimg;
            public TextView typetext;
            public TextView itemtext;
            public SurfaceView itemsurface;
            public ImageView itemvideoimg;
            public TextView itemsmilenumb;
            public TextView itemcommentnumb;
            public TextView itemsharenumb;
            public JokesEntity.ItemsBean itemsBean;
            public ImageView itemsmile;
            public ImageView itemsad;
            public ImageView comment;
            public ImageView share;
            private UMShareListener umShareListener = new UMShareListener() {
                @Override
                public void onResult(SHARE_MEDIA share_media) {

                }

                @Override
                public void onError(SHARE_MEDIA share_media, Throwable throwable) {
                    Toast.makeText(getContext(), "分享失败", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCancel(SHARE_MEDIA share_media) {
                    Toast.makeText(getContext(), "取消分享", Toast.LENGTH_LONG).show();
                }
            };

            public ViewHolderVideo(View convertView, JokesEntity.ItemsBean itemsBean) {
                this.itemsBean = itemsBean;
                this.headimg = (ImageView) convertView.findViewById(R.id.jokesitmevideoheadimg_id);
                this.username = (TextView) convertView.findViewById(R.id.jokesitemvideouesrname_id);
                this.typeimg = (ImageView) convertView.findViewById(R.id.jokesitemvideotypeimg_id);
                this.typetext = (TextView) convertView.findViewById(R.id.jokesitemvideotypetext_id);
                this.itemtext = (TextView) convertView.findViewById(R.id.jokesitemvideotext_id);
                this.itemsurface = (SurfaceView) convertView.findViewById(R.id.jokesitemvideo_id);
                this.itemvideoimg = (ImageView) convertView.findViewById(R.id.jokesitemvideoimg_id);
                this.itemsmilenumb = (TextView) convertView.findViewById(R.id.jokesitemvideosmilenumb_id);
                this.itemcommentnumb = (TextView) convertView.findViewById(R.id.jokesitemvideocommentnumb_id);
                this.itemsharenumb = (TextView) convertView.findViewById(R.id.jokesitemvideosharenumb_id);
                this.itemsmile = (ImageView) convertView.findViewById(R.id.jokesitemvideosmileimg_id);
                this.itemsad = (ImageView) convertView.findViewById(R.id.jokesitemvideosadimg_id);
                this.comment = (ImageView) convertView.findViewById(R.id.jokesitemvideocommentimg_id);
                this.share = (ImageView) convertView.findViewById(R.id.jokesitemvideoshareimg_id);
                itemsmile.setOnClickListener(this);
                itemsad.setOnClickListener(this);
                comment.setOnClickListener(this);
                share.setOnClickListener(this);
                itemvideoimg.setOnClickListener(this);
                itemsurface.setOnClickListener(this);
                notifyDataSetChanged();
            }

            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.jokesitemvideoimg_id:
                        view.setVisibility(View.INVISIBLE);
                        curPosition = (int) view.getTag();
                        break;
                    case R.id.jokesitemvideo_id:
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                            curPosition = -1;
                        }
                        break;
                    case R.id.jokesitemimgsmileimg_id:
                        String r = itemsmilenumb.getText().toString();
                        int a = Integer.parseInt(r);
                        itemsmilenumb.setText(a + 1 + "");
                        itemsmile.setEnabled(false);
                        itemsad.setEnabled(true);
                        break;
                    case R.id.jokesitemimgsadimg_id:
                        String s = itemsmilenumb.getText().toString();
                        int b = Integer.parseInt(s);
                        itemsmilenumb.setText(b - 1 + "");
                        itemsmile.setEnabled(true);
                        itemsad.setEnabled(false);
                        break;
                    case R.id.jokesitemimgcommentimg_id:
                        break;
                    case R.id.jokesitemimgshareimg_id:
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle("分享给朋友");
                        //把布局文件先填充成View对象
                        View inflate = View.inflate(getActivity(), R.layout.alertdialogmenu, null);
                        ImageView weibo = (ImageView) inflate.findViewById(R.id.weibo_id);
                        ImageView weixin = (ImageView) inflate.findViewById(R.id.weixin_id);
                        ImageView qq = (ImageView) inflate.findViewById(R.id.qq_id);
                        ImageView copy = (ImageView) inflate.findViewById(R.id.copy_id);
                        ImageView collection = (ImageView) inflate.findViewById(R.id.collection_id);
                        ImageView report = (ImageView) inflate.findViewById(R.id.report_id);
                        weibo.setOnClickListener(this);
                        weixin.setOnClickListener(this);
                        qq.setOnClickListener(this);
                        copy.setOnClickListener(this);
                        collection.setOnClickListener(this);
                        report.setOnClickListener(this);
                        //把填充得来的view对象设置为对话框显示内容
                        builder.setView(inflate);
                        builder.show();
                        break;
                    case R.id.weibo_id:
                        ShareAction actionsina = new ShareAction(getActivity());
                        actionsina.withTitle("给你看一条好笑视频");
                        //shareAction.withText(itemsBean.getContent());
                        actionsina.withText(itemsBean.getContent());
                        UMVideo umVideo = new UMVideo(itemsBean.getHigh_url());
                        actionsina.withMedia(umVideo);
                        actionsina.withTargetUrl(itemsBean.getHigh_url());
                        actionsina.withTargetUrl("http://www.qiushibaike.com/share/"+itemsBean.getId()+"?source=qqapp");
                        actionsina.setPlatform(SHARE_MEDIA.SINA).setCallback(umShareListener).share();
                        break;
                    case R.id.weixin_id:
                        ShareAction action = new ShareAction(getActivity());
                        action.withTitle("给你看一条好笑视频");
                        //shareAction.withText(itemsBean.getContent());
                        action.withText(itemsBean.getContent());
                        umVideo = new UMVideo(itemsBean.getHigh_url());
                        action.withMedia(umVideo);
                        action.withTargetUrl(itemsBean.getHigh_url());
                        action.withTargetUrl("http://www.qiushibaike.com/share/"+itemsBean.getId()+"?source=qqapp");
                        action.setPlatform(SHARE_MEDIA.WEIXIN).setCallback(umShareListener).share();
                        break;
                    case R.id.qq_id:
                        ShareAction actionqq = new ShareAction(getActivity());
                        actionqq.withTitle("给你看一条好笑视频");
                        //shareAction.withText(itemsBean.getContent());
                        actionqq.withText(itemsBean.getContent());
                        umVideo = new UMVideo(itemsBean.getHigh_url());
                        actionqq.withMedia(umVideo);
                        actionqq.withTargetUrl(itemsBean.getHigh_url());
                        actionqq.withTargetUrl("http://www.qiushibaike.com/share/"+itemsBean.getId()+"?source=qqapp");
                        actionqq.setPlatform(SHARE_MEDIA.QQ).setCallback(umShareListener).share();
                        break;
                    case R.id.copy_id:
                        break;
                    case R.id.collection_id:
                        break;
                    case R.id.report_id:
                        break;
                    default:
                        break;
                }
                notifyDataSetChanged();
            }


        }

    }
}
