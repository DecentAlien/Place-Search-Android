package com.example.yuchengliu.placesearch;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.example.yuchengliu.placesearch.R;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlacePhotoMetadata;
import com.google.android.gms.location.places.PlacePhotoMetadataBuffer;
import com.google.android.gms.location.places.PlacePhotoMetadataResponse;
import com.google.android.gms.location.places.PlacePhotoResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class PlaceImg extends Fragment {
    private JSONObject place;
    private GeoDataClient mGeoDataClient;
    private TextView no_img;
    private ScrollView scroll_img;
    private LinearLayout linear;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.place_img, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        no_img = (TextView) getActivity().findViewById(R.id.no_imgs);
        scroll_img = (ScrollView) getActivity().findViewById(R.id.scroll_img_table);
        linear = (LinearLayout) getActivity().findViewById(R.id.linear);
        PlaceDetail placedetail = (PlaceDetail) getActivity();
        place = placedetail.getPlace_detail_json();
        mGeoDataClient = Places.getGeoDataClient(getActivity());
        getPhotos();
    }

    private void getPhotos() {
        try {
            final String place_id = place.getString("place_id");

            final Task<PlacePhotoMetadataResponse> photoMetadataResponse = mGeoDataClient.getPlacePhotos(place_id);
            photoMetadataResponse.addOnCompleteListener(new OnCompleteListener<PlacePhotoMetadataResponse>() {
                @Override
                public void onComplete(@NonNull Task<PlacePhotoMetadataResponse> task) {
                    PlacePhotoMetadataResponse photos = task.getResult();
                    PlacePhotoMetadataBuffer photoMetadataBuffer = photos.getPhotoMetadata();
                    if(photoMetadataBuffer.getCount() > 0) {
                        for(int i = 0; i < photoMetadataBuffer.getCount(); i++) {
                            PlacePhotoMetadata photoMetadata = photoMetadataBuffer.get(i);
                            Task<PlacePhotoResponse> photoResponse = mGeoDataClient.getPhoto(photoMetadata);
                            photoResponse.addOnCompleteListener(new OnCompleteListener<PlacePhotoResponse>() {
                                @Override
                                public void onComplete(@NonNull Task<PlacePhotoResponse> task) {
                                    PlacePhotoResponse photo = task.getResult();
                                    Bitmap bitmap = photo.getBitmap();

                                    ImageView img = new ImageView(getActivity().getApplicationContext());
                                    TableRow.LayoutParams imgParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                                    imgParams.gravity = Gravity.CENTER;
                                    img.setLayoutParams(imgParams);
                                    img.setPadding(0, convertDimen(10), 0, convertDimen(10));
                                    img.setImageBitmap(bitmap);
                                    img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                    img.setAdjustViewBounds(true);
                                    linear.addView(img);
                                    scroll_img.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }
                    else {
                        no_img.setVisibility(View.VISIBLE);
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private int convertDimen(int dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}
