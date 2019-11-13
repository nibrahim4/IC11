package com.example.ic11;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.picasso.Picasso;

import java.util.List;

public class ImageAdapter extends ArrayAdapter<Image> {

    public ImageAdapter(@NonNull Context context, int resource, @NonNull List<Image> images) {
        super(context, resource, images);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        Image image = getItem(position);

        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.image_item,parent, false);
        }

        ImageView iv_picture = convertView.findViewById(R.id.iv_picture);
        Log.d("demo", "image url : " + image.url);

        if(image.url != null && !image.url.equals("null")){

            Picasso.get().load(image.url).into(iv_picture);
        }


        return convertView;
    }
}
