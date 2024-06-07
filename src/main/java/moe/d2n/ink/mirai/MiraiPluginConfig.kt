package moe.d2n.ink.mirai

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object MiraiPluginConfig : AutoSavePluginConfig("InkEngine") {
    @ValueDescription("编译后的 Ink 文件")
    val mainFile: String by value("main.ink.json")

    @ValueDescription("默认触发指令")
    val command: String by value("/ink")

    @ValueDescription("开发模式 使用 /reload 指令重新加载")
    val devMode: Boolean by value(false)

    @ValueDescription("角色不存在则自动创建角色")
    val autoCreateStory: Boolean by value(false)

    @ValueDescription("启用存档功能")
    val enableSave: Boolean by value(true)
}
