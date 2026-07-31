package com.oxohang.fanfreeform.diagnostics;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

public final class CompatibilityReportProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override public String getType(Uri uri) { return "text/plain"; }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode) || !CompatibilityReport.FILE_NAME.equals(
                uri.getLastPathSegment()) || getContext() == null) {
            throw new FileNotFoundException("Unknown compatibility report");
        }
        File report = new File(new File(getContext().getCacheDir(), "reports"),
                CompatibilityReport.FILE_NAME);
        return ParcelFileDescriptor.open(report, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        String[] columns = projection == null
                ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
                : projection;
        MatrixCursor cursor = new MatrixCursor(columns, 1);
        MatrixCursor.RowBuilder row = cursor.newRow();
        File report = getContext() == null ? null
                : new File(new File(getContext().getCacheDir(), "reports"),
                CompatibilityReport.FILE_NAME);
        for (String column : columns) {
            if (OpenableColumns.DISPLAY_NAME.equals(column)) row.add(column,
                    "HyperGesture-compatibility-report.txt");
            else if (OpenableColumns.SIZE.equals(column)) row.add(column,
                    report == null ? 0L : report.length());
            else row.add(column, null);
        }
        return cursor;
    }

    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
    @Override public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }
    @Override public int update(Uri uri, ContentValues values, String selection,
                                String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
