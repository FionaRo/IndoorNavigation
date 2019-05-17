package hse.sergeeva.indoornavigation.models.decoders

import java.lang.Math.max
import kotlin.experimental.and
import kotlin.math.min

class ManchesterDecoder {
    companion object {

        fun decode(bytes: List<Int>): List<Int> {

            val minError = 10000
            var result = listOf<Int>()

            for (i in 0..2) {
                val check = check(bytes, i)
                if (check.first == 0) return check.second
                if (minError > check.first) result = check.second
            }

            return result
        }

        private fun compress(bytes: List<Int>): List<Int> {
            var maxSub = 0
            var minSub = 20
            var sub = 1
            for (i in 1 until bytes.size) {
                if (bytes[i] != bytes[i - 1]) {
                    maxSub = max(maxSub, min(20, sub))
                    minSub = min(minSub, max(2, sub))
                    sub = 0
                }
                sub++
            }
            maxSub = max(maxSub, min(20, sub))
            minSub = min(minSub, max(2, sub))

            val mean = (maxSub + minSub) / 2

            val compressedBytes = mutableListOf<Int>()

            sub = 1
            for (i in 1 until bytes.size) {
                if (bytes[i] != bytes[i - 1]) {
                    compressedBytes += bytes[i - 1]
                    if (sub >= mean)
                        compressedBytes += bytes[i - 1]
                    sub = 0
                }
                sub++
            }

            compressedBytes += bytes.last()
            if (sub > maxSub)
                compressedBytes += bytes.last()

            return compressedBytes
        }

        private fun check(bytes: List<Int>, start: Int): Pair<Int, List<Int>> {
            val originBytes = arrayListOf<Int>()
            var errors = 0

            for (i in start until bytes.size - 2 step 3) {

                if (bytes[i] == bytes[i + 1] && bytes[i + 1] != bytes[i + 2])
                    originBytes.add(bytes[i + 2])
                else if (bytes[i] != bytes[i + 1] && bytes[i + 1] == bytes[i + 2])
                    originBytes.add(bytes[i + 2])
                else {
                    errors++
                    if (bytes[i + 1] == bytes[i + 2])
                        originBytes.add(bytes[i + 2])
                    else if (i != 0 && bytes[i - 1] == bytes[i]) {
                        if (bytes[i] == 1)
                            originBytes.add(0)
                        else
                            originBytes.add(1)
                    } else
                        originBytes.add(bytes[i])
                }
            }

            return Pair(errors, originBytes)
        }

        private fun check12(bytes: List<Int>, start: Int): Pair<Boolean, List<Int>> {
            val originBytes = arrayListOf<Int>()

            for (i in start until bytes.size - 2 step 3) {
                if (bytes[i] == bytes[i + 1] || bytes[i + 1] != bytes[i + 2])
                    return Pair(false, originBytes)

                if (bytes[i] > bytes[i + 1])
                    originBytes.add(0)
                else
                    originBytes.add(1)
            }

            return Pair(true, originBytes)
        }

        private fun check21(bytes: List<Int>, start: Int): Pair<Boolean, List<Int>> {
            val originBytes = arrayListOf<Int>()

            for (i in start until bytes.size - 2 step 3) {
                if (bytes[i] != bytes[i + 1] || bytes[i + 1] == bytes[i + 2])
                    return Pair(false, originBytes)

                if (bytes[i + 1] > bytes[i + 2])
                    originBytes.add(0)
                else
                    originBytes.add(1)

            }

            return Pair(true, originBytes)

        }

        fun findPreamble(bytes: List<Int>): Int {
            val preamble = arrayListOf(1, 0, 1, 0, 1, 0, 1, 0)
            for (i in 0 until bytes.size) {
                if (i + preamble.size - 1 > bytes.size) return -1
                var found = 8
                for (j in 0 until preamble.size) {
                    if (bytes[i + j] != preamble[j]) {
                        found = j
                        break
                    }
                }
                if (found == 8 || found > 4 && i == 0)
                    return i + 8
            }
            return -1
        }
    }
}

