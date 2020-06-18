package com.getcapacitor.plugin;

import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@NativePlugin()
public class Share extends Plugin {

  @PluginMethod()
  public void share(PluginCall call) {
    String title = call.getString("title", "");
    String text = call.getString("text");
    String base64Filename = call.getString("base64Filename");
    String base64Data = call.getString("base64Data");
    String url = call.getString("url");
    String dialogTitle = call.getString("dialogTitle", "Share");

    if (text == null && url == null && base64Data == null) {
      call.error("Must provide a URL, message or file");
      return;
    }

    Intent intent = new Intent(Intent.ACTION_SEND);
    String type = "text/plain";

    if(base64Data != null) {
      // save cachedFile to cache dir
      if(base64Filename == null) {
        base64Filename = "file";
      }
      File cachedFile = new File(getCacheDir(), base64Filename);
      try (FileOutputStream fos = new FileOutputStream(cachedFile)) {
        byte[] decodedData = Base64.decode(base64Data, Base64.DEFAULT);
        fos.write(decodedData);
        fos.flush();
      } catch (IOException e) {
        call.reject("Failed to cache file");
        return;
      }
      Uri contentUri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", cachedFile);
      intent.putExtra(Intent.EXTRA_STREAM, contentUri);
    }

    if (text != null) {
      // If they supplied both text and url fields, concat em
      if (url != null && url.startsWith("http")) {
        text = text + " " + url;
      }
      intent.putExtra(Intent.EXTRA_TEXT, text);
    } else if (url != null) {
      if (url.startsWith("http")) {
        intent.putExtra(Intent.EXTRA_TEXT, url);
      } else if (url.startsWith("file:")) {
        type = getMimeType(url);
        Uri fileUrl = FileProvider.getUriForFile(getActivity(), getContext().getPackageName() + ".fileprovider", new File(Uri.parse(url).getPath()));
        intent.putExtra(Intent.EXTRA_STREAM, fileUrl);
      } else {
        call.error("Unsupported url");
        return;
      }
    }
    intent.setTypeAndNormalize(type);

    if (title != null) {
      intent.putExtra(Intent.EXTRA_SUBJECT, title);
    }

    Intent chooser = Intent.createChooser(intent, dialogTitle);
    chooser.addCategory(Intent.CATEGORY_DEFAULT);

    getActivity().startActivity(chooser);
    call.success();
  }

  private String getMimeType(String url) {
    String type = null;
    String extension = MimeTypeMap.getFileExtensionFromUrl(url);
    if (extension != null) {
      type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }
    return type;
  }

  private File getCacheDir() {
    File cacheDir = new File(getContext().getFilesDir(), "capfilesharer");
    if (!cacheDir.exists()) {
      cacheDir.mkdirs();
    } else {
      for (File f : cacheDir.listFiles()) {
        f.delete();
      }
    }
    return cacheDir;
  }
}
