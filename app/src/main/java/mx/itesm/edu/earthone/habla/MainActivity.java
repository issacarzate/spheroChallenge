package mx.itesm.edu.earthone.habla;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.orbotix.ConvenienceRobot;
import com.orbotix.DualStackDiscoveryAgent;
import com.orbotix.common.DiscoveryException;
import com.orbotix.common.ResponseListener;
import com.orbotix.common.Robot;
import com.orbotix.common.RobotChangedStateListener;
import com.orbotix.common.internal.AsyncMessage;
import com.orbotix.common.internal.DeviceResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener, RobotChangedStateListener, ResponseListener {

    private Button go, stop, back;

    private final int REQUEST_PERMISSION = 42;    //Pedir permiso
    private float ROBOT_SPEED = 6.0f;   //Velocidad de robot

    private int direction;

    private ConvenienceRobot convenienceRobot;

    private TextToSpeech textToSpeech = null;
    private final int CHECK_TTS = 1000;
    private final int CHECK_STT = 1007;


    private TextView textView;
    private EditText editText;
    private Button bRead, bListen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bRead = (Button) findViewById(R.id.btalk);
        bListen = (Button) findViewById(R.id.blisten);
        editText = (EditText) findViewById(R.id.etText);
        textView = (TextView) findViewById(R.id.textView);
        bRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String texto = editText.getText().toString();
                talkToMe(texto, 1);
            }
        });

        bListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                if(intent.resolveActivity(getPackageManager()) != null){
                    startActivityForResult(intent, CHECK_STT);
                }else{
                    Toast.makeText(getApplicationContext(), "You do not have Speech To Text", Toast.LENGTH_LONG).show();
                }
            }
        });

        Intent ttsIntent = new Intent();
        ttsIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(ttsIntent, CHECK_TTS);

        DualStackDiscoveryAgent.getInstance().addRobotStateListener(this);
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            int hasLocationPermission = checkSelfPermission( Manifest.permission.ACCESS_COARSE_LOCATION );
            if( hasLocationPermission != PackageManager.PERMISSION_GRANTED ) {
                Log.e( "Sphero", "Location permission has not already been granted" );
                List<String> permissions = new ArrayList<String>();
                permissions.add( Manifest.permission.ACCESS_COARSE_LOCATION);
                requestPermissions(permissions.toArray(new String[permissions.size()] ), REQUEST_PERMISSION );
            } else {
                Log.d( "Sphero", "Location permission already granted" );
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case CHECK_TTS:
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    textToSpeech = new TextToSpeech(this, this);
                } else {

                    Intent installIntent = new Intent();
                    installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(installIntent);
                }
                break;
            case CHECK_STT:
                if(resultCode == RESULT_OK && data != null){
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    textView.setText(result.get(0));
                    if(result.get(0).equals("go")){
                        if(convenienceRobot == null){
                            Toast.makeText(MainActivity.this, "Conectate a un sphero", Toast.LENGTH_SHORT).show();
                        }else {
                            convenienceRobot.setLed(.5f, .5f, 0f);
                            direction = 180;
                            convenienceRobot.drive(direction, ROBOT_SPEED);
                        }
                    }
                    if(result.get(0).equals("back")){
                        if(convenienceRobot == null){
                            Toast.makeText(MainActivity.this, "Conectate a un sphero", Toast.LENGTH_SHORT).show();
                        }else {
                            convenienceRobot.setLed(.5f, .5f, 0f);
                            direction = 0;
                            convenienceRobot.drive(direction, ROBOT_SPEED);
                        }
                    }
                    if(result.get(0).equals("stop")){
                        if(convenienceRobot == null){
                            Toast.makeText(MainActivity.this, "Conectate a un sphero", Toast.LENGTH_SHORT).show();
                        }else {
                            convenienceRobot.setLed(1f, 0f, 0f);
                            convenienceRobot.stop();
                        }
                    }
                    if(!result.get(0).equals("go") || !result.get(0).equals("back") || !result.get(0).equals("stop")){
                        Toast.makeText(MainActivity.this, "Comando no reconocido", Toast.LENGTH_SHORT).show();
                    }
                }

                break;

        }


    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (textToSpeech != null) {
                int result = textToSpeech.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS language is not supported", Toast.LENGTH_LONG).show();
                } else {
                    talkToMe("TTS is ready", 0);
                }
            }
        } else {
            Toast.makeText(this, "TTS initialization failed",
                    Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    private void talkToMe(String text, int qmode) {
        if (qmode == 1)
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null);
        else
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public void handleResponse(DeviceResponse deviceResponse, Robot robot) {

    }

    @Override
    public void handleStringResponse(String s, Robot robot) {

    }

    @Override
    public void handleAsyncMessage(AsyncMessage asyncMessage, Robot robot) {

    }

    @Override
    public void handleRobotChangedState(Robot robot, RobotChangedStateNotificationType robotChangedStateNotificationType) {
        switch (robotChangedStateNotificationType){
            case Online:
                convenienceRobot = new ConvenienceRobot(robot);
                convenienceRobot.addResponseListener(this);
                convenienceRobot.enableCollisions(true);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch ( requestCode ) {
            case REQUEST_PERMISSION: {
                for( int i = 0; i < permissions.length; i++ ) {
                    if( grantResults[i] == PackageManager.PERMISSION_GRANTED ) {
                        startDiscovery();
                        Log.d( "Permissions", "Permission Granted: " + permissions[i] );
                    } else if( grantResults[i] == PackageManager.PERMISSION_DENIED ) {
                        Log.d( "Permissions", "Permission Denied: " + permissions[i] );
                    }
                }
            }
            break;
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startDiscovery();

    }

    private void startDiscovery() {
        if( !DualStackDiscoveryAgent.getInstance().isDiscovering() ) {
            try {
                DualStackDiscoveryAgent.getInstance().startDiscovery( this );
            } catch (DiscoveryException e) {
                Log.e("Sphero", "DiscoveryException: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onStop() {
        if( DualStackDiscoveryAgent.getInstance().isDiscovering() ) {
            DualStackDiscoveryAgent.getInstance().stopDiscovery();
        }
        if( convenienceRobot != null ) {
            convenienceRobot.disconnect();
            convenienceRobot = null;
        }

        super.onStop();
    }
}
