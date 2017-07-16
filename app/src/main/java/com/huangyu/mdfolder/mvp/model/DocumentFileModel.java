package com.huangyu.mdfolder.mvp.model;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.support.v4.provider.DocumentFile;

import com.huangyu.library.app.BaseApplication;
import com.huangyu.mdfolder.R;
import com.huangyu.mdfolder.utils.DocumentFileUtils;
import com.huangyu.mdfolder.utils.MimeTypeUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by huangyu on 2017/7/14.
 */

public class DocumentFileModel {

    /**
     * 获取文件列表
     *
     * @param uri 文件uri
     * @return
     */
    public DocumentFile[] getFileList(Uri uri) {
        try {
            Class<?> c = Class.forName("android.support.v4.provider.TreeDocumentFile");
            Constructor<?> constructor = c.getDeclaredConstructor(DocumentFile.class, Context.class, Uri.class);
            constructor.setAccessible(true);
            DocumentFile dir = (DocumentFile) constructor.newInstance(null, BaseApplication.getInstance().getApplicationContext(), uri);
            return dir.listFiles();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 打开文件
     *
     * @param context 上下文
     * @param file    文件
     * @return true/false
     */
    public boolean openFile(Context context, File file) {
        if (file != null && !file.isDirectory() && file.exists()) {
            try {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(Intent.ACTION_VIEW);
                Uri uri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    uri = FileProvider.getUriForFile(context, "com.huangyu.mdfolder.fileprovider", file);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    uri = Uri.fromFile(file);
                }
                intent.setDataAndType(uri, MimeTypeUtils.getMIMEType(file.getPath()));
                context.startActivity(intent);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * 分享文件
     *
     * @param context  上下文
     * @param fileList 文件列表
     * @return true/false
     */
    public boolean shareFile(Context context, List<File> fileList) {
        try {
            boolean multiple = fileList.size() > 1;
            Intent intent = new Intent(multiple ? android.content.Intent.ACTION_SEND_MULTIPLE : android.content.Intent.ACTION_SEND);
            if (multiple) {
                shareMultiFiles(context, intent, fileList);
            } else {
                shareSingleFile(context, intent, fileList.get(0));
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void shareSingleFile(Context context, Intent intent, File file) {
        String mimeType = MimeTypeUtils.getMIMEType(file.getPath());
        intent.setType(mimeType);
        Uri uri;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            uri = FileProvider.getUriForFile(context, "com.huangyu.mdfolder.fileprovider", file);
//            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        } else {
        uri = Uri.fromFile(file);
//        }
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.menu_share)));
    }

    private void shareMultiFiles(Context context, Intent intent, List<File> fileList) {
        ArrayList<Uri> uriArrayList = new ArrayList<>();
        for (File file : fileList) {
            Uri uri;
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                uri = FileProvider.getUriForFile(context, "com.huangyu.mdfolder.fileprovider", file);
//                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            } else {
            uri = Uri.fromFile(file);
//            }
            uriArrayList.add(uri);
        }
        intent.setType("*/*");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriArrayList);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.menu_share)));
    }

    public void inputStreamToFile(InputStream is, File file) throws IOException {
        OutputStream os = new FileOutputStream(file);
        int bytesRead;
        byte[] buffer = new byte[8192];
        while ((bytesRead = is.read(buffer, 0, 8192)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        os.close();
        is.close();
    }

    public boolean isFileExists(String path) {
        DocumentFile documentFile = DocumentFileUtils.getDocumentFile(new File(path), false);
        return documentFile != null && documentFile.exists();
    }

    public boolean isFolderExists(String path) {
        DocumentFile documentFile = DocumentFileUtils.getDocumentFile(new File(path), true);
        return documentFile != null && documentFile.exists();
    }

    public DocumentFile addFile(DocumentFile file, String mimeType, String displayName) {
        return file.createFile(mimeType, displayName);
    }

    public DocumentFile addFolder(DocumentFile file, String displayName) {
        return file.createDirectory(displayName);
    }

    public boolean deleteFile(DocumentFile file) {
        return file.delete();
    }

    public boolean moveFile(DocumentFile srcFile, DocumentFile destFile) {
        return copyOrMoveFile(srcFile, destFile, true);
    }

    public boolean moveFolder(DocumentFile srcDir, DocumentFile destDir) {
        return copyOrMoveDir(srcDir, destDir, true);
    }

    public boolean copyFile(DocumentFile srcFile, DocumentFile destFile) {
        return copyOrMoveFile(srcFile, destFile, false);
    }

    public boolean copyFolder(DocumentFile srcDir, DocumentFile destDir) {
        return copyOrMoveDir(srcDir, destDir, false);
    }

    public boolean renameFile(DocumentFile file, String newName) {
        return file.renameTo(newName);
    }

    public boolean hideFile(DocumentFile file, String newName) {
        return file.renameTo("." + newName);
    }

    public boolean showFile(DocumentFile file, String newName) {
        return file.renameTo(newName.replaceFirst("\\.", ""));
    }

    private boolean copyOrMoveDir(DocumentFile srcDir, DocumentFile destDir, boolean isMove) {
        if (srcDir == null || destDir == null) return false;
        String srcPath = DocumentFileUtils.getPath(srcDir.getUri()) + File.separator;
        String destPath = DocumentFileUtils.getPath(destDir.getUri()) + File.separator;
        if (destPath.contains(srcPath)) return false;
        if (!srcDir.exists() || !srcDir.isDirectory()) return false;
        if (!createOrExistsDir(new File(destPath))) return false;
        DocumentFile[] files = srcDir.listFiles();
        for (DocumentFile file : files) {
            File oneDestFile = new File(destPath + file.getName());
            DocumentFile destFile;
            if (file.isFile()) {
                destFile = DocumentFileUtils.getDocumentFile(oneDestFile, false);
                if (!copyOrMoveFile(file, destFile, isMove)) return false;
            } else if (file.isDirectory()) {
                destFile = DocumentFile.fromFile(oneDestFile);
                if (!copyOrMoveDir(file, destFile, isMove)) return false;
            }
        }
        return !isMove || srcDir.delete();
    }

    private boolean copyOrMoveFile(DocumentFile srcFile, DocumentFile destFile, boolean isMove) {
        if (srcFile == null || destFile == null) return false;
        if (!srcFile.exists() || !srcFile.isFile()) return false;
//        if (destFile.exists() && destFile.isFile()) return false;
//        if (!createOrExistsDir(destFile.getParentFile())) return false;
        return toMoveFile(srcFile, destFile, isMove);
    }

    private boolean toMoveFile(DocumentFile srcFile, DocumentFile destFile, boolean isMove) {
        if (srcFile.isFile()) {
            copy(srcFile, destFile);
//                ContentResolver resolver = BaseApplication.getInstance().getApplicationContext().getContentResolver();
//                DocumentsContract.moveDocument(resolver, srcFile.getUri(), srcFile.getParentFile().getUri(), destFile.getUri());
            return !isMove || srcFile.delete();
        } else {
            return false;
        }
    }

    private boolean copy(DocumentFile srcFile, DocumentFile destFile) {
        InputStream inStream = null;
        OutputStream outStream = null;
        try {
            ContentResolver contentResolver = BaseApplication.getInstance().getApplicationContext().getContentResolver();
            inStream = contentResolver.openInputStream(srcFile.getUri());
            outStream = contentResolver.openOutputStream(destFile.getUri());
            byte[] buffer = new byte[16384];
            int bytesRead;
            while ((bytesRead = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inStream.close();
                outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean createOrExistsDir(File file) {
        if (file != null) {
            if (file.exists()) {
                return file.isDirectory();
            } else {
                DocumentFileUtils.getDocumentFile(file, true);
            }
            return true;
        }
        return false;
    }

}
