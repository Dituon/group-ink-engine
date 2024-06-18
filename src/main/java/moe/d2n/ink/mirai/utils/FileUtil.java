package moe.d2n.ink.mirai.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 文件工具
 *
 * @author Moyuyanli
 * @date 2024/6/18 10:42
 */
public class FileUtil {

    private FileUtil() {
    }

    /**
     * 读取文件
     *
     * @param file 文件
     * @return 文件内容
     * @throws IOException io异常
     */
    public static String readStr(File file) throws IOException {
        var result = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        if (result.startsWith("\uFEFF")) {
            result = result.substring(1);
        }
        return result;
    }


}
