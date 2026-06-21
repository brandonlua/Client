package org.lwjgl.opengl

import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.glfw.GLFWWindowSizeCallback
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.system.MemoryUtil
import java.nio.Buffer
import java.nio.ByteBuffer
import kotlin.math.sqrt


object Display {
    @JvmStatic
    var title: String = ""
        set(value) {
            field = value
            if (isCreated) {
                GLFW.glfwSetWindowTitle(handle, value)
            }
        }
    @JvmStatic
    var handle: Long = -1
    private var resizable = false

    @JvmStatic
    var displayMode: DisplayMode = DisplayMode(640, 480, 24, 60)

    @JvmStatic
    var width = 0

    @JvmStatic
    var height = 0

    @JvmStatic
    var xPos = 0

    @JvmStatic
    var yPos = 0

    @JvmStatic
    val desktopDisplayMode: DisplayMode?
        get() { return availableDisplayModes.maxByOrNull { it.width * it.height } }
    private var window_resized = false
    private var sizeCallback: GLFWWindowSizeCallback? = null

    private var cached_icons: Array<ByteBuffer>? = null
    @JvmStatic
    fun setIcon(icons: Array<ByteBuffer>): Int {
        if (!this.cached_icons.contentEquals(icons)) {
            cached_icons = icons.map { cloneByteBuffer(it) }.toTypedArray()
        }

        if (this.isCreated) {
            GLFW.glfwSetWindowIcon(this.handle, iconsToGLFWBuffer(this.cached_icons!!))
            return 1
        } else {
            return 0
        }
    }


    private fun cloneByteBuffer(original: ByteBuffer): ByteBuffer {
        val clone = BufferUtils.createByteBuffer(original.capacity())
        val old_position = original.position()
        clone.put(original)
        (original as Buffer).position(old_position)
        (clone as Buffer).flip()

        return clone
    }
    private fun iconsToGLFWBuffer(icons: Array<ByteBuffer>): GLFWImage.Buffer {
        val buffer = GLFWImage.create(icons.size)
        icons.forEach { icon ->
            val size: Int = icon.limit() / 4
            val dimension = sqrt(size.toDouble()).toInt()
            GLFWImage.malloc().use { image ->
                buffer.put(image.set(dimension, dimension, icon))
            }
        }
        buffer.flip()
        return buffer
    }

    @JvmStatic
    fun update() {
        window_resized = false
        GLFW.glfwPollEvents()
        if ( Mouse.isCreated() ) {
            Mouse.poll()
        }

        if ( Keyboard.isCreated() ) {
            Keyboard.poll()
        }
        GLFW.glfwSwapBuffers(handle)
    }

    @JvmStatic
    @JvmOverloads
    fun create(pixelFormat: PixelFormat? = null) {
        GLFWErrorCallback.createPrint(System.err).set()

        if(System.getProperty("os.name").lowercase().contains("linux")) {
            GLFW.glfwInitHint(GLFW.GLFW_PLATFORM, GLFW.GLFW_PLATFORM_X11)
        }

        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, if (resizable) GLFW.GLFW_TRUE else GLFW.GLFW_FALSE)
        handle =
            GLFW.glfwCreateWindow(displayMode.width, displayMode.height, title, MemoryUtil.NULL, MemoryUtil.NULL)
        width = displayMode.width
        height = displayMode.height
        GLFW.glfwMakeContextCurrent(handle)
        GL.createCapabilities()
        sizeCallback = GLFWWindowSizeCallback.create(Display::resizeCallback)
        GLFW.glfwSetWindowSizeCallback(handle, sizeCallback)
        Mouse.create()
        Keyboard.create()
        GLFW.glfwShowWindow(handle)
        if (this.cached_icons != null) {
            this.setIcon(this.cached_icons!!)
        }
    }

    @JvmStatic
    fun setFullscreen(fullscreen: Boolean) {
        println("setFullscreen: $fullscreen")
        runCatching {
            this.resizeCallback(handle, displayMode.width, displayMode.height)
            if (fullscreen) {
                var monitor = GLFW.glfwGetPrimaryMonitor()

                GLFW.glfwSetWindowMonitor(
                    handle,
                    monitor,
                    0,
                    0,
                    width,
                    height,
                    displayMode.frequency
                )
                xPos = displayMode.width / 2
                yPos = displayMode.height / 2
            } else {
                xPos -= width / 2
                yPos -= height / 2
                GLFW.glfwSetWindowMonitor(
                    handle,
                    0L,
                    xPos,
                    yPos,
                    width,
                    height,
                    -1
                )
            }
            GLFW.glfwSetWindowSize(handle, width, height)
        }.onFailure {
            it.printStackTrace()
        }
    }
    @JvmStatic
    val availableDisplayModes: Array<DisplayMode>
        get() {
            val primaryMonitor = GLFW.glfwGetPrimaryMonitor()
            if (primaryMonitor == MemoryUtil.NULL) {
                return arrayOf()
            }
            val videoModes = GLFW.glfwGetVideoModes(primaryMonitor) ?: error("No video modes found")
            return videoModes.map { mode ->
                DisplayMode(
                    mode.width(),
                    mode.height(),
                    mode.redBits() + mode.blueBits() + mode.greenBits(),
                    mode.refreshRate()
                )
            }.toHashSet().toTypedArray()
        }

    private fun resizeCallback(window: Long, width: Int, height: Int) {
        if (window == handle) {
            window_resized = true
            Display.width = width
            Display.height = height
        }
    }

    fun destroyWindow() {
        sizeCallback!!.free()
        Mouse.destroy()
        Keyboard.destroy()
        GLFW.glfwDestroyWindow(handle)
    }

    @JvmStatic
    fun destroy() {
        destroyWindow()
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)?.free()
    }

    @JvmStatic
    val isCreated: Boolean
        get() = handle != -1L

    @JvmStatic
    val isCloseRequested: Boolean
        get() = GLFW.glfwWindowShouldClose(handle)

    @JvmStatic
    val isActive: Boolean
        get() = true

    @JvmStatic
    fun setResizable(isResizable: Boolean) {
        resizable = isResizable
        if (isCreated) {
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, if (resizable) GLFW.GLFW_TRUE else GLFW.GLFW_FALSE)
        }
    }

    @JvmStatic
    fun sync(fps: Int) {
        Sync.sync(fps)
    }

    @JvmStatic
    fun setVSyncEnabled(enabled: Boolean) {
        GLFW.glfwSwapInterval(if (enabled) 1 else 0)
    }

    @JvmStatic
    fun wasResized(): Boolean = window_resized
}