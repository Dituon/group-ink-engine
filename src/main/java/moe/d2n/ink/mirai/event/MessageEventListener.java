package moe.d2n.ink.mirai.event;

import com.bladecoder.ink.runtime.Story;
import moe.d2n.ink.core.ChoiceException;
import moe.d2n.ink.mirai.InkEngine;
import moe.d2n.ink.mirai.MiraiPluginConfig;
import moe.d2n.ink.mirai.MiraiStoryContext;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static moe.d2n.ink.mirai.utils.FileUtil.readStr;
import static moe.d2n.ink.mirai.utils.InkUtil.checkSDKVersion;

/**
 * 消息事件监听
 *
 * @author Moyuyanli
 * @date 2024/6/18 9:36
 */
public class MessageEventListener extends SimpleListenerHost {

    /**
     * 缓存
     */
    public Story story;
    /**
     * 群上下文
     */
    public Map<Long, Map<Long, MiraiStoryContext>> groupStoryMap = new HashMap<>();
    /**
     * 插件本体
     */
    public InkEngine inkEngine = InkEngine.INSTANCE;
    /**
     * 配置
     */
    private MiraiPluginConfig config = InkEngine.INSTANCE.config;


    /**
     * 群消息入口
     *
     * @param event 消息事件
     */
    @EventHandler
    public void messageEntry(GroupMessageEvent event) {

        String msg = event.getMessage().contentToString();
        Group group = event.getSubject();
        Member sender = event.getSender();

        /*
        1.匹配自定义指令
        2.匹配呼出信息
        后续操作通过 EventServer.nextUserMessageForGroup(x,x)持续监听
         */

        if (config.getDevMode() && msg.equals("/reload")) {
            try {
                story = new Story(readStr(
                        inkEngine.getDataFolderPath().resolve("data").resolve(config.getMainFile()).toFile()
                ));
                checkSDKVersion(story);
                groupStoryMap = new HashMap<>();
                event.getGroup().sendMessage("重载成功");
                return;
            } catch (Exception ignored) {
            }
        }

        if (!msg.equals(config.getCommand())) {
            return;
        }

        execution(event);

        while (true) {
            GroupMessageEvent e = EventServer.nextUserMessageForGroup(group, sender);
            if (Pattern.matches("\\d", e.getMessage().contentToString())) {
                execution(e);
            } else {
                return;
            }
        }
    }

    protected void execution(GroupMessageEvent event) {
        boolean hasChoose = false;
        int chooseNum = 0;

        At at = null;
        for (SingleMessage singleMessage : event.getMessage()) {
            if (singleMessage instanceof At) {
                at = (At) singleMessage;
            } else if (singleMessage instanceof PlainText) {
                var text = (PlainText) singleMessage;
                try {
                    chooseNum = Integer.parseInt(text.contentToString().trim());
                    hasChoose = true;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        NormalMember chooseTarget = null;
        if (at != null) {
            chooseTarget = event.getGroup().get(at.getTarget());
            hasChoose = chooseTarget != null;
        }

        if (hasChoose && chooseTarget != null && chooseTarget.getId() == event.getSender().getId()) {
            event.getGroup().sendMessage("你不能选择自己");
            return;
        }

        if (!hasChoose) {
            return;
        }

        var memberStory = inkEngine.getContext(event.getSender());

        try {
            if (memberStory.choose(chooseNum, chooseTarget)) {
                var smsg = memberStory.getMessage();
                var mb = new MessageChainBuilder();
                mb.append(MessageSource.quote(event.getMessage()));
                smsg.forEach(mb::append);
                event.getGroup().sendMessage(mb.asMessageChain());
            }
        } catch (ChoiceException ex) {
            event.getGroup().sendMessage(ex.getMessage());
        }
    }

}
