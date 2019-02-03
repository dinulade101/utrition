package com.mchacks.utrition;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    TextView textView;
    File file;
    Uri fileUri;
    final int RC_TAKE_PHOTO = 1;
    String mCurrentPhotoPath;
    private final String USER_AGENT = "Mozilla/5.0";
    private OkHttpClient okHttpClient = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Button btnCamera = (Button)findViewById(R.id.btnCamera);
        imageView = (ImageView)findViewById(R.id.imageView);
        textView = (TextView)findViewById(R.id.textView);

        if(okHttpClient == null)
        {
            okHttpClient = new OkHttpClient();
        }
        //startActivity(new Intent(MainActivity.this, Pop.class));

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("myApp", HttpPost("https://us-central1-utrition.cloudfunctions.net/api/preproc"));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

        final int REQUEST_TAKE_PHOTO = 1;
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if (intent.resolveActivity(getPackageManager()) != null){
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex){

                    }
                    if (photoFile != null){
                        //Uri photoUri = FileProvider.getUriForFile(getApplicationContext(), "com.mchacks.utrition.fileprovider", photoFile);
                        Uri photoUri = Uri.fromFile(photoFile);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                        StrictMode.setVmPolicy(builder.build());
                        startActivityForResult(intent, REQUEST_TAKE_PHOTO);
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        try {
            Log.d("myApp", "called");
            File file = new File(mCurrentPhotoPath);
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), Uri.fromFile(file));
            if (bitmap != null) {
                  bitmap = getRotatedBitmap(bitmap);
//                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
//                    Matrix matrix = new Matrix();
//                    matrix.postRotate(90);
//                    bitmap = Bitmap.createBitmap(bitmap, 0,0,bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//                }
                getTextFromBitmap(bitmap);
                imageView.setImageBitmap(bitmap);
            }
        } catch (Exception error){
            error.printStackTrace();
        }

    }

//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (resultCode == RESULT_OK) {
//            Bitmap photo = (Bitmap) data.getExtras().get("data");
//            getTextFromBitmap(photo);
//
//            // CALL THIS METHOD TO GET THE URI FROM THE BITMAP
//            //Uri tempUri = getImageUri(getApplicationContext(), photo);
//
//            // CALL THIS METHOD TO GET THE ACTUAL PATH
//            //File finalFile = new File(getRealPathFromURI(tempUri));
//
//            System.out.println(mCurrentPhotoPath);
//        }
//    }



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

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileNmae = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalCacheDir();
        File image = File.createTempFile(imageFileNmae, ".jpg", storageDir);

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void getTextFromBitmap(Bitmap bitmap){

//        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
//        if(!textRecognizer.isOperational()){
//            Toast.makeText(getApplicationContext(), "Could not scan ingredients", Toast.LENGTH_SHORT).show();
//        }
//        else{
//            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
//            SparseArray<TextBlock> items = textRecognizer.detect(frame);
//            StringBuilder stringBuilder = new StringBuilder();
//            for (int i =0; i<items.size(); i++){
//                TextBlock item = items.valueAt(i);
//                stringBuilder.append(item.getValue());
//                stringBuilder.append("\n");
//            }
//            textView.setText(stringBuilder.toString());
//        }

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        detector.processImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                Log.e("myApp","success initial");
                                try {
                                    processTextRecognitionResult(firebaseVisionText);
                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("myApp","faled");
                                e.printStackTrace();
                            }
                        }
                );

    }

    public Bitmap getRotatedBitmap(Bitmap bitmap) throws IOException {
        ExifInterface ei = new ExifInterface(mCurrentPhotoPath);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBitmap = null;
        switch(orientation){
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotatedBitmap = rotateImage(bitmap, 90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotatedBitmap = rotateImage(bitmap, 180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotatedBitmap = rotateImage(bitmap, 270);
                break;
            case ExifInterface.ORIENTATION_NORMAL:
            default:
                rotatedBitmap = bitmap;
        }
        return rotatedBitmap;
    }

    public Bitmap rotateImage(Bitmap source, float angle){
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0,0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void processTextRecognitionResult(FirebaseVisionText texts) throws MalformedURLException {
        List<FirebaseVisionText.TextBlock> blocks = texts.getTextBlocks();
        if (blocks.size() == 0){
            Log.e("myApp", "block size = 0");
            return;
        }
        List<String> output = new ArrayList<String>();
        for (int i=0; i< blocks.size(); i++){
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j =0; j < lines.size(); j++){
//                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
//                for (int k =0; k < elements.size(); k++){
//
//                }
                String elem = lines.get(j).getText();
                output.add(elem);
                Log.d("myApp", elem);
            }
        }

        //make REST API call
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sendData(null);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public void sendData(List<String> data) throws IOException, JSONException {
//        String url = "https://us-central1-utrition.cloudfunctions.net/api/preproc";
//        URL obj = new URL(url);
//        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
//
//        con.setRequestMethod("POST");
//        con.setRequestProperty("Accept-Language", "en-US, en;q=0.5");
//        con.setRequestProperty("Content-Type", "application/json");
//
//        String json = "['Ingredients: Sugar']";
//
//        con.setDoOutput(true);
//
//        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
//        wr.writeBytes(json.toString());
//        wr.flush();
//        wr.close();
//
//        int responseCode = con.getResponseCode();
//        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
//        String inputLine;
//        StringBuffer response = new StringBuffer();
//
//        while ((inputLine = in.readLine()) != null){
//            response.append(inputLine);
//        }
//        in.close();
//
//        Log.d("myApp", response.toString());
//        //HttpClient httpClient = new DefaultHttpClient();

//        URL url = new URL("https://postman-echo.com/get?foo1=bar1&foo2=bar2");
//        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//        conn.setRequestMethod("GET");
//        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
//        conn.setRequestProperty("Accept","application/json");
//        conn.setDoOutput(true);
//        conn.setDoInput(true);
//
//        Log.i("JSON", "");
//        OutputStream os = conn.getOutputStream();
//        //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
//        os.write("['INGREDIENTS: TORULA YEAST, CITRIC ACID,', 'LACTIC ACID, WHEAT STARCH. INGREDIENTS']".getBytes("UTF-8"));
//
//        //os.flush();
//        os.close();
//
//        Log.i("STATUS", String.valueOf(conn.getResponseCode()));
//        Log.i("MSG" , conn.getResponseMessage());
//
//        conn.disconnect();
//    } catch (Exception e) {
//        e.printStackTrace();
//    }
//
//        Thread okHttpExecuteThread = new Thread() {
//            @Override
//            public void run() {
//
//                String url = "https://us-central1-utrition.cloudfunctions.net/api/preproc";
//
//                try {
//
//                    // Create okhttp3.Call object with post http request method.
//                    Call call = createHttpPostMethodCall(url);
//
//                    // Execute the request and get the response synchronously.
//                    Response response = call.execute();
//
//                    // If request process success.
//                    boolean respSuccess = response.isSuccessful();
//                    if (respSuccess) {
//
//                        // Parse and get server response text data.
//                        String respData = parseResponseText(response);
//
//                        // Notify activity main thread to update UI display text with Handler.
//                        sendChildThreadMessageToMainThread(respData);
//                    } else {
//                        sendChildThreadMessageToMainThread("Ok http post request failed.");
//                    }
//                } catch(Exception ex)
//                {
//                    Log.e(TAG_OK_HTTP_ACTIVITY, ex.getMessage(), ex);
//                    sendChildThreadMessageToMainThread(ex.getMessage());
//                }
//            }
//        };
//
//        // Start the child thread.
//        okHttpExecuteThread.start();

    }

    private String HttpPost(String myUrl) throws IOException, JSONException {
        String result = "";

        URL url = new URL(myUrl);

        // 1. create HttpURLConnection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        // 2. build JSON object
        JSONObject jsonObject = buidJsonObject();

        // 3. add JSON content to POST request body
        setPostRequestContent(conn, jsonObject);

        // 4. make POST request to the given URL
        conn.connect();

        InputStream in = new BufferedInputStream(conn.getInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = reader.readLine()) != null){
            response.append(inputLine);
        }
        in.close();



        // 5. return response message
        return response.toString();

    }

    private Call createHttpPostMethodCall(String url)
    {
        // Create okhttp3 form body builder.
        FormBody.Builder formBodyBuilder = new FormBody.Builder();

        // Add form parameter
        formBodyBuilder.add("q", "trump");
        formBodyBuilder.add("first", "2");

        // Build form body.
        FormBody formBody = formBodyBuilder.build();

        // Create a http request object.
        Request.Builder builder = new Request.Builder();
        builder = builder.url(url);
        builder = builder.post(formBody);
        Request request = builder.build();

        // Create a new Call object with post method.
        Call call = okHttpClient.newCall(request);

        return call;
    }

    private JSONObject buidJsonObject() throws JSONException {

        JSONObject jsonObject = new JSONObject();
//        jsonObject.accumulate("name", etName.getText().toString());
//        jsonObject.accumulate("country",  etCountry.getText().toString());
//        jsonObject.accumulate("twitter",  etTwitter.getText().toString());

        return jsonObject;
    }

    private void setPostRequestContent(HttpURLConnection conn,
                                       JSONObject jsonObject) throws IOException {

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write("[\"INGREDIENTS: TORULA YEAST, CITRIC ACID,\", \"LACTIC ACID, WHEAT STARCH. INGREDIENTS\"]");
        Log.i(MainActivity.class.toString(), jsonObject.toString());
        writer.flush();
        writer.close();
        os.close();
    }

}


