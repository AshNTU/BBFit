package com.bbfit.hackdemo;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.media.Image;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaPlayer;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.voice.Languages;
import com.segway.robot.sdk.voice.Recognizer;
import com.segway.robot.sdk.voice.Speaker;
import com.segway.robot.sdk.voice.VoiceException;
import com.segway.robot.sdk.voice.audiodata.RawDataListener;
import com.segway.robot.sdk.voice.grammar.GrammarConstraint;
import com.segway.robot.sdk.voice.grammar.Slot;
import com.segway.robot.sdk.voice.recognition.RecognitionListener;
import com.segway.robot.sdk.voice.recognition.RecognitionResult;
import com.segway.robot.sdk.voice.recognition.WakeupListener;
import com.segway.robot.sdk.voice.recognition.WakeupResult;
import com.segway.robot.sdk.voice.tts.TtsListener;

// for locomotion
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.sbv.AngularVelocity;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.locomotion.sbv.LinearVelocity;
import java.util.Timer;
import java.util.TimerTask;

// for tracking
import com.segway.robot.algo.dts.DTSPerson;
import com.segway.robot.algo.dts.PersonTrackingListener;
import com.bbfit.hackdemo.controller.SimpleController;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.head.Head;
import com.segway.robot.sdk.locomotion.sbv.Base;
import com.segway.robot.sdk.vision.DTS;
import com.segway.robot.sdk.vision.Vision;
import android.widget.Toast;
import android.graphics.Rect;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;





public class MainActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    private static final int SHOW_MSG = 0x0001;
    private static final int APPEND = 0x000f;
    private static final int CLEAR = 0x00f0;
    private static final int SMILE = 0x0002;
    private static final int TALK = 0x0003;
    private static final int WINK = 0x0004;
    private static final int HAPPY = 0X0005;
    private static final int PICTURE = 0X006;

    private ServiceBinder.BindStateListener mRecognitionBindStateListener;
    private ServiceBinder.BindStateListener mSpeakerBindStateListener;
    private ServiceBinder.BindStateListener mBaseBindStateListener;
    private ServiceBinder.BindStateListener mVisionBindStateListener;
    private PersonTrackingListener mPersonTrackingListener;


    private boolean isBeamForming = false;
    private boolean bindSpeakerService;
    private boolean bindRecognitionService;
    private AtomicBoolean speakFinish = new AtomicBoolean(false);
    private int speakCounter;

    private int mSpeakerLanguage;
    private int mRecognitionLanguage;
    private ImageView mFaceImageView;
    private Recognizer mRecognizer;
    private Speaker mSpeaker;
    private WakeupListener mWakeupListener;
    private RecognitionListener mRecognitionListener;
    private RawDataListener mRawDataListener;
    private TtsListener mTtsListener;
    private GrammarConstraint mTwoSlotGrammar;
    private GrammarConstraint mThreeSlotGrammar;
    private GrammarConstraint mGreetSlotGrammar;
    private GrammarConstraint mPosSlotGrammar;
//    private GrammarConstraint mCatSlotGrammar;
    private VoiceHandler mHandler = new VoiceHandler(this);

    private AnimationDrawable faceAnimation;

    private boolean gameButtonMode = false;
    private boolean gameEnds = false;
    private boolean musicPlays = false;

    MediaPlayer lineSound;
    MediaPlayer sleepSound;

    // for locomotion
    private Base mBase;
    private ServiceBinder.BindStateListener mBaseServiceBindStateListener;
    private ServiceBinder.BindStateListener mHeadBindStateListener;

    private boolean mBaseIsBind = false;
    private Timer mTimer;
    private static final int ONE_SEC = 1000;
    private int danceCW = 1;
    private ScheduledExecutorService mScheduler;
    private int mFinishStatus =1;

    enum stageOfCondition{
        GREET,
        HOW_R_U,
        SLEEP_WELL,
        CAT,
        POKE_FACE,
        FEEL_PAIN,
        EXERCISE
    };

    // for tracking
    private DTS mDTS;
    private Vision mVision;
    private Head mHead;
    private boolean mHeadBind;

    private boolean mBaseBind;

    enum DtsState{
        STOP,
        DETECTING,
        TRACKING
    }


    boolean mHeadFollow = true;
    boolean mBaseFollow;

    DtsState mDtsState =  DtsState.STOP;
    SimpleController mController;

    stageOfCondition mStageOfCondition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        Log.d("hello123", "hello kic");
        mRecognizer = Recognizer.getInstance();
        mSpeaker = Speaker.getInstance();
        // for tracking
        mVision = Vision.getInstance();
        mHead = Head.getInstance();
        mBase = Base.getInstance();

        initButtons();
        initListeners();
        mFaceImageView.setOnClickListener(this);
        startSequence();
        //startTalk();

        lineSound = MediaPlayer.create(this,R.raw.line_dance);
        sleepSound = MediaPlayer.create(this,R.raw.sleep_music);

        mScheduler = Executors.newSingleThreadScheduledExecutor();



    }

    @Override
    public void onWindowFocusChanged (boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);

        faceAnimation.start();
    }

    /*
    @brief make the robot dance
     */
    public void lineDanceStart(){
        mScheduler.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        mBase.setAngularVelocity(2 * danceCW);
                        danceCW = -1*danceCW;
                    }
                }, 0, 2, TimeUnit.SECONDS);
    }



    public void lineDanceStop(){
        mScheduler.shutdown();
        mBase.setAngularVelocity(0);
    }


    public void playLineMusic(View view) {
        lineSound.start();
    }

    public void stopLineMusic(View view) {
        lineSound.stop();
        // kill the previous instance and create again
        lineSound = MediaPlayer.create(this,R.raw.line_dance);
    }

    public void playSleepMusic(View view) {
        sleepSound.start();
    }

    public void stopSleepMusic(View view) {
        sleepSound.stop();
        // kill the previous instance and create again
        sleepSound = MediaPlayer.create(this,R.raw.sleep_music);
    }



    public static class VoiceHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        private VoiceHandler(MainActivity instance) {
            mActivity = new WeakReference<>(instance);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = mActivity.get();
            if (mainActivity != null) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case SHOW_MSG:
                        mainActivity.showMessage((String) msg.obj, msg.arg1);
                        break;
                }
            }
        }
    }



    @Override
    protected void onRestart() {
        super.onRestart();
    }

    // init UI.
    private void initButtons() {
        mFaceImageView = (ImageView) findViewById(R.id.face_imageview);
        mFaceImageView.setBackgroundResource(R.drawable.default_anim);
        faceAnimation = (AnimationDrawable) mFaceImageView.getBackground();
    }

    // start action sequence
    private void startSequence(){
        //bind the recognition service.
        mRecognizer.bindService(MainActivity.this, mRecognitionBindStateListener);

        //bind the speaker service.
        mSpeaker.bindService(MainActivity.this, mSpeakerBindStateListener);

        // bind base
        mVision.bindService(this, mVisionBindStateListener);
        mHead.bindService(this, mHeadBindStateListener);
        mBase.bindService(this, mBaseBindStateListener);

    }

    private void startTalk(){

        try {
            System.out.println("Recognize called");
            mRecognizer.startRecognition(mWakeupListener, mRecognitionListener);
        } catch (VoiceException e) {
            Log.e(TAG, "Exception: Recognize called", e);
        }

    }

    private void detectPerson(){
        if (mDtsState != DtsState.DETECTING) {
            mDtsState = DtsState.DETECTING;
            Log.d(TAG,"Detecting person...");
            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ignored) {
                    }
                    DTSPerson[] dtsPersons = mDTS.detectPersons(5 * 1000 * 1000);
                    mDtsState = DtsState.STOP;
                    //Rect[] rects = new Rect[dtsPersons.length];
                    //for (int i = 0; i < dtsPersons.length; i++) {
                    //    rects[i] = dtsPersons[i].getDrawingRect();
                    //}
//                    mTextureView.drawRect(rects);
                    Log.d(TAG,"Detect finish, " + dtsPersons.length + " person detected.");
                }
            }.start();
        }
    }


    /*
    @brief track person
     */
    private void trackPerson(){
        System.out.println(mDtsState);
        if(mBaseBind) Log.d(TAG, "base bind is true");
        else Log.d(TAG, "base bind is false");
        if (mDtsState != DtsState.TRACKING) {
            Log.d(TAG,"Tracking...");
            mDTS.startPersonTracking(null, 300 * 1000 * 100, mPersonTrackingListener);
            mDtsState = DtsState.TRACKING;
            Log.d(TAG,"Tracking...");
        } else {
            mDTS.stopPersonTracking();
            mDtsState = DtsState.STOP;
            Log.d(TAG,"Stop Tracking");
        }
    }

    private void baseFollow(){
        if (!mBaseBind) {
//            showToast("Connect to Base First...");
            return;
        }
        if (!mBaseFollow) {
//            showToast("Enable Base Follow");
            mBaseFollow = true;
        } else {
//            showToast("Disable Base Follow");
            mBaseFollow = false;
            mController = null;
            mBase.stop();
        }
    }


    //init listeners.
    private void initListeners() {



        mHeadBindStateListener = new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                mHeadBind = true;
                mHead.setMode(Head.MODE_SMOOTH_TACKING);
                mHead.setWorldPitch(0.3f);
            }

            @Override
            public void onUnbind(String reason) {
                mHeadBind = false;
            }
        };
        // for tracking
        mVisionBindStateListener = new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                mDTS =  mVision.getDTS();
                mDTS.setVideoSource(DTS.VideoSource.CAMERA);
//                Surface surface = new Surface(mTextureView.getPreview().getSurfaceTexture());
//                mDTS.setPreviewDisplay(surface);
                mDTS.start();
            }

            @Override
            public void onUnbind(String reason) {
                mDTS = null;
            }
        };

        mPersonTrackingListener = new PersonTrackingListener() {
            @Override
            public void onPersonTracking(final DTSPerson person) {
                Log.d(TAG, "onPersonTracking: " + person);
                if (person == null) {
                    return;
                }
//            mTextureView.drawRect(person.getDrawingRect());
                if (mHeadFollow) {
                    mHead.setMode(Head.MODE_SMOOTH_TACKING);
                    mHead.setWorldYaw(person.getTheta() / 2);
                    mHead.setWorldPitch(person.getPitch());
                }
                if (mBaseFollow) {
                    SimpleController controller = mController;
                    if (controller == null) {
                        mController = new SimpleController(new SimpleController.StateListener() {
                            @Override
                            public void onFinish() {
                                mController = null;
                            }
                        }, mBase);
                        controller = mController;
                        controller.setTargetRobotPose(person.getX(), person.getY(), person.getTheta());
                        controller.updateandFollow();
                        controller.startProcess();
                    } else {
                        controller.setTargetRobotPose(person.getX(), person.getY(), person.getTheta());
                        //controller.updatePoseAndDistance();
                        controller.updateandFollow();
                    }
                    mFinishStatus = controller.trackStatus;
                }
            }

            @Override
            public void onPersonTrackingError(int errorCode, String message) {
//            Log.d("Person tracking error: code=" + errorCode + " message=" + message);
                mDtsState = DtsState.STOP;
            }
        };


        mBaseBindStateListener = new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                mBaseBind = true;
            }

            @Override
            public void onUnbind(String reason) {
                mBaseBind = false;
            }
        };


        // get Base Instance
        mBase = Base.getInstance();
        // bindService, if not, all Base api will not work.
        mBase.bindService(getApplicationContext(), new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                mBaseIsBind = true;
                mTimer = new Timer();
                mTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        final AngularVelocity av = mBase.getAngularVelocity();
                        final LinearVelocity lv = mBase.getLinearVelocity();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {}
                        });
                    }
                }, 50, 200);
            }

            @Override
            public void onUnbind(String reason) {
                mBaseIsBind = false;
                if (mTimer != null) {
                    mTimer.cancel();
                    mTimer = null;
                }
            }
        });


        mRecognitionBindStateListener = new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                Message connectMsg = mHandler.obtainMessage(SHOW_MSG, CLEAR, 0,
                        getString(R.string.recognition_connected));
                mHandler.sendMessage(connectMsg);
                try {
                    //get recognition language when service bind.
                    mRecognitionLanguage = mRecognizer.getLanguage();
                    initControlGrammar();
                    switch (mRecognitionLanguage) {
                        case Languages.EN_US:
                            addEnglishGrammar();
                            break;
                        case Languages.ZH_CN:
                            addChineseGrammar();
                            break;
                    }
                } catch (VoiceException | RemoteException e) {
                    Log.e(TAG, "Exception: ", e);
                }
                bindRecognitionService = true;
                if (bindSpeakerService) {
                    //both speaker service and recognition service bind, enable function buttons.

                }
            }

            @Override
            public void onUnbind(String s) {
                //speaker service or recognition service unbind, disable function buttons.
                Message connectMsg = mHandler.obtainMessage(SHOW_MSG, CLEAR, 0, getString(R.string.recognition_disconnected));
                mHandler.sendMessage(connectMsg);
            }
        };

        mSpeakerBindStateListener = new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                try {
                    Message connectMsg = mHandler.obtainMessage(SHOW_MSG, CLEAR, 0,
                            getString(R.string.speaker_connected));
                    mHandler.sendMessage(connectMsg);
                    //get speaker service language.
                    mSpeakerLanguage = mSpeaker.getLanguage();
                } catch (VoiceException e) {
                    Log.e(TAG, "Exception: ", e);
                }
                bindSpeakerService = true;
                if (bindRecognitionService) {
                    //both speaker service and recognition service bind, enable function buttons.
                    startTalk();
                }
            }

            @Override
            public void onUnbind(String s) {
                //speaker service or recognition service unbind, disable function buttons.
                Message connectMsg = mHandler.obtainMessage(SHOW_MSG, CLEAR, 0, getString(R.string.speaker_disconnected));
                mHandler.sendMessage(connectMsg);
            }
        };

        mWakeupListener = new WakeupListener() {
            @Override
            public void onStandby() {
                Log.d(TAG, "onStandby");
                Message statusMsg = mHandler.obtainMessage(SHOW_MSG, CLEAR, 0, "wakeup start, you can say \"OK loomo\".");
                mHandler.sendMessage(statusMsg);
                System.out.println("Wake-up called");

            }

            @Override
            public void onWakeupResult(WakeupResult wakeupResult) {

                //show the wakeup result and wakeup angle.
                Log.d(TAG, "wakeup word:" + wakeupResult.getResult() + ", angle: " + wakeupResult.getAngle());
                //Message resultMsg = mHandler.obtainMessage(SHOW_MSG, CLEAR, 0, "wakeup result:" + wakeupResult.getResult() + ", angle:" + wakeupResult.getAngle());
                Message resultMsg = mHandler.obtainMessage(SHOW_MSG, CLEAR, 0, "test");
                mHandler.sendMessage(resultMsg);

                speakCounter = 0;
            }

            @Override
            public void onWakeupError(String s) {
                //show the wakeup error reason.
                Log.d(TAG, "onWakeupError");
                Message errorMsg = mHandler.obtainMessage(SHOW_MSG, CLEAR, 0, "wakeup error: " + s);
                mHandler.sendMessage(errorMsg);
            }
        };

        mRecognitionListener = new RecognitionListener() {
            @Override
            public void onRecognitionStart() {
                if(gameEnds) return;
                Log.d(TAG, "onRecognitionStart");
                Message statusMsg = mHandler.obtainMessage(SHOW_MSG, CLEAR, 0, "recognition start, you can say \"turn left\".");
                mHandler.sendMessage(statusMsg);
            }

            @Override
            public boolean onRecognitionResult(RecognitionResult recognitionResult) {
                //show the recognition result and recognition result confidence.
                Log.d(TAG, "recognition phase: " + recognitionResult.getRecognitionResult() +
                        ", confidence:" + recognitionResult.getConfidence());
                String result = recognitionResult.getRecognitionResult();
                Message resultMsg = mHandler.obtainMessage(SHOW_MSG, CLEAR, 0, "recognition result:" + result + ", confidence:" + recognitionResult.getConfidence());
                mHandler.sendMessage(resultMsg);

//                speakFinish = false;
                speakFinish.set(false);

                if (result.contains("greet") || result.contains("hi")){
                    Log.d(TAG, "greet");
                    try {
                        //do stuff here to start the demo
                        //detectPerson();

                        trackPerson();
                        baseFollow();
                        while(mFinishStatus !=0){};
                        mBaseFollow = false;


                        //

                        Message talkMsg = mHandler.obtainMessage(SHOW_MSG, TALK, 0, "change talk image");
                        mHandler.sendMessage(talkMsg);

                        // next stage is asking for sleeping
                        mStageOfCondition = stageOfCondition.SLEEP_WELL;

                        if (result.contains("grandma")){
                            mSpeaker.speak("hi grandma, did you sleep well last night?", mTtsListener);
                        } else if(result.contains("grandpa")) {
                            mSpeaker.speak("hi grandpa, did you sleep well last night?", mTtsListener);
                        } else{
//                            mSpeaker.speak("hi there, what's up? how was your night?", mTtsListener);
                            mSpeaker.speak("hello, did you sleep well last night?", mTtsListener);
                        }
                    } catch (VoiceException e) {
                        Log.w(TAG, "Exception: ", e);
                    }
                } else if (mStageOfCondition == stageOfCondition.SLEEP_WELL && (result.contains("yes") || result.contains("ya"))){
                    Log.d(TAG, "positive answer");
                    try {
                        //do stuff here to start the demo
                        Message talkMsg = mHandler.obtainMessage(SHOW_MSG, PICTURE, 0, "change talk image");
                        mHandler.sendMessage(talkMsg);

                        // next stage is poke
                        mStageOfCondition = stageOfCondition.CAT;
                        gameButtonMode = true;

                        mSpeaker.speak("that's great, can you tell me what's in this picture?", mTtsListener);
                    } catch (VoiceException e) {
                        Log.w(TAG, "Exception: ", e);
                    }
                } else if (mStageOfCondition == stageOfCondition.CAT && (result.contains("cat") || result.contains("kitten"))){
                    Log.d(TAG, "positive answer");
                    try {
                        //do stuff here to start the demo
                        Message talkMsg = mHandler.obtainMessage(SHOW_MSG, TALK, 0, "change talk image");
                        mHandler.sendMessage(talkMsg);

                        // next stage is poke
                        mStageOfCondition = stageOfCondition.POKE_FACE;
                        gameButtonMode = true;

                        mSpeaker.speak("that's great, poke my cheek please!", mTtsListener);
                    } catch (VoiceException e) {
                        Log.w(TAG, "Exception: ", e);
                    }
                } else {
                    synchronized (speakFinish) {
                        speakFinish.set(true);
                        speakFinish.notify();
                    }
                }

//                while(speakFinish == false){
//
//
//                }

                synchronized (speakFinish) {
                    while (!speakFinish.get()) {
                        try {
                            speakFinish.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }
                }

                return true;
            }

            @Override
            public boolean onRecognitionError(String s) {
                //show the recognition error reason.
                Log.d(TAG, "onRecognitionError: " + s);
                Message errorMsg = mHandler.obtainMessage(SHOW_MSG, TALK, 0, "recognition error: " + s);
                mHandler.sendMessage(errorMsg);

                if(gameEnds){
                    Log.d(TAG, "game Ends, return True");
                    return false;
                }

                if(!gameButtonMode) {
                    speakFinish.set(false);


                    try {
                        //do stuff here to start the demo
                        mSpeaker.speak("Sorry, can you repeat that?", mTtsListener);
                        speakCounter++;
                    } catch (VoiceException e) {
                        Log.w(TAG, "Exception: ", e);
                    }
                }

                synchronized (speakFinish) {
                    while (!speakFinish.get()) {
                        try {
                            speakFinish.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }
                }

                if (speakCounter==3){
                    return false;
                } else {
                    return true; //to wakeup
                }
            }
        };



        mRawDataListener = new RawDataListener() {
            @Override
            public void onRawData(byte[] bytes, int i) {
                createFile(bytes, "raw.pcm");
            }
        };

        mTtsListener = new TtsListener() {
            @Override
            public void onSpeechStarted(String s) {
                //s is speech content, callback this method when speech is starting.
                Log.d(TAG, "onSpeechStarted() called with: s = [" + s + "]");
                Message statusMsg = mHandler.obtainMessage(SHOW_MSG, CLEAR, 0, "speech start");
                mHandler.sendMessage(statusMsg);

            }

            @Override
            public void onSpeechFinished(String s) {
                //s is speech content, callback this method when speech is finish.
                Log.d(TAG, "onSpeechFinished() called with: s = [" + s + "]");
                Message statusMsg = mHandler.obtainMessage(SHOW_MSG, SMILE, 0, "Smile again");
                mHandler.sendMessage(statusMsg);
//                speakFinish = true;
                synchronized (speakFinish) {
                    speakFinish.set(true);
                    speakFinish.notify();
                }
            }

            @Override
            public void onSpeechError(String s, String s1) {
                //s is speech content, callback this method when speech occurs error.
                Log.d(TAG, "onSpeechError() called with: s = [" + s + "], s1 = [" + s1 + "]");
                Message statusMsg = mHandler.obtainMessage(SHOW_MSG, CLEAR, 0, "speech error: " + s1);
                mHandler.sendMessage(statusMsg);
            }
        };
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.face_imageview:
                if (gameButtonMode) {
                    try {
                        Log.d(TAG, "button is PRESSED! ");
                        Message talkMsg = mHandler.obtainMessage(SHOW_MSG, WINK, 0, "change talk image");
                        mHandler.sendMessage(talkMsg);

                        // next stage is poke
                        mStageOfCondition = stageOfCondition.EXERCISE;
                        gameButtonMode = false;
                        mSpeaker.speak("Ouch! That'good. Let's exercise now!", mTtsListener);
                        gameEnds = true;
                        lineSound.start();
                        musicPlays = true;
                        lineDanceStart();
                    } catch (VoiceException e) {
                        Log.w(TAG, "Exception: ", e);
                    }
                }

                else if(musicPlays){
                    lineSound.stop();
                    lineDanceStop();
                    musicPlays = false;
                    try {
                        Log.d(TAG, "stop the music ");
                        Message talkMsg = mHandler.obtainMessage(SHOW_MSG, SMILE, 0, "change talk image");
                        mHandler.sendMessage(talkMsg);

                        // next stage is poke
                        mStageOfCondition = stageOfCondition.GREET;
                        gameButtonMode = false;
                        gameEnds = false;
                        mSpeaker.speak("That's it for today? Good bye!", mTtsListener);
                    } catch (VoiceException e) {
                        Log.w(TAG, "Exception: ", e);
                    }

                }
        }

    }

    private void addEnglishGrammar() throws VoiceException, RemoteException {
        String grammarJson = "{\n" +
                "         \"name\": \"play_media\",\n" +
                "         \"slotList\": [\n" +
                "             {\n" +
                "                 \"name\": \"play_cmd\",\n" +
                "                 \"isOptional\": false,\n" +
                "                 \"word\": [\n" +
                "                     \"play\",\n" +
                "                     \"close\",\n" +
                "                     \"pause\"\n" +
                "                 ]\n" +
                "             },\n" +
                "             {\n" +
                "                 \"name\": \"media\",\n" +
                "                 \"isOptional\": false,\n" +
                "                 \"word\": [\n" +
                "                     \"the music\",\n" +
                "                     \"the video\"\n" +
                "                 ]\n" +
                "             }\n" +
                "         ]\n" +
                "     }";
        mTwoSlotGrammar = mRecognizer.createGrammarConstraint(grammarJson);
        mRecognizer.addGrammarConstraint(mTwoSlotGrammar);
        mRecognizer.addGrammarConstraint(mThreeSlotGrammar);
        mRecognizer.addGrammarConstraint(mGreetSlotGrammar);
        mRecognizer.addGrammarConstraint(mPosSlotGrammar);
//        mRecognizer.addGrammarConstraint(mCatSlotGrammar);
    }

    private void addChineseGrammar() throws VoiceException, RemoteException {
        Slot play = new Slot("play", false, Arrays.asList("播放", "打开", "关闭", "暂停"));
        Slot media = new Slot("media", false, Arrays.asList("音乐", "视频", "电影"));
        List<Slot> slotList = new LinkedList<>();
        slotList.add(play);
        slotList.add(media);
        mTwoSlotGrammar = new GrammarConstraint("play_media", slotList);
        mRecognizer.addGrammarConstraint(mTwoSlotGrammar);
        mRecognizer.addGrammarConstraint(mThreeSlotGrammar);
    }

    // init control grammar, it can't control robot. :)
    private void initControlGrammar() {

        switch (mRecognitionLanguage) {
            case Languages.EN_US:
                Slot moveSlot = new Slot("move");
                Slot toSlot = new Slot("to");
                Slot orientationSlot = new Slot("orientation");
                List<Slot> controlSlotList = new LinkedList<>();
                moveSlot.setOptional(false);
                moveSlot.addWord("turn");
                moveSlot.addWord("move");
                controlSlotList.add(moveSlot);

                toSlot.setOptional(true);
                toSlot.addWord("to the");
                controlSlotList.add(toSlot);

                orientationSlot.setOptional(false);
                orientationSlot.addWord("right");
                orientationSlot.addWord("left");
                controlSlotList.add(orientationSlot);

                mThreeSlotGrammar = new GrammarConstraint("three slots grammar", controlSlotList);

                Slot greetSlot = new Slot("greet");
                Slot opSlot = new Slot("op");
                Slot patientSlot = new Slot("patient");
                List<Slot> greetSlotList = new LinkedList<>();

                greetSlot.setOptional(false);
                greetSlot.addWord("greet");
                greetSlot.addWord("say hi");
                greetSlotList.add(greetSlot);

                opSlot.setOptional(true);
                opSlot.addWord("to");
                greetSlotList.add(opSlot);

                patientSlot.setOptional(true);
                patientSlot.addWord("grandpa");
                patientSlot.addWord("grandma");
                greetSlotList.add(patientSlot);

                mGreetSlotGrammar = new GrammarConstraint("greet slots grammar", greetSlotList);

                Slot yesSlot = new Slot("yes");
                Slot meSlot = new Slot("i did");
                List<Slot> positiveSlotList = new LinkedList<>();

                yesSlot.setOptional(false);
                yesSlot.addWord("yes");
                yesSlot.addWord("ya");
                yesSlot.addWord("cat");
                positiveSlotList.add(yesSlot);

                meSlot.setOptional(true);
                meSlot.addWord("I did");
                meSlot.addWord("I have");
                positiveSlotList.add(meSlot);

                mPosSlotGrammar = new GrammarConstraint("positive answer grammar", positiveSlotList);

//                Slot catSlot = new Slot("cat");
//                Slot theSlot = new Slot("a");
//                List<Slot> catSlotList = new LinkedList<>();
//
//                catSlot.setOptional(false);
//                catSlot.addWord("cat");
//                catSlot.addWord("kitten");
//                catSlotList.add(catSlot);
//
//                theSlot.setOptional(true);
//                theSlot.addWord("a");
//                catSlotList.add(theSlot);
//
//                mCatSlotGrammar = new GrammarConstraint("cat answer grammar", catSlotList);

                break;

            case Languages.ZH_CN:
                Slot helloSlot;
                Slot friendSlot;
                Slot otherSlot;
                List<Slot> sayHelloSlotList = new LinkedList<>();

                helloSlot = new Slot("hello", false, Arrays.asList(
                        "你好",
                        "你们好"));
                friendSlot = new Slot("friend", true, Arrays.asList(
                        "各位",
                        "我的朋友们"
                ));
                otherSlot = new Slot("other", false, Arrays.asList(
                        "我叫赛格威",
                        "很高兴在里见到大家"
                ));
                sayHelloSlotList.add(helloSlot);
                sayHelloSlotList.add(friendSlot);
                sayHelloSlotList.add(otherSlot);
                mThreeSlotGrammar = new GrammarConstraint("three slots grammar", sayHelloSlotList);
                break;
        }
    }

    private void showMessage(String msg, final int pattern) {
        switch (pattern) {
            case CLEAR:
                System.out.println(msg);
                break;
            case APPEND:
                System.out.println(msg);
                break;
            case TALK:
//                mFaceImageView.setImageResource(R.mipmap.talk);
                mFaceImageView.setImageResource(0);
                faceAnimation.stop();
                mFaceImageView.setBackgroundResource(R.drawable.speaking_anim);
                faceAnimation = (AnimationDrawable) mFaceImageView.getBackground();
                faceAnimation.start();
                break;
            case SMILE:
                mFaceImageView.setImageResource(0);
                faceAnimation.stop();
                mFaceImageView.setBackgroundResource(R.drawable.default_anim);
                faceAnimation = (AnimationDrawable) mFaceImageView.getBackground();
                faceAnimation.start();
                break;
            case WINK:
//                mFaceImageView.setImageResource(R.mipmap.wink);
                mFaceImageView.setImageResource(0);
                faceAnimation.stop();
                mFaceImageView.setBackgroundResource(R.drawable.default_anim);
                faceAnimation = (AnimationDrawable) mFaceImageView.getBackground();
                faceAnimation.start();
                break;
            case PICTURE:
                mFaceImageView.setImageResource(R.mipmap.cat);
//                faceAnimation.stop();
//                mFaceImageView.setBackgroundResource(R.mipmap.cat);
                //faceAnimation = (AnimationDrawable) mFaceImageView.getBackground();
                break;
        }
    }

    private void createFile(byte[] buffer, String fileName) {
        RandomAccessFile randomFile = null;
        try {
            randomFile = new RandomAccessFile(FILE_PATH + fileName, "rw");
            long fileLength = randomFile.length();
            randomFile.seek(fileLength);
            randomFile.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (randomFile != null) {
                try {
                    randomFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (null != this.getCurrentFocus()) {
            InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            return mInputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDestroy() {
        if (mRecognizer != null) {
            mRecognizer = null;
        }
        if (mSpeaker != null) {
            mSpeaker = null;
        }
        super.onDestroy();
    }
}
