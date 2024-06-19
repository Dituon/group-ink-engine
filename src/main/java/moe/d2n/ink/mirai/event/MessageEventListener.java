package moe.d2n.ink.mirai.event;

import com.bladecoder.ink.runtime.Story;
import moe.d2n.ink.core.ChoiceException;
import moe.d2n.ink.mirai.InkEngine;
import moe.d2n.ink.mirai.MiraiPluginConfig;
import moe.d2n.ink.mirai.MiraiStoryContext;
import moe.d2n.ink.mirai.TriggerType;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
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

        MessageChain message = event.getMessage();
        String msg = message.contentToString();
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

        if (msg.equals(config.getCommand())) {
            execution(event);
            if (config.getTriggerType() == TriggerType.AWAKEN) {
                while (true) {
                    GroupMessageEvent e = EventServer.nextUserMessageForGroup(group, sender);
                    if (e != null && Pattern.compile("\\d").matcher(e.getMessage().contentToString()).find()) {
                        execution(e);
                    } else {
                        return;
                    }
                }
            }
        }

        if (config.getTriggerType() == TriggerType.QUOTE) {
            if (!message.contains(QuoteReply.Key)) {
                return;
            }
            QuoteReply quoteReply = message.get(QuoteReply.Key);
            MessageSource source;
            if (quoteReply != null) {
                source = quoteReply.getSource();
            } else {
                return;
            }
            if (source.getBotId() == source.getFromId()) {
                if (Pattern.compile("\\d").matcher(event.getMessage().contentToString()).find()) {
                    execution(event);
                }
            }
        }

    }

    protected void execution(GroupMessageEvent event) {
        MessageChain message = event.getMessage();
        Member sender = event.getSender();

        int chooseNum = 0;

        At at;
        NormalMember chooseTarget = null;

        for (SingleMessage singleMessage : message) {
            if (singleMessage instanceof At) {
                at = (At) singleMessage;
                chooseTarget = event.getGroup().get(at.getTarget());
                if (chooseTarget == null) {
                    return;
                } else {
                    if (chooseTarget.getId() == event.getBot().getId()) {
                        chooseTarget = null;
                        continue;
                    }
                    if (chooseTarget.getId() == sender.getId()) {
                        event.getGroup().sendMessage("你不能选择自己");
                        return;
                    }
                }
            } else {
                Pattern compile = Pattern.compile("^\\d");
                Matcher matcher = compile.matcher(singleMessage.contentToString().trim());

                if (matcher.find()) {
                    chooseNum = Integer.parseInt(matcher.group());
                }
            }
        }

        var memberStory = inkEngine.getContext(sender);

        try {
            inkEngine.getLogger().info(String.format("%s -> %s", sender.getId(), chooseNum));
            if (memberStory.choose(chooseNum, chooseTarget)) {
                var mb = new MessageChainBuilder();
                mb.append(MessageSource.quote(message));
                mb.append(memberStory.getMessage());
                event.getGroup().sendMessage(mb.build());
            }
        } catch (ChoiceException ex) {
            event.getGroup().sendMessage(ex.getMessage());
        }
    }

}
