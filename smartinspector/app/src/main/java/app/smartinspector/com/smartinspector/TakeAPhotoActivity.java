package app.smartinspector.com.smartinspector;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;

public class TakeAPhotoActivity extends AppCompatActivity {

    private static final int REQUEST_TAKE_PHOTO = 0;
    Uri mUriPhotoTaken;
    Button mTakeAPhotoButton;
    Button mSubmitButton;
    ImageView mMainImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_aphoto);

        mMainImage = (Button) findViewById(R.id.take_a_photo_button);

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
    }

    private void submit() {

    }

    private void takeAPhoto() {
        ContentValues values = new ContentValues(1);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
        Uri mCameraTempUri = this.getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);


        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        if (mCameraTempUri != null) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraTempUri);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        }

        if(intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_TAKE_PHOTO);
        }
    }


}
