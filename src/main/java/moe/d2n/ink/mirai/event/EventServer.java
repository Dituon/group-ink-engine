package moe.d2n.ink.mirai.event;

import moe.d2n.ink.mirai.InkEngine;
import moe.d2n.ink.mirai.MiraiPluginConfig;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.GroupMessageEvent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 事件服务
 *
 * @author Moyuyanli
 * @date 2024/6/18 9:29
 */
public class EventServer {

    private static EventServer instance;

    private final EventChannel<GroupMessageEvent> eventChannel;

    private EventServer(EventChannel<GroupMessageEvent> eventChannel) {
        this.eventChannel = eventChannel;
    }

    /**
     * 加载基本的事件监听器<p/>
     * 包含最简单的过滤信息
     */
    public static void loadingServer() {
        GlobalEventChannel globalEventChannel = GlobalEventChannel.INSTANCE;
        EventChannel<GroupMessageEvent> eventChannel = globalEventChannel.filterIsInstance(GroupMessageEvent.class);
        MiraiPluginConfig config = InkEngine.INSTANCE.config;
        eventChannel = eventChannel.filter(it -> config.getEnableGroup().contains(it.getGroup().getId()));
        instance = new EventServer(eventChannel);
    }

    /**
     * 注册消息
     */
    public static void registerEvent() {
        instance.eventChannel.registerListenerHost(new MessageEventListener());
    }


    /**
     * 获取用户的下一条群消息
     *
     * @param group 群
     * @param user  用户
     * @return 下一条消息事件
     */
    public static GroupMessageEvent nextUserMessageForGroup(Group group, User user) {
        return nextUserMessageForGroup(group.getId(), user.getId());
    }

    /**
     * 获取用户的下一条群消息
     *
     * @param groupId 群id
     * @param userId  用户id
     * @return 下一条消息事件
     */
    public static GroupMessageEvent nextUserMessageForGroup(Long groupId, Long userId) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<GroupMessageEvent> result = new AtomicReference<>();
        instance.eventChannel
                .filter(filter -> filter.getSubject().getId() == groupId && filter.getSender().getId() == userId)
                .subscribeOnce(GroupMessageEvent.class, event -> {
                    result.set(event);
                    latch.countDown();
                });
        try {
            if (latch.await(3, TimeUnit.MINUTES)) {
                return result.get();
            } else {
                InkEngine.log().debug("获取用户下一条消息超时");
                return null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("获取用户下一条消息失败");
        }
    }

}
