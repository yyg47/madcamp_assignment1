package io.madcamp.yh.mc_assignment1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import org.json.*;

import java.io.*;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Tab2Fragment extends Fragment {
    public static final String ARG_PAGE = "ARG_PAGE";
    private int mPage;
    private Context context;
    private View top;

    private boolean isFabOpen;

    private static final int REQ_IMG_FILE = 1;
    private static final int REQ_TAKE_PHOTO = 2;

    private RecyclerView recyclerView;
    private Tab2Adapter adapter;

    private Uri tempPhotoUri;

    /* TabPagerAdapter에서 Fragment 생성할 때 사용하는 메소드 */
    public static Tab2Fragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        Tab2Fragment fragment = new Tab2Fragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPage = getArguments().getInt(ARG_PAGE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        top = inflater.inflate(R.layout.fragment_tab2, container, false);
        this.context = top.getContext();

        /* -- 여기서부터 작성해주세요 -- */
        initializeFloatingActionButton();
        initializeRecyclerView();

        return top;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        saveImageListToInternal();
    }

    public void initializeFloatingActionButton() {
        final FloatingActionButton[] fab = new FloatingActionButton[4];

        final Animation fab_open = AnimationUtils.loadAnimation(context, R.anim.fab_open);
        final Animation fab_close = AnimationUtils.loadAnimation(context, R.anim.fab_close);

        fab[0] = (FloatingActionButton)top.findViewById(R.id.fab);
        fab[1] = (FloatingActionButton)top.findViewById(R.id.fab1);
        fab[2] = (FloatingActionButton)top.findViewById(R.id.fab2);
        fab[3] = (FloatingActionButton)top.findViewById(R.id.fab3);

        isFabOpen = false;

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                switch(id) {
                    case R.id.fab:
                        anim();
                        break;
                    case R.id.fab1:
                        anim();
                        addImageFromFile();
                        break;
                    case R.id.fab2:
                        anim();
                        addImageFromCamera();
                        break;
                    case R.id.fab3:
                        anim();
                        removeAllItems();
                        break;
                }
            }

            public void anim() {
                if(isFabOpen) {
                    for(int i = 1; i < 4; i++) {
                        fab[i].startAnimation(fab_close);
                        fab[i].setClickable(false);
                    }
                    isFabOpen = false;
                } else {
                    for(int i = 1; i < 4; i++) {
                        fab[i].startAnimation(fab_open);
                        fab[i].setClickable(true);
                    }
                    isFabOpen = true;
                }
            }
        };

        for(int i = 0; i < 4; i++) {
            fab[i].setOnClickListener(onClickListener);
        }
    }

    final static String listPath = "images.json";

    public void saveImageListToInternal() {
        FileOutputStream fos;
        try {
            fos = context.openFileOutput(listPath, Context.MODE_PRIVATE);
            String res = packJSON();
            Log.d("saveImageListToInternal", res);
            fos.write(res.getBytes());
            fos.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Pair<Uri, String>> loadImageListFromInternal() {
        ArrayList<Pair<Uri, String>> list = new ArrayList<>();
        FileInputStream fis;
        InputStreamReader isr;
        BufferedReader br;
        StringBuilder sb;
        String line;
        try {
            fis = context.openFileInput(listPath);
            isr = new InputStreamReader(fis);
            br = new BufferedReader(isr);
            sb = new StringBuilder();
            while((line = br.readLine()) != null) {
                sb.append(line);
            }
            fis.close();
            String src = sb.toString();
            Log.d("loadImageListInternal", src);
            unpackJSON(src, list);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private String packJSON() {
        try {
            JSONArray arr = new JSONArray();
            ArrayList<Pair<Uri, String>> list = adapter.dataset;
            if(list != null) {
                for (Pair<Uri, String> p : list) {
                    JSONObject item = new JSONObject();
                    String path = p.first.getPath();
                    Log.d("path", path);
                    item.put("uri", path);
                    item.put("tag", p.second.toString());
                    arr.put(item);
                }
            }
            return arr.toString();
        } catch(JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void unpackJSON(String src, ArrayList<Pair<Uri, String>> list) {
        try {
            JSONArray arr = new JSONArray(src);
            for(int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.getJSONObject(i);
                Uri uri = Uri.parse(item.getString("uri"));
                String tag = item.getString("tag");
                list.add(new Pair<>(uri, tag));
            }
        } catch(JSONException e) {
            Log.d("Exception", "JSON Parsing Failed");
        }
    }

    public void initializeRecyclerView() {
        recyclerView = (RecyclerView)top.findViewById(R.id.recycler_view);
        adapter = new Tab2Adapter(loadImageListFromInternal());
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 4));
        recyclerView.setAdapter(adapter);

        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                context, recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
            }

            @Override
            public void onLongItemClick(View view, int position) {
                final int idx = position;
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Image " + idx);
                builder.setItems(new CharSequence[]{"삭제"},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch(which) {
                                    case 0:
                                        Toast.makeText(context, "Delete " + idx, Toast.LENGTH_SHORT).show();
                                        removeItem(idx);

                                        Log.d("Deleted", "" + idx);
                                        break;
                                }
                            }
                        });
                builder.show();
            }
        }));
    }

    public void addImageFromFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQ_IMG_FILE);
    }

    public void addImageFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(context.getPackageManager()) != null) {
            File photoFile = null;
            try {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = timeStamp;
                File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                photoFile = File.createTempFile(imageFileName, ".jpg", storageDir);
            } catch(IOException e) {
                e.printStackTrace();
            }
            if(photoFile != null) {
                tempPhotoUri = FileProvider.getUriForFile(context, "io.madcamp.yh.mc_assignment1.fileprovider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, tempPhotoUri);
                startActivityForResult(intent, REQ_TAKE_PHOTO);
            }
        }
    }

    private static String decodeFilename(Uri uri) {
        try {
            String path = URLDecoder.decode(uri.toString(), "UTF-8");
            String filename = path.substring(path.lastIndexOf("/") + 1);
            if(filename.length() < 1) filename = "noname";
            return filename;

        } catch(Exception e) {
            return "noname";
        }
    }

    private void removeItem(int idx) {
        if(idx < 0 || idx >= adapter.dataset.size()) return;
        Uri uri = adapter.remove(idx);
        File file = new File(uri.getPath());
        if(file.exists() && file.delete()) {
            Log.d("Delete File", uri.getPath());
        }
        saveImageListToInternal();
    }

    private void removeAllItems() {
        int l = adapter.dataset.size();
        for(int i = l; --i >= 0;) {
            removeItem(i);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK && (requestCode == REQ_IMG_FILE || requestCode == REQ_TAKE_PHOTO)) {
            Uri uri = null;
            String filename = null;
            switch(requestCode) {
                case REQ_IMG_FILE:
                    uri = data.getData();
                    filename = decodeFilename(uri);
                    break;
                case REQ_TAKE_PHOTO:
                    uri = tempPhotoUri;
                    filename = decodeFilename(tempPhotoUri);
                    break;
            }

            Uri newUri = copyToInternal(uri);
            adapter.add(newUri, filename);
            saveImageListToInternal();
        }
    }

    private Uri copyToInternal(Uri uri) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = timeStamp + ".jpg";
        try {
            String newDir = context.getFilesDir().toString();
            File fout = new File(newDir, "images");
            fout.mkdir();

            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            FileOutputStream fos = context.openFileOutput(imageFileName, Context.MODE_PRIVATE);

            /* scaling */
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float scaleFactor = 1.f;
            if(width < height) scaleFactor = 256.f / (float)width;
            else scaleFactor = 256.f / (float)height;
            if(scaleFactor < 1.f) {
                Matrix matrix = new Matrix();
                matrix.postScale(scaleFactor, scaleFactor);
                Bitmap resized = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
                bitmap.recycle();
                bitmap = resized;
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            Uri newUri = Uri.parse(context.getFileStreamPath(imageFileName).getAbsolutePath());
            Log.d("newUri", newUri.getPath());
            return newUri;
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
