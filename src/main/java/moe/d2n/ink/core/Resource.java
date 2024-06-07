package moe.d2n.ink.core;

import moe.d2n.ink.mirai.InkEngine;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

public class Resource {
    private final boolean cache;
    protected File file;
    protected URL url;

    public Resource(String pathOrUrl, boolean cache) throws FileNotFoundException {
        this.cache = cache;
        if (pathOrUrl == null || pathOrUrl.isEmpty()) {
            throw new IllegalArgumentException("路径或网址不能为空");
        }
        try {
            url = new URL(pathOrUrl);
        } catch (MalformedURLException e) {
            // 解析为本地文件路径
            File file = new File(pathOrUrl);
            if (!file.exists()) {
                throw new FileNotFoundException("文件不存在");
            }
        }
    }

    // 下载二进制文件到指定路径
    public void download(String outputFilePath) throws IOException {
        try (InputStream inputStream = openStream();
             OutputStream outputStream = new FileOutputStream(outputFilePath)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private void saveFileFromUrl(URL url, File destination) throws IOException {
        try (InputStream inputStream = url.openStream();
             FileOutputStream outputStream = new FileOutputStream(destination)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    public InputStream openStream() throws IOException {
        if (url != null) {
            if (cache && InkEngine.INSTANCE.imageCachePath != null) {
                File cachedFile = new File(InkEngine.INSTANCE.imageCachePath.toFile(), url.getPath());
                if (!cachedFile.exists()) {
                    saveFileFromUrl(url, cachedFile);
                }
                return new FileInputStream(cachedFile);
            }
            return url.openStream();
        } else if (file != null) {
            return new FileInputStream(file);
        } else {
            throw new RuntimeException("url and file both are null");
        }
    }
}
