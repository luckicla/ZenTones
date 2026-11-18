package com.zentones.app.editor

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.min

/** Audio en mono 16 bit. */
class Pcm(val data: ShortArray, val sampleRate: Int) {
    val seconds: Double get() = data.size.toDouble() / sampleRate

    /** Picos para dibujar la forma de onda (0..1). */
    fun peaks(buckets: Int): FloatArray {
        if (data.isEmpty()) return FloatArray(buckets)
        val per = (data.size / buckets).coerceAtLeast(1)
        return FloatArray(buckets) { b ->
            var m = 0
            val s = b * per
            for (i in s until min(s + per, data.size)) m = maxOf(m, abs(data[i].toInt()))
            m / 32768f
        }
    }
}

/** Decodifica MP3, AAC, OGG, OPUS, FLAC, WAV… con los códecs del propio Android. */
object AudioDecoder {

    fun decode(ctx: Context, uri: Uri, maxSeconds: Int = 600): Pcm =
        MediaExtractor().run { setDataSource(ctx, uri, null); decodeFrom(this, maxSeconds) }

    fun decode(file: File, maxSeconds: Int = 600): Pcm =
        MediaExtractor().run { setDataSource(file.absolutePath); decodeFrom(this, maxSeconds) }

    private fun decodeFrom(ex: MediaExtractor, maxSeconds: Int): Pcm {
        try {
            val track = (0 until ex.trackCount).firstOrNull {
                ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("El archivo no tiene audio")
            ex.selectTrack(track)
            val fmt = ex.getTrackFormat(track)
            val mime = fmt.getString(MediaFormat.KEY_MIME)!!
            var sr = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var ch = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var isFloat = false

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(fmt, null, null, 0)
            codec.start()
            var out = ShortArray(sr * 10)
            var size = 0
            val info = MediaCodec.BufferInfo()
            var inDone = false
            var outDone = false
            try {
                while (!outDone) {
                    if (!inDone) {
                        val i = codec.dequeueInputBuffer(10_000)
                        if (i >= 0) {
                            val b = codec.getInputBuffer(i)!!
                            val n = ex.readSampleData(b, 0)
                            if (n < 0) {
                                codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inDone = true
                            } else {
                                codec.queueInputBuffer(i, 0, n, ex.sampleTime, 0)
                                ex.advance()
                            }
                        }
                    }
                    val o = codec.dequeueOutputBuffer(info, 10_000)
                    if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val f = codec.outputFormat
                        sr = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        ch = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        isFloat = f.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                            f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                    } else if (o >= 0) {
                        val b = codec.getOutputBuffer(o)!!
                        b.position(info.offset)
                        b.limit(info.offset + info.size)
                        b.order(ByteOrder.nativeOrder())
                        val chs = ch.coerceAtLeast(1)
                        if (isFloat) {
                            val fb = b.asFloatBuffer()
                            val frames = fb.remaining() / chs
                            if (size + frames > out.size) out = out.copyOf(maxOf(out.size * 2, size + frames))
                            for (k in 0 until frames) {
                                var acc = 0f
                                for (c in 0 until chs) acc += fb.get()
                                out[size++] = (acc / chs * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                            }
                        } else {
                            val sb = b.asShortBuffer()
                            val frames = sb.remaining() / chs
                            if (size + frames > out.size) out = out.copyOf(maxOf(out.size * 2, size + frames))
                            for (k in 0 until frames) {
                                var acc = 0
                                for (c in 0 until chs) acc += sb.get()
                                out[size++] = (acc / chs).toShort()
                            }
                        }
                        codec.releaseOutputBuffer(o, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outDone = true
                        if (size >= maxSeconds * sr) outDone = true
                    }
                }
            } finally {
                runCatching { codec.stop() }
                codec.release()
            }
            return Pcm(out.copyOf(size), sr)
        } finally {
            ex.release()
        }
    }
}
