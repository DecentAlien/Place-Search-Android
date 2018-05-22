package com.example.yuchengliu.placesearch;

import android.media.Rating;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.JsonReader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class PlaceInfo extends Fragment {
    private JSONObject place;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.place_info, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        TableRow address_row = (TableRow) getActivity().findViewById(R.id.address_row);
        TextView addr_text = (TextView) getActivity().findViewById(R.id.addr_text);
        TableRow phone_row = (TableRow) getActivity().findViewById(R.id.phone_row);
        TextView phone_text = (TextView) getActivity().findViewById(R.id.phone_text);
        TableRow price_row = (TableRow) getActivity().findViewById(R.id.price_row);
        TextView price_text = (TextView) getActivity().findViewById(R.id.price_text);
        TableRow rating_row = (TableRow) getActivity().findViewById(R.id.rating_row);
        RatingBar rating = (RatingBar) getActivity().findViewById(R.id.rating);
        TableRow url_row = (TableRow) getActivity().findViewById(R.id.url_row);
        TextView url_text = (TextView) getActivity().findViewById(R.id.url_text);
        TableRow website_row = (TableRow) getActivity().findViewById(R.id.website_row);
        TextView website_text = (TextView) getActivity().findViewById(R.id.website_text);

        super.onActivityCreated(savedInstanceState);
        PlaceDetail placedetail = (PlaceDetail) getActivity();
        place = placedetail.getPlace_detail_json();
        try {
            if(place.has("formatted_address")) {
                addr_text.setText(place.getString("formatted_address"));
            }
            else {
                address_row.setVisibility(View.GONE);
            }

            if(place.has("formatted_phone_number")) {
                phone_text.setText(place.getString("formatted_phone_number"));
            }
            else {
                phone_row.setVisibility(View.GONE);
            }

            if(place.has("price_level")) {
                String price_level = "";
                for(int i = 0; i < Integer.parseInt(place.getString("price_level")); i++) {
                    price_level += "$";
                }
                price_text.setText(price_level);
            }
            else {
                price_row.setVisibility(View.GONE);
            }

            if(place.has("rating")) {
                rating.setRating(Float.parseFloat(place.getString("rating")));
            }
            else {
                rating_row.setVisibility(View.GONE);
            }

            if(place.has("url")) {
                url_text.setText(place.getString("url"));
            }
            else {
                url_row.setVisibility(View.GONE);
            }

            if(place.has("website")) {
                website_text.setText(place.getString("website"));
            }
            else {
                website_row.setVisibility(View.GONE);
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
