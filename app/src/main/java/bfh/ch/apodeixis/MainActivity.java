package bfh.ch.apodeixis;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
//import android.hardware.camera2.*;
import android.location.GpsStatus;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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


public class MainActivity extends ListActivity {

    private MediaPlayer mediaPlayer;
    private final String USER = "apodeixisMQTTUser";
    private final String PASS = "MQTTPass7";
    //private final String BROKER = "tcp://broker.mqttdashboard.com";
    private final String BROKER = "tcp://147.87.117.73";
    private final String PORT = "1883";
    private final String MQTT_BROKER = BROKER + ":" + PORT;
    private final String TOPIC = "LabDem/Server2HW";

    private String device_id = null;
    private Context c;

    private Camera camera;
    private Parameters params;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(USER);
        options.setPassword(PASS.toCharArray());

        if(device_id == null) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            device_id = tm.getDeviceId();
        }

        c = this.getApplicationContext();

        final MqttAndroidClient client = new MqttAndroidClient(MainActivity.this, MQTT_BROKER, device_id);

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
                String deviceName="androidElia";
                //check the hardware type, android device has type 3
                if (!tokens[0].equals("3")){
                    return;
                }
                //check the device name
                if (!tokens[1].equals(deviceName)){
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
        } catch (MqttException e) {
            e.printStackTrace();
        }

        final Button button = (Button) findViewById(R.id.btn_connect);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //read user data

                /*
                Intent datBundle = new Intent();

                EditText etBroker = (EditText) findViewById(R.id.et_broker);
                EditText etPort = (EditText) findViewById(R.id.et_port);

                String broker = "tcp://" + etBroker.getText().toString();
                String port = etPort.getText().toString();

                datBundle.putExtra((String.valueOf(R.id.et_broker)), broker);
                datBundle.putExtra((String.valueOf(R.id.et_port)), port);
                */


                try {
                    client.publish("App/test", "message".getBytes(), 1, false);
                    //send toast
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(c, "Published: " + "message", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (MqttException e) {
                    e.printStackTrace();
                }

            }
        });




    }


    private void turnFlashOnFor(int time) {
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

    private void playSoundWithName(String soundName){
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
