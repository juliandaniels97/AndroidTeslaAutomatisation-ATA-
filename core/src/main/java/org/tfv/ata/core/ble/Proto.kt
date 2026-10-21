package org.tfv.ata.core.ble

import java.io.ByteArrayOutputStream

internal class ProtoWriter {
  private val out = ByteArrayOutputStream()

  fun bytes(field: Int, value: ByteArray?): ProtoWriter {
    if (value == null) return this
    key(field, 2)
    varint(value.size.toLong())
    out.write(value)
    return this
  }

  fun message(field: Int, value: ByteArray?): ProtoWriter = bytes(field, value)

  fun string(field: Int, value: String): ProtoWriter = bytes(field, value.toByteArray(Charsets.UTF_8))

  fun varint(field: Int, value: Long): ProtoWriter {
    key(field, 0)
    varint(value)
    return this
  }

  fun bool(field: Int, value: Boolean): ProtoWriter = varint(field, if (value) 1 else 0)

  fun enum(field: Int, value: Int): ProtoWriter = varint(field, value.toLong())

  fun fixed32(field: Int, value: Int): ProtoWriter {
    key(field, 5)
    out.write(value and 0xFF)
    out.write((value ushr 8) and 0xFF)
    out.write((value ushr 16) and 0xFF)
    out.write((value ushr 24) and 0xFF)
    return this
  }

  fun float(field: Int, value: Float): ProtoWriter = fixed32(field, java.lang.Float.floatToIntBits(value))

  fun toByteArray(): ByteArray = out.toByteArray()

  private fun key(field: Int, wire: Int) = varint(((field.toLong()) shl 3) or wire.toLong())

  private fun varint(v: Long) {
    var value = v
    while (true) {
      val b = (value and 0x7F).toInt()
      value = value ushr 7
      if (value != 0L) out.write(b or 0x80) else { out.write(b); break }
    }
  }
}

internal fun buildProto(block: ProtoWriter.() -> Unit): ByteArray = ProtoWriter().apply(block).toByteArray()

internal class ProtoField(val wire: Int, val bytesValue: ByteArray?, val varintValue: Long, val fixed32Value: Int)

internal class ProtoReader(private val data: ByteArray) {
  val fields = HashMap<Int, MutableList<ProtoField>>()

  init {
    var pos = 0
    loop@ while (pos < data.size) {
      val (key, k1) = readVarint(pos) ?: break
      pos = k1
      val field = (key ushr 3).toInt()
      when ((key and 0x7).toInt()) {
        0 -> {
          val (v, p) = readVarint(pos) ?: break@loop
          pos = p
          add(field, ProtoField(0, null, v, 0))
        }
        1 -> {
          if (pos + 8 > data.size) break@loop
          pos += 8
          add(field, ProtoField(1, null, 0, 0))
        }
        2 -> {
          val (len, p) = readVarint(pos) ?: break@loop
          if (len < 0 || len > data.size - p) break@loop
          pos = p
          val end = pos + len.toInt()
          add(field, ProtoField(2, data.copyOfRange(pos, end), 0, 0))
          pos = end
        }
        5 -> {
          if (pos + 4 > data.size) break@loop
          val v = (data[pos].toInt() and 0xFF) or
            ((data[pos + 1].toInt() and 0xFF) shl 8) or
            ((data[pos + 2].toInt() and 0xFF) shl 16) or
            ((data[pos + 3].toInt() and 0xFF) shl 24)
          pos += 4
          add(field, ProtoField(5, null, 0, v))
        }
        else -> break@loop
      }
    }
  }

  fun has(field: Int): Boolean = fields.containsKey(field)
  fun bytes(field: Int): ByteArray? = fields[field]?.firstOrNull()?.bytesValue
  fun varint(field: Int): Long? = fields[field]?.firstOrNull()?.varintValue
  fun fixed32(field: Int): Int? = fields[field]?.firstOrNull()?.fixed32Value
  fun message(field: Int): ProtoReader? = bytes(field)?.let { ProtoReader(it) }

  private fun add(field: Int, f: ProtoField) {
    fields.getOrPut(field) { mutableListOf() }.add(f)
  }

  /** Returns (value, nextPos), or null when the varint is truncated or longer than 10 bytes. */
  private fun readVarint(start: Int): Pair<Long, Int>? {
    var pos = start
    var result = 0L
    var shift = 0
    while (pos < data.size && shift < 64) {
      val b = data[pos].toInt() and 0xFF
      result = result or ((b and 0x7F).toLong() shl shift)
      pos++
      if (b and 0x80 == 0) return result to pos
      shift += 7
    }
    return null
  }
}
