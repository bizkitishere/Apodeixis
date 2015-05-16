package bfh.ch.apodeixis;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.media.MediaPlayer;
import android.os.Bundle;
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


public class MainActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String broker = "tcp://broker.mqttdashboard.com";
        String port = "1883";

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);

        final Context c = this.getApplicationContext();

        final MqttAndroidClient client = new MqttAndroidClient(MainActivity.this, broker, "123");

        //callback, for when a message arrives
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {

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

                MediaPlayer mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.cough1);
                mediaPlayer.start();
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

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
                    client.subscribe("App/test", 2, null, null);
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
