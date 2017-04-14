package moe.shizuku.bridge;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import moe.shizuku.bridge.service.FileSaveService;

/**
 * Created by Rikka on 2017/3/26.
 */

public class SaveActivity extends Activity {

    private static final String TAG = "SaveActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_SEND)) {
            Uri uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);

            if (uri != null && uri.getScheme().equals("file")) {
                String path = uri.getPath();
                if (!path.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                    Toast.makeText(this, R.string.file_uri_cross_user, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }

            String type = getIntent().getType();
            if (type != null && getIntent().getType().startsWith("bridge")) {
                type = type.substring("bridge".length() + 1);
            }

            if (uri != null) {
                FileSaveService.startSaveFile(this, uri, type, isForwardActivity());
            }
            finish();
        }
    }

    protected boolean isForwardActivity() {
        return false;
    }
}
