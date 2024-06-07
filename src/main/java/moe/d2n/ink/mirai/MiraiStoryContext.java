package moe.d2n.ink.mirai;

import com.bladecoder.ink.runtime.Story;
import moe.d2n.ink.core.ChoiceException;
import moe.d2n.ink.core.Resource;
import moe.d2n.ink.core.StoryContext;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.utils.ExternalResource;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiraiStoryContext extends StoryContext {
    public static final long START_TIME = Instant.now().toEpochMilli();
    protected final Member self;
    protected MiraiStoryContext prevTargetContext;
    protected MessageChain prevMessage;
    protected Map<Long, Map<Long, MiraiStoryContext>> groupStoryMap = InkEngine.INSTANCE.groupStoryMap;

    public static final int ACTION_AT_SELF = 0b00000001;
    public static final int ACTION_AT_TARGET = 0b00000010;
    public static final int ACTION_SEND_IMG = 0b00000100;

    public static final String AT_SELF_TAG = "$AT_SELF";
    public static final String AT_TARGET_TAG = "$AT_TARGET";
    public static final String IMG_TAG_START = "$IMG[";
    public static final String IMG_TAG_END = "]";
    public static final String IMG_TAG_CACHE = "$CACHE:";

    public static final String IMG_TAG_REGEX = Pattern.quote(IMG_TAG_START) + "(.*?)" + Pattern.quote(IMG_TAG_END);

    public static final Pattern TAG_PATTERN = Pattern.compile(String.join("|", new String[]{
            Pattern.quote(AT_SELF_TAG), Pattern.quote(AT_TARGET_TAG), IMG_TAG_REGEX
    }));
    public static final int INIT_ACTION = 0b00000000;
    protected int action = INIT_ACTION;
    protected int date;

    final Object[] prevCall = new Object[3];


    public MiraiStoryContext(Story story, Member member) {
        super(story);
        self = member;
        try {
            var vars = story.getVariablesState();
            vars.set("self", member.getNick());
            vars.set("self_id", String.valueOf(member.getId()));
            vars.set("group", member.getGroup().getName());
            date = (int) TimeUnit.MILLISECONDS.toDays(Instant.now().toEpochMilli());
            initFunctions();
        } catch (Exception ignored) {
        }
    }

    public String getPrevText() {
        return super.prevText;
    }

    public void setPrevText(String text) {
        super.prevText = text;
    }


    private MessageChain parseMessage(String input) {
        if (action == INIT_ACTION) {
            return new MessageChainBuilder().append(input).asMessageChain();
        }

        Matcher matcher = TAG_PATTERN.matcher(input);
        MessageChainBuilder result = new MessageChainBuilder();

        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                result.append(input.substring(lastEnd, matcher.start()));
            }

            String match = matcher.group();
            if (match.startsWith(IMG_TAG_START) && match.endsWith(IMG_TAG_END)) {
                String link = matcher.group(1);
                try {
                    Image img = uploadImage(link);
                    result.append(img);
                } catch (IOException e) {
                    System.err.println("获取/上传图片失败: " + link);
                }
            } else if (match.equals(AT_SELF_TAG)) {
                result.append(new At(self.getId()));
            } else if (match.equals(AT_TARGET_TAG)) {
                result.append(new At(prevTargetContext.self.getId()));
            }

            lastEnd = matcher.end();
        }

        if (lastEnd < input.length()) {
            result.append(input.substring(lastEnd));
        }

        return result.asMessageChain();
    }

    public MessageChain getMessage() {
        if (prevMessage == null || story.canContinue()) {
            var msg = parseMessage(getText());
            Arrays.fill(prevCall, null);
            prevMessage = msg;
            return msg;
        }
        return prevMessage;
    }

    protected Image uploadImage(String link) throws IOException {
        Resource res;
        if (link.startsWith(IMG_TAG_CACHE)) {
            res = new Resource(link.substring(IMG_TAG_CACHE.length()), true);
        } else {
            res = new Resource(link, false);
        }
        try (var stream = res.openStream()) {
            try (ExternalResource resource = ExternalResource.create(stream)) {
                return self.getGroup().uploadImage(resource);
            }
        }
    }

    /**
     * @return 返回true则说明选择成功或无需选择
     */
    public boolean choose(int index, Member member) throws ChoiceException {
        index = index - 1;
        if (story.canContinue() || index == -1) {
            return true;
        }
        final boolean hasMemberChoicesFlag = !prevHasMemberChoicesIndexes.isEmpty() && prevHasMemberChoicesIndexes.contains(index);
        if (hasMemberChoicesFlag) {
            if (member == null) {
                if (prevTargetContext == null) {
                    throw new ChoiceException("没有指定目标, 请先At目标");
                }
                member = prevTargetContext.self;
            }
            Long groupId = member.getGroup().getId();
            Long userId = member.getId();
            final Member finalMember = member;
            var targetContext = Optional.ofNullable(groupStoryMap)
                    .map(map -> map.get(groupId))
                    .map(map -> map.get(userId))
                    .orElseGet(() -> InkEngine.INSTANCE.hasContext(finalMember) ?
                            InkEngine.INSTANCE.getContext(finalMember) : null
                    );
            if (targetContext == null) {
                if (MiraiPluginConfig.INSTANCE.getAutoCreateStory()) {
                    targetContext = InkEngine.INSTANCE.getContext(finalMember);
                } else {
                    throw new ChoiceException("目标没有创建角色");
                }
            }

            if (prevTargetContext == null || member != prevTargetContext.self) {
                try {
                    if (prevTargetContext == null) {
                        story.getVariablesState().set("has_target", true);
                    }
                    var nameCard = member.getNameCard();
                    story.getVariablesState().set("target", nameCard.isEmpty() ? member.getNick() : nameCard);
                    story.getVariablesState().set("target_id", String.valueOf(member.getId()));
                } catch (Exception ignored) {
                }
            }
            this.prevTargetContext = targetContext;
        }

        try {
            story.chooseChoiceIndex(index);
            return true;
        } catch (Exception e) {
            throw new ChoiceException("错误的序号");
        }
    }
    
    public MiraiStoryContext getPrevTargetContext() {
        if (prevTargetContext == null) {
            var vars = story.getVariablesState();
            String targetId = (String) vars.get("target_id");
            if (targetId.isEmpty()) {
                return null;
            }
            var member = self.getGroup().get(Long.parseLong(targetId));
            if (member == null) {
                return null;
            }
            return InkEngine.INSTANCE.getContext(member);
        }
        return prevTargetContext;
    }

    protected void initFunctions() throws Exception {
        var that = this;

        story.bindExternalFunction(
                "set_target",
                new Story.ExternalFunction1<String, Boolean>() {
                    @Override
                    public Boolean call(String id) {
                        try {
                            var member = that.self.getGroup().get(Long.parseLong(id));
                            if (member == null) return false;
                            that.prevTargetContext = InkEngine.INSTANCE.getContext(member);
                            return true;
                        } catch (Exception ignored) {
                            return false;
                        }
                    }
                }
        );

        story.bindExternalFunction(
                "get_name",
                new Story.ExternalFunction1<String, String>() {
                    @Override
                    public String call(String id) {
                        try {
                            var member = that.self.getGroup().get(Long.parseLong(id));
                            if (member == null) return "";
                            var nameCard = member.getNameCard();
                            return nameCard.isEmpty() ? member.getNick() : nameCard;
                        } catch (Exception ignored) {
                            return "";
                        }
                    }
                },
                true
        );

        story.bindExternalFunction(
                "get_timestamp",
                new Story.ExternalFunction0<Integer>() {
                    @Override
                    public Integer call() {
                        return (int) (Instant.now().toEpochMilli() - START_TIME / 1000);
                    }
                },
                true
        );

        story.bindExternalFunction(
                "teleport_target",
                new Story.ExternalFunction1<String, Boolean>() {
                    @Override
                    public Boolean call(String path) {
                        try {
                            prevTargetContext.story.choosePathString(path);
                            return true;
                        } catch (Exception ignored) {
                            return false;
                        }
                    }
                },
                true
        );

        story.bindExternalFunction(
                "cache_image",
                new Story.ExternalFunction2<String, Boolean, String>() {
                    protected String call(String s, Boolean cache) {
                        action |= ACTION_SEND_IMG;
                        return IMG_TAG_START + (cache ? IMG_TAG_CACHE : "") + s + IMG_TAG_END;
                    }
                },
                true
        );
        story.bindExternalFunction(
                "at_self",
                new Story.ExternalFunction0<String>() {
                    protected String call() {
                        action |= ACTION_AT_SELF;
                        return AT_SELF_TAG;
                    }
                },
                true
        );
        story.bindExternalFunction(
                "at_target",
                new Story.ExternalFunction0<String>() {
                    protected String call() {
                        action |= ACTION_AT_TARGET;
                        return AT_TARGET_TAG;
                    }
                },
                true
        );
        story.bindExternalFunction(
                "add_target_int_var",
                new Story.ExternalFunction2<String, Integer, Integer>() {
                    @Override
                    protected Integer call(String s, Integer i) {
                        var state = that.getPrevTargetContext().story.getVariablesState();
                        try {
                            int num = (Integer) state.get(s);
                            if (i == 0) return num;
                            that.getPrevTargetContext().story.getVariablesState().set(s, num + i);
                            return num + i;
                        } catch (Exception ignored) {
                            return i;
                        }
                    }
                }
        );

        story.bindExternalFunction(
                "add_target_float_var",
                new Story.ExternalFunction2<String, Float, Float>() {
                    @Override
                    protected Float call(String attr, Float num) {
                        var state = that.getPrevTargetContext().story.getVariablesState();
                        try {
                            float currentValue = (Float) state.get(attr);
                            state.set(attr, currentValue + num);
                            return currentValue + num;
                        } catch (Exception ignored) {
                            return num;
                        }
                    }
                }
        );

        story.bindExternalFunction(
                "get_target_int_var",
                new Story.ExternalFunction1<String, Integer>() {
                    @Override
                    public Integer call(String attr) {
                        var state = that.getPrevTargetContext().story.getVariablesState();
                        try {
                            return (Integer) state.get(attr);
                        } catch (Exception ignored) {
                            return 0;
                        }
                    }
                },
                true
        );

        story.bindExternalFunction(
                "get_target_float_var",
                new Story.ExternalFunction1<String, Float>() {
                    @Override
                    public Float call(String attr) {
                        var state = that.getPrevTargetContext().story.getVariablesState();
                        try {
                            return (Float) state.get(attr);
                        } catch (Exception ignored) {
                            return 0.0f;
                        }
                    }
                },
                true
        );

        story.bindExternalFunction(
                "get_target_string_var",
                new Story.ExternalFunction1<String, String>() {
                    @Override
                    public String call(String attr) {
                        var state = that.getPrevTargetContext().story.getVariablesState();
                        try {
                            return (String) state.get(attr);
                        } catch (Exception ignored) {
                            return "";
                        }
                    }
                },
                true
        );

        story.bindExternalFunction(
                "get_target_bool_var",
                new Story.ExternalFunction1<String, Boolean>() {
                    @Override
                    public Boolean call(String attr) {
                        var state = that.getPrevTargetContext().story.getVariablesState();
                        try {
                            return (Boolean) state.get(attr);
                        } catch (Exception ignored) {
                            return false;
                        }
                    }
                },
                true
        );

        story.bindExternalFunction(
                "set_target_int_var",
                new Story.ExternalFunction2<String, Integer, Integer>() {
                    @Override
                    protected Integer call(String attr, Integer num) {
                        var state = that.getPrevTargetContext().story.getVariablesState();
                        try {
                            int oldValue = (Integer) state.get(attr);
                            state.set(attr, num);
                            return oldValue;
                        } catch (Exception ignored) {
                            return 0;
                        }
                    }
                }
        );

        story.bindExternalFunction(
                "set_target_float_var",
                new Story.ExternalFunction2<String, Float, Float>() {
                    @Override
                    protected Float call(String attr, Float num) {
                        var state = that.getPrevTargetContext().story.getVariablesState();
                        try {
                            float oldValue = (Float) state.get(attr);
                            state.set(attr, num);
                            return oldValue;
                        } catch (Exception ignored) {
                            return 0.0f;
                        }
                    }
                }
        );

        story.bindExternalFunction(
                "set_target_string_var",
                new Story.ExternalFunction2<String, String, String>() {
                    @Override
                    protected String call(String attr, String str) {
                        var state = that.getPrevTargetContext().story.getVariablesState();
                        try {
                            String oldValue = (String) state.get(attr);
                            state.set(attr, str);
                            return oldValue;
                        } catch (Exception ignored) {
                            return "";
                        }
                    }
                }
        );

        story.bindExternalFunction(
                "set_target_bool_var",
                new Story.ExternalFunction2<String, Boolean, Boolean>() {
                    @Override
                    protected Boolean call(String attr, Boolean bool) {
                        var state = that.getPrevTargetContext().story.getVariablesState();
                        try {
                            Boolean oldValue = (Boolean) state.get(attr);
                            state.set(attr, bool);
                            return oldValue;
                        } catch (Exception ignored) {
                            return false;
                        }
                    }
                }
        );

        story.bindExternalFunction(
                "eval_target_function",
                param -> {
                    Object[] args = Arrays.copyOfRange(param, 1, param.length);
                    return that.getPrevTargetContext().story.evaluateFunction(
                            (String) param[0],
                            null,
                            args
                    );
                }
        );

        story.bindExternalFunction(
                "eval_target_function_safe",
                param -> {
                    Object[] args = Arrays.copyOfRange(param, 2, param.length);
                    if (prevCall[0] != null && prevCall[0].equals(param[0]) && prevCall[1] == param[1]) {
                        return prevCall[2];
                    } else {
                        prevCall[0] = param[0]; // function name
                        prevCall[1] = param[1]; // hash
                        var result = that.getPrevTargetContext().story.evaluateFunction(
                                (String) param[0],
                                null,
                                args
                        );
                        prevCall[2] = result;
                        return result;
                    }
                }
        );
    }
}
