package com.example.yuchengliu.placesearch;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Step;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlaceMap extends Fragment implements OnMapReadyCallback{
    private AutoCompleteTextView input_loc;
    private Spinner travel_mode;
    private SupportMapFragment map_frag;
    private JSONObject place;
    private String ori_lat, ori_lng;
    private RequestQueue req_queue;
    private GoogleMap mMap;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.place_map, container, false);
        return rootView;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        input_loc = (AutoCompleteTextView) getActivity().findViewById(R.id.input_loc);
        final PlaceAutoCompleteAdapter autocompleteadapter = new PlaceAutoCompleteAdapter(getActivity().getApplicationContext());
        input_loc.setAdapter(autocompleteadapter);
        input_loc.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                geoCoding(parent.getItemAtPosition(position).toString());
            }
        });

        travel_mode = (Spinner) getActivity().findViewById(R.id.travel_model);
        travel_mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(!input_loc.getText().toString().matches("")) {
                    geoCoding(input_loc.getText().toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                return;
            }
        });
        if(mMap == null) {
            map_frag = (SupportMapFragment) this.getChildFragmentManager().findFragmentById(R.id.map);
            map_frag.getMapAsync(this);
        }
        PlaceDetail placedetail = (PlaceDetail) getActivity();
        place = placedetail.getPlace_detail_json();
        req_queue = Volley.newRequestQueue(getActivity().getApplicationContext());
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        double lat = 34.0522, lng = -118.2437;
        String name = "";
        try {
            name = place.getString("name");
            JSONObject place_loc = place.getJSONObject("geometry").getJSONObject("location");
            lat = Double.parseDouble(place_loc.getString("lat"));
            lng = Double.parseDouble(place_loc.getString("lng"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        LatLng location = new LatLng(lat, lng);
        googleMap.addMarker(new MarkerOptions().position(location).title(name));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(location));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
    }

    private void geoCoding(String place) {
        String loc_input = place.replace(" ", "+");
        final String geocode_url = "https://place-search-android.appspot.com/geocode?addr=" + loc_input;
        JsonObjectRequest geocodeRequest = new JsonObjectRequest
                (Request.Method.GET, geocode_url, null, new Response.Listener<JSONObject>() {
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getString("status").equals("OK")) {
                                ori_lat = response.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getString("lat");
                                ori_lng = response.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getString("lng");
                                getMapDirection();
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
                        Toast.makeText(getActivity().getApplicationContext(), "ERROR: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        req_queue.add(geocodeRequest);
    }

    private void getMapDirection() {
        Double place_lat = 34.0522, place_lng = -118.2437;
        if(ori_lat == null || ori_lng == null) {
            return;
        }
        else {
            try {
                place_lat = Double.parseDouble(place.getJSONObject("geometry").getJSONObject("location").getString("lat"));
                place_lng = Double.parseDouble(place.getJSONObject("geometry").getJSONObject("location").getString("lng"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            final LatLng place_geo = new LatLng(place_lat, place_lng);
            final LatLng origin = new LatLng(Double.parseDouble(ori_lat), Double.parseDouble(ori_lng));
            final String mode = travel_mode.getSelectedItem().toString().toLowerCase();
            GoogleDirection.withServerKey(getString(R.string.GOOGLE_GEO_KEY))
                            .from(origin)
                            .to(place_geo)
                            .transportMode(mode)
                            .execute(new DirectionCallback() {
                                @Override
                                public void onDirectionSuccess(Direction direction, String rawBody) {
                                    mMap.clear();
                                    mMap.addMarker(new MarkerOptions().position(origin));
                                    mMap.addMarker(new MarkerOptions().position(place_geo));
                                    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                                    for (Leg leg: direction.getRouteList().get(0).getLegList()) {
                                        ArrayList<LatLng> positionList = leg.getDirectionPoint();
                                        for (LatLng directionPosition : positionList)
                                            boundsBuilder.include(directionPosition);
                                    }
                                    LatLngBounds latLngBounds = boundsBuilder.build();
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 80));
                                    if(mode.equals("transit")) {
                                        List<Step> stepList = direction.getRouteList().get(0).getLegList().get(0).getStepList();
                                        ArrayList<PolylineOptions> polylineOptionList = DirectionConverter.createTransitPolyline(getActivity().getApplicationContext(), stepList, 6, Color.BLUE, 6, Color.RED);
                                        for (PolylineOptions polylineOption : polylineOptionList) {
                                            mMap.addPolyline(polylineOption);
                                        }
                                    }
                                    else {
                                        ArrayList<LatLng> directionPositionList = direction.getRouteList().get(0).getLegList().get(0).getDirectionPoint();
                                        PolylineOptions polylineOptions = DirectionConverter.createPolyline(getActivity().getApplicationContext(), directionPositionList, 6, Color.BLUE);
                                        mMap.addPolyline(polylineOptions);
                                    }

                                }

                                @Override
                                public void onDirectionFailure(Throwable t) {
                                    Log.i("Error path:", "t.getMessage()");
                                }
                            });
        }
    }
}
