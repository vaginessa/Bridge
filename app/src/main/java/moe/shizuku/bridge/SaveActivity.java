package moe.shizuku.bridge;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import java.util.ArrayList;

import moe.shizuku.bridge.service.FileSaveService;

/**
 * Created by Rikka on 2017/3/26.
 */

public class SaveActivity extends Activity {

    private static final String TAG = "SaveActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getAction() == null
                || (!getIntent().getAction().equals(Intent.ACTION_SEND) && !getIntent().getAction().equals(Intent.ACTION_SEND_MULTIPLE))) {
            finish();
            return;
        }

        if (!getIntent().hasExtra(Intent.EXTRA_STREAM)) {
            Toast.makeText(this, R.string.no_extra, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!checkCrossUser()) {
            Toast.makeText(this, R.string.file_uri_cross_user, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        FileSaveService.startClearCache(this);
        FileSaveService.startSaveFile(this, getIntent(), isForwardActivity());
        finish();
    }

    @Override
    public Intent getIntent() {
        Intent intent = super.getIntent();
        String type = intent.getType();
        if (type != null && type.startsWith("bridge")) {
            intent.setType(type.substring("bridge".length() + 1));
        }
        return intent;
    }

    private boolean checkCrossUser() {
        Uri uri;
        if (getIntent().getAction().equals(Intent.ACTION_SEND)) {
            uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        } else {
            ArrayList<Uri> uris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            uri = uris.isEmpty() ? null : uris.get(0);
        }

        if (uri != null && uri.getScheme().equals("file")) {
            String path = uri.getPath();
            if (!path.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                return false;
            }
        }
        return true;
    }

    protected boolean isForwardActivity() {
        return false;
    }
}
