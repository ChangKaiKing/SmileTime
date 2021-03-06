package com.king.smiletime.video;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.king.smiletime.LoadappActivity;
import com.king.smiletime.R;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Administrator on 2016/11/17.
 */

public class VideoFragment extends Fragment implements View.OnClickListener{

    private View view;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private String[] tabNames;
    private List<Fragment> fragments;
    private MyFragmentPagerAdapter adapter;
    private String url;
    private TextView textbtn;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.videofragment, null);
        //界面实例的初始化
        initWigets();
        //关于ViewPager的操作
        aboutView();
        //关于TabLayout
        aboutTabLayout();
        return view;
    }

    /**
     * 界面实例的初始化
     */
    private void initWigets() {
        tabLayout =  (TabLayout) view.findViewById(R.id.tab_id);
        viewPager = (ViewPager) view.findViewById(R.id.vpger_id);
        tabNames = getResources().getStringArray(R.array.tabNames3);
        textbtn = (TextView) view.findViewById(R.id.tv_id);
        url="http://live.qiushibaike.com/live/all/list?count=30&page=;&rqcnt=;";
        textbtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(getContext(), LoadappActivity.class);
        startActivity(intent);

    }

    /**
     * 关于ViewPager的操作
     */
    private void aboutView() {
        //数据源
        fragments = new LinkedList<>();
        for(String tabName:tabNames){
            if(tabName.equals(tabNames[0])){
                ThirdFragment fragment = new ThirdFragment();
                Bundle args = new Bundle();
                args.putString("tabName",tabName);
                args.putString("url1",url);
                fragment.setArguments(args);
                fragments.add(fragment);

            }else if(tabName.equals(tabNames[1])){
                OtherFragment fragment =new OtherFragment();
                Bundle args = new Bundle();
                args.putString("tabName",tabName);
                args.putString("url","");
                fragment.setArguments(args);
                fragments.add(fragment);

            }


        }

        //适配器
        adapter = new MyFragmentPagerAdapter(getFragmentManager());
        //绑定
        viewPager.setAdapter(adapter);
    }



    private final  class MyFragmentPagerAdapter extends FragmentStatePagerAdapter {

        public MyFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabNames[position];
        }
    }

    /**
     * 关于TabLayout
     */
    private void aboutTabLayout() {
        tabLayout.setupWithViewPager(viewPager);
    }


}
