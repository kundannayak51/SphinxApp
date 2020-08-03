package com.example.sphinxapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.animation.Animator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class QuestionsActivity extends AppCompatActivity {

    public static final String FILE_NAME = "SPHINXAPP";
    public static final String KEY_NAME = "QUESTION";

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();

    private TextView question,noIndicator;
    private FloatingActionButton bookmarkBtn;
    private LinearLayout optionsContainer;
    private Button shareBtn,nextBtn;
    private int count = 0;
    private List<QuestionModel> questionModelList;
    private  int position = 0;
    private int score = 0;
    private String setId;
    private Dialog loadingDialog;
    private InterstitialAd mInterstitialAd;

    private List<QuestionModel> bookmarksList;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private Gson gson;
    private int matchedQuestionPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        Toolbar toolbar = findViewById(R.id.toolbar_sets);
        setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        loadAds();

        question = findViewById(R.id.question);
        noIndicator = findViewById(R.id.no_indicator);
        bookmarkBtn = findViewById(R.id.bookmark_btn);
        optionsContainer = findViewById(R.id.options_container);
        shareBtn = findViewById(R.id.share_btn);
        nextBtn = findViewById(R.id.next_btn);

        preferences = getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
        gson = new Gson();

        getBookmarks();

        bookmarkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(modelMatch()){
                    //if question already exist in bookmark list,then remove
                    bookmarksList.remove(matchedQuestionPosition);
                    bookmarkBtn.setImageDrawable(getDrawable(R.drawable.bookmark_border));

                }else {
                    bookmarksList.add(questionModelList.get(position));
                    bookmarkBtn.setImageDrawable(getDrawable(R.drawable.bookmark));
                }
            }
        });

        //getting category and setNo. through intent
        //category = getIntent().getStringExtra("category");
        setId = getIntent().getStringExtra("setId");

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_corners));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);

        questionModelList = new ArrayList<>();

        loadingDialog.show();
        //Database query
        myRef
                .child("SETS").child(setId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()){

                    String id = dataSnapshot1.getKey();
                    String question = dataSnapshot1.child("question").getValue().toString();
                    String a = dataSnapshot1.child("optionA").getValue().toString();
                    String b = dataSnapshot1.child("optionB").getValue().toString();
                    String c = dataSnapshot1.child("optionC").getValue().toString();
                    String d = dataSnapshot1.child("optionD").getValue().toString();
                    String correctAns = dataSnapshot1.child("correctAns").getValue().toString();
                    //int set = dataSnapshot1.child("setNo").getValue();

                    questionModelList.add(new QuestionModel(id,question,a,b,c,d,correctAns,setId));
                }
                if(questionModelList.size() > 0){

                    for(int i=0;i<4;i++){
                        optionsContainer.getChildAt(i).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                checkAnswer((Button)v);
                            }
                        });
                    }
                    //setting first question by default
                    playAnim(question,0,questionModelList.get(position).getQuestion());

                    //Animation on clicking next Btn
                    nextBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            nextBtn.setEnabled(false);
                            nextBtn.setAlpha(0.7f);
                            enableOption(true);
                            position++;
                            if(position == questionModelList.size()){
                                if(mInterstitialAd.isLoaded()){
                                    mInterstitialAd.show();
                                    return;
                                }
                                //go to score Activity
                                Intent scoreIntent = new Intent(QuestionsActivity.this,ScoreActivity.class);
                                scoreIntent.putExtra("score",score);
                                scoreIntent.putExtra("total",questionModelList.size());
                                startActivity(scoreIntent);
                                finish();
                                return;
                            }
                            count = 0;
                            playAnim(question,0,questionModelList.get(position).getQuestion());
                        }
                    });

                    //share button
                    shareBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String body = questionModelList.get(position).getQuestion() + "\n" + "\n" +
                                    questionModelList.get(position).getA() + "\n" +
                                    questionModelList.get(position).getB() + "\n" +
                                    questionModelList.get(position).getC() + "\n" +
                                    questionModelList.get(position).getD();

                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("text/plain");
                            shareIntent.putExtra(Intent.EXTRA_SUBJECT,"Sphinx Challenge");
                            shareIntent.putExtra(Intent.EXTRA_TEXT,body);
                            startActivity(Intent.createChooser(shareIntent,"Share via"));
                        }
                    });
                }else{
                    finish();
                    Toast.makeText(QuestionsActivity.this,"No Questions available",Toast.LENGTH_SHORT).show();
                }
                loadingDialog.dismiss();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(QuestionsActivity.this,databaseError.getMessage(),Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        storeBookmarks();
    }

    private void playAnim(final View view, final int value, final String data){
        for(int i=0;i<4;i++){
            optionsContainer.getChildAt(i).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#989898")));
        }

        view.animate().alpha(value).scaleX(value).scaleY(value).setDuration(500).setStartDelay(100)
                .setInterpolator(new DecelerateInterpolator()).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if(value == 0 && count < 4){
                    String option = "";
                    if(count == 0){
                        option = questionModelList.get(position).getA();
                    }else if(count == 1){
                        option = questionModelList.get(position).getB();
                    }else if(count == 2){
                        option = questionModelList.get(position).getC();
                    }else if(count == 3){
                        option = questionModelList.get(position).getD();
                    }

                    playAnim(optionsContainer.getChildAt(count),0,option);
                    count++;
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {

                if(value == 0){
                    try{
                        ((TextView)view).setText(data);
                        //setting number indicator
                        noIndicator.setText((position+1)+"/"+questionModelList.size());

                        //for bookmark
                        if(modelMatch()){

                            bookmarkBtn.setImageDrawable(getDrawable(R.drawable.bookmark));

                        }else {

                            bookmarkBtn.setImageDrawable(getDrawable(R.drawable.bookmark_border));
                        }

                    }catch (ClassCastException ex){
                        ((Button)view).setText(data);
                    }
                    view.setTag(data);
                    playAnim(view,1,data);
                }else{
                    enableOption(true);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }
    private void checkAnswer(Button selectedOption){
        //as user click on any option,rest other options will be disabled
        enableOption(false);
        nextBtn.setEnabled(true);
        nextBtn.setAlpha(1);
        if(selectedOption.getText().toString().equals(questionModelList.get(position).getAnswer())){
            //correct ans
            selectedOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            score++;
        }else{
            //incorrect
            selectedOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ff0000")));
            //for showing correct option to user
            Button correctOption = (Button)optionsContainer.findViewWithTag(questionModelList.get(position).getAnswer());
            correctOption.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        }

    }

    private void enableOption(boolean enable){
        for(int i=0;i<4;i++){
            optionsContainer.getChildAt(i).setEnabled(enable);
            if(enable){
                optionsContainer.getChildAt(i).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#989898")));

            }
        }
    }

    private void getBookmarks(){
        String json = preferences.getString(KEY_NAME,"");

        Type type = new TypeToken<List<QuestionModel>>(){}.getType();

        bookmarksList = gson.fromJson(json,type);

        if(bookmarksList == null){
            bookmarksList = new ArrayList<>();
        }
    }
    private boolean modelMatch(){
        boolean matched = false;
        int i=0;
        for(QuestionModel model : bookmarksList){

            if(model.getQuestion().equals(questionModelList.get(position).getQuestion())
            && model.getAnswer().equals(questionModelList.get(position).getAnswer())
            && model.getSet().equals(questionModelList.get(position).getSet())){
                matched = true;
                matchedQuestionPosition = i;
            }
            i++;
        }
        return matched;
    }

    private void storeBookmarks(){
        String json = gson.toJson(bookmarksList);
        editor.putString(KEY_NAME,json);
        editor.commit();
    }

    private void loadAds() {
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.interstitialAd));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        mInterstitialAd.setAdListener(new AdListener(){
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                mInterstitialAd.loadAd(new AdRequest.Builder().build());

                Intent scoreIntent = new Intent(QuestionsActivity.this,ScoreActivity.class);
                scoreIntent.putExtra("score",score);
                scoreIntent.putExtra("total",questionModelList.size());
                startActivity(scoreIntent);
                finish();
                return;
            }
        });

    }
}
