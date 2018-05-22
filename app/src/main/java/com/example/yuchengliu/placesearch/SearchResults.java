package com.example.yuchengliu.placesearch;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SearchResults extends AppCompatActivity {
    private TextView no_results;
    private JSONObject result_json;
    private ScrollView scroll;
    private TableLayout table;
    private JSONArray[] result_page_arr = new JSONArray[3];
    private String[] next_token = new String[2];
    private int curPage = 0;
    private LinearLayout page_btn_group;
    private Button pre_page, next_page;
    private RequestQueue req_queue;
    private SharedPreferences favlist;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        no_results = (TextView) findViewById(R.id.no_results);
        scroll = (ScrollView) findViewById(R.id.scroll_table);
        table = (TableLayout) findViewById(R.id.results_table);
        page_btn_group = (LinearLayout) findViewById(R.id.page_btn_group);
        pre_page = (Button) findViewById(R.id.pre_btn);
        next_page = (Button) findViewById(R.id.next_btn);
        req_queue = Volley.newRequestQueue(this);
        favlist = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Intent intent = getIntent();
        try {
            result_json = new JSONObject(intent.getStringExtra("result_string"));
            if(result_json.getString("status").equals("ZERO_RESULTS")) {
                no_results.setVisibility(TextView.VISIBLE);
            }
            if(result_json.getString("status").equals("OK")) {
                result_page_arr[curPage] = result_json.getJSONArray("results");
                scroll.setVisibility(ScrollView.VISIBLE);
                page_btn_group.setVisibility(LinearLayout.VISIBLE);
                createTable(result_page_arr[curPage]);
                if(result_json.has("next_page_token")) {
                    next_token[curPage] = result_json.getString("next_page_token");
                    next_page.setEnabled(true);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        pre_page.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                curPage--;
                createTable(result_page_arr[curPage]);
                next_page.setEnabled(true);
                if(curPage == 0) {
                    pre_page.setEnabled(false);
                }
            }
        });

        next_page.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(result_page_arr[curPage+1] == null) {
                    new GetNextPageTask().execute(curPage);
                }
                else {
                    createTable(result_page_arr[curPage+1]);
                    if(curPage == 1 || next_token[curPage+1] == null) {
                        next_page.setEnabled(false);
                    }
                }
                curPage++;
                pre_page.setEnabled(true);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(result_page_arr[curPage]!= null) {
            createTable(result_page_arr[curPage]);
        }
    }

    private void createTable(JSONArray result) {
        table.removeAllViews();
        for(int i = 0; i < result.length(); i++) {
            try {
                final JSONObject place= result.getJSONObject(i);
                TableRow tr = new TableRow(this);
                tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.setPadding(0, convertDimen(14), 0, convertDimen(14));

                ImageView cgIcon = new ImageView(this);
                Picasso.get().load(place.getString("icon")).resize(convertDimen(50), convertDimen(50)).centerCrop().into(cgIcon);
                TableRow.LayoutParams cgIconParams =  new TableRow.LayoutParams(convertDimen(50), TableRow.LayoutParams.MATCH_PARENT);
                cgIconParams.setMargins(convertDimen(5), 0, convertDimen(10), 0);
                cgIcon.setLayoutParams(cgIconParams);
                tr.addView(cgIcon);

                LinearLayout linear = new LinearLayout(this);
                linear.setLayoutParams(new TableRow.LayoutParams(convertDimen(270), TableRow.LayoutParams.WRAP_CONTENT));
                linear.setOrientation(LinearLayout.VERTICAL);

                TextView place_name = new TextView(this);
                final String name = place.getString("name");
                final String place_id = place.getString("place_id");
                place_name.setText(name);
                place_name.setTypeface(place_name.getTypeface(), Typeface.BOLD);
                place_name.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                place_name.setPadding(0, convertDimen(4), 0, 0);
                linear.addView(place_name);

                TextView place_addr = new TextView(this);
                place_addr.setText(place.getString("vicinity"));
                place_addr.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                linear.addView(place_addr);

                linear.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new GetDetailTask().execute(place_id);
                    }
                });

                tr.addView(linear);

                final ImageView favIcon = new ImageView(this);
                if(favlist.contains(place.getString("place_id"))) {
                    favIcon.setImageResource(R.drawable.heart_fill_red);
                }
                else {
                    favIcon.setImageResource(R.drawable.heart_outline_black);
                }
                TableRow.LayoutParams imgParams = new TableRow.LayoutParams(convertDimen(50), TableRow.LayoutParams.MATCH_PARENT);
                imgParams.gravity = Gravity.CENTER;
                favIcon.setLayoutParams(imgParams);

                favIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if(!favlist.contains(place.getString("place_id"))) {
                                Toast.makeText(SearchResults.this, name + " has been added to favorite", Toast.LENGTH_SHORT).show();
                                favIcon.setImageResource(R.drawable.heart_fill_red);
                                SharedPreferences.Editor editor = favlist.edit();
                                editor.putString(place_id, place.toString());
                                editor.commit();
                            }
                            else {
                                Toast.makeText(SearchResults.this, name + " has been removed from favorite", Toast.LENGTH_SHORT).show();
                                favIcon.setImageResource(R.drawable.heart_outline_black);
                                SharedPreferences.Editor editor = favlist.edit();
                                editor.remove(place_id);
                                editor.apply();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                tr.addView(favIcon);

                table.addView(tr);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    private int convertDimen(int dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private class GetNextPageTask extends AsyncTask<Integer, Void, Void> {
        private ProgressDialog dialog;

        public GetNextPageTask() {
            dialog = new ProgressDialog(SearchResults.this);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage(getString(R.string.fetching_next_page));
            dialog.show();
        }

        @Override
        protected Void doInBackground(Integer ... params) {
            final int page = params[0];
            String next_token_url = "https://place-search-android.appspot.com/nextpage?token=" + next_token[page];
            JsonObjectRequest next_token_Request = new JsonObjectRequest
                    (Request.Method.GET, next_token_url, null, new Response.Listener<JSONObject>() {
                        public void onResponse(JSONObject response) {
                            try {
                                result_page_arr[page+1] = response.getJSONArray("results");
                                createTable(result_page_arr[page+1]);
                                if(response.has("next_page_token")) {
                                    next_token[page+1] = response.getString("next_page_token");
                                }
                                else {
                                    next_page.setEnabled(false);
                                }
                                if (dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }
                            Toast.makeText(SearchResults.this, "ERROR: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
            req_queue.add(next_token_Request);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            return;
        }
    }


    private class GetDetailTask extends AsyncTask<String, Void, Void> {
        private ProgressDialog dialog;
        private Intent intent;

        public GetDetailTask() {
            dialog = new ProgressDialog(SearchResults.this);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage(getString(R.string.fetching_details));
            dialog.show();
        }

        @Override
        protected Void doInBackground(String ... params) {
            intent = new Intent(SearchResults.this, PlaceDetail.class);
            String place_detail_url = "https://place-search-android.appspot.com/placedetail?id=" + params[0];
            JsonObjectRequest detailRequest = new JsonObjectRequest
                    (Request.Method.GET, place_detail_url, null, new Response.Listener<JSONObject>() {
                        public void onResponse(JSONObject response) {
                            intent.putExtra("detail_result_string", response.toString());
                            startActivity(intent);
                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }
                            Toast.makeText(SearchResults.this, "ERROR: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
            req_queue.add(detailRequest);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            return;
        }
    }
}



