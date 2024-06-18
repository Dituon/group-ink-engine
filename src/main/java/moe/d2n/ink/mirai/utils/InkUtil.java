package moe.d2n.ink.mirai.utils;

import com.bladecoder.ink.runtime.Story;
import moe.d2n.ink.mirai.InkEngine;

import static moe.d2n.ink.mirai.InkEngine.INK_SDK_VERSION;

/**
 * ink工具
 *
 * @author Moyuyanli
 * @date 2024/6/18 10:44
 */
public class InkUtil {

    private InkUtil() {
    }


    public static boolean checkSDKVersion(Story story) {
        int version = (Integer) story.getVariablesState().get("SDK_VERSION");
        boolean flag = version <= INK_SDK_VERSION;
        if (flag) {
            InkEngine.log().warning("当前插件版本过低，请更新至最新版本");
            InkEngine.log().warning("当前插件支持的SDK版本： " + INK_SDK_VERSION + " ，故事SDK版本： " + version);
        }
        return flag;
    }

}
