package com.example.sphinxapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.GridView;

import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import java.util.List;


public class SetsActivity extends AppCompatActivity {

    private GridView gridView;
    private InterstitialAd mInterstitialAd;
    private List<String> sets;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sets);

        Toolbar toolbar = findViewById(R.id.toolbar_sets);
        setSupportActionBar(toolbar);

        loadAds();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getting value from intent
        getSupportActionBar().setTitle(getIntent().getStringExtra("title"));

        gridView = findViewById(R.id.gridview);
        sets = CategoriesActivity.list.get(getIntent().getIntExtra("position", 0)).getSets();
        //getting sets through intent
        GridAdapter adapter = new GridAdapter(sets,getIntent().getStringExtra("title"),mInterstitialAd);
        gridView.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
    private void loadAds() {
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.interstitialAd));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

    }
}
