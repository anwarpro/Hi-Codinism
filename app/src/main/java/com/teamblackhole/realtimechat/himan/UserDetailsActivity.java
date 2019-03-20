package com.teamblackhole.realtimechat.himan;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.chart.common.listener.Event;
import com.anychart.chart.common.listener.ListenersInterface;
import com.anychart.charts.Pie;
import com.anychart.enums.Align;
import com.anychart.enums.LegendLayout;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.facebook.share.model.ShareHashtag;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private ShareDialog shareDialog;
    private String name, surname, imageUrl;
    private String TAG = "UserDetailsActivity";
    private List<FacebookPage> likesCategories;
    private AnyChartView anyChartView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        Bundle inBundle = getIntent().getExtras();
        name = inBundle.getString("name");
        surname = inBundle.getString("surname");
        imageUrl = inBundle.getString("imageUrl");

        TextView nameView = findViewById(R.id.nameAndSurname);
        nameView.setText("" + name + " " + surname);

        CircleImageView imageView = findViewById(R.id.profileImage);

        Picasso.get().load(imageUrl).into(imageView);

        anyChartView = findViewById(R.id.any_chart_view);
        anyChartView.setProgressBar(findViewById(R.id.progress_bar));

        getData();

    }

    private void getData() {
        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {

                Log.e("Graph", object.toString());
                Log.e("Graph", response.toString());

                try {

                    if (object.has("likes")) {

                        likesCategories = new ArrayList<>();

                        JSONObject likes = object.getJSONObject("likes");
                        JSONArray data = likes.getJSONArray("data");

                        for (int i = 0; i < data.length(); i++) {

                            JSONObject page = data.getJSONObject(i);

                            FacebookPage fp = new FacebookPage();

                            if (page.has("category")) {
                                fp.setCategory(page.getString("category"));
                            }
                            if (page.has("id")) {
                                fp.setId(page.getString("id"));
                            }
                            if (page.has("name")) {
                                fp.setName(page.getString("name"));
                            }

                            likesCategories.add(fp);
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                chart();
                            }
                        });

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        //Here we put the requested fields to be returned from the JSONObject
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id, first_name, last_name, email, birthday, gender, likes.limit(1000){category,name,about}");
        request.setParameters(parameters);
        request.executeAsync();

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.share:
                share();
                break;

            case R.id.logout:
                logout();
                break;
        }
    }

    private void logout() {
        LoginManager.getInstance().logOut();
        Intent login = new Intent(UserDetailsActivity.this, MainActivity.class);
        startActivity(login);
        finish();
    }

    private void chart() {
        Pie pie = AnyChart.pie();

        pie.setOnClickListener(new ListenersInterface.OnClickListener(new String[]{"x", "value"}) {
            @Override
            public void onClick(Event event) {
                StringBuilder apps = new StringBuilder();
                String category = event.getData().get("x");

                for (FacebookPage fp : likesCategories) {
                    if (fp.getCategory().contains(category)) {
                        apps.append(fp.getName() + ", ");
                    }
                }

                Toast.makeText(UserDetailsActivity.this, apps.toString(), Toast.LENGTH_LONG).show();
            }
        });


        List<String> cats = new ArrayList<>();
        for (FacebookPage fp : likesCategories) {
            cats.add(fp.getCategory());
        }

        HashSet<String> uniqueCategories = new HashSet<>(cats);

        List<DataEntry> data = new ArrayList<>();

        int max = 0;
        String interested = "";

        for (String category : uniqueCategories) {
            int count = Collections.frequency(cats, category);
            if (max < count) {
                max = count;
                interested = category;
            }
            data.add(new ValueDataEntry(category, count));
            Log.i("Category", "category: " + category);
        }

        if (max != 0) {
            showDecision("May be your are mostly interested in \t\t\'" + interested + "\'\n type of things");
        } else {
            showDecision("I can't take any decision for lack of information");
        }

        pie.data(data);

        pie.title("Categories imported of like pages");

        pie.labels().position("outside");

        pie.legend().title().enabled(true);
        pie.legend().title()
                .text("Categories")
                .padding(0d, 0d, 10d, 0d);

        pie.legend()
                .position("center-bottom")
                .itemsLayout(LegendLayout.HORIZONTAL)
                .align(Align.CENTER);

        anyChartView.setChart(pie);

    }

    private void showDecision(String msg) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(msg);
        alertDialogBuilder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        arg0.dismiss();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    private void share() {
        shareDialog = new ShareDialog(this);
        List<String> taggedUserIds = new ArrayList<String>();
        taggedUserIds.add("{USER_ID}");
        taggedUserIds.add("{USER_ID}");
        taggedUserIds.add("{USER_ID}");

        ShareLinkContent content = new ShareLinkContent.Builder()
                .setContentUrl(Uri.parse("http://www.sitepoint.com"))
                .setContentTitle("This is a content title")
                .setContentDescription("This is a description")
                .setShareHashtag(new ShareHashtag.Builder().setHashtag("#sitepoint").build())
                .setPeopleIds(taggedUserIds)
                .setPlaceId("{PLACE_ID}")
                .build();

        shareDialog.show(content);
    }

    private void getPosts() {
        new GraphRequest(
                AccessToken.getCurrentAccessToken(), "/me/posts", null, HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        Log.e(TAG, response.toString());
                    }
                }
        ).executeAsync();
    }
}
