package moe.shizuku.bridge.service;

import android.Manifest;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import moe.shizuku.bridge.BuildConfig;
import moe.shizuku.bridge.ChooserActivity;
import moe.shizuku.bridge.R;
import moe.shizuku.bridge.utils.FileUtils;
import moe.shizuku.bridge.utils.FilenameResolver;
import moe.shizuku.bridge.utils.ResolveInfoHelper;

public class FileSaveService extends IntentService {

    private static final String TAG = "FileSaveService";

    private static final String ACTION_CLEAR_CACHE = BuildConfig.APPLICATION_ID + ".intent.action.CLEAR_CACHE";

    private static final String EXTRA_SHARE = BuildConfig.APPLICATION_ID + ".intent.extra.SHARE";

    public FileSaveService() {
        super("FileSaveService");
    }

    public static void startClearCache(Context context) {
        Intent intent = new Intent(context, FileSaveService.class);
        intent.setAction(ACTION_CLEAR_CACHE);
        context.startService(intent);
    }

    public static void startSaveFile(Context context, Intent intent, boolean share) {
        intent = new Intent(intent);
        intent.setComponent(ComponentName.createRelative(context, FileSaveService.class.getName()));
        intent.putExtra(EXTRA_SHARE, share);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || !intent.hasExtra(Intent.EXTRA_STREAM)) {
            return;
        }

        switch (intent.getAction()) {
            case Intent.ACTION_SEND:
            case Intent.ACTION_SEND_MULTIPLE:
                handleSaveFile(intent);
                break;
            case ACTION_CLEAR_CACHE:
                FileUtils.clearCache(this);
                break;
        }
    }

    private static class Result {
        private File file;
        private Uri uri;
        private Exception e;

        public Result(File file, Uri uri) {
            this.file = file;
            this.uri = uri;
        }

        public Result(Exception e) {
            this.e = e;
        }
    }

    private void handleSaveFile(Intent intent) {
        boolean share = intent.getBooleanExtra(EXTRA_SHARE, false);

        if (!checkPermission(share)) {
            Log.d(TAG, "no write storage permission");

            toast(getString(R.string.fail_no_write_storage_permission));
            return;
        }

        ArrayList<Uri> uris;
        if (intent.getAction().equals(Intent.ACTION_SEND)) {
            uris = new ArrayList<>();
            uris.add(intent.<Uri>getParcelableExtra(Intent.EXTRA_STREAM));
        } else {
            uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        }

        File firstFile = null;
        Uri firstUri = null;

        ArrayList<Uri> u = new ArrayList<>();
        for (Uri uri : uris) {
            Result result = save(uri, share);

            if (result.e != null) {
                toast(getString(R.string.save_failed, result.e.getMessage()));
                continue;
            }

            if (firstFile == null) {
                firstFile = result.file;
                firstUri = result.uri;
            }

            if (!share) {
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(result.file)));
            }

            u.add(result.uri);
        }

        if (firstFile == null) {
            return;
        }

        String path = FileUtils.getRelativePathOfExternalStorage(firstFile);
        if (!share) {
            if (intent.getAction().equals(Intent.ACTION_SEND) || u.size() == 1) {
                toast(getString(R.string.saved, path));
            } else {
                path = path.substring(0, path.length() - firstFile.getName().length());
                toast(getResources().getQuantityString(R.plurals.saved_multiple, u.size() - 1, u.size() - 1, firstFile.getName(), path));
            }
        } else {
            intent.setPackage(null)
                    .setComponent(null)
                    .removeExtra(EXTRA_SHARE);

            if (intent.getAction().equals(Intent.ACTION_SEND)) {
                intent.putExtra(Intent.EXTRA_STREAM, firstUri);
            } else {
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, u);
            }

            ChooserActivity.start(this, intent, ResolveInfoHelper.filter(getPackageManager().queryIntentActivities(intent, 0), false));
        }
    }

    private boolean checkPermission(boolean share) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!share && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private File getFile(Uri uri, boolean share) {
        String filename = FilenameResolver.query(getContentResolver(), uri, Long.toString(System.currentTimeMillis()));
        if (share) {
            return FileUtils.getCacheFile(this, "files/" + filename);
        } else {
            // too bad
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme()) && uri.getPath().contains("ScreenshotProvider")) {
                return FileUtils.getExternalStoragePublicFile(Environment.DIRECTORY_PICTURES, "Screenshots", filename);
            } else {
                return FileUtils.getExternalStoragePublicFile(Environment.DIRECTORY_DOWNLOADS, "Bridge", filename);
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Result save(Uri uri, boolean share) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);

            if (is == null) {
                Log.d(TAG, "openInputStream failed");

                return new Result(new RuntimeException("openInputStream failed"));
            }

            File file = getFile(uri, share);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();

            FileOutputStream outputStream = new FileOutputStream(file);

            int bytesRead;
            byte[] buffer = new byte[1024];
            while ((bytesRead = is.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            Log.d(TAG, "saved " + file.getAbsolutePath());

            if (share) {
                return new Result(file, Uri.fromFile(file));
            } else {
                return new Result(file, FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", file));
            }
        } catch (IOException | SecurityException e) {
            e.printStackTrace();

            return new Result(e);
        }
    }

    private void toast(final String message) {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
