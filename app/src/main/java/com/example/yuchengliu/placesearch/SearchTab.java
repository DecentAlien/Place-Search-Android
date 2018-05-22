package com.example.yuchengliu.placesearch;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class SearchTab extends Fragment {
    private EditText keyword_view, distance_view, loc_input_view;
    private Spinner cgSpinner;
    private RadioGroup loc_radios;
    private RadioButton current_btn, other_btn;
    private AutoCompleteTextView searchPlace;
    private TextView kw_warning, loc_warning;
    private Button searchbtn, clearbtn;
    private String usr_loc, geocode_result;
    private String keyword, category, distance_in_mile;
    private Double distance_in_meter;
    private RequestQueue html_req_queue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.searchtab, container, false);
        return rootView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        cgSpinner = (Spinner) getActivity().findViewById(R.id.cg);
        ArrayAdapter<CharSequence> arradapter = ArrayAdapter.createFromResource(getActivity().getApplicationContext(),
                R.array.categories, android.R.layout.simple_spinner_item);
        arradapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cgSpinner.setAdapter(arradapter);
        keyword_view = (EditText) getActivity().findViewById(R.id.kw);
        distance_view = (EditText) getActivity().findViewById(R.id.distance);
        loc_input_view = (EditText) getActivity().findViewById(R.id.input_loc);
        loc_radios = (RadioGroup) getActivity().findViewById(R.id.loc_radios);
        current_btn = (RadioButton) getActivity().findViewById(R.id.current_btn);
        current_btn.setChecked(true);
        searchPlace = (AutoCompleteTextView) getActivity().findViewById(R.id.input_loc);
        searchPlace.setEnabled(false);
        kw_warning = (TextView) getActivity().findViewById(R.id.kw_warning);
        loc_warning = (TextView) getActivity().findViewById(R.id.loc_warning);
        kw_warning.setVisibility(View.GONE);
        loc_warning.setVisibility(View.GONE);
        searchbtn = (Button) getActivity().findViewById(R.id.searchbtn);
        clearbtn = (Button) getActivity().findViewById(R.id.clearbtn);
        html_req_queue = Volley.newRequestQueue(getActivity().getApplicationContext());

        loc_radios.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.current_btn:
                        searchPlace.setEnabled(false);
                        break;
                    case R.id.other_btn:
                        searchPlace.setEnabled(true);
                        break;
                }
            }
        });
        PlaceAutoCompleteAdapter autocompleteadapter = new PlaceAutoCompleteAdapter(getActivity().getApplicationContext());
        searchPlace.setAdapter(autocompleteadapter);
        searchbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kw_warning.setVisibility(View.GONE);
                loc_warning.setVisibility(View.GONE);
                if(keyword_view.getText().toString().trim().matches("") ||
                        (loc_radios.getCheckedRadioButtonId() == R.id.other_btn
                                && loc_input_view.getText().toString().trim().matches(""))) {
                    Toast.makeText(getActivity().getApplicationContext(), R.string.validation_err, Toast.LENGTH_SHORT).show();
                    if (keyword_view.getText().toString().trim().matches("")) {
                        kw_warning.setVisibility(View.VISIBLE);
                    }
                    if (loc_radios.getCheckedRadioButtonId() == R.id.other_btn
                            && loc_input_view.getText().toString().trim().matches("")) {
                        loc_warning.setVisibility(View.VISIBLE);
                    }
                }
                else {
                    initSearch();
                }
            }
        });
        clearbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kw_warning.setVisibility(View.GONE);
                loc_warning.setVisibility(View.GONE);
                keyword_view.setText("");
                distance_view.setText("");
                current_btn.setChecked(true);
                loc_input_view.setText("");
                cgSpinner.setSelection(0);
            }
        });
    }

    private void initSearch() {
        keyword = keyword_view.getText().toString();
        category = cgSpinner.getSelectedItem().toString().replace(" ", "+").toLowerCase();
        distance_in_mile = distance_view.getText().toString().matches("")? "10" : distance_view.getText().toString();
        distance_in_meter = Double.parseDouble(distance_in_mile) * 1609.34;
        if(loc_radios.getCheckedRadioButtonId() == R.id.other_btn) {
            geoCoding();
        }
        else {
            MainActivity mainactivity = (MainActivity) getActivity();
            usr_loc = mainactivity.getUsrloc();
            if(usr_loc == null) {
                Toast.makeText(getActivity().getApplicationContext(), R.string.location_fail, Toast.LENGTH_SHORT);
            }
            else{
                Log.i("location", usr_loc);
                new NearbySearchTask().execute(keyword, category, distance_in_meter.toString(), usr_loc);
            }
        }
    }

    private void geoCoding() {
        String loc_input = loc_input_view.getText().toString().replace(" ", "+");
        final String geocode_url = "https://place-search-android.appspot.com/geocode?addr=" + loc_input;
        JsonObjectRequest geocodeRequest = new JsonObjectRequest
                (Request.Method.GET, geocode_url, null, new Response.Listener<JSONObject>() {
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getString("status").equals("OK")) {
                                geocode_result = response.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getString("lat") +
                                        "," + response.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getString("lng");
                                new NearbySearchTask().execute(keyword, category, distance_in_meter.toString(), geocode_result);
                            } else {
                                Toast.makeText(getActivity().getApplicationContext(), R.string.geocode_fail, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getActivity().getApplicationContext(), "Connection Error: Please check your Internet.", Toast.LENGTH_LONG).show();
                    }
                });
        html_req_queue.add(geocodeRequest);
    }



    private class NearbySearchTask extends AsyncTask<String, Void, Void> {
        private ProgressDialog dialog;
        private Intent intent;

        public NearbySearchTask() {
            dialog = new ProgressDialog(getActivity());
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage(getString(R.string.fetching_results));
            dialog.show();
        }

        @Override
        protected Void doInBackground(String ... params) {
            intent = new Intent(getActivity().getApplicationContext(), SearchResults.class);
            params[1] = params[1].equals("default") ? "" : params[1];
            String nearby_url = "https://place-search-android.appspot.com/nearbysearch?" + "keyword=" + params[0] + "&type=" + params[1] + "&radius=" + params[2] + "&location=" + params[3];
            JsonObjectRequest nearbyRequest = new JsonObjectRequest
                    (Request.Method.GET, nearby_url, null, new Response.Listener<JSONObject>() {
                        public void onResponse(JSONObject response) {
                            intent.putExtra("result_string", response.toString());
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
                            Toast.makeText(getActivity().getApplicationContext(), "Connection Error: Please check your Internet.", Toast.LENGTH_LONG).show();
                        }
                    });
            html_req_queue.add(nearbyRequest);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            return;
        }
    }
}
