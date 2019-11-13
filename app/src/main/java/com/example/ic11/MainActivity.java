package com.example.ic11;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/***
 * Group 20
 * Nia Ibrahim
 * Nadia Dorado
 */

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    public Button btn_takePhoto;
    Bitmap bitmapUpload = null;
    public String TAG = "demo";
    public ImageView iv_picture;
    public ProgressBar progressBar;
    public ArrayList<Image> images = new ArrayList<Image>();
    public String uuid;
    public ImageAdapter ad  = null;
    public ListView lv_sources;
    public FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    public StorageReference storageReference = firebaseStorage.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("MyAlbum");

        btn_takePhoto = findViewById(R.id.btn_takePhoto);
        btn_takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        iv_picture = findViewById(R.id.iv_picture);
        progressBar = findViewById(R.id.progressBar);
        lv_sources = findViewById(R.id.lv_images);

        Task<ListResult> results = storageReference.child("images").listAll();

        results.addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {

                final ArrayList<Image> localImages = new ArrayList<Image>();
               for(int i=0; i<listResult.getItems().size(); i++){
                   final Image image = new Image();

                   image.storagePath = listResult.getItems().get(i).getPath();

                  listResult.getItems().get(i).getDownloadUrl()
                          .addOnCompleteListener(new OnCompleteListener<Uri>() {
                      @Override
                      public void onComplete(@NonNull Task<Uri> task) {


                          image.url = task.getResult().toString();
                          images.add(image);

                          Log.d(TAG, "image size: " + images.size());
                          ad = new ImageAdapter(MainActivity.this,
                                  android.R.layout.simple_list_item_1, images);

                          // give adapter to ListView UI element to render
                          lv_sources.setAdapter(ad);

                          lv_sources.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
                              @Override
                              public void onItemClick(final AdapterView<?> parent, final View view, final int position, long id) {
                                  Image img = (Image) parent.getItemAtPosition(position);
                                  Log.d(TAG, "Storage Path: " + img.storagePath);
                                  StorageReference deleteRef = storageReference.child(img.storagePath);

                                  deleteRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                      @Override
                                      public void onSuccess(Void aVoid) {

                                          ad.remove((Image) parent.getItemAtPosition(position));
                                          Toast.makeText(MainActivity.this, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                                      }
                                  }).addOnFailureListener(new OnFailureListener() {
                                      @Override
                                      public void onFailure(@NonNull Exception e) {
                                          Log.d(TAG, "Exception: " + e.getMessage());
                                          Toast.makeText(MainActivity.this, "Error deleting image!", Toast.LENGTH_SHORT).show();
                                      }
                                  });
                              }
                          });
                      }
                  });

              }



            }
        });

    }
//UPLOAD IMAGE TO CLOUD
    private void uploadImage(Bitmap photoBitmap) {

        final Image image = new Image();

         uuid = UUID.randomUUID().toString();

        final StorageReference imageRepo = storageReference.child("images/" + uuid + ".png");

        image.storagePath = imageRepo.getPath();

//        Converting the Bitmap into a bytearrayOutputstream....
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        photoBitmap.compress(Bitmap.CompressFormat.PNG, 50, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = imageRepo.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "onFailure: " + e.getMessage());
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "onSuccess: " + "Image Uploaded!!!");
            }
        });


        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
//                return null;
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                return imageRepo.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Image Download URL" + task.getResult());
                    String imageURL = task.getResult().toString();

                    SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(uuid, imageURL);
                    editor.apply();

                    image.url = imageURL;
                    images.add(image);

//                    ListView lv_sources = findViewById(R.id.lv_images);

                    Log.d(TAG, "image size: " + images.size());
                    ad = new ImageAdapter(MainActivity.this,
                            android.R.layout.simple_list_item_1, images);

                    // give adapter to ListView UI element to render
                    lv_sources.setAdapter(ad);


                    lv_sources.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(final AdapterView<?> parent, final View view, final int position, long id) {

                            Image img = (Image) parent.getItemAtPosition(position);
                            Log.d(TAG, "Storage Path: " + img.storagePath);

                            StorageReference deleteRef = storageReference.child(img.storagePath);

                            deleteRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    ad.remove((Image) parent.getItemAtPosition(position));
                                    Toast.makeText(MainActivity.this, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MainActivity.this, "Error deleting image!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });

                }
            }
        });

        //        ProgressBar......

        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                progressBar.setProgress((int) progress);
                Toast.makeText(MainActivity.this, "Upload is " + progress + "% done", Toast.LENGTH_SHORT).show();;
            }


        });

    }

    //    TAKE PHOTO USING CAMERA...
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        Camera Callback........
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            bitmapUpload = imageBitmap;
            uploadImage(bitmapUpload);
        }

        progressBar.setProgress(0);
    }
}
