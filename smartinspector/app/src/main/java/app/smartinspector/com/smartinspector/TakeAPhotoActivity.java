package app.smartinspector.com.smartinspector;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.FaceRectangle;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class TakeAPhotoActivity extends AppCompatActivity {

    private static final int REQUEST_TAKE_PHOTO = 0;
    public static final String TAG = "machine learning";
    Uri mUriPhotoTaken;
    Button mTakeAPhotoButton;
    Button mSubmitButton;
    ImageView mMainImage;
    private TextView mStatusText;
    private Bitmap mBitmap;
    private EmotionServiceClient mClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_aphoto);

        mMainImage = (ImageView) findViewById(R.id.main_image);

        mTakeAPhotoButton = (Button) findViewById(R.id.take_a_photo_button);
        mTakeAPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeAPhoto();
            }
        });

        mSubmitButton = (Button) findViewById(R.id.submit_button);
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submit();
            }
        });

        mStatusText = (TextView) findViewById(R.id.status_text);

        if (mClient == null) {
            mClient = new EmotionServiceRestClient(getString(R.string.subscription_key));
        }
    }

    private void submit() {
        uiActive(false);
        mStatusText.setText("Analysing");
        // Do emotion detection using auto-detected faces.
        try {
            new doRequest(false).execute();
        } catch (Exception e) {
            Log.d(TAG, "Error encountered. Exception is: " + e.toString());
            uiActive(true);
        }
    }

    private List<RecognizeResult> processWithAutoFaceDetection() throws EmotionServiceException, IOException {
        Log.d("emotion", "Start emotion detection with auto-face detection");

        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long startTime = System.currentTimeMillis();
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE STARTS HERE
        // -----------------------------------------------------------------------

        List<RecognizeResult> result = null;
        //
        // Detect emotion by auto-detecting faces in the image.
        //
        result = this.mClient.recognizeImage(inputStream);

        String json = gson.toJson(result);
        Log.d("result", json);

        Log.d("emotion", String.format("Detection done. Elapsed time: %d ms", (System.currentTimeMillis() - startTime)));
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE ENDS HERE
        // -----------------------------------------------------------------------
        return result;
    }

    private List<RecognizeResult> processWithFaceRectangles() throws EmotionServiceException, com.microsoft.projectoxford.face.rest.ClientException, IOException {
        Log.d("emotion", "Do emotion detection with known face rectangles");
        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long timeMark = System.currentTimeMillis();
        Log.d("emotion", "Start face detection using Face API");
        FaceRectangle[] faceRectangles = null;
        String faceSubscriptionKey = getString(R.string.faceSubscription_key);
        FaceServiceRestClient faceClient = new FaceServiceRestClient(faceSubscriptionKey);
        Face faces[] = faceClient.detect(inputStream, false, false, null);
        Log.d("emotion", String.format("Face detection is done. Elapsed time: %d ms", (System.currentTimeMillis() - timeMark)));

        if (faces != null) {
            faceRectangles = new FaceRectangle[faces.length];

            for (int i = 0; i < faceRectangles.length; i++) {
                // Face API and Emotion API have different FaceRectangle definition. Do the conversion.
                com.microsoft.projectoxford.face.contract.FaceRectangle rect = faces[i].faceRectangle;
                faceRectangles[i] = new com.microsoft.projectoxford.emotion.contract.FaceRectangle(rect.left, rect.top, rect.width, rect.height);
            }
        }

        List<RecognizeResult> result = null;
        if (faceRectangles != null) {
            inputStream.reset();

            timeMark = System.currentTimeMillis();
            Log.d("emotion", "Start emotion detection using Emotion API");
            // -----------------------------------------------------------------------
            // KEY SAMPLE CODE STARTS HERE
            // -----------------------------------------------------------------------
            result = this.mClient.recognizeImage(inputStream, faceRectangles);

            String json = gson.toJson(result);
            Log.d("result", json);
            // -----------------------------------------------------------------------
            // KEY SAMPLE CODE ENDS HERE
            // -----------------------------------------------------------------------
            Log.d("emotion", String.format("Emotion detection is done. Elapsed time: %d ms", (System.currentTimeMillis() - timeMark)));
        }
        return result;
    }

    private class postResults extends AsyncTask<String, String, Boolean> {
        private void complete(final String message) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateStatus(message);
                    uiActive(true);
                    AlertDialog.Builder builder = new AlertDialog.Builder(TakeAPhotoActivity.this);
                    builder.setMessage("Thank you")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // The 'which' argument contains the index position
                                    // of the selected item
                                }});
                    AlertDialog dialog = builder.create();
                }
            });
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            updateStatus("Upading result to machine learning");

            try {
                URL url = new URL(getString(R.string.machine_learning_host));
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(2000);
                conn.setRequestProperty("Authorization", "Bearer "
                        + getString(R.string.machine_learning_key));
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(strings[0]);
                writer.flush();
                writer.close();
                os.close();
                Log.d(TAG, "response code: " + conn.getResponseCode()
                    + " " + conn.getResponseMessage());
                InputStream is = new BufferedInputStream(conn.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String inputLine = "";
                StringBuffer sb = new StringBuffer();
                while ((inputLine = br.readLine()) != null) {
                    sb.append(inputLine);
                }
                Log.d(TAG, sb.toString());
            } catch (Exception ex) {
                complete("Failed: " + ex.toString());
                Log.wtf(TAG, ex.toString());
                return false;
            }
            complete("Done");
            return true;
        }
    }

    private void uiActive(boolean active) {
        mSubmitButton.setEnabled(active);
        mTakeAPhotoButton.setEnabled(active);
    }
    private void updateStatus(String status) {
        mStatusText.setText(status);
    }

    private class doRequest extends AsyncTask<String, String, List<RecognizeResult>> {
        // Store error message
        private Exception e = null;
        private boolean useFaceRectangles = false;

        public doRequest(boolean useFaceRectangles) {
            this.useFaceRectangles = useFaceRectangles;
        }

        @Override
        protected List<RecognizeResult> doInBackground(String... args) {
            if (this.useFaceRectangles == false) {
                try {
                    return processWithAutoFaceDetection();
                } catch (Exception e) {
                    this.e = e;    // Store error
                }
            } else {
                try {
                    return processWithFaceRectangles();
                } catch (Exception e) {
                    this.e = e;    // Store error
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<RecognizeResult> result) {
            super.onPostExecute(result);
            // Display based on error existence

            if (this.useFaceRectangles == false) {
                updateStatus("Recognizing emotions with auto-detected face");
                Log.d(TAG, "\n\nRecognizing emotions with auto-detected face rectangles...\n");
            } else {
                updateStatus("Recognizing emotions with Face API");
                Log.d(TAG, "\n\nRecognizing emotions with existing face rectangles from Face API...\n");
            }
            if (e != null) {
                updateStatus("Error: " + e.getMessage());
                this.e = null;
            } else {
                if (result.size() == 0) {
                    Log.d(TAG, "No emotion detected :(");
                    if (this.useFaceRectangles == false) {
                        String faceSubscriptionKey = getString(R.string.faceSubscription_key);

                        try {
                            new doRequest(true).execute();
                        } catch (Exception e) {
                            Log.d(TAG, "Error encountered. Exception is: " + e.toString());
                            failed();
                        }
                    } else {
                        failed();
                    }
                } else {
                    Integer count = 0;
                    // Covert bitmap to a mutable bitmap by copying it
                    Bitmap bitmapCopy = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas faceCanvas = new Canvas(bitmapCopy);
                    faceCanvas.drawBitmap(mBitmap, 0, 0, null);
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5);
                    paint.setColor(Color.RED);

                    for (RecognizeResult r : result) {
                        Log.d(TAG, String.format("\nFace #%1$d \n", count));
                        Log.d(TAG, String.format("\t anger: %1$.5f\n", r.scores.anger));
                        Log.d(TAG, String.format("\t contempt: %1$.5f\n", r.scores.contempt));
                        Log.d(TAG, String.format("\t disgust: %1$.5f\n", r.scores.disgust));
                        Log.d(TAG, String.format("\t fear: %1$.5f\n", r.scores.fear));
                        Log.d(TAG, String.format("\t happiness: %1$.5f\n", r.scores.happiness));
                        Log.d(TAG, String.format("\t neutral: %1$.5f\n", r.scores.neutral));
                        Log.d(TAG, String.format("\t sadness: %1$.5f\n", r.scores.sadness));
                        Log.d(TAG, String.format("\t surprise: %1$.5f\n", r.scores.surprise));
                        Log.d(TAG, String.format("\t face rectangle: %d, %d, %d, %d", r.faceRectangle.left, r.faceRectangle.top, r.faceRectangle.width, r.faceRectangle.height));
                        faceCanvas.drawRect(r.faceRectangle.left,
                                r.faceRectangle.top,
                                r.faceRectangle.left + r.faceRectangle.width,
                                r.faceRectangle.top + r.faceRectangle.height,
                                paint);
                        count++;
                        new postResults().execute(
                                String.format(
                                "{\n" +
                                        "  \"Inputs\": {\n" +
                                        "    \"input1\": [\n" +
                                        "      {\n" +
                                        "        \"anger\": %1$.5f,\n" +
                                        "        \"contempt\": %1$.5f,\n" +
                                        "        \"disgust\": %1$.5f,\n" +
                                        "        \"fear\": %1$.5f,\n" +
                                        "        \"happiness\": %1$.5f,\n" +
                                        "        \"neutral\": %1$.5f,\n" +
                                        "        \"sadness\": %1$.5f,\n" +
                                        "        \"surprise\": %1$.5f\n" +
                                        "      }\n" +
                                        "    ]\n" +
                                        "  },\n" +
                                        "  \"GlobalParameters\": {}\n" +
                                        "}",
                                        r.scores.anger,
                                        r.scores.contempt,
                                        r.scores.disgust,
                                        r.scores.fear,
                                        r.scores.happiness,
                                        r.scores.neutral,
                                        r.scores.sadness,
                                        r.scores.surprise
                                )
                        );
                    }
                    ImageView imageView = (ImageView) findViewById(R.id.main_image);
                    imageView.setImageDrawable(new BitmapDrawable(getResources(), mBitmap));
                }
            }
        }

        private void failed() {
            updateStatus("No emotion detected");
            AlertDialog.Builder builder = new AlertDialog.Builder(TakeAPhotoActivity.this);
            builder.setMessage("Please take the photo again.")
                    .setTitle("Failed to detect your emotion")
                    .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // The 'which' argument contains the index position
                    // of the selected item
                }});
            AlertDialog dialog = builder.create();
            dialog.show();
            uiActive(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    Uri imageUri;
                    if (data == null || data.getData() == null) {
                        imageUri = mUriPhotoTaken;
                    } else {
                        imageUri = data.getData();
                    }
                    assignImage(imageUri);
                }
                break;
            default:
                break;
        }
    }

    private void assignImage(Uri imageUri) {
        mMainImage.setImageURI(imageUri);
        mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                imageUri, getContentResolver());
        uiActive(true);
    }


    private void takeAPhoto() {
        ContentValues values = new ContentValues(1);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
        mUriPhotoTaken = this.getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);


        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        if (mUriPhotoTaken != null) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoTaken);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        }

        if(intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_TAKE_PHOTO);
        }
    }


}
