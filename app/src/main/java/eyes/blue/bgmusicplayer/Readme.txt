Port music player from https://github.com/szantog82/SimpleMusicPlayer in 2020-05-24
Include:
1. 5 java source code in package eyes.blue.bgmusicplayer
2. 4 layout files prefix with bgmusic_player_
3. a menu file named bgmusic_player_mainactivity_menu.xml
4. a block of String value in value.xml start from "<!-- For BG media player -->"
5. Mp3 file named warner_tautz_off_broadway.mp3 in RAW folder.
6. Setting in build.gradle
	implementation "androidx.media:media:1.1.0"

7. Settings in Color.xml:
	<!-- For bg music player -->
    <color name="DrawerLayoutBackground">#ffeeeeee</color>
    <color name="DrawerLayoutHighlighted">#ffaaaa</color>

8. content in Menifest:
	<activity
            android:name="eyes.blue.bgmusicplayer.MainActivity"
            android:configChanges="orientation|keyboardHidden"
            android:screenOrientation="landscape" />

        <service android:name=".bgmusicplayer.PlaybackService" />
        <service android:name=".bgmusicplayer.NotificationIntentService" />
