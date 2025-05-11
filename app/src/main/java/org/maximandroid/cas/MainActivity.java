package org.maximandroid.cas;

import android.app.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import android.content.*;

import org.json.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class MainActivity extends Activity {

    private Cells cellsView;
    private Button addButton;
    private Maxima maxima;

    private String currentWorkspaceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Metrics.init(this);
        StorageUtil.instance = new StorageUtil(this);

        RelativeLayout mainLayout = new RelativeLayout(this);

        final ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        cellsView = new Cells(this);
        cellsView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        scrollView.addView(cellsView);
        mainLayout.addView(scrollView);

        addButton = new Button(this);
        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        buttonParams.bottomMargin = Metrics.padding;
        buttonParams.rightMargin = Metrics.padding;

        addButton.setLayoutParams(buttonParams);
        addButton.setText("New");
        addButton.setTextSize(Globals.ACTION_TEXT_SIZE);
        addButton.setWidth(4 * Metrics.padding);
        addButton.setHeight(2 * Metrics.padding);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cellsView.addCell("");
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
        mainLayout.addView(addButton);

        setContentView(mainLayout);

        cellsView.setOnRunClickListener(new Cells.OnRunClickListener() {
            @Override
            public void onRunClick(CodeCell cell, String code) {
                String result = maxima.executeCodeNullable(code);
                if (result == null) {
                    String text = String.format("execute code `%s` failed", code);
                    Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                    return;
                }
                cell.displayOutput(result);
            }
        });
        cellsView.addCell("");

        maxima = new Maxima(this);
        maxima.onCreate();

        if (!StorageUtil.checkAndCreateWithPermission()) {
            Toast.makeText(this, "No external storage permission!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.saveFile) {
            if (Objects.isNull(currentWorkspaceName)) {
                setCurrentWorkspaceName(StorageUtil.timestampName());
            }
            saveWorkspaceFile();
        } else if (itemId == R.id.openFile) {
            StorageUtil.checkAndOpenFileWithPermission();
        } else if (itemId == R.id.newFile) {
            initCurrentWorkspaceName();
            cellsView.removeAllViews();
            cellsView.addCell("");
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == StorageUtil.REQUEST_CODE_READ_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                StorageUtil.displayFileChooser();
            } else {
                Toast.makeText(this, "Request storage permission!", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == StorageUtil.REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                StorageUtil.createMaximaAndroidDir();
            }
        }
    }

    protected void setCurrentWorkspaceName(String fileName) {
        this.currentWorkspaceName = fileName;
        setTitle(fileName);
    }

    protected void initCurrentWorkspaceName() {
        this.currentWorkspaceName = null;
        setTitle("Maxima Android");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.v("MA", "onActivityResult: " + requestCode + ", " + resultCode + ", " + Objects.isNull(data));

        if (requestCode == StorageUtil.MAXIMA_INSTALL_CODE && resultCode == Activity.RESULT_OK) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    maxima.startMaxima();
                }
            }).start();
        }

        if (requestCode == StorageUtil.FILE_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            loadWorkspaceFile(uri);
        }
    }

    protected void saveWorkspaceFile() {
        getAllOutputSources(new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                String path = StorageUtil.saveJson(value, currentWorkspaceName);
                String text = "Cell data has been saved to " + path;
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    public void getAllOutputSources(ValueCallback<String> receiver) {
        JSONArray cells = new JSONArray();
        int childCount = cellsView.getChildCount();

        for (int i = 0; i < childCount; i++) {
            View shouldCell = cellsView.getChildAt(i);
            if (shouldCell instanceof CodeCell) {
                CodeCell cell = (CodeCell) shouldCell;
                cell.getOutputSources(new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        try {
                            JSONArray output = new JSONArray(value);
                            JSONObject object = new JSONObject();
                            object.put("input", cell.getInputSource());
                            object.put("output", output);
                            cells.put(object);

                            if (cells.length() == childCount) {
                                receiver.onReceiveValue(cells.toString(2));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    private void loadWorkspaceFile(Uri uri) {
        Log.v("MA", "load uri: " + uri.toString());
        setCurrentWorkspaceName(uri.getLastPathSegment());

        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }

            String json = stringBuilder.toString().replace("\\\\", "\\\\\\\\");
            loadWorkspace(json);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadWorkspace(String json) throws JSONException {
        JSONArray cells = new JSONArray(json);
        int cellCount = cells.length();

        cellsView.removeAllViews();
        Log.v("MA", "workspace cell count: " + cellCount);

        for (int i = 0; i < cellCount; i++) {
            final JSONObject object = cells.getJSONObject(i);
            final String input = object.getString("input");
            final JSONArray outputs = object.getJSONArray("output");

            final CodeCell cell = cellsView.addCell(input);
            final int outputCount = outputs.length();

            cell.outputWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    for (int j = 0; j < outputCount; j++) {
                        try {
                            String source = outputs.getString(j);
                            Log.v("MA", "raw: " + source);
                            final String appendOutput = "javascript:window.appendOutput(`" + source + "`)";
                            cell.outputWebView.loadUrl(appendOutput);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

        }
    }

}
