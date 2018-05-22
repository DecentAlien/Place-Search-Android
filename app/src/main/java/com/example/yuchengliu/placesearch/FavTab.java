package com.example.yuchengliu.placesearch;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class FavTab extends Fragment {
    private TextView no_favs;
    private ScrollView scroll;
    private TableLayout table;
    private SharedPreferences fav_list;
    private Map<String, ?> fav_map;
    private RequestQueue req_queue;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.favtab, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        no_favs = getActivity().findViewById(R.id.no_favs);
        scroll = (ScrollView) getActivity().findViewById(R.id.scroll_fav_table);
        table = (TableLayout) getActivity().findViewById(R.id.favs_table);
        req_queue = Volley.newRequestQueue(getActivity().getApplicationContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        createFavsTable();
    }

    private void createFavsTable() {
        fav_list = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        fav_map = fav_list.getAll();
        if(fav_map.isEmpty()) {
            no_favs.setVisibility(View.VISIBLE);
            scroll.setVisibility(View.GONE);
        }
        else {
            no_favs.setVisibility(View.GONE);
            scroll.setVisibility(View.VISIBLE);
            table.removeAllViews();
            for (Map.Entry<String, ?> entry : fav_map.entrySet()) {
                try {
                    final JSONObject place = new JSONObject(entry.getValue().toString());
                    TableRow tr = new TableRow(getActivity().getApplicationContext());
                    tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tr.setPadding(0, convertDimen(14), 0, convertDimen(14));

                    ImageView cgIcon = new ImageView(getActivity().getApplicationContext());
                    Picasso.get().load(place.getString("icon")).resize(convertDimen(50), convertDimen(50)).centerCrop().into(cgIcon);
                    TableRow.LayoutParams cgIconParams = new TableRow.LayoutParams(convertDimen(50), TableRow.LayoutParams.MATCH_PARENT);
                    cgIconParams.setMargins(convertDimen(5), 0, convertDimen(10), 0);
                    cgIcon.setLayoutParams(cgIconParams);
                    tr.addView(cgIcon);

                    LinearLayout linear = new LinearLayout(getActivity().getApplicationContext());
                    linear.setLayoutParams(new TableRow.LayoutParams(convertDimen(270), TableRow.LayoutParams.WRAP_CONTENT));
                    linear.setOrientation(LinearLayout.VERTICAL);

                    TextView place_name = new TextView(getActivity().getApplicationContext());
                    final String name = place.getString("name");
                    final String place_id = place.getString("place_id");
                    place_name.setText(name);
                    place_name.setTypeface(place_name.getTypeface(), Typeface.BOLD);
                    place_name.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    place_name.setPadding(0, convertDimen(4), 0, 0);
                    linear.addView(place_name);

                    TextView place_addr = new TextView(getActivity().getApplicationContext());
                    place_addr.setText(place.getString("vicinity"));
                    place_addr.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    linear.addView(place_addr);

                    linear.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new GetFavDetail().execute(place_id);
                        }
                    });

                    tr.addView(linear);

                    final ImageView favIcon = new ImageView(getActivity().getApplicationContext());
                    favIcon.setImageResource(R.drawable.heart_fill_red);
                    TableRow.LayoutParams imgParams = new TableRow.LayoutParams(convertDimen(50), TableRow.LayoutParams.MATCH_PARENT);
                    imgParams.gravity = Gravity.CENTER;
                    favIcon.setLayoutParams(imgParams);

                    favIcon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                if (!fav_list.contains(place.getString("place_id"))) {
                                    Toast.makeText(getActivity().getApplicationContext(), name + " has been added to favorite", Toast.LENGTH_SHORT).show();
                                    favIcon.setImageResource(R.drawable.heart_fill_red);
                                    SharedPreferences.Editor editor = fav_list.edit();
                                    editor.putString(place_id, place.toString());
                                    editor.commit();
                                } else {
                                    Toast.makeText(getActivity().getApplicationContext(), name + " has been removed from favorite", Toast.LENGTH_SHORT).show();
                                    favIcon.setImageResource(R.drawable.heart_outline_black);
                                    SharedPreferences.Editor editor = fav_list.edit();
                                    editor.remove(place_id);
                                    editor.apply();
                                    createFavsTable();
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
    }
    private int convertDimen(int dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private class GetFavDetail extends AsyncTask<String, Void, Void> {
        private ProgressDialog dialog;
        private Intent intent;

        public GetFavDetail() {
            dialog = new ProgressDialog(getActivity());
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage(getString(R.string.fetching_details));
            dialog.show();
        }

        @Override
        protected Void doInBackground(String ... params) {
            intent = new Intent(getActivity().getApplicationContext(), PlaceDetail.class);
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
                            Toast.makeText(getActivity().getApplicationContext(), "ERROR: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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