package org.maximandroid.cas;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.*;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.widget.*;
import android.view.*;

public class CodeCell extends LinearLayout {
    private int cellId;
    private MultiAutoCompleteTextView completeTextView;
    public WebView outputWebView;

    private LinearLayout outputContainer;
    private OnCellActionListener actionListener;

    private int actionButtonWidth;
    private int actionButtonHeight;

    public interface OnCellActionListener {
        void onRunClick(CodeCell cell, String code);

        void onDeleteClick(int cellId);
    }

    public CodeCell(Context context) {
        super(context);
        init(null);
    }

    public CodeCell(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CodeCell(Context context, int cellId, String initialContent, OnCellActionListener listener) {
        super(context);
        this.cellId = cellId;
        this.actionListener = listener;
        init(null);

        this.completeTextView.setText(initialContent);
    }

    private void init(AttributeSet attrs) {
        setOrientation(VERTICAL);
        setPadding(0, 0, 0, Metrics.padding);

        actionButtonWidth = Metrics.padding * 4;
        actionButtonHeight = (int) (Metrics.padding * 1.5);

        // Create input section
        createInputSection();

        // Create output container (initially empty)
        outputContainer = new LinearLayout(getContext());
        outputContainer.setOrientation(VERTICAL);
        outputContainer.setVisibility(GONE);
        outputContainer.setPadding(Metrics.padding, Metrics.padding, Metrics.padding, Metrics.padding / 2);
        addView(outputContainer);

        createOutputSection();
    }

    @SuppressLint("DefaultLocale")
    private void createInputSection() {
        LinearLayout inputContainer = new LinearLayout(getContext());
        inputContainer.setOrientation(VERTICAL);
        inputContainer.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));

        // Button container
        LinearLayout buttonContainer = new LinearLayout(getContext());
        buttonContainer.setOrientation(HORIZONTAL);
        buttonContainer.setPadding(Metrics.padding, Metrics.padding, Metrics.padding, Metrics.padding / 2);
        buttonContainer.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));

        // Cell number label
        TextView numberLabel = new TextView(getContext());
        numberLabel.setText(String.format("In[%d]:", cellId));
        numberLabel.setTextSize(14);
        numberLabel.setTextColor(Color.GRAY);
        numberLabel.setPadding(0, 0, Metrics.padding, 0);
        buttonContainer.addView(numberLabel);

        // Run button
        Button runButton = createActionButton("Run", 0xF01FBF23);
        runButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null) {
                    actionListener.onRunClick(CodeCell.this, completeTextView.getText().toString());
                }
            }
        });
        buttonContainer.addView(runButton);

        // Delete button
        Button deleteButton = createActionButton("Delete", 0xF0FB3438);
        deleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actionListener != null) {
                    actionListener.onDeleteClick(cellId);
                }
            }
        });
        buttonContainer.addView(deleteButton);

//        Button debugButton = createActionButton("Debug", 0xF09C33FE);

        // Input editor
        completeTextView = new MultiAutoCompleteTextView(getContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.MaximaCompletionList)
        );

        completeTextView.setTokenizer(new MaximaTokenizer());
        completeTextView.setAdapter(adapter);
        completeTextView.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));

        completeTextView.setTextSize(14);
        completeTextView.setTextColor(Color.BLACK);
        completeTextView.setBackgroundColor(Color.TRANSPARENT);
        completeTextView.setTypeface(Typeface.MONOSPACE);
        completeTextView.setHint("Enter code here ...");
        completeTextView.setHintTextColor(Color.LTGRAY);
        completeTextView.setPadding(Metrics.padding, Metrics.padding / 2, Metrics.padding, Metrics.padding);

        inputContainer.addView(buttonContainer);
        inputContainer.addView(completeTextView);
        addView(inputContainer);
    }

    @SuppressLint("ClickableViewAccessibility")
    private Button createActionButton(String text, int color) {
        final Button button = new Button(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                actionButtonWidth,
                actionButtonHeight
        );
        params.setMargins(0, 0, Metrics.padding, 0);
        button.setLayoutParams(params);
        button.setWidth(actionButtonWidth);
        button.setHeight(actionButtonHeight);
        button.setText(text);
        button.setTextSize(Globals.ACTION_TEXT_SIZE);
        button.setPadding(6, 6, 6, 6);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setTextColor(color);
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction();
                if (action == (MotionEvent.ACTION_UP)) {
                    button.setTextSize(Globals.ACTION_TEXT_SIZE);
                } else if (action == MotionEvent.ACTION_DOWN) {
                    button.setTextSize(Globals.ACTION_TEXT_SIZE_PRESSED);
                }
                return false;
            }
        });

        return button;
    }

    public void displayOutput(String result) {
        displayMaximaCmdResults(result);
    }

    @SuppressLint({"DefaultLocale", "SetJavaScriptEnabled"})
    public void createOutputSection() {
        outputContainer.removeAllViews();

        TextView outputLabel = new TextView(getContext());
        outputLabel.setText(String.format("Out[%d]:", cellId));
        outputLabel.setTextSize(14);
        outputLabel.setTextColor(Color.GRAY);
        outputContainer.addView(outputLabel);

        outputWebView = new WebView(getContext());
        outputWebView.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));

        WebView.setWebContentsDebuggingEnabled(true);
        outputWebView.setInitialScale((int) (100 * Settings.outputScale));
        outputWebView.getSettings().setJavaScriptEnabled(true);

        ((Activity) getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                outputWebView.loadUrl(Globals.maximaURL);
            }
        });

        outputContainer.addView(outputWebView);
        outputContainer.setVisibility(VISIBLE);
    }

    public String getInputSource() {
        Editable editable = completeTextView.getText();
        return Objects.isNull(editable) ? "" : editable.toString();
    }

    public void getOutputSources(ValueCallback<String> receiver) {
        outputWebView.evaluateJavascript("javascript:getSources()", receiver);
    }

    private void displayMaximaCmdResults(String resString) {
        Log.v("MoA cmd", resString);
        String[] resArray = resString.split("\\$\\$\\$\\$\\$\\$");
        for (int i = 0; i < resArray.length; i++) {
            if (i % 2 == 0) {
                /* normal text, as we are outside of $$$$$$...$$$$$$ */
            } else {
                /* tex commands, as we are inside of $$$$$$...$$$$$$ */
                String rawTeX = substCRinMBOX(resArray[i]);
                rawTeX = substitute(rawTeX, "\n", " \\\\\\\\ ");
                rawTeX = substituteMBoxVerb(rawTeX);

                final String updateMath = "javascript:updateMath('" + rawTeX + "')";
                ((Activity) getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        outputWebView.loadUrl(updateMath);
                    }
                });
            }
        }
    }

    private String substCRinMBOX(String str) {
        String resValue = "";
        String tmpValue = str;
        int p;
        while ((p = tmpValue.indexOf("mbox{")) != -1) {
            resValue = resValue + tmpValue.substring(0, p) + "mbox{";
            int p2 = tmpValue.indexOf("}", p + 5);
            assert (p2 > 0);
            String tmp2Value = tmpValue.substring(p + 5, p2);
            resValue = resValue
                    + substitute(tmp2Value, "\n", "}\\\\\\\\ \\\\mbox{");
            tmpValue = tmpValue.substring(p2, tmpValue.length());
        }
        resValue = resValue + tmpValue;
        return (resValue);
    }

    static private String substituteMBoxVerb(String rawTeX) {
        Pattern pat = Pattern.compile("\\\\\\\\mbox\\{\\\\\\\\verb\\|(.)\\|\\}");
        Matcher m = pat.matcher(rawTeX);
        StringBuffer sb = new StringBuffer();
        if (m.find()) {
            m.appendReplacement(sb, "\\\\\\\\text{$1");
            while (m.find()) {
                m.appendReplacement(sb, "$1");
            }
            m.appendTail(sb);
            return sb.toString() + "}";
        }
        return (rawTeX);
    }

    static private String substitute(String input, String pattern,
                                     String replacement) {
        int index = input.indexOf(pattern);

        if (index == -1) {
            return input;
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append(input.substring(0, index)).append(replacement);

        if (index + pattern.length() < input.length()) {
            String rest = input.substring(index + pattern.length());
            buffer.append(substitute(rest, pattern, replacement));
        }
        return buffer.toString();
    }

    public int getCellId() {
        return cellId;
    }
}
