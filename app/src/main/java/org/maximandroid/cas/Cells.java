package org.maximandroid.cas;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class Cells extends LinearLayout {
    private int cellCount = 0;
    private OnRunClickListener runClickListener;
    private OnAddClickListener addClickListener;

    public interface OnRunClickListener {
        void onRunClick(CodeCell cell, String code);
    }

    public interface OnAddClickListener {
        void onAddClick();
    }

    public Cells(Context context) {
        super(context);
        init();
    }

    public Cells(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setBackgroundColor(Color.WHITE);
    }

    public void setOnRunClickListener(OnRunClickListener listener) {
        this.runClickListener = listener;
    }

    public void setOnAddClickListener(OnAddClickListener listener) {
        this.addClickListener = listener;
    }

    public CodeCell addCell(String initialContent) {
        final int currentId = ++cellCount;
        CodeCell codeCell = new CodeCell(getContext(), currentId, initialContent, new CodeCell.OnCellActionListener() {
            @Override
            public void onRunClick(CodeCell cell, String code) {
                if (runClickListener != null) {
                    runClickListener.onRunClick(cell, code);
                }
            }
            @Override
            public void onDeleteClick(int cellId) {
                removeCell(cellId);
            }
        });
        addView(codeCell);
        return codeCell;
    }

    public void removeCell(int cellId) {
        CodeCell cell = findCellById(cellId);
        if (cell != null) {
            removeView(cell);
        }
    }

    @Override
    public void removeAllViews() {
        super.removeAllViews();
        this.cellCount = 0;
    }

    public CodeCell findCellById(int cellId) {
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i) instanceof CodeCell) {
                CodeCell cell = (CodeCell) getChildAt(i);
                if (cell.getCellId() == cellId) {
                    return cell;
                }
            }
        }
        return null;
    }
}
