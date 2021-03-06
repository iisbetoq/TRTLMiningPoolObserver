package ml.fifty9.poolmonitor;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import ml.fifty9.poolmonitor.model.statsaddress.Charts;
import ml.fifty9.poolmonitor.model.statsaddress.Stats;

/**
 * A simple {@link Fragment} subclass.
 * @author HemantJ, Aditya Gupta
 */
public class DashboardFragment extends Fragment {

    private TextView hashRate, lastShare, paid, balance, submittedHashes, walletText;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    LineChart hashChart;
    List<List<Long>> hashes;
    Charts chart;
    Stats stats;
    private SharedPreferences walletPref;

    public DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_dashboard, container, false);

        mSwipeRefreshLayout = view.findViewById(R.id.swipeDashboardRefreshLayout);
        walletText = view.findViewById(R.id.wallet_address_text);
        hashRate = view.findViewById(R.id.hash_rate);
        lastShare = view.findViewById(R.id.last_share_text);
        paid = view.findViewById(R.id.paid_text);
        balance = view.findViewById(R.id.balance_text);
        submittedHashes = view.findViewById(R.id.hash_submitted_text);
        hashChart = view.findViewById(R.id.hash_rate_chart);

        walletPref = this.getActivity().getSharedPreferences("WALLET_PREFS",0);
        String walletString = walletPref.getString("wallet","");

        if (walletString.isEmpty()) {

        }
        else {
            walletText.setText(walletString);
        }

        walletText.setOnClickListener(view1 -> {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Wallet Address", walletText.getText());
            clipboard.setPrimaryClip(clip);
            Snackbar snackbar = Snackbar
                    .make(view1,"Wallet Address copied to clipboard", Snackbar.LENGTH_SHORT);
            snackbar.show();
        });

        try {
            chart = ((ParentActivity)this.getActivity()).getChart();
            stats = ((ParentActivity)this.getActivity()).getStats();
            hashes = chart.getHashrate();

            displayStats();
            populateChart();
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.d("E", e.getLocalizedMessage());
        }

        // Refresh items
        mSwipeRefreshLayout.setOnRefreshListener(this::refresh);

        return view;
    }

    public void refresh() {
        ((ParentActivity)this.getActivity()).callAPI();
    }

    public String getHashRate() {

        long totalTime = 0;
        long totalHashes = 0;

        for (int i = 0; i < hashes.size(); i++) {
            List<Long> entry = hashes.get(i);
            totalHashes = totalHashes + entry.get(1) * entry.get(2);
            totalTime = totalTime + entry.get(2);
        }

        String hashRate = String.valueOf(totalHashes / totalTime) + " H/sec";

        return hashRate;

    }



    public String convertCoin(String coin) {
        int coins = Integer.parseInt(coin);
        coins = coins/100;
        return String.valueOf(coins) + " TRTL";
    }

    public String getDate(String timeStampString) {

        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        sdf.setTimeZone(tz);
        Long timeLong = Long.parseLong(timeStampString);
        java.util.Date time=new java.util.Date((long)timeLong*1000);
        String localTime = sdf.format(time);

        Date startDate = time;
        Date endDate   = cal.getTime();

        long duration  = endDate.getTime() - startDate.getTime();
        long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long diffInHours = TimeUnit.MILLISECONDS.toHours(duration);

        if (diffInMinutes > 60) {
            localTime = diffInHours + " Hours ago";
        }
        else {
            localTime = diffInMinutes + " Minutes ago";
        }

        return localTime;
    }

    public void displayStats() {

        String bal = stats.getBalance();
        if (null == bal)
            balance.setText("0 TRTL");
        else
            balance.setText(convertCoin(bal));

        String paidBal = stats.getPaid();
        if (null == paidBal)
            paid.setText("0 TRTL");
        else
            paid.setText(convertCoin(paidBal));

        String hashes = stats.getHashes();
        if (null == hashes)
            submittedHashes.setText(R.string.null_value_string);
        else
            submittedHashes.setText(stats.getHashes());

        String lastShareString = stats.getLastShare();
        if (null == lastShareString)
            lastShare.setText(R.string.null_value_string);
        else
            lastShare.setText(getDate(stats.getLastShare()));

        if (null == stats.getHashrate())
            hashRate.setText("0 H/s");
        else
            hashRate.setText(stats.getHashrate()+"/sec");

    }

    public void populateChart() {

        if (null == hashes) {
            Log.d("data","No hash data found");
        }

        else {

            List<Entry> entries = new ArrayList<Entry>();
            long i = 0;
            for (List<Long> hash : hashes) {
                entries.add(new Entry(i, hash.get(1)));
                i = i+1;
            }


            LineDataSet dataSet = new LineDataSet(entries, "");
            dataSet.disableDashedLine();
            dataSet.disableDashedHighlightLine();
            dataSet.setColor(Color.GRAY);
            dataSet.setFillColor(Color.LTGRAY);
            dataSet.setDrawFilled(true);
            dataSet.setFillAlpha(200);
            dataSet.setValueTextColor(Color.BLACK);
            dataSet.setDrawValues(false);
            dataSet.setDrawCircles(true);
            dataSet.setCircleRadius(2);
            dataSet.setCircleColor(Color.BLACK);

            LineData lineData = new LineData(dataSet);
            lineData.setValueTextSize(0);
            lineData.setDrawValues(false);

            Legend legend = hashChart.getLegend();
            legend.setEnabled(false);

            YAxis leftAxis = hashChart.getAxisLeft();
            leftAxis.disableAxisLineDashedLine();
            leftAxis.removeAllLimitLines();
            leftAxis.disableGridDashedLine();

            YAxis rightAxis = hashChart.getAxisRight();
            rightAxis.setDrawLabels(false); // no axis labels
            rightAxis.setDrawAxisLine(false); // no axis line
            rightAxis.setDrawGridLines(false); // no grid lines
            rightAxis.disableAxisLineDashedLine();
            rightAxis.removeAllLimitLines();
            rightAxis.disableGridDashedLine();

            XAxis xAxis = hashChart.getXAxis();
            xAxis.setDrawLabels(false); // no axis labels
            xAxis.setDrawAxisLine(false); // no axis line
            xAxis.setDrawGridLines(false); // no grid lines
            xAxis.disableAxisLineDashedLine();
            xAxis.removeAllLimitLines();
            xAxis.disableGridDashedLine();


            hashChart.setBackgroundColor(Color.WHITE);
            Description description = new Description();
            description.setTextAlign(Paint.Align.LEFT);
            description.setText("Hashes Submitted");
            description.setPosition(0,0);
            hashChart.setDescription(description);
            hashChart.setData(lineData);
            hashChart.invalidate();
            hashChart.fitScreen();
        }
    }



}
