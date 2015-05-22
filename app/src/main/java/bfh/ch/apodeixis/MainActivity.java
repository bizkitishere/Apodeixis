package bfh.ch.apodeixis;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import android.os.Vibrator;
import android.preference.PreferenceManager;

public class MainActivity extends ActionBarActivity {

    //MQTT
    private final String USER = "apodeixisMQTTUser";
    private final String PASS = "MQTTPass7";
    private final String TOPIC = "LabDem/Server2HW";
    //private final String BROKER = "tcp://broker.mqttdashboard.com";
    private String broker;
    private String port;
    private String device_name;
    private String mqtt_broker ;
    private MqttAndroidClient client;

    private String device_id = null;
    private Context c;

    //resources to use when mqtt siganl arrives
    private Camera camera;
    private Parameters params;
    private Handler handler = new Handler();
    private MediaPlayer mediaPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(device_id == null) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            device_id = tm.getDeviceId();
        }

        c = this.getApplicationContext();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startSettingsActivity();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startSettingsActivity() {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    public void connectToMqtt(View v){
        loadPreferences();
        mqtt_broker = broker + ":" + port;
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(USER);
        options.setPassword(PASS.toCharArray());

        client = new MqttAndroidClient(MainActivity.this, mqtt_broker, device_id);

        //callback, for when a message arrives
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                //make toast
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(c, "Lost MQTT connection", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void messageArrived(String s, final MqttMessage mqttMessage) throws Exception {
                //make toast
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(c, "Message arrived: " + mqttMessage.toString(), Toast.LENGTH_SHORT).show();
                    }
                });

                String tokens[] = mqttMessage.toString().split(";");

                //check the hardware type, android device has type 3
                if (!tokens[0].equals("3")){
                    return;
                }
                //check the device name
                if (!tokens[1].equals(device_name)){
                    return;
                }
                switch (tokens[2]){
                    case "sound":
                        playSoundWithName(tokens[3]);
                        break;
                    case "vibrate":
                        Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
                        int vibrationTime = Integer.parseInt(tokens[3]);
                        // Vibrate for vibrationTime milliseconds
                        v.vibrate(vibrationTime);
                        break;
                    case "flashlight":
                        int flashTime = Integer.parseInt(tokens[3]);
                        turnFlashOnFor(flashTime);
                        break;
                    default:
                        Toast.makeText(c, "Action "+tokens[2]+" is not implemented", Toast.LENGTH_SHORT).show();
                        break;

                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                //not used
            }
        });

        //listens to the successfull connection to the broker
        IMqttActionListener listener = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
                //make toast
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(c, "Connected successfully", Toast.LENGTH_SHORT).show();
                    }
                });

                try {
                    client.subscribe(TOPIC, 1, null, null);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, final Throwable throwable) {
                //make toast
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(c, throwable.getCause().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        };
        try {
            client.connect(options, null, listener);
            Button b1= (Button)findViewById(R.id.btn_disconnect);
            b1.setVisibility(View.VISIBLE);
            Button b2= (Button)findViewById(R.id.btn_connect);
            b2.setVisibility(View.INVISIBLE);
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    public void disconnectFromMqtt(View v){
        try {
            client.unsubscribe(TOPIC);
            client.disconnect();
            Button b1= (Button)findViewById(R.id.btn_disconnect);
            b1.setVisibility(View.INVISIBLE);
            Button b2= (Button)findViewById(R.id.btn_connect);
            b2.setVisibility(View.VISIBLE);
            Toast.makeText(c, "disconnected", Toast.LENGTH_LONG).show();
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    private void loadPreferences(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        broker= "tcp://"+prefs.getString("brokerKey", "@string/pref_default_broker");
        port= prefs.getString("portKey", "@string/pref_default_port");
        device_name= prefs.getString("deviceNameKey", "@string/pref_default_deviceName");
    }

    /**
     * turns the flash light on for a given amount of time
     * @param time how long the flash light should be turned on, millicesonds
     */
    protected void turnFlashOnFor(int time) {
        camera = Camera.open();
        params = camera.getParameters();
        params.setFlashMode(Parameters.FLASH_MODE_TORCH);
        camera.setParameters(params);
        camera.startPreview();

        handler.postDelayed(new Runnable() {
            public void run() {
                camera.stopPreview();
                camera.release();
            }
        }, time);//falsh is on for flashtime, after it stop the preview

    }

    /**
     * plays a sound, in /res/raw
     * @param soundName name of the sound to play
     */
    protected void playSoundWithName(String soundName){
        int file=-1;
        switch (soundName){
            case "cough1":
                file=R.raw.cough1;
                break;
            case "cough2":
                file=R.raw.cough2;
                break;
            default:
                Toast.makeText(c, "Sound "+soundName+" is not implemented", Toast.LENGTH_SHORT).show();
                break;
        }

        if(file != -1) {
            mediaPlayer = MediaPlayer.create(MainActivity.this, file);
            mediaPlayer.start();
        }

    }

}
