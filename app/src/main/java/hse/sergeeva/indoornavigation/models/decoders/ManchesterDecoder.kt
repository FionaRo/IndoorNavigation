package hse.sergeeva.indoornavigation.models.decoders

import java.lang.Math.max
import kotlin.math.min

class ManchesterDecoder {
    companion object {

        private const val messageLen = 12

        fun decode(bytes: List<Int>): Int {

            /*var result = mutableListOf<Int>()

            for (i in 0..2) {
                val check12 = check12(bytes, i)
                if (check12.first) return check12.second
                if (check12.second != -1) result.add(check12.second)

                val check21 = check21(bytes, i)
                if (check21.first) return check21.second
                if (check21.second != -1) result.add(check21.second)
            }*/

            val result = compress(bytes)
            if (result.size < 40) return -1
            val message = arrayListOf<Int>()

            for (i in 0 until result.size - 1 step 2) {
                if (bytes[i] < bytes[i + 1])
                    message.add(1)
                else
                    message.add(0)
            }

            val preambleEnd = findPreamble(message)
            if (preambleEnd == -1 || preambleEnd + messageLen > message.size) return -1

            val messageBytes = arrayListOf<Int>()
            for (i in preambleEnd until preambleEnd + messageLen) {
                messageBytes.add(message[i])
            }

            return HammingDecoder.decode(messageBytes).second

            /*val hashMap = hashMapOf<Int, Int>()
            for (el in result) {
                if (!hashMap.containsKey(el))
                    hashMap[el] = 0
                hashMap[el]?.plus(1)
            }

            var maxCount = 0
            var maxElement = 0
            for (el in hashMap) {
                if (el.value > maxCount) {
                    maxCount = el.value
                    maxElement = el.key
                }
            }

            return maxElement*/
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

        private fun check12(bytes: List<Int>, start: Int): Pair<Boolean, Int> {
            val originBytes = arrayListOf<Int>()

            for (i in start until bytes.size - 2 step 3) {
                if (bytes[i] == bytes[i + 1] || bytes[i + 1] != bytes[i + 2])
                    return Pair(false, -1)

                if (bytes[i] > bytes[i + 1])
                    originBytes.add(0)
                else
                    originBytes.add(1)
            }

            val preambleEnd = findPreamble(originBytes)
            if (preambleEnd == -1 || preambleEnd + messageLen > originBytes.size) return Pair(false, -1)

            val messageBytes = arrayListOf<Int>()
            for (i in preambleEnd until preambleEnd + messageLen) {
                messageBytes.add(originBytes[i])
            }

            return HammingDecoder.decode(messageBytes)
        }

        private fun check21(bytes: List<Int>, start: Int): Pair<Boolean, Int> {
            val originBytes = arrayListOf<Int>()

            for (i in start until bytes.size - 2 step 3) {
                if (bytes[i] != bytes[i + 1] || bytes[i + 1] == bytes[i + 2])
                    return Pair(false, -1)

                if (bytes[i] > bytes[i + 1])
                    originBytes.add(0)
                else
                    originBytes.add(1)

            }

            val preambleEnd = findPreamble(originBytes)
            if (preambleEnd == -1 || preambleEnd + messageLen - 1 > bytes.size) return Pair(false, -1)

            val messageBytes = arrayListOf<Int>()
            for (i in preambleEnd until preambleEnd + 8) {
                messageBytes.add(bytes[i])
            }

            return HammingDecoder.decode(messageBytes)
        }

        private fun findPreamble(bytes: ArrayList<Int>): Int {
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

