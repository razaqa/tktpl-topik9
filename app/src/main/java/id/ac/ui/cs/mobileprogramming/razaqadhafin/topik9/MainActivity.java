package id.ac.ui.cs.mobileprogramming.razaqadhafin.topik9;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.assets.RenderableSource;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

@SuppressLint("StaticFieldLeak")
public class MainActivity extends AppCompatActivity {

    private final int PERMISSION_CODE = 1;

    private ArFragment arFragment;
    private ModelRenderable renderable;

    private String retrofitBaseUrl;
    private String fileUrl;
    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isPermissionsNotGranted()) {
            requestPermissions();
        }

        EditText editTextLink = (EditText) findViewById(R.id.editTextLink);
        EditText editTextFileName = (EditText) findViewById(R.id.editTextFileName);

        Button downloadButton = (Button) findViewById(R.id.buttonDownload);
        downloadButton.setOnClickListener((view) -> {
            fileName = editTextFileName.getText().toString();
            fileUrl = editTextLink.getText().toString();
            retrofitBaseUrl = fileUrl.split("/")[0] + "//" + fileUrl.split("/")[2];
            downloadFile(fileUrl);
            buildModel();
        });

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            AnchorNode anchorNode = new AnchorNode(hitResult.createAnchor());
            anchorNode.setRenderable(renderable);
            arFragment.getArSceneView().getScene().addChild(anchorNode);
        });
    }

    private void buildModel() {
        File downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File( downloadPath + File.separator + fileName);

        RenderableSource renderableSource = RenderableSource
                .builder()
                .setSource(this, Uri.parse(file.getPath()), RenderableSource.SourceType.GLB)
                .setRecenterMode(RenderableSource.RecenterMode.ROOT)
                .build();
        ModelRenderable
                .builder()
                .setSource(this, renderableSource)
                .setRegistryId(file.getPath())
                .build()
                .thenAccept(modelRenderable -> {
                    Toast.makeText(this, "Model built", Toast.LENGTH_SHORT).show();;
                    renderable = modelRenderable;
                });
    }

    protected boolean isPermissionsNotGranted() {
        return ActivityCompat.checkSelfPermission(
                MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                        MainActivity.this,
                        Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED;
    }

    protected void requestPermissions() {
        ActivityCompat.requestPermissions(
                MainActivity.this,
                new String[] {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.INTERNET
                },
                PERMISSION_CODE);
    }

    protected void downloadFile(String url) {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(retrofitBaseUrl);
        Retrofit retrofit = builder.build();

        FileDownloadClient fileDownloadClient = retrofit.create(FileDownloadClient.class);
        Call<ResponseBody> call = fileDownloadClient.downloadFileStream(url);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                new WriteFileAsyncTask().execute(response);
                Toast.makeText(MainActivity.this, "succeed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(MainActivity.this, "failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            File downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File( downloadPath + File.separator + fileName);

            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                byte[] fileReader = new byte[4096];
                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(file);
                while (true) {
                    int read = inputStream.read(fileReader);
                    if (read == -1) {
                        break;
                    }
                    outputStream.write(fileReader, 0, read);
                    fileSizeDownloaded += read;
                }
                outputStream.flush();
                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }

    private class WriteFileAsyncTask extends AsyncTask<Response<ResponseBody>, Void, Void> {
        @Override
        protected Void doInBackground(Response<ResponseBody>... responses) {
            writeResponseBodyToDisk(responses[0].body());
            return null;
        }
    }
}