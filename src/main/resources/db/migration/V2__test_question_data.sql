-- 测试数据：题库（组卷测试用）
-- 5 道单选(难度1) + 3 道多选(难度2) + 10 道判断(难度1)，creator_id=1
-- 注意：数量与组卷测试 config 精确匹配（count 超过库存可测"题目不足"报错）

-- 单选题：type=1, difficulty=1, 每题 2 分
INSERT INTO `question` (`type`, `difficulty`, `category`, `title`, `options`, `answer`, `analysis`, `score`, `creator_id`) VALUES
(1, 1, 'Java基础', 'Java 中用于定义常量的关键字是？',
 '["static", "final", "const", "define"]', 'B',
 'final 修饰变量表示常量，赋值后不可修改；Java 中没有 const 和 define 关键字。', 2.00, 1),
(1, 1, 'Java基础', '下列哪个不是 Java 的基本数据类型？',
 '["int", "String", "double", "boolean"]', 'B',
 'String 是引用类型，其余均为基本类型。', 2.00, 1),
(1, 1, 'Java基础', 'Java 源文件编译后生成的字节码文件扩展名是？',
 '[".java", ".class", ".exe", ".jar"]', 'B',
 'javac 编译 .java 源文件生成 .class 字节码文件。', 2.00, 1),
(1, 1, 'Java基础', 'Java 中所有类的根父类是？',
 '["Object", "Class", "System", "Main"]', 'A',
 'java.lang.Object 是所有 Java 类的顶层父类。', 2.00, 1),
(1, 1, 'Java基础', '用于获取字符串长度的方法是？',
 '["size()", "length()", "count()", "getSize()"]', 'B',
 'String 的 length() 返回字符个数；size() 是集合的方法。', 2.00, 1),

-- 多选题：type=2, difficulty=2, 每题 4 分
(2, 2, 'Java基础', '下列关于 Java 面向对象特性的说法，正确的有？',
 '["封装是隐藏对象内部实现细节", "继承允许子类复用父类的属性和方法", "多态指同一方法调用在不同对象上有不同行为", "Java 支持多继承（一个类直接继承多个类）"]', 'ABC',
 '封装、继承、多态是面向对象三大特性；Java 类只支持单继承，多继承通过接口实现。', 4.00, 1),
(2, 2, 'Java集合', '下列关于 HashMap 的说法，正确的有？',
 '["允许 null 键和 null 值", "线程安全", "底层是数组+链表+红黑树", "元素无序"]', 'ACD',
 'HashMap 非线程安全，需要线程安全可用 ConcurrentHashMap。', 4.00, 1),
(2, 2, 'Java集合', '下列属于 Java 集合框架中的接口的有？',
 '["List", "Set", "Map", "Arrays"]', 'ABC',
 'Arrays 是工具类不是接口；List、Set、Map 都是集合接口。', 4.00, 1);

-- 判断题：type=3, difficulty=1, 每题 2 分
INSERT INTO `question` (`type`, `difficulty`, `category`, `title`, `answer`, `analysis`, `score`, `creator_id`) VALUES
(3, 1, 'Java基础', 'Java 是一门跨平台的语言，一次编译到处运行。', '对',
 'Java 编译为字节码，由各平台的 JVM 解释执行。', 2.00, 1),
(3, 1, 'Java基础', 'Java 中的接口可以包含构造方法。', '错',
 '接口不能有构造方法，也不能被实例化。', 2.00, 1),
(3, 1, 'Java基础', '抽象类中可以包含非抽象的普通方法。', '对',
 '抽象类可以同时包含抽象方法和普通方法。', 2.00, 1),
(3, 1, 'Java基础', 'Java 中 == 比较两个 String 的内容是否相同。', '错',
 '== 比较引用地址，内容比较要用 equals()。', 2.00, 1),
(3, 1, 'Java基础', 'static 修饰的成员属于类而不属于对象实例。', '对',
 '静态成员在类加载时初始化，被所有实例共享。', 2.00, 1),
(3, 1, 'Java基础', 'Java 的垃圾回收机制可以保证程序永远不会内存溢出。', '错',
 'GC 只回收不可达对象，存在引用泄漏或堆不足时仍会 OOM。', 2.00, 1),
(3, 1, 'Java基础', '一个 Java 源文件中最多只能有一个 public 类。', '对',
 '且 public 类名必须与文件名一致。', 2.00, 1),
(3, 1, 'Java基础', 'try 块中写了 return，finally 块就不会执行。', '错',
 'finally 块无论如何都会执行（除非 JVM 退出）。', 2.00, 1),
(3, 1, 'Java基础', 'Java 中数组下标从 1 开始。', '错',
 '数组下标从 0 开始，越界抛 ArrayIndexOutOfBoundsException。', 2.00, 1),
(3, 1, 'Java基础', 'Integer 的自动装箱会把 int 包装为 Integer 对象。', '对',
 '编译器自动调用 Integer.valueOf() 完成装箱。', 2.00, 1);
