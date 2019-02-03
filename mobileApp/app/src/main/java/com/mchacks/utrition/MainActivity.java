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
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

import org.json.JSONArray;
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
    TextView textViewBottom;
    File file;
    Uri fileUri;
    final int RC_TAKE_PHOTO = 1;
    String mCurrentPhotoPath;
    private final String USER_AGENT = "Mozilla/5.0";
    private OkHttpClient okHttpClient = null;
    String inputString;
    String resultText = "";
    ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //System.setProperty("http.keepAlive", "false");

        Button btnCamera = (Button)findViewById(R.id.btnCamera);
        imageView = (ImageView)findViewById(R.id.imageView);
        textView = (TextView)findViewById(R.id.text_view_large);
        textViewBottom = (TextView)findViewById(R.id.text_view_large);
        textViewBottom.setMovementMethod(new ScrollingMovementMethod());
        progressBar = (ProgressBar)findViewById(R.id.progressBar1);
        progressBar.setVisibility(View.GONE);


        imageView.setImageResource(R.drawable.logo);

        if(okHttpClient == null)
        {
            okHttpClient = new OkHttpClient();
        }
        //startActivity(new Intent(MainActivity.this, Pop.class));

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

    public void parseJSON(String jsonString) throws JSONException {
        resultText = "";
        JSONObject object = new JSONObject(jsonString.toString());
        JSONArray array = object.getJSONArray("ingredients");
        printJSON(array);
    }

    public void printJSON(JSONArray array) throws JSONException {
        for (int i = 0; i< array.length(); i++){
            JSONObject obj = array.getJSONObject(i);
            printDetailsFromJSONObj(obj);
        }
    }

    public void printDetailsFromJSONObj(JSONObject obj) {
        String name = "";
        try {
            name = obj.getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String description = "";
        try {
            description = obj.getString("description");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String url = "";
        try {
            url = obj.getString("url");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject sentiment;
        String score = "";
        try {
            sentiment = obj.getJSONObject("sentiment");
            score = sentiment.getString("score");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("myApp", name);
        Log.d("myApp", description);

        // analyze score
        float scoreF = Float.parseFloat(score);

        resultText = resultText
                + "<b>Name:</b> "
                + name + "<br>"
                + "<b>Description:</b> "
                + description
                + "<br>"
                + url
                + "<br>"
                + score
                + "<br>";

        if (scoreF < -0.5){
            resultText += "<span style='color:#FF88CF'><b>Could have health impacts</b><br></span>";
        }

        resultText +=
                 "----------------------<br>";
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
        inputString = output.toString();
        Log.d("myApp",inputString);

        //make REST API call
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runOnUiThread(new Runnable(){
                        @Override
                        public void run(){
                            progressBar.setVisibility(View.VISIBLE);
                        }
                    });
                    parseJSON(HttpPost("https://us-central1-utrition.cloudfunctions.net/api/preproc"));
                    runOnUiThread(new Runnable(){
                        @Override
                        public void run(){
                            textViewBottom.setText(Html.fromHtml(resultText));
                            progressBar.setVisibility(View.GONE);
                        }
                    });
              }
                catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable(){
                        @Override
                        public void run(){
                            textViewBottom.setText(Html.fromHtml("Please try scanning again."));
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable(){
                        @Override
                        public void run(){
                            textViewBottom.setText(Html.fromHtml("Please try scanning again."));
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                    e.printStackTrace();
                }
            }
        });



        thread.start();


    }

    private String HttpPost(String myUrl) throws IOException, JSONException {
        String result = "";
        HttpURLConnection conn;
        StringBuffer response;

        URL url = new URL(myUrl);

        // 1. create HttpURLConnection
        conn = (HttpURLConnection) url.openConnection();
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
        response = new StringBuffer();

        while ((inputLine = reader.readLine()) != null) {
            response.append(inputLine);
        }

        conn.disconnect();




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
        //writer.write("[\"INGREDIENTS: TORULA YEAST, CITRIC ACID,\", \"LACTIC ACID, WHEAT STARCH. INGREDIENTS\"]");
        writer.write("[\""+inputString+"\"]");
        Log.i(MainActivity.class.toString(), jsonObject.toString());
        writer.flush();
        writer.close();
        os.close();
    }

}


