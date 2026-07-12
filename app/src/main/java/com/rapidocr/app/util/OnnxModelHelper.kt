package com.rapidocr.app.util

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object OnnxModelHelper {
    private const val ONNX_MAGIC = 0x0D0A0A0A

    fun createMinimalOnnxModel(
        filePath: String,
        inputShape: List<Long> = listOf(1, 3, 640, 640),
        outputShape: List<Long> = listOf(1, 1, 640, 640)
    ): Boolean {
        return try {
            val file = File(filePath)
            file.parentFile?.mkdirs()

            val buffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN)

            buffer.putInt(ONNX_MAGIC)
            buffer.putInt(8)
            buffer.putLong(1)
            buffer.putLong(1)
            buffer.putLong(1)
            buffer.putLong(7)

            buffer.put(createTensorProto("input", inputShape))
            buffer.put(createTensorProto("output", outputShape))

            val graphBytes = createMinimalGraph(inputShape, outputShape)
            buffer.put(graphBytes)

            FileOutputStream(file).use { fos ->
                fos.write(buffer.array(), 0, buffer.position())
            }
            file.exists() && file.length() > 0
        } catch (e: Exception) {
            false
        }
    }

    private fun createTensorProto(name: String, shape: List<Long>): ByteArray {
        val buf = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN)

        val nameBytes = name.toByteArray(Charsets.UTF_8)
        buf.putInt(nameBytes.size)
        buf.put(nameBytes)

        buf.putInt(1)

        for (dim in shape) {
            buf.putLong(dim)
        }

        buf.putInt(1)
        buf.putLong(1)

        val result = ByteArray(buf.position())
        buf.rewind()
        buf.get(result)
        return result
    }

    private fun createMinimalGraph(inputShape: List<Long>, outputShape: List<Long>): ByteArray {
        val buf = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN)

        buf.putLong(1)
        val nodeNameBytes = "Identity".toByteArray(Charsets.UTF_8)
        buf.putInt(nodeNameBytes.size)
        buf.put(nodeNameBytes)

        buf.putLong(1)
        val inputNameBytes = "input".toByteArray(Charsets.UTF_8)
        buf.putInt(inputNameBytes.size)
        buf.put(inputNameBytes)

        buf.putLong(1)
        val outputNameBytes = "output".toByteArray(Charsets.UTF_8)
        buf.putInt(outputNameBytes.size)
        buf.put(outputNameBytes)

        val result = ByteArray(buf.position())
        buf.rewind()
        buf.get(result)
        return result
    }
}
