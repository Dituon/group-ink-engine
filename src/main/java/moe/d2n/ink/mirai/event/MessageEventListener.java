package moe.d2n.ink.mirai.event;

import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.GroupMessageEvent;

/**
 * 消息事件监听
 *
 * @author Moyuyanli
 * @date 2024/6/18 9:36
 */
public class MessageEventListener extends SimpleListenerHost {


    /**
     * 群消息入口
     * @param event 消息事件
     */
    @EventHandler
    public void messageEntry(GroupMessageEvent event) {

    }
}
