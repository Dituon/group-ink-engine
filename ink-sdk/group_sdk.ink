VAR SDK_VERSION = 2

/**
在运行时会被动态替换为当前系统日期，只读
*/
VAR sys_date = 0

/**
运行时动态替换为玩家昵称，只读
*/
VAR self = "SELF"

/**
运行时动态替换为玩家ID (通常为QQ号)，只读
*/
VAR self_id = "0"

/**
在运行时会被动态替换为当前选中的目标昵称，只读
*/
VAR target = "某人"
VAR target_id = "0"
VAR has_target = false

/**
在运行时会被动态替换为当前群名，只读
*/
VAR group = "GROUP"

/**
将玩家ID设置为目标，如果没有创建故事则新建
返回: bool 是否成功
*/
EXTERNAL set_target(id)

===function set_target(id)===
~ return true


/**
根据玩家ID获取昵称
lookaheadSafe
返回: string
*/
EXTERNAL get_name(id)

===function get_name(id)===
~ return "player"

/**
At玩家
lookaheadSafe
返回: 用于占位的字符串, 在运行时替换为At
*/
EXTERNAL at_self()

===function at_self()===
~ return "@" + self

/**
At目标
lookaheadSafe
返回: 用于占位的字符串, 在运行时替换为At
*/
EXTERNAL at_target()

===function at_target()===
~ return "@" + target

/**
插入图片
href: 图片链接或路径, cache: 是否缓存
lookaheadSafe
返回: 用于占位的字符串, 在运行时替换为图像
*/
EXTERNAL cache_image(href, cache)

===function cache_image(href, cache)===
{
    - cache:
        ~ return "[缓存的图片: " + href + "]"
    - else:
        ~ return "[图片: " + href + "]"
}

===function image(href)===
~ return cache_image(href, true)

===function uncached_image(href)===
~ return cache_image(href, false)

/**
attr: string, num: int
将目标传送到某个场景
返回: bool 是否成功
示例: teleport_target("die")
*/
EXTERNAL teleport_target(path)

===function teleport_target(path)===
~ return true

/**
获取从程序启动到现在的时间戳, 单位为秒
lookaheadSafe
返回: int
*/
EXTERNAL get_timestamp()

===function get_timestamp()===
~ return 0

/**
attr: string, num: int
增加目标的某个number变量, 等同于 target.[attr] += num
返回: int 目标增加后的变量
示例: add_target_int_var("hp", -10)
*/
EXTERNAL add_target_int_var(attr, num)

===function add_target_int_var(attr, num)===
~ return num

/**
attr: string, num: float
增加目标的某个number变量, 等同于 target.[attr] += num
返回: float 目标增加后的变量
示例: add_target_float_var("hp", -10.5)
*/
EXTERNAL add_target_float_var(attr, num)

===function add_target_float_var(attr, num)===
~ return num

/**
attr: string
lookaheadSafe
获取目标的某个变量, 等同于 target.[attr]
返回: int
示例: get_target_int_var("hp")
*/
EXTERNAL get_target_int_var(attr)

===function get_target_int_var(attr)===
~ return 0

/**
attr: string
lookaheadSafe
获取目标的某个变量, 等同于 target.[attr]
返回: float
示例: get_target_float_var("hp)
*/
EXTERNAL get_target_float_var(attr)

===function get_target_float_var(attr)===
~ return 0.0

/**
attr: string
lookaheadSafe
获取目标的某个变量, 等同于 target.[attr]
返回: string
示例: get_target_string_var(attr)
*/
EXTERNAL get_target_string_var(attr)

===function get_target_string_var(attr)===
~ return "$" + attr

/**
attr: string
lookaheadSafe
获取目标的某个变量, 等同于 target.[attr]
返回: bool
示例: get_target_bool_var(attr)
*/
EXTERNAL get_target_bool_var(attr)

===function get_target_bool_var(attr)===
~ return false

/**
attr: string, num: int
设置目标的某个number变量, 等同于 target.[attr] = num
返回: int 目标改变前的变量
示例: set_target_int_var("hp", 100)
*/
EXTERNAL set_target_int_var(attr, num)

===function set_target_int_var(attr, num)===
~ return 0

/**
attr: string, num: float
设置目标的某个number变量, 等同于 target.[attr] = num
返回: float 目标改变前的变量
示例: set_target_float_var("hp", 100.5)
*/
EXTERNAL set_target_float_var(attr, num)

===function set_target_float_var(attr, num)===
~ return 0.0

/**
attr: string, str: string
设置目标的某个string变量, 等同于 target.[attr] = str
返回: string 目标改变前的变量
示例: set_target_string_var("name", "张三")
*/
EXTERNAL set_target_string_var(attr, str)

===function set_target_string_var(attr, str)===
~ return "$" + attr

/**
attr: string, b: bool
设置目标的某个bool变量, 等同于 target.[attr] = b
返回: bool 目标改变前的变量
示例: set_target_bool_var("is_alive", true)
*/
EXTERNAL set_target_bool_var(attr, b)

===function set_target_bool_var(attr, b)===
~ return false

/**
fun_name: string, ...params: any
执行某个函数, 等同于 target.[fun_name](...params)
返回: any 目标函数的返回值
示例: eval_target_function("add_hp", 1)
*/
EXTERNAL eval_target_function(fun_name, p0)

===function eval_target_function(fun_name, p0)====
~ return p0

/**
fun_name: string, hash: int, ...params: any
执行某个函数, 等同于 target.[fun_name](...params)
在引擎中会根据hash防止前瞻执行
返回: any 目标函数的返回值
示例: eval_target_function("add_hp", 0, 1)
*/
EXTERNAL eval_target_function_safe(fun_name, hash, p0)

===function eval_target_function_safe(fun_name, hash, p0)====
~ return p0


