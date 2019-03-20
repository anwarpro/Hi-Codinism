package com.teamblackhole.realtimechat.himan;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String EMAIL = "email";
    private LoginButton loginButton;
    private CallbackManager callbackManager;
    private String userId;
    private URL profilePicture;
    private String firstName;
    private String lastName;
    private String email;
    private String birthday;
    private String gender;
    private List<AppCategory> list = new ArrayList();


    UsageStatsManager mUsageStatsManager;
    private AnyChartView anyChartView;

    SharedPreferences sharedPref;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mUsageStatsManager = (UsageStatsManager) getSystemService("usagestats"); //Context.USAGE_STATS_SERVICE

        installedApps();

        anyChartView = findViewById(R.id.any_chart_view);
        anyChartView.setProgressBar(findViewById(R.id.progress_bar));
        anyChartView.setAlwaysDrawnWithCacheEnabled(false);

        sharedPref = getSharedPreferences("com.codinism.hicondinism", Context.MODE_PRIVATE);

//        take decision by max apps used time
//        it take max used three apps categories and take max occurrence
//        category as a interested category

        boolean isUsed = sharedPref.getBoolean("isUsed", true);

        thinkBy(isUsed);

        callbackManager = CallbackManager.Factory.create();

        loginButton = findViewById(R.id.login_button);
        loginButton.setReadPermissions("email", "user_birthday", "user_posts",
                "user_likes", "business_management");

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code

                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {

                        Log.e("Graph", object.toString());
                        Log.e("Graph", response.toString());

                        try {
                            userId = object.getString("id");
                            profilePicture = new URL("https://graph.facebook.com/" + userId + "/picture?width=500&height=500");
                            if (object.has("first_name"))
                                firstName = object.getString("first_name");
                            if (object.has("last_name"))
                                lastName = object.getString("last_name");
                            if (object.has("email"))
                                email = object.getString("email");
                            if (object.has("birthday"))
                                birthday = object.getString("birthday");
                            if (object.has("gender"))
                                gender = object.getString("gender");

                            Intent main = new Intent(MainActivity.this, UserDetailsActivity.class);
                            main.putExtra("name", firstName);
                            main.putExtra("surname", lastName);
                            main.putExtra("imageUrl", profilePicture.toString());
                            startActivity(main);
                            finish();
                        } catch (JSONException | MalformedURLException e) {
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
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
                exception.printStackTrace();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences.Editor editor = sharedPref.edit();
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_used:
                editor.putBoolean("isUsed", true);
                editor.commit();

                finish();
                startActivity(getIntent());

                break;
            // action with ID action_settings was selected
            case R.id.action_installed:
                editor.putBoolean("isUsed", false);
                editor.commit();

                finish();
                startActivity(getIntent());

                break;
            default:
                break;
        }

        return true;
    }

    public static String getAppNameFromPkgName(Context context, String PackageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo info = packageManager.getApplicationInfo(PackageName, PackageManager.GET_META_DATA);
            String appName = (String) packageManager.getApplicationLabel(info);
            return appName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    private void thinkBy(boolean isUsed) {
        if (isUsed) {
            List<UsageStats> usageStatsList =
                    getUsageStatistics(UsageStatsManager.INTERVAL_YEARLY);

            if (usageStatsList.size() == 0) {
                return;
            }
            Collections.sort(usageStatsList, new LastTimeLaunchedComparatorDesc());

            new FetchCategoryTask(installed, true, usageStatsList).execute();

            return;
        }

//        else by installed apps categories
        new FetchCategoryTask(installed, false, null).execute();

    }

    private void chart(String type) {

        Log.i("test", "chart: " + type);

        final Pie pie = AnyChart.pie();
        pie.autoRedraw();

        pie.setOnClickListener(new ListenersInterface.OnClickListener(new String[]{"x", "value"}) {
            @Override
            public void onClick(Event event) {

                StringBuilder apps = new StringBuilder();
                String category = event.getData().get("x");

                for (AppCategory ac : list) {
                    if (ac.getCategories().contains(category)) {
                        apps.append(getAppNameFromPkgName(getApplicationContext(), ac.getPackName()) + ", ");
                    }
                }

                Toast.makeText(MainActivity.this, apps.toString(), Toast.LENGTH_LONG).show();
            }
        });

        List<String> cats = new ArrayList<>();

        for (AppCategory ac : list) {
            cats.addAll(ac.getCategories());
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

        pie.title("Categories imported of " + type + " apps");

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private List<String> installed = new ArrayList<>();


    public void installedApps() {
        List<PackageInfo> packList = getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packList.size(); i++) {
            PackageInfo packInfo = packList.get(i);
            if ((packInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String appName = packInfo.packageName;
                installed.add(appName);
            }
        }
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

    /**
     * The {@link Comparator} to sort a collection of {@link UsageStats} sorted by the timestamp
     * last time the app was used in the descendant order.
     */
    private static class LastTimeLaunchedComparatorDesc implements Comparator<UsageStats> {

        @Override
        public int compare(UsageStats left, UsageStats right) {
            return Long.compare(right.getTotalTimeInForeground(), left.getTotalTimeInForeground());
        }
    }

    /**
     * intervalType argument.
     *
     * @param intervalType The time interval by which the stats are aggregated.
     *                     Corresponding to the value of {@link UsageStatsManager}.
     *                     E.g. {@link UsageStatsManager#INTERVAL_DAILY}, {@link
     *                     UsageStatsManager#INTERVAL_WEEKLY},
     * @return A list of {@link android.app.usage.UsageStats}.
     */
    public List<UsageStats> getUsageStatistics(int intervalType) {
        // Get the app statistics since one year ago from the current time.
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);

        List<UsageStats> queryUsageStats = mUsageStatsManager
                .queryUsageStats(intervalType, cal.getTimeInMillis(),
                        System.currentTimeMillis());

        if (queryUsageStats.size() == 0) {

            //show for open settings
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage(getString(R.string.explanation_access_to_appusage_is_not_enabled));
            alertDialogBuilder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

        }
        return queryUsageStats;
    }

    private class FetchCategoryTask extends AsyncTask<String, Void, String> {

        private final String TAG = FetchCategoryTask.class.getSimpleName();
        private final boolean isUsed;
        private final List<UsageStats> usageStatsLists;
        private List<String> installed;

        public FetchCategoryTask(List<String> installed, boolean isUsed, List<UsageStats> usageStatsLists) {
            this.installed = installed;
            this.isUsed = isUsed;
            this.usageStatsLists = usageStatsLists;
        }

        private AppCategory getCategory(String query_url, String name) {

            try {
                Document doc = Jsoup.connect(query_url).get();
                Elements link = doc.select("a[class=\"hrTbp R8zArc\"]");
                int i = 0;

                AppCategory ap = null;

                for (String s : link.eachText()) {
                    if (i++ == 0) {
                        ap = new AppCategory(s, new ArrayList<String>());
                        ap.setPackName(name);
                    } else {
                        ap.setCategories(s);
                    }
                }
                return ap;
            } catch (Exception e) {
//                Log.e("DOc", e.toString());
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isUsed) {
                        chart("most used");
                    } else {
                        chart("installed");
                    }
                }
            });
        }

        @Override
        protected String doInBackground(String... errors) {

            AppCategory category;

            List<AppCategory> appCategories = new ArrayList<>();

            list = null;

            if (this.isUsed) {

                int i = 0;
                int maxtake = 10;

                if (installed.size() > 10) {
                    maxtake = maxtake + (installed.size() / 2);
                }

                //take most used 10 apps as a countable

                for (UsageStats us : usageStatsLists) {
                    if (installed.contains(us.getPackageName())) {
                        String query_url = "https://play.google.com/store/apps/details?id=" + us.getPackageName();  //GOOGLE_URL + packageInfo.packageName;
                        Log.i(TAG, query_url);
                        category = getCategory(query_url, us.getPackageName());
                        if (category != null) {
                            appCategories.add(category);
                            if (i++ == maxtake) {
                                break;
                            }
                        }
                    }
                }

            } else {
                for (String s : installed) {
                    String query_url = "https://play.google.com/store/apps/details?id=" + s;  //GOOGLE_URL + packageInfo.packageName;
                    Log.i(TAG, query_url);
                    category = getCategory(query_url, s);
                    if (category != null) {
                        appCategories.add(category);
                    }
                }

            }

            list = appCategories;

            return null;
        }

    }
}
