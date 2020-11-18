package com.gunshippenguin.openflood;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.gson.Gson;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Activity allowing the user to play the actual game.
 */
public class GameActivity extends AppCompatActivity
        implements EndGameDialogFragment.EndGameDialogFragmentListener,
        SeedDialogFragment.SeedDialogFragmentListener {

    private final int UPDATE_SETTINGS = 1;

    private Game game;
    private SharedPreferences sp;
    private SharedPreferences.Editor spEditor;

    private FloodView floodView;
    private TextView stepsTextView;

    private int lastColor;

    private boolean gameFinished;

    // Paints to be used for the board
    private Paint paints[];

    LinearLayout adlayout;
    InterstitialAd mInterstitialAd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);


        AdView mAdView;
         

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mInterstitialAdd();

       


        ScheduledExecutorService scheduledExecutorService= Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                  runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                          if (mInterstitialAd.isLoaded()) {
                              mInterstitialAd.show();
                          }
                          else {
//                              Toast.makeText(GameActivity.this, "Interstitial Not Loaded", Toast.LENGTH_SHORT).show();
                          }

                          mInterstitialAdd();

                      }
                  });
            }
        },1,1, TimeUnit.MINUTES);



        // Initialize the SharedPreferences and SharedPreferences editor
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        spEditor = sp.edit();

        // Get the FloodView
        floodView = (FloodView) findViewById(R.id.floodView);

        // Initialize the paints array and pass it to the FloodView
        initPaints();
        floodView.setPaints(paints);

        ImageView settingsButton = (ImageView) findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
                                              @Override
                                              public void onClick(View v) {
              Intent launchSettingsIntent = new Intent(GameActivity.this, SettingsActivity.class);
              startActivityForResult(launchSettingsIntent, UPDATE_SETTINGS);
          }
      }
        );

        ImageView infoButton = (ImageView) findViewById(R.id.infoButton);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launchSettingsIntent = new Intent(GameActivity.this, InfoActivity.class);
                startActivity(launchSettingsIntent);
            }
        });

        ImageView newGameButton = (ImageView) findViewById(R.id.newGameButton);
        newGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newGame();
            }
        });
        newGameButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                SeedDialogFragment seedDialogFragment = new SeedDialogFragment();
                seedDialogFragment.show(getSupportFragmentManager(), "SeedDialog");
                return true;
            }
        });

        // Get the steps text view
        stepsTextView = (TextView) findViewById(R.id.stepsTextView);

        if (sp.contains("state_saved")) {
            // Restore previous game
            restoreGame();
        } else {
            // Set up a new game
            newGame();
        }



        adlayout=(LinearLayout) findViewById(R.id.adlayout);

        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network

            adlayout.setVisibility(View.VISIBLE);
        }
        else
        {


            adlayout.setVisibility(View.GONE);
        }


    }

    private void mInterstitialAdd() {
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-9149322965349927/1139453282");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Load the next interstitial.
                // mInterstitialAd.loadAd(new AdRequest.Builder().build());
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    //Log.d("TAG", "The interstitial wasn't loaded yet.");
                }
            }

        });
    }


    @Override
    public void onStop() {
        super.onStop();

        spEditor.putBoolean("state_saved", true);
        spEditor.putString("state_board", new Gson().toJson(game.getBoard()));
        spEditor.putInt("state_steps", game.getSteps());
        spEditor.putString("state_seed", game.getSeed());
        spEditor.apply();
    }

    private int getBoardSize(){
        int defaultBoardSize = getResources().getInteger(R.integer.default_board_size);
        if (!sp.contains("board_size")) {
            spEditor.putInt("board_size", defaultBoardSize);
            spEditor.apply();
        }
        return sp.getInt("board_size", defaultBoardSize);
    }

    private int getNumColors(){
        int defaultNumColors = getResources().getInteger(R.integer.default_num_colors);
        if (!sp.contains("num_colors")) {
            spEditor.putInt("num_colors", defaultNumColors);
            spEditor.apply();
        }
        return sp.getInt("num_colors", defaultNumColors);
    }

    private void initPaints() {
        int[] colors;
        if (sp.getBoolean("use_old_colors", false)){
            colors = getResources().getIntArray(R.array.oldBoardColorScheme);
        } else {
            colors = getResources().getIntArray(R.array.boardColorScheme);
        }

        paints = new Paint[colors.length];
        for (int i = 0; i < colors.length; i++) {
            paints[i] = new Paint();
            paints[i].setColor(colors[i]);
        }
        return;
    }

    private void initGame() {
        gameFinished = false;
        lastColor = game.getColor(0, 0);

        layoutColorButtons();

        stepsTextView.setText(game.getSteps() + " / " + game.getMaxSteps());
        floodView.setBoardSize(getBoardSize());
        floodView.drawGame(game);
    }

    private void newGame() {
        game = new Game(getBoardSize(), getNumColors());
        initGame();
    }

    private void newGame(String seed) {
        game = new Game(getBoardSize(), getNumColors(), seed);
        initGame();
    }

    private void restoreGame() {
        int board[][] = new Gson().fromJson(sp.getString("state_board", null), int[][].class);
        int steps = sp.getInt("state_steps", 0);
        String seed = sp.getString("state_seed", null);

        if (board == null || seed == null) {
            newGame();
        } else {
            game = new Game(board, getBoardSize(), getNumColors(), steps, seed);
            initGame();
        }
    }

    private void layoutColorButtons() {
        // Add color buttons
        LinearLayout buttonLayout = (LinearLayout) findViewById(R.id.buttonLayout);
        buttonLayout.removeAllViews();
        int buttonPadding = (int) getResources().getDimension(R.dimen.color_button_padding);
        for (int i = 0; i < getNumColors(); i++) {
            final int localI = i;
            ColorButton newButton = new ColorButton(this);
            newButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.startAnimation(AnimationUtils.loadAnimation(GameActivity.this, R.anim.button_anim));
                    if (localI != lastColor) {
                        doColor(localI);
                    }
                }
            });
            newButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));
            newButton.setPadding(buttonPadding, buttonPadding, buttonPadding, buttonPadding);

            newButton.setColorBlindText(Integer.toString(i + 1));
            newButton.setColor(paints[i].getColor());
            buttonLayout.addView(newButton);
        }
        return;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UPDATE_SETTINGS) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                // Only start a new game if the settings have been changed
                if (extras.getBoolean("gameSettingsChanged")) {
                    newGame();
                }
                if (extras.getBoolean("colorSettingsChanged")) {
                    initPaints();
                    floodView.setPaints(paints);
                    layoutColorButtons();
                }
            }
        }
    }

    private void doColor(int color) {
        if (gameFinished || game.getSteps() >= game.getMaxSteps()) {
            return;
        }

        game.flood(color);
        floodView.drawGame(game);
        lastColor = color;
        stepsTextView.setText(game.getSteps() + " / " + game.getMaxSteps());

        if (game.checkWin() || game.getSteps() == game.getMaxSteps()) {
            gameFinished = true;
            showEndGameDialog();
        }

        return;
    }

    public void onNewGameClick() {
        newGame();
        return;
    }

    public void onReplayClick() {
        newGame(game.getSeed());
    }

    public void onLaunchSeedDialogClick() {
        SeedDialogFragment seedDialogFragment = new SeedDialogFragment();
        seedDialogFragment.show(getSupportFragmentManager(), "SeedDialog");
        return;
    }

    public void onGetSeedClick() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("seed", game.getSeed());
        clipboard.setPrimaryClip(clip);
        Toast toast = Toast.makeText(this, getString(R.string.game_seed_copied), Toast.LENGTH_SHORT);
        toast.show();
        return;
    }

    public void onNewGameFromSeedClick(String seed) {
        newGame(seed);
    }

    private void showEndGameDialog() {
        DialogFragment endGameDialog = new EndGameDialogFragment();
        Bundle args = new Bundle();
        args.putInt("steps", game.getSteps());
        args.putBoolean("game_won", game.checkWin());
        args.putString("seed", game.getSeed());
        endGameDialog.setArguments(args);
        endGameDialog.show(getSupportFragmentManager(), "EndGameDialog");
        return;
    }
}
