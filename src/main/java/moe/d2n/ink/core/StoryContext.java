package moe.d2n.ink.core;

import com.bladecoder.ink.runtime.Story;

import java.util.ArrayList;
import java.util.List;

public class StoryContext {
    protected List<Integer> prevHasMemberChoicesIndexes = new ArrayList<>(16);
    protected String prevText;
    public Story story;
    public int day = StoryTimer.INSTANCE.day;

    public StoryContext(Story story) {
        this.story = story;

        try {
            var vars = story.getVariablesState();
            vars.set("sys_date", day);
            story.bindExternalFunction(
                    "add_target_attr",
                    new Story.ExternalFunction2<String, Integer, Integer>() {
                        @Override
                        protected Integer call(String s, Integer integer) {
                            return integer;
                        }
                    }
            );
        } catch (Exception ignored) {
        }
    }

    public String getText() {
        if (prevText == null || story.canContinue()) {
            if (day != StoryTimer.INSTANCE.day) {
                try {
                    story.getVariablesState().set("sys_date", day);
                } catch (Exception ignored){
                }
            }
            var sb = getStoryNextLinesBuilder(story);
            var str = sb == null ? "" : sb.toString();
            prevText = str;
            return str;
        }
        return prevText;
    }

    public StringBuilder getStoryNextLinesBuilder(Story story) {
        if (!story.canContinue()) {
            return null;
        }
        var sb = new StringBuilder();
        try {
            while (story.canContinue()) {
                sb.append(story.Continue());
            }
            var choices = story.getCurrentChoices();
            if (choices.isEmpty()) return sb;
            sb.append("\n选项: ");
            prevHasMemberChoicesIndexes.clear();
            for (int index = 0; index < choices.size(); index++) {
                var c = choices.get(index).getText();
                if (c.contains("$member")) {
                    prevHasMemberChoicesIndexes.add(index);
                    c = c.replace("$member", "（选项+@ 某人以选择）");
                }
                sb.append('\n').append(index + 1).append(". ").append(c);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return sb;
    }

    public boolean choose(int index, String targetName) throws ChoiceException {
        index = index - 1;
        if (prevText == null) {
            return true;
        }
        if (!prevHasMemberChoicesIndexes.isEmpty() && prevHasMemberChoicesIndexes.contains(index)) {
            if (targetName == null) {
                throw new ChoiceException("未选择目标");
            }
            try {
                story.getVariablesState().set("target", targetName);
                story.getVariablesState().set("target_id", targetName);
            } catch (Exception ignored) {
            }
        }
        try {
            story.chooseChoiceIndex(index);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
