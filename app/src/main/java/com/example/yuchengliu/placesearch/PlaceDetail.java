package com.example.yuchengliu.placesearch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.places.Place;

import org.json.JSONException;
import org.json.JSONObject;

public class PlaceDetail extends AppCompatActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;

    private String place_id, place_name;
    private int[] tabIcons = {R.drawable.info_outline, R.drawable.photos, R.drawable.maps, R.drawable.review};
    private String[] tabTitles = {"INFO", "PHOTOS", "MAP", "REVIEWS"};
    private JSONObject place_detail_result_json, place_detail_json;
    private SharedPreferences favlist;
    private Menu menu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        favlist = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());


        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);


        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        for(int i = 0; i < tabLayout.getTabCount(); i++) {
            tabLayout.getTabAt(i).setCustomView(getTabView(i));
        }

        Intent intent = getIntent();
        try {
            place_detail_result_json = new JSONObject(intent.getStringExtra("detail_result_string"));
            if(place_detail_result_json.getString("status").equals("OK")) {
                place_detail_json = place_detail_result_json.getJSONObject("result");
                place_name = place_detail_json.getString("name");
                getSupportActionBar().setTitle(place_name);
                place_id = place_detail_json.getString("place_id");
            }
            else {
                Toast.makeText(this, R.string.detail_fail, Toast.LENGTH_SHORT);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
    }



    private View getTabView(int position) {
        FrameLayout frame = (FrameLayout) LayoutInflater.from(this).inflate(R.layout.custom_tab_2, null);
        TextView tabContent = (TextView) frame.findViewById(R.id.textView);
        tabContent.setText(tabTitles[position]);
        tabContent.setCompoundDrawablesRelativeWithIntrinsicBounds(tabIcons[position], 0, 0, 0);
        return tabContent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_place_detail, menu);
        this.menu = menu;
        if(favlist.contains(place_id)) {
            menu.getItem(1).setIcon(R.drawable.heart_fill_white);
        }
        else {
            menu.getItem(1).setIcon(R.drawable.heart_outline_white);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_fav:
                if(!favlist.contains(place_id)) {
                    Toast.makeText(PlaceDetail.this, place_name + " has been added to favorite", Toast.LENGTH_SHORT).show();
                    menu.getItem(1).setIcon(R.drawable.heart_fill_white);
                    SharedPreferences.Editor editor = favlist.edit();
                    editor.putString(place_id, place_detail_json.toString());
                    editor.commit();
                }
                else {
                    Toast.makeText(PlaceDetail.this, place_name + " has been removed from favorite", Toast.LENGTH_SHORT).show();
                    menu.getItem(1).setIcon(R.drawable.heart_outline_white);
                    SharedPreferences.Editor editor = favlist.edit();
                    editor.remove(place_id);
                    editor.apply();
                }

                return true;

            case R.id.action_share:
                try {
                    String share_url = place_detail_json.has("website") ?
                            "https://twitter.com/intent/tweet?text=Check%20out%20" + place_detail_json.getString("name") + "%20located%20at%20" + place_detail_json.getString("formatted_address") + ".%20Website:%20"+ place_detail_json.getString("website") + "&hashtags=TravelAndEntertainmentSearch"
                            : "https://twitter.com/intent/tweet?text=Check%20out%20" + place_detail_json.getString("name") + "%20located%20at%20" + place_detail_json.getString("formatted_address") + ".%20Website:%20"+ place_detail_json.getString("url") + "&hashtags=TravelAndEntertainmentSearch";
                    Uri uri_url = Uri.parse(share_url);
                    Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri_url);
                    startActivity(launchBrowser);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private static final int PAGE_NUM = 4;
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0:
                    PlaceInfo tab0 = new PlaceInfo();
                    return tab0;

                case 1:
                    PlaceImg tab1 = new PlaceImg();
                    return tab1;

                case 2:
                    PlaceMap tab2 = new PlaceMap();
                    return tab2;

                case 3:
                    PlaceReview tab3 = new PlaceReview();
                    return tab3;

                default:
                    return null;

            }
        }

        @Override
        public int getCount() {
            return PAGE_NUM;
        }
    }

    public JSONObject getPlace_detail_json() {
        return place_detail_json;
    }
}
