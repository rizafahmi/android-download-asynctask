package com.example.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;


public class MainActivity extends Activity {

    private static long enqueue;
    private static DownloadManager dm;


    static String zipfile = "Shalat-free.zip";
    static String directory = "/asshalat_pro_files/";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);


            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)){
                        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(enqueue);
                        Cursor c = dm.query(query);
                        if (c.moveToFirst()){
                            int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);

                            int bytesDownloaded = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            int bytesTotal = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                            final int dlProgress = (bytesDownloaded * 100 / bytesTotal);



                            if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                                Log.v("MainActivityLog", "Downloas Successfull");

                                // Start unzipping...
                            }
                        }
                    }
                }
            };
            getActivity().registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            startDownload(rootView);

            final ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
            final TextView progressTextView = (TextView) rootView.findViewById(R.id.progressTextView);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean downloading = true;
                    while (downloading) {
                        DownloadManager.Query q = new DownloadManager.Query();
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse("http://amplified.co.id/as-shalat/" + zipfile));
                        request.setDestinationInExternalPublicDir(directory,zipfile);
                        enqueue = dm.enqueue(request);
                        q.setFilterById(enqueue);

                        Cursor cursor = dm.query(q);
                        cursor.moveToFirst();
                        int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                        if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL){
                            downloading = false;
                        }

                        final int dlProgress = (int) ((bytesDownloaded * 100l)/bytesTotal);

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setProgress(dlProgress);
                            }
                        });

                        cursor.close();
                    }
                }
            });


            return rootView;
        }

        public void startDownload(View view){
            boolean isDownloading = false;
            // Get sdcard location
            File externalStorage = Environment.getExternalStorageDirectory();
            File directoryStorage = new File(externalStorage + directory);

            if (!directoryStorage.exists()){
                directoryStorage.mkdir();
                isDownloading = false;
            }

            Log.v("MainActivityLog", externalStorage.getAbsolutePath() + directory + zipfile);

            // Check if the zip file exist?
            File file = new File(externalStorage.getAbsolutePath() + directory + zipfile);

            // If not, download file started
            if (!file.exists()){
                isDownloading = false;
            }else{
                isDownloading = true;
            }

            if (!isDownloading){

                new DownloadFileFromURL(view).execute("http://amplified.co.id/as-shalat/" + zipfile);



            }else{
                ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
                TextView progressTextView = (TextView) view.findViewById(R.id.progressTextView);
                progressBar.setVisibility(View.INVISIBLE);
                progressTextView.setText("Download Finished.");
            }


        }

        private class DownloadFileFromURL extends AsyncTask<String, String, String> {
            View view;
            ProgressBar progressBar;
            TextView progressTextView;

            private DownloadFileFromURL(View view){
                this.view = view;
            }

            @Override
            protected void onPreExecute(){
                super.onPreExecute();
                Log.v("MainActivityLog", "Begin Download");
                progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
                progressTextView = (TextView) view.findViewById(R.id.progressTextView);

            }
            @Override
            protected String doInBackground(String... f_url) {
                int count;
                try{
                    URL url = new URL(f_url[0]);
                    URLConnection connection = url.openConnection();
                    connection.connect();
                    int lengthOfFile = connection.getContentLength();

                    InputStream input = new BufferedInputStream(url.openStream(), 8192);

                    File externalStorage = Environment.getExternalStorageDirectory();
                    OutputStream output = new FileOutputStream(externalStorage.toString() + directory + zipfile);

                    byte data[] = new byte[1024];
                    long total = 0;

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        publishProgress("" + (int)((total*100/lengthOfFile)));

                        output.write(data, 0, count);
                    }

                    output.flush();
                    output.close();
                    input.close();
                } catch (Exception e) {
                    Log.e("Error: ", e.getMessage());
                }
                return null;
            }

            protected  void onProgressUpdate(String... progress) {
                super.onProgressUpdate(progress);
                Log.v("MainActivityLog", Integer.parseInt(progress[0]) + " %");
                progressBar.setProgress(Integer.parseInt(progress[0]));
                progressTextView.setText(progress[0] + " %");
            }

            @Override
            protected void onPostExecute(String file_url) {
                // Hide the progress bar
                progressBar.setVisibility(View.INVISIBLE);
                // Show text
                progressTextView.setText("Download Finished.");
            }
        }
    }


}
