package krishna.example.com.snoreee;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static int SAMPLE_RATE = 44100; //The Sampling Rate
    boolean mShouldContinue = false; // Indicates if recording / playback should stop
    private String LOG_TAG = "FFS";
    private Button start_Button;
    private FFT fft;
    private TextView snoreStatus;
    private int snoretime =0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fft = new FFT(1024);
        snoreStatus = (TextView)findViewById(R.id.snore_status);
        start_Button = (Button)findViewById(R.id.start_button);
        start_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mShouldContinue = !mShouldContinue;
                if(mShouldContinue){
                    recordAudio();
                }
            }
        });


    }

    void recordAudio() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                // buffer size in bytes
                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);

                if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    bufferSize = SAMPLE_RATE * 2;
                }

                short[] audioBuffer = new short[bufferSize / 2];

                AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);

                if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(LOG_TAG, "Audio Record can't initialize!");
                    return;
                }
                record.startRecording();

                Log.v(LOG_TAG, "Start recording");

                long shortsRead = 0;
                while (mShouldContinue) {
                    int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
                    shortsRead += numberOfShort;

                    // Do something with the audioBuffer
                    double[] y = new double[1024];
                    double[] x = shortToDouble(audioBuffer);
                    fft.fft(x,y);
                    int j = calulatePES(x);
                    if(j ==1){
                        snoretime++;
                        if(snoretime>5) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    snoreStatus.setText("Please stop snoring!!!");
                                }
                            });
                            snoretime =0;
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }else {
                        snoretime = 0;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                snoreStatus.setText("Thanks for not snoring");
                            }
                        });

                    }
                }

                record.stop();
                record.release();
                Log.v(LOG_TAG, String.format("Recording stopped. Samples read: %d", shortsRead));
            }
        }).start();
    }

    public double[] shortToDouble(short[] x){
        double y[] =  new double[x.length];
        for(int  i=0;i<x.length;i++){
            y[i] = (double)x[i];
        }
        return  y;
    }

    public int calulatePES(double[] y){
        double el =0,eh =0;
        for(int i=0;i<52;i++){
            el = el + y[i]*y[i];
        }
        for(int i=53;i<512;i++){
            eh = eh + y[i]*y[i];
        }
        double pes = el/(el+eh);
        //System.out.println(pes);
        if(pes <0.65)
            return  1;
        else
            return 0;
    }
}
