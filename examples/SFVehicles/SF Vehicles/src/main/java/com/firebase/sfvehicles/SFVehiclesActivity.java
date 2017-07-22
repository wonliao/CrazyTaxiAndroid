package com.firebase.sfvehicles;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.sfvehicles.model.CarInfo;
import com.firebase.sfvehicles.model.CarPos;
import com.github.stuxuhai.jpinyin.ChineseHelper;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.studio.rai.live2d2.Live2DRender;
import com.studio.rai.live2d2.live2d.L2DModelSetting;
import com.studio.rai.live2d2.live2d.MyL2DModel;
import com.tsy.sdk.myokhttp.MyOkHttp;
import com.tsy.sdk.myokhttp.response.JsonResponseHandler;
import com.tsy.sdk.myokhttp.response.RawResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jp.live2d.Live2D;

public class SFVehiclesActivity extends FragmentActivity {

    private final String TAG = SFVehiclesActivity.class.getSimpleName();

    private static final GeoLocation INITIAL_CENTER = new GeoLocation(25.072844, 121.5210583);
    private static final int INITIAL_ZOOM_LEVEL = 14;
    private static final String GEO_FIRE_DB = "https://crazytaxi-b3f28.firebaseio.com";
    private static final String GEO_FIRE_REF = GEO_FIRE_DB + "/cars_pos";

    private GoogleMap map;
    //private Circle searchCircle;
    private GeoFire geoFire;
    private GeoQuery geoQuery;

    private Map<String,Marker> markers;

    private List<CarInfo> carInfoList = new ArrayList<>();
    private List<CarPos> carPosList = new ArrayList<>();

    private Live2DRender mLive2DRender;
    private L2DModelSetting mModelSetting;
    private MyL2DModel mModel;
    private GLSurfaceView mGlSurfaceView;
    private TextView status;

    private WebView mWebView;
    Button mCloseBtn;
    private int searchType = 2;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private String mAnswer;
    private MyOkHttp mMyOkhttp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Live2D.init();
        initView();
    }

    private void initView() {

        mMyOkhttp = new MyOkHttp();

        mGlSurfaceView = (GLSurfaceView) findViewById(R.id.main_glSurface);
//        mGlSurfaceView.setZOrderOnTop(true);

        //et = (EditText) findViewById(R.id.main_et);

        setupLive2DModels();
        mGlSurfaceView.setRenderer(mLive2DRender);

        initButton();
    }

    private void setupLive2DModels() {
        try {
            //String modelName = "tsumiki";
            String modelName = "Epsilon_free";
            //String modelName = "izumi_illust";
            //String modelName = "hibiki";
            mModelSetting = new L2DModelSetting(this, modelName);
            mModel = new MyL2DModel(this, mModelSetting);

            mLive2DRender = new Live2DRender();
            mLive2DRender.setModel(mModel);

            startMap();

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
/*
    @Override
    protected void onStop() {
        super.onStop();
        // remove all event listeners to stop updating in the background
        this.geoQuery.removeAllListeners();
        for (Marker marker: this.markers.values()) {
            marker.remove();
        }
        this.markers.clear();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // add an event listener to start updating locations again
        this.geoQuery.addGeoQueryEventListener(this);
    }
*/

    private void questionSubmit(String question) {

        switch (searchType) {
            case 0:
                break;
            case 1:
                callLUIS(question);
                break;
            case 2:
                callTuling123(question);
                break;
        }
    }

    private void initButton() {

        mWebView = (WebView)findViewById(R.id.callCalWebView);

        Button testBtn = (Button)findViewById(R.id.chestBtn);
        if(testBtn != null) {
            testBtn.setOnClickListener(new Button.OnClickListener() {

                @Override
                public void onClick(View v) {
                    test(v);
                }

            });
        }

        Button qnaBtn = (Button)findViewById(R.id.qna_btn);
        if(qnaBtn != null) {
            qnaBtn.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchType = 0;
                    Button button = (Button)findViewById(R.id.questionButton);
                    button.setBackgroundColor(Color.parseColor("#ff00ddff"));
                }
            });
        }

        Button luisBtn = (Button)findViewById(R.id.luis_btn);
        if(luisBtn != null) {
            luisBtn.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchType = 1;
                    Button button = (Button)findViewById(R.id.questionButton);
                    button.setBackgroundColor(Color.parseColor("#ff009688"));
                }
            });
        }

        Button tulingBtn = (Button)findViewById(R.id.tuling_btn);
        if(tulingBtn != null) {
            tulingBtn.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchType = 2;
                    Button button = (Button)findViewById(R.id.questionButton);
                    button.setBackgroundColor(Color.parseColor("#ffff8800"));
                }
            });
        }

        // 送出
        Button button = (Button)findViewById(R.id.questionButton);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                /*
                mWebView.setVisibility(View.INVISIBLE);
                mCloseBtn.setVisibility(View.INVISIBLE);

                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                TextView textview = (TextView)findViewById(R.id.questionText);
                String question = textview.getText().toString();

                questionSubmit(question);
                */

                //private void promptSpeechInput() {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                        getString(R.string.speech_prompt));
                try {
                    startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.speech_not_supported),
                            Toast.LENGTH_SHORT).show();
                }
                //}
            }
        });

        mCloseBtn = (Button)findViewById(R.id.closeBtn);
        if(mCloseBtn != null) {
            mCloseBtn.setOnClickListener(new WebView.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mWebView.setVisibility(View.INVISIBLE);
                    mCloseBtn.setVisibility(View.INVISIBLE);
                }
            });
        }


        TextView ttsBtn = (TextView)findViewById(R.id.tts_tv);
        ttsBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                mModel.lipStop();
            }
        });
    }

    public void test(View view) {
        mAnswer = "我的老天鵝，你這個色狼，九四八七九四狂";
        TextView _v = (TextView)(findViewById(R.id.tts_tv));
        _v.setText(mAnswer);
        //m_syn.SpeakToAudio(mAnswer);
        mModel.lipSynch(mAnswer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int)event.getX();
        int y = (int)event.getY();

        if (event.getAction() == MotionEvent.ACTION_MOVE)
            mModel.onTouch(x, y);

        return false;
    }

    private void callTuling123(String question) {

        String url = "http://www.tuling123.com/openapi/api";

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("key", "96dd6767a03447f48a10fd108d0e7983");
            jsonObject.put("info", question);
            jsonObject.put("loc", "台湾台北市");
            jsonObject.put("lon", "25.0482323");
            jsonObject.put("lat", "121.5371275");
            jsonObject.put("userid", "1");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mMyOkhttp.post()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .jsonParams(jsonObject.toString())
                .tag(this)
                .enqueue(new JsonResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, JSONObject response) {
                        Log.d("won test", "doPost onSuccess JSONObject:" + response);

                        try{

                            mAnswer = response.getString("text");
                            mAnswer = ChineseHelper.convertToTraditionalChinese(mAnswer);
                            Log.e("won test", mAnswer);

                            TextView _v = (TextView)(findViewById(R.id.tts_tv));
                            _v.setText(mAnswer);
                            //m_syn.SpeakToAudio(mAnswer);
                            mModel.lipSynch(mAnswer);
                        }catch(Exception obj){
                            Log.e("won test ==> ", obj.toString());
                        }
                    }

                    @Override
                    public void onSuccess(int statusCode, JSONArray response) {
                        Log.d("won test", "doPost onSuccess JSONArray:" + response);
                    }

                    @Override
                    public void onFailure(int statusCode, String error_msg) {
                        Log.d("won test", "doPost onFailure:" + error_msg);
                    }
                });

    }

    private void callLUIS(String question) {

        String url = "https://crazytaxi.stamplayapp.com/api/webhook/v1/crazytaxi/catch?q="+question;

//        String url = "https://westus.api.cognitive.microsoft.com/luis/v2.0/apps/1a5eff99-4dbd-4b86-8c2c-2c7b314493ca?subscription-key=f9a1366042a3474eaa9c4c3ddd882dd2&timezoneOffset=0&verbose=true&q="+question;

        //Map<String, String> params = new HashMap<>();
        //params.put("question", question);

        mMyOkhttp.get()
                .url(url)
                .tag(this)
                .enqueue(new JsonResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, JSONObject response) {
                        Log.d("won test", "doPost onSuccess JSONObject:" + response);

                        try{
                            JSONObject topScoringIntent = response.getJSONObject("topScoringIntent");
                            String intent = topScoringIntent.getString("intent");

                            Log.e("won test ==> ", "intent(" + intent + ")");
                            if(intent.equals("None")) {

                                String query = response.getString("query");
                                Log.e("won test ==> ", "query(" + query + ")");
                                mAnswer = "很抱歉，我的主人，我不懂您的問題";
                                TextView _v = (TextView)(findViewById(R.id.tts_tv));
                                _v.setText(mAnswer);
                                //m_syn.SpeakToAudio(mAnswer);
                                mModel.lipSynch(mAnswer);
                            } else if(intent.equals("名字")) {

                                mAnswer = "我是五五六八八 AI，先進叫車系統。很高興為您服務，我的主人。";
                                TextView _v = (TextView)(findViewById(R.id.tts_tv));
                                _v.setText(mAnswer);
                                //m_syn.SpeakToAudio(mAnswer);
                                mModel.lipSynch(mAnswer);
                            } else if(intent.equals("找車")) {

                                String carType = "0";
                                String address = "";

                                JSONArray entities = response.getJSONArray("entities");
                                for(int i=0; i<entities.length(); i++) {

                                    JSONObject obj = entities.getJSONObject(i);
                                    String type = obj.getString("type");

                                    // 車型
                                    if (type.equals("車型::計程車")) {
                                        carType = "0";
                                    }
                                    else if (type.equals("車型::舒適型")) {
                                        carType = "1";
                                    }
                                    else if (type.equals("車型::豪華型")) {
                                        carType = "2";
                                    }
                                    else if (type.equals("車型::九人座")) {
                                        carType = "3";
                                    }
                                    else if (type.equals("地點")) {

                                        address = obj.getString("entity");
                                        address = address.replaceAll(" ", "");
                                    }
                                }
                                Log.e("won test", "車型("+carType+") 地點("+address+")");

                                if(carType.equals("0")) {

                                    mAnswer = "非常抱歉，目前系統不支援呼叫計程車，請改呼叫豪華車。";
                                    TextView _v = (TextView)(findViewById(R.id.tts_tv));
                                    _v.setText(mAnswer);
                                    //m_syn.SpeakToAudio(mAnswer);
                                    mModel.lipSynch(mAnswer);
                                } else {
                                    getGoogleMapsAddress(carType, address);
                                }
                            }
                        }catch(Exception obj){
                            Log.e("won test ==> ", obj.toString());
                        }
                    }

                    @Override
                    public void onSuccess(int statusCode, JSONArray response) {
                        Log.d("won test", "doPost onSuccess JSONArray:" + response);
                    }

                    @Override
                    public void onFailure(int statusCode, String error_msg) {
                        Log.d("won test", "doPost onFailure:" + error_msg);
                    }
                });
    }

    // 向 google maps 取得正確地址
    private void getGoogleMapsAddress(final String carType, String address) {

        String url = "http://52.197.124.196/luis/index.php?action=getGoogleAddress&address=" + address;

        mMyOkhttp.get()
                .url(url)
                .tag(this)
                .enqueue(new RawResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, String response) {

                        Log.d("won test", "doGet onSuccess:" + response);

                        String carName = "";
                        switch (carType) {
                            case "1": carName = "舒適型"; break;
                            case "2": carName = "豪華型"; break;
                            case "3": carName = "九人座"; break;
                        }
                        mAnswer = "你即將在" + response + "叫一台" + carName + "。為你派車中，請稍候!" ;
                        TextView _v = (TextView)(findViewById(R.id.tts_tv));
                        _v.setText(mAnswer);
                        //m_syn.SpeakToAudio(mAnswer);
                        mModel.lipSynch(mAnswer);

                        // call 叫車 API
                        String callCarUrl = "https://17-vr-live.wonliao.com/luis/index.php?action=callCar&car_type=" + carType + "&address=" + response;

                        mWebView.getSettings().setJavaScriptEnabled(true);
                        mWebView.setWebChromeClient(new WebChromeClient() {
                            public void onProgressChanged(WebView view, int progress) {
                                Log.e("won test", "test 1");
                            }
                        });
                        mWebView.setWebViewClient(new WebViewClient() {
                            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                                Log.e("won test", "test 2");
                            }
                        });

                        mWebView.loadUrl(callCarUrl);
                        mWebView.setVisibility(View.VISIBLE);
                        mCloseBtn.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onFailure(int statusCode, String error_msg) {
                        Log.d("won test", "doGet onFailure:" + error_msg);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    TextView txtSpeechInput = (TextView)findViewById(R.id.questionText);
                    txtSpeechInput.setText(result.get(0));


                    mWebView.setVisibility(View.INVISIBLE);
                    mCloseBtn.setVisibility(View.INVISIBLE);

                    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                    TextView textview = (TextView)findViewById(R.id.questionText);
                    String question = textview.getText().toString();

                    questionSubmit(question);
                }
                break;
            }

        }
    }

    private void startMap(){
        Intent intent = new Intent();
        intent.setClass(this, MapActivity.class);
        this.startActivity(intent);
    }

}
