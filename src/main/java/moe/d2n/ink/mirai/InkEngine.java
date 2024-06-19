package moe.d2n.ink.mirai;


import com.bladecoder.ink.runtime.Story;
import moe.d2n.ink.mirai.event.EventServer;
import moe.d2n.ink.mirai.utils.InkUtil;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.utils.MiraiLogger;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static moe.d2n.ink.mirai.utils.FileUtil.readStr;

public class InkEngine extends JavaPlugin {
    public static final InkEngine INSTANCE = new InkEngine();
    public static final String VERSION = "0.1.0";
    public static final int INK_SDK_VERSION = 1;
    public Story story;
    public Map<Long, Map<Long, MiraiStoryContext>> groupStoryMap = new HashMap<>();
    protected final Path savesPath;
    public Path imageCachePath;
    public MiraiPluginConfig config = MiraiPluginConfig.INSTANCE;

    private InkEngine() {
        super(new JvmPluginDescriptionBuilder("moe.d2n.ink", VERSION)
                .name("InkEngine")
                .author("Dituon")
                .build());
        savesPath = getDataFolderPath().resolve("saves");
    }

    @Override
    public void onEnable() {
        reloadPluginConfig(MiraiPluginConfig.INSTANCE);
        try {
            var mainFile = getDataFolderPath().resolve("data").resolve(config.getMainFile()).toFile();
            if (!mainFile.exists()) {
                if (!mainFile.mkdirs()) {
                    throw new RuntimeException("故事路径创建失败，请手动创建!");
                }
                var defaultMainFile = "main.ink.json";
                try (var in = getResourceAsStream(defaultMainFile)) {
                    assert in != null;
                    Files.copy(in, mainFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            story = new Story(readStr(mainFile));
            InkUtil.checkSDKVersion(story);
        } catch (IOException e) {
            getLogger().error("读取文件失败");
            throw new RuntimeException(e);
        } catch (Exception e) {
            getLogger().error("文件解析失败");
            throw new RuntimeException(e);
        }
        ZoneId zoneId = ZoneId.systemDefault();
        getLogger().info("当前时区：" + zoneId);

        imageCachePath = getDataFolderPath().resolve("cache").toAbsolutePath();
        if (!Files.exists(imageCachePath)) {
            try {
                Files.createDirectories(imageCachePath);
            } catch (IOException e) {
                getLogger().error("创建缓存目录失败");
            }
        }

        getLogger().info("\u001B[95m\n\n" +
                "▀██▀          ▀██         ▀██▀▀▀▀█                    ██                   \n" +
                " ██  ▄▄ ▄▄▄    ██  ▄▄      ██  ▄    ▄▄ ▄▄▄     ▄▄▄ ▄ ▄▄▄  ▄▄ ▄▄▄     ▄▄▄▄  \n" +
                " ██   ██  ██   ██ ▄▀       ██▀▀█     ██  ██   ██ ██   ██   ██  ██  ▄█▄▄▄██ \n" +
                " ██   ██  ██   ██▀█▄       ██        ██  ██    █▀▀    ██   ██  ██  ██      \n" +
                "▄██▄ ▄██▄ ██▄ ▄██▄ ██▄    ▄██▄▄▄▄▄█ ▄██▄ ██▄  ▀████▄ ▄██▄ ▄██▄ ██▄  ▀█▄▄▄▀ \n" +
                "                                             ▄█▄▄▄▄▀                       \n" +
                "                                                                           " +
                "v" + VERSION + "\n");


        EventServer.loadingServer();
        EventServer.registerEvent();
    }

    public Story cloneStory() {
        var clonedStory = new Story(
                story.getMainContentContainer(),
                story.getListDefinitions().getLists()
        );
        try {
            clonedStory.resetState();
        } catch (Exception ex) {
            getLogger().error("重置状态失败", ex);
        }
        return clonedStory;
    }

    /**
     * 判断用户是否已有存档上下文
     */
    public boolean hasContext(Member m) {
        var groupId = m.getGroup().getId();
        if (!groupStoryMap.containsKey(groupId)) {
            return Files.exists(getSavePath(m));
        }
        if (groupStoryMap.get(groupId).containsKey(m.getId())) {
            return true;
        }
        return Files.exists(getSavePath(m));
    }

    protected Path getSavePath(Member m) {
        return savesPath.resolve(String.valueOf(m.getGroup().getId())).resolve(m.getId() + ".json");
    }

    /**
     * 返回现有或创建新的存档上下文
     */
    public MiraiStoryContext getContext(Member m) {
        var groupId = m.getGroup().getId();
        if (!groupStoryMap.containsKey(groupId)) {
            groupStoryMap.put(groupId, new HashMap<>(m.getGroup().getMembers().size()));
        }

        var storyMap = groupStoryMap.get(groupId);

        var senderId = m.getId();
        return storyMap.computeIfAbsent(senderId, id -> {
            Story newStory = cloneStory();
            if (!config.getEnableSave()) {
                return new MiraiStoryContext(newStory, m);
            }
            var base = savesPath.resolve(String.valueOf(groupId));
            var stateFile = base.resolve(id + ".json").toFile();
            if (stateFile.exists()) {
                try {
                    newStory.getState().loadJson(readStr(stateFile));
                } catch (Exception ex) {
                    getLogger().error("读取存档文件失败", ex);
                }
            }

            var context = new MiraiStoryContext(newStory, m);
            var textPath = base.resolve(id + ".txt");
            if (Files.exists(textPath)) {
                try {
                    context.setPrevText(
                            Files.readString(textPath, StandardCharsets.UTF_8)
                    );
                } catch (IOException ignored) {
                }
            }
            return context;
        });
    }

    public void saveAll() {
        if (!config.getEnableSave()) {
            return;
        }
        groupStoryMap.forEach((groupId, memberMap) -> memberMap.forEach((memberId, context) -> {
            try {
                // save state
                var base = savesPath.resolve(String.valueOf(groupId));
                var savePath = base.resolve(memberId + ".json");
                if (!Files.exists(savePath)) {
                    Files.createDirectories(savePath.getParent());
                    Files.createFile(savePath);
                    getLogger().info("新增 " + memberId + " 存档");
                }
                var json = context.story.getState().toJson();
                Files.write(savePath, json.getBytes(StandardCharsets.UTF_8));

                // save text
                if (context.getPrevText() == null || context.getPrevText().isEmpty()) return;
                savePath = base.resolve(memberId + ".txt");
                if (!Files.exists(savePath)) {
                    Files.createDirectories(savePath.getParent());
                    Files.createFile(savePath);
                }
                Files.write(savePath, context.getPrevText().getBytes(StandardCharsets.UTF_8));
            } catch (Exception ex) {
                getLogger().warning("保存失败", ex);
            }
        }));
    }

    public static MiraiLogger log() {
        return INSTANCE.getLogger();
    }

    @Override
    public void onDisable() {
        saveAll();
    }
}
