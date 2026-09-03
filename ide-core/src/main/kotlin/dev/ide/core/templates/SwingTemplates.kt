package dev.ide.core.templates

import dev.ide.model.template.ProjectScaffold
import dev.ide.model.template.ProjectTemplate
import dev.ide.model.template.TemplateArgs
import dev.ide.model.template.TemplateCategory
import dev.ide.model.template.TemplateId
import dev.ide.model.template.TemplateParameter

/**
 * 面向 Java 桌面 UI 程序的"新建项目"模板。
 *
 * 两者生成的都是基于 `java.awt`/`javax.swing` 编写的普通 Swing 代码，不涉及 IDE 如何运行
 * 它：在桌面上由宿主 JDK 自带的 Swing 运行，在设备上则把程序的引用重新映射到自带的
 * `:awt-toolkit` 上。无论哪种方式代码都是相同的，这正是要点所在。
 *
 * 它们只使用自带工具包所实现的控件与布局管理器（`JFrame`、`JPanel`、`JLabel`、
 * `JButton`、`BorderLayout`/`FlowLayout`/`GridLayout`、`paintComponent`、动作与鼠标监听器），
 * 因此生成的项目在两种环境下行为一致。任何更复杂的用法都能在桌面运行但在设备上失败，
 * 那比干脆不提供更糟糕。
 */
internal object SwingTemplateSupport {
    /** 每个 Swing 模板都会向单个 `app` 模块写入一个可运行的类。 */
    fun swingApp(scaffold: ProjectScaffold, args: TemplateArgs, className: String, source: String) {
        JavaTemplateSupport.singleModule(scaffold, args.name, "app", "java-lib")
        scaffold.writeText(
            "app/src/main/java/${JavaTemplateSupport.pkgPath(args.packageName)}/$className.java",
            source,
        )
    }
}

/**
 * 一个带有标签和按钮的窗口：这是最小的、仍然算得上是一个程序的 Swing 程序，也几乎是
 * 每个教程开头都会展示的例子。
 */
object SwingAppTemplate : ProjectTemplate {
    override val id = TemplateId("swing-app")
    override val displayName = "Swing 桌面应用"
    override val description = "一个带窗口的 Java 应用：包含一个标签和一个可响应点击的按钮的窗口。"
    override val category = TemplateCategory.JAVA
    override val iconId = "java"

    override fun parameters(): List<TemplateParameter> = emptyList()

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        val pkg = args.packageName
        SwingTemplateSupport.swingApp(
            scaffold, args, "MainWindow",
            """
            package $pkg;

            import java.awt.BorderLayout;
            import java.awt.Color;
            import java.awt.Font;
            import javax.swing.JButton;
            import javax.swing.JFrame;
            import javax.swing.JLabel;
            import javax.swing.JPanel;
            import javax.swing.WindowConstants;

            /** ${args.name}：一个带有标签和按钮的 Swing 窗口。 */
            public class MainWindow {

                private int clicks = 0;

                private void show() {
                    JFrame frame = new JFrame("${args.name}");
                    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

                    JLabel label = new JLabel("Click the button below");
                    label.setFont(new Font("SansSerif", Font.PLAIN, 18));
                    label.setForeground(new Color(0x22, 0x22, 0x22));

                    JPanel content = new JPanel(new BorderLayout());
                    content.setBackground(Color.WHITE);
                    content.add(label, BorderLayout.CENTER);

                    JButton button = new JButton("Say hello");
                    button.addActionListener(event -> {
                        clicks++;
                        label.setText("Hello! You clicked " + clicks + (clicks == 1 ? " time" : " times"));
                    });
                    content.add(button, BorderLayout.SOUTH);

                    frame.getContentPane().add(content, BorderLayout.CENTER);
                    frame.setSize(360, 220);
                    frame.setVisible(true);
                }

                public static void main(String[] args) {
                    new MainWindow().show();
                }
            }
            """,
        )
    }
}

/**
 * 一个自行绘制的 `JPanel`，这是人们使用 Swing 的另一半用途：自定义 `paintComponent`
 * 渲染，外加一个能改变绘制内容的按钮，以便看到重绘（repaint）的循环过程。
 */
object SwingCanvasTemplate : ProjectTemplate {
    override val id = TemplateId("swing-canvas")
    override val displayName = "Swing 自定义绘制"
    override val description = "一个绘制自定义图形的窗口：带 paintComponent 的 JPanel，点击时触发重绘。"
    override val category = TemplateCategory.JAVA
    override val iconId = "java"

    override fun parameters(): List<TemplateParameter> = emptyList()

    override fun generate(scaffold: ProjectScaffold, args: TemplateArgs) {
        val pkg = args.packageName
        SwingTemplateSupport.swingApp(
            scaffold, args, "DrawingWindow",
            """
            package $pkg;

            import java.awt.BorderLayout;
            import java.awt.Color;
            import java.awt.Dimension;
            import java.awt.Font;
            import java.awt.Graphics;
            import java.awt.Graphics2D;
            import java.awt.RenderingHints;
            import javax.swing.JButton;
            import javax.swing.JFrame;
            import javax.swing.JPanel;
            import javax.swing.WindowConstants;

            /** ${args.name}：一个面板自行绘制图形的 Swing 窗口。 */
            public class DrawingWindow {

                /** 一个自行绘制的面板。请重写 paintComponent（而不是 paint），并且始终先调用 super。 */
                static class Canvas extends JPanel {

                    private int shapes = 3;

                    Canvas() {
                        setPreferredSize(new Dimension(400, 260));
                        setBackground(Color.WHITE);
                    }

                    void addShape() {
                        shapes++;
                        // repaint() 请求绘制新的一帧；下一次 paintComponent 会绘制出新的状态。
                        repaint();
                    }

                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        g2.setColor(new Color(0x2D, 0x6C, 0xDF));
                        g2.fillRoundRect(20, 20, getWidth() - 40, 56, 16, 16);

                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
                        g2.drawString("${args.name}", 40, 56);

                        g2.setColor(new Color(0xDF, 0x6C, 0x2D));
                        for (int i = 0; i < shapes; i++) {
                            g2.fillOval(30 + i * 44, 110, 32, 32);
                        }

                        g2.setColor(Color.DARK_GRAY);
                        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
                        g2.drawString(shapes + " shapes", 30, 180);
                    }
                }

                private void show() {
                    JFrame frame = new JFrame("${args.name}");
                    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

                    Canvas canvas = new Canvas();
                    JButton button = new JButton("Add a shape");
                    button.addActionListener(event -> canvas.addShape());

                    frame.getContentPane().add(canvas, BorderLayout.CENTER);
                    frame.getContentPane().add(button, BorderLayout.SOUTH);
                    frame.pack();
                    frame.setVisible(true);
                }

                public static void main(String[] args) {
                    new DrawingWindow().show();
                }
            }
            """,
        )
    }
}
