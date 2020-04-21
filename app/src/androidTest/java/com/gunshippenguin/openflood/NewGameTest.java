package com.gunshippenguin.openflood;


import androidx.test.espresso.ViewInteraction;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class NewGameTest {

    @Rule
    public ActivityTestRule<GameActivity> mActivityTestRule = new ActivityTestRule<>(GameActivity.class);

    @Test
    public void newGameTest() {
        // New game button
        ViewInteraction newGameButton = onView(
                allOf(withId(R.id.newGameButton), isDisplayed()));

        // Generate a new game several times
        newGameButton.perform(click());
        newGameButton.perform(click());
        newGameButton.perform(click());
        newGameButton.perform(click());
        newGameButton.perform(click());

        // Long click new game button for the new game from seed dialog
        newGameButton.perform(longClick());

        // Hit cancel to close the dialog
        ViewInteraction cancelNewGameFromSeedButton = onView(
                allOf(withId(R.id.cancelStartGameFromSeedButton), withText("Cancel"), isDisplayed()));
        cancelNewGameFromSeedButton.perform(click());

        // Reopen the new game from seed dialog
        newGameButton.perform(longClick());

        // Add the seed 'abc' to the EditText
        ViewInteraction seedEditText = onView(
                allOf(withId(R.id.seedEditText), isDisplayed()));
        seedEditText.perform(replaceText("abc"), closeSoftKeyboard());

        // Press start to start a new game from the given seed
        ViewInteraction startNewGameFromSeedButton = onView(
                allOf(withId(R.id.startGameFromSeedButton), withText("Start"), isDisplayed()));
        startNewGameFromSeedButton.perform(click());
    }

}
