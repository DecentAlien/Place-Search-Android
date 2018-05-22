package com.example.yuchengliu.placesearch;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.Spinner;
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
import com.squareup.picasso.Transformation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlaceReview extends Fragment {
    private JSONObject place, place_default_order, yelp_reviews_json, yelp_reviews_json_default_order;
    private TextView no_reviews;
    private Spinner reviews_switcher, order_switcher;
    private ScrollView review_scroll_table;
    private TableLayout review_table;
    private RequestQueue req_queue;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.place_review, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        no_reviews = (TextView) getActivity().findViewById(R.id.no_reviews);
        reviews_switcher = (Spinner) getActivity().findViewById(R.id.review_seitch);
        reviews_switcher.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch(position) {
                    case 0:
                        createReviewsTable();
                        break;

                    case 1:
                        createReviewsTable();
                        break;

                    default:
                        return;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                return;
            }
        });
        order_switcher = (Spinner) getActivity().findViewById(R.id.order_switch);
        order_switcher.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0) {
                    try {
                        if(place_default_order != null && yelp_reviews_json_default_order != null) {
                            place = new JSONObject(place_default_order.toString());
                            yelp_reviews_json = new JSONObject(yelp_reviews_json_default_order.toString());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    createReviewsTable();
                }
                else {
                    sortGoogleReviews();
                    sortYelpReviews();
                    createReviewsTable();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                return;
            }
        });
        review_scroll_table = (ScrollView) getActivity().findViewById(R.id.scroll_review_table);
        review_table = (TableLayout) getActivity().findViewById(R.id.reviews_table);
        PlaceDetail placedetail = (PlaceDetail) getActivity();
        place = placedetail.getPlace_detail_json();
        try {
            place_default_order = new JSONObject(place.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        req_queue = Volley.newRequestQueue(getActivity().getApplicationContext());
        getYelpReviews();
        createReviewsTable();
    }




    private void getYelpReviews() {
        String place_name = "", address = "", city = "", state = "", country = "";
        try {
            place_name = place.getString("name");
            JSONArray address_component = place.getJSONArray("address_components");
            for(int i = 0; i < address_component.length(); i++) {
                JSONArray types = address_component.getJSONObject(i).getJSONArray("types");
                if(types.getString(0).equals("route")) {
                    address = address_component.getJSONObject(i).getString("long_name").replace(" ", "+");
                }
                if(types.getString(0).equals("locality")) {
                    city = address_component.getJSONObject(i).getString("long_name").replace(" ", "+");
                }
                if(types.getString(0).equals("administrative_area_level_1")) {
                    state = address_component.getJSONObject(i).getString("short_name").replace(" ", "+");
                }
                if(types.getString(0).equals("country")) {
                    country = address_component.getJSONObject(i).getString("short_name").replace(" ", "+");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String yelp_match_url = "https://place-search-android.appspot.com/yelpmatch?name=" + place_name + "&city=" + city + "&state=" + state + "&country=" + country + "&address=" + address;
        JsonObjectRequest yelp_business_Request = new JsonObjectRequest
                (Request.Method.GET, yelp_match_url, null, new Response.Listener<JSONObject>() {
                    public void onResponse(JSONObject response) {
                        try {
                            if(response.has("businesses") && response.getJSONArray("businesses").length() > 0) {
                                String id = response.getJSONArray("businesses").getJSONObject(0).getString("id");
                                new YelpReviewsRequest().execute(id);
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
        req_queue.add(yelp_business_Request);
    }

    private void createReviewsTable() {
        review_table.removeAllViews();
        int position = reviews_switcher.getSelectedItemPosition();
        switch (position) {
            case 0 :
                if(place.has("reviews")) {
                    no_reviews.setVisibility(View.GONE);
                    try {
                        JSONArray reviews = place.getJSONArray("reviews");
                        for(int i = 0; i < reviews.length(); i++) {
                            final JSONObject review = reviews.getJSONObject(i);
                            TableRow tr = new TableRow(getActivity().getApplicationContext());
                            tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                            tr.setPadding(0, convertDimen(15), 0, convertDimen(15));


                            ImageView avatar = new ImageView(getActivity().getApplicationContext());
                            TableRow.LayoutParams avatarParams =  new TableRow.LayoutParams(convertDimen(50), convertDimen(50));
                            avatarParams.setMargins(convertDimen(5), 0, convertDimen(10), 0);
                            avatar.setLayoutParams(avatarParams);
                            if(review.has("profile_photo_url")) {
                                Picasso.get().load(review.getString("profile_photo_url")).resize(convertDimen(50), convertDimen(50)).centerCrop().into(avatar);
                            }
                            else {
                                Picasso.get().load(Uri.parse(getString(R.string.google_default))).resize(convertDimen(50), convertDimen(50)).centerCrop().transform(new CircleTransform()).into(avatar);
                            }

                            avatar.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        String review_url = review.getString("author_url");
                                        Uri uri_url = Uri.parse(review_url);
                                        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri_url);
                                        startActivity(launchBrowser);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                            tr.addView(avatar);

                            LinearLayout linear = new LinearLayout(getActivity().getApplicationContext());
                            linear.setLayoutParams(new TableRow.LayoutParams(convertDimen(320), TableRow.LayoutParams.WRAP_CONTENT));
                            linear.setOrientation(LinearLayout.VERTICAL);

                            linear.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        String review_url = review.getString("author_url");
                                        Uri uri_url = Uri.parse(review_url);
                                        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri_url);
                                        startActivity(launchBrowser);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            
                            TextView author = new TextView(getActivity().getApplicationContext());
                            author.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            author.setText(review.getString("author_name"));
                            author.setTextColor(getResources().getColor(R.color.colorPrimary));
                            linear.addView(author);

                            RatingBar rating = new RatingBar(getActivity().getApplicationContext(), null, android.R.attr.ratingBarStyleSmall);
                            rating.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            rating.setIsIndicator(true);
                            rating.setNumStars(5);
                            rating.setStepSize(1);
                            rating.setRating(Float.parseFloat(review.getString("rating")));
                            linear.addView(rating);
                            
                            TextView review_time = new TextView(getActivity().getApplicationContext());
                            review_time.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            Date date = new Date(Long.valueOf(review.getString("time"))*1000);
                            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(date);
                            review_time.setText(time);
                            linear.addView(review_time);
                            
                            TextView review_text = new TextView(getActivity().getApplicationContext());
                            review_text.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            review_text.setText(review.getString("text"));
                            linear.addView(review_text);
                            
                            tr.addView(linear);
                            review_table.addView(tr);

                        }   
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    review_scroll_table.setVisibility(View.VISIBLE);
                }
                else {
                    review_scroll_table.setVisibility(View.GONE);
                    no_reviews.setVisibility(View.VISIBLE);
                }
                break;
                
            case 1:
                try {
                    if(yelp_reviews_json != null && yelp_reviews_json.has("reviews") && yelp_reviews_json.getJSONArray("reviews").length() > 0) {
                        no_reviews.setVisibility(View.GONE);
                        JSONArray yelp_reviews = yelp_reviews_json.getJSONArray("reviews");
                        for(int i = 0; i < yelp_reviews.length(); i++) {
                            final JSONObject review = yelp_reviews.getJSONObject(i);
                            JSONObject user = review.getJSONObject("user");
                            TableRow tr = new TableRow(getActivity().getApplicationContext());
                            tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                            tr.setPadding(0, convertDimen(15), 0, convertDimen(15));


                            ImageView avatar = new ImageView(getActivity().getApplicationContext());
                            TableRow.LayoutParams avatarParams =  new TableRow.LayoutParams(convertDimen(50), convertDimen(50));
                            avatarParams.setMargins(convertDimen(5), 0, convertDimen(10), 0);
                            avatar.setLayoutParams(avatarParams);
                            if(user.has("image_url")) {
                                Picasso.get().load(user.getString("image_url")).resize(convertDimen(50), convertDimen(50)).centerCrop().transform(new CircleTransform()).into(avatar);
                            }
                            else {
                                Picasso.get().load(Uri.parse(getString(R.string.yelp_default))).resize(convertDimen(50), convertDimen(50)).centerCrop().transform(new CircleTransform()).into(avatar);
                            }

                            avatar.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        String review_url = review.getString("url");
                                        Uri uri_url = Uri.parse(review_url);
                                        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri_url);
                                        startActivity(launchBrowser);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            tr.addView(avatar);

                            LinearLayout linear = new LinearLayout(getActivity().getApplicationContext());
                            linear.setLayoutParams(new TableRow.LayoutParams(convertDimen(320), TableRow.LayoutParams.WRAP_CONTENT));
                            linear.setOrientation(LinearLayout.VERTICAL);

                            linear.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        String review_url = review.getString("url");
                                        Uri uri_url = Uri.parse(review_url);
                                        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri_url);
                                        startActivity(launchBrowser);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                            TextView author = new TextView(getActivity().getApplicationContext());
                            author.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            author.setText(user.getString("name"));
                            author.setTextColor(getResources().getColor(R.color.colorPrimary));
                            linear.addView(author);

                            RatingBar rating = new RatingBar(getActivity().getApplicationContext(), null, android.R.attr.ratingBarStyleSmall);
                            rating.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            rating.setIsIndicator(true);
                            rating.setNumStars(5);
                            rating.setStepSize(1);
                            rating.setRating(Float.parseFloat(review.getString("rating")));
                            linear.addView(rating);

                            TextView review_time = new TextView(getActivity().getApplicationContext());
                            review_time.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            review_time.setText(review.getString("time_created"));
                            linear.addView(review_time);

                            TextView review_text = new TextView(getActivity().getApplicationContext());
                            review_text.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            review_text.setText(review.getString("text"));
                            linear.addView(review_text);
                            tr.addView(linear);
                            review_table.addView(tr);
                        }
                    }
                    else {
                        review_scroll_table.setVisibility(View.GONE);
                        no_reviews.setVisibility(View.VISIBLE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                review_scroll_table.setVisibility(View.VISIBLE);
                break;

            default:
                return;
        }
    }



    private int convertDimen(int dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public class CircleTransform implements Transformation {
        @Override
        public Bitmap transform(Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());

            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;

            Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
            if (squaredBitmap != source) {
                source.recycle();
            }

            Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());

            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            BitmapShader shader = new BitmapShader(squaredBitmap,
                    BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setAntiAlias(true);

            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);

            squaredBitmap.recycle();
            return bitmap;
        }

        @Override
        public String key() {
            return "circle";
        }
    }

    private void sortGoogleReviews() {
        int order = order_switcher.getSelectedItemPosition();
        if(place.has("reviews")) {
            try {
                JSONArray reviews = place.getJSONArray("reviews");
                List reviewList = new ArrayList<JSONObject>(reviews.length());
                for(int i = 0; i < reviews.length(); i++) {
                    JSONObject obj = reviews.getJSONObject(i);
                    reviewList.add(obj);
                }
                switch(order) {
                    case 0:
                        break;

                    case 1:
                        Collections.sort(reviewList, new HighestRatingComparator());
                        break;

                    case 2:
                        Collections.sort(reviewList, new LowestRatingComparator());
                        break;

                    case 3:
                        Collections.sort(reviewList, new MostRecentComparator());
                        break;

                    case 4:
                        Collections.sort(reviewList, new LastRecentComparator());
                        break;

                    default:
                        return;
                }
                JSONArray newArr = new JSONArray();
                for(int i = 0; i < reviews.length(); i++) {
                    newArr.put((JSONObject) reviewList.get(i));
                }
                place.put("reviews", newArr);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            return;
        }
    }

    private void sortYelpReviews() {
        int order = order_switcher.getSelectedItemPosition();
        try {
            if(yelp_reviews_json != null && yelp_reviews_json.has("reviews") && yelp_reviews_json.getJSONArray("reviews").length() > 0) {
                JSONArray reviews = yelp_reviews_json.getJSONArray("reviews");
                List reviewList = new ArrayList<JSONObject>(reviews.length());
                for(int i = 0; i < reviews.length(); i++) {
                    JSONObject obj = reviews.getJSONObject(i);
                    reviewList.add(obj);
                }
                switch(order) {
                    case 0:
                        break;

                    case 1:
                        Collections.sort(reviewList, new HighestRatingComparator());
                        break;

                    case 2:
                        Collections.sort(reviewList, new LowestRatingComparator());
                        break;

                    case 3:
                        Collections.sort(reviewList, new MostRecentComparator());
                        break;

                    case 4:
                        Collections.sort(reviewList, new LastRecentComparator());
                        break;

                    default:
                        return;
                }
                JSONArray newArr = new JSONArray();
                for(int i = 0; i < reviews.length(); i++) {
                    newArr.put((JSONObject) reviewList.get(i));
                }
                yelp_reviews_json.put("reviews", newArr);
            }
            else {
                return;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public class LowestRatingComparator implements Comparator<JSONObject> {
        public int compare(JSONObject review1, JSONObject review2) {
            try {
                return review1.getString("rating").compareTo(review2.getString("rating"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    public class HighestRatingComparator implements Comparator<JSONObject> {
        public int compare(JSONObject review1, JSONObject review2) {
            try {
                return review2.getString("rating").compareTo(review1.getString("rating"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    public class MostRecentComparator implements Comparator<JSONObject> {
        public int compare(JSONObject review1, JSONObject review2) {
            try {
                if(review1.has("time")) {
                    return review2.getString("time").compareTo(review1.getString("time"));
                }
                if(review1.has("time_created")) {
                    return review2.getString("time_created").compareTo(review1.getString("time_created"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    public class LastRecentComparator implements Comparator<JSONObject> {
        public int compare(JSONObject review1, JSONObject review2) {
            try {
                if(review1.has("time")) {
                    return review1.getString("time").compareTo(review2.getString("time"));
                }
                if(review1.has("time_created")) {
                    return review1.getString("time_created").compareTo(review2.getString("time_created"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    private class YelpReviewsRequest extends AsyncTask<String, Void, Void> {
        private String place_id;

        @Override
        protected Void doInBackground(String... param) {
            place_id = param[0];
            String yelp_review_url = "https://place-search-android.appspot.com/yelpreviews?id=" + place_id;
            JsonObjectRequest yelp_review_Request = new JsonObjectRequest
                    (Request.Method.GET, yelp_review_url, null, new Response.Listener<JSONObject>() {
                        public void onResponse(JSONObject response) {
                            yelp_reviews_json = response;
                            try {
                                yelp_reviews_json_default_order = new JSONObject(yelp_reviews_json.toString());
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
            req_queue.add(yelp_review_Request);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            return;
        }
    }
    
}
