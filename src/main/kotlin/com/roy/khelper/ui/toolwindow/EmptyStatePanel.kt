package com.roy.khelper.ui.toolwindow

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.*
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JPanel

class EmptyStatePanel : JPanel(BorderLayout()) {

    private val bundle = ResourceBundle.getBundle("messages.EmptyStatePanelBundle")

    init {
        // reduce top spacing a bit so the panel starts higher
        border = JBUI.Borders.empty(8)

        val titleText = bundle.getString("title")
        val authorText = bundle.getString("author")
        val descText = bundle.getString("desc")
        val linkText = bundle.getString("link")

        // the content panel will switch between vertical and horizontal orientation
        val content = JPanel()
        content.layout = BoxLayout(content, BoxLayout.Y_AXIS)
        content.alignmentX = 0.5f

        // Load theme-aware icon (SVG) via IconLoader
        val svgPath = "/icons/pluginIcon.svg"
        val icon: Icon? = try {
            IconLoader.getIcon(svgPath, javaClass)
        } catch (_: Throwable) {
            null
        }

        // Image panel paints the icon and scales it using Graphics2D transform so vector icons remain crisp and theme-aware
        val imagePanel = object : JPanel() {
            init {
                isOpaque = false
            }

            override fun getPreferredSize(): Dimension = JBUI.size(160, 160)

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2 = g as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                val pad = JBUI.scale(8)
                val availableW = width - pad * 2
                val availableH = height - pad * 2
                if (availableW <= 0 || availableH <= 0) return

                if (icon != null) {
                    val iw = icon.iconWidth.takeIf { it > 0 } ?: 64
                    val ih = icon.iconHeight.takeIf { it > 0 } ?: 64
                    val scale = kotlin.math.min(availableW.toDouble() / iw, availableH.toDouble() / ih)
                    val drawW = (iw * scale).toInt()
                    val drawH = (ih * scale).toInt()
                    val x = (width - drawW) / 2
                    val y = (height - drawH) / 2

                    // Use a transformed Graphics2D so vector icons (SVG) render at the requested size and remain theme-aware
                    val gCopy = g2.create() as Graphics2D
                    try {
                        gCopy.translate(x.toDouble(), y.toDouble())
                        gCopy.scale(scale, scale)
                        icon.paintIcon(this, gCopy, 0, 0)
                    } finally {
                        gCopy.dispose()
                    }
                }
            }
        }.apply { alignmentX = 0.5f }

        // Info panel with title/author/description/link
        val infoPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
        }

        val title = JBLabel(titleText).apply { font = font.deriveFont(Font.BOLD, 18f); alignmentX = 0.0f }
        val author = JBLabel(authorText).apply { font = font.deriveFont(Font.PLAIN, 13f); alignmentX = 0.0f }
        val desc = JBLabel(descText).apply {
            font = font.deriveFont(Font.PLAIN, 13f)
            alignmentX = 0.0f
        }

        val link = ActionLink(linkText) {
            BrowserUtil.browse(linkText)
        }.apply { alignmentX = 0.0f }

        infoPanel.add(title)
        infoPanel.add(Box.createVerticalStrut(6))
        infoPanel.add(author)
        infoPanel.add(Box.createVerticalStrut(8))
        infoPanel.add(desc)
        infoPanel.add(Box.createVerticalStrut(10))
        infoPanel.add(link)

        // Add components to content. We'll adjust orientation on resize.
        // start a bit higher: small top strut instead of large glue
        content.add(Box.createVerticalStrut(JBUI.scale(12)))
        content.add(imagePanel)
        content.add(Box.createVerticalStrut(JBUI.scale(10)))
        content.add(infoPanel)
        content.add(Box.createVerticalGlue())

        // Responsive behavior: switch orientation depending on width
        fun adjustOrientation(w: Int) {
            val wasHorizontal = content.layout is BoxLayout && (content.layout as BoxLayout).axis == BoxLayout.X_AXIS
            val shouldBeHorizontal = w >= JBUI.scale(520)
            if (shouldBeHorizontal == wasHorizontal) return

            content.removeAll()
            if (shouldBeHorizontal) {
                content.layout = BoxLayout(content, BoxLayout.X_AXIS)
                // horizontal: icon on the left, info on the right with some spacing
                content.add(Box.createHorizontalGlue())
                content.add(imagePanel)
                content.add(Box.createHorizontalStrut(JBUI.scale(18)))
                content.add(infoPanel)
                content.add(Box.createHorizontalGlue())
                // left-align info text in horizontal layout
                title.alignmentX = 0.0f
                author.alignmentX = 0.0f
                desc.alignmentX = 0.0f
                link.alignmentX = 0.0f
                infoPanel.alignmentX = 0.0f
            } else {
                content.layout = BoxLayout(content, BoxLayout.Y_AXIS)
                // vertical: put the icon on top and center of the text
                content.add(Box.createVerticalStrut(JBUI.scale(12)))
                content.add(imagePanel)
                content.add(Box.createVerticalStrut(JBUI.scale(10)))
                content.add(infoPanel)
                content.add(Box.createVerticalGlue())
                // center-align info text in vertical layout
                title.alignmentX = 0.5f
                author.alignmentX = 0.5f
                desc.alignmentX = 0.5f
                link.alignmentX = 0.5f
                infoPanel.alignmentX = 0.5f
            }
            content.revalidate()
            content.repaint()
        }

        // Listen for resize to toggle layout
        content.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                adjustOrientation(content.width)
            }

            override fun componentShown(e: ComponentEvent) {
                adjustOrientation(content.width)
            }
        })

        add(content, BorderLayout.CENTER)
    }
}
