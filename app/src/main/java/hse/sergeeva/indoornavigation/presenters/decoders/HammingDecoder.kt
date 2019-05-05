package hse.sergeeva.indoornavigation.presenters.decoders

import java.lang.Math.log
import kotlin.experimental.and
import kotlin.math.ceil

class HammingDecoder {

    companion object {
        private fun getMatrix(width: Int): ArrayList<ByteArray> {
            val matrix = arrayListOf<ByteArray>()

            val height = ceil(log(width.toDouble())).toInt()

            for (i in 0 until width) {
                matrix.add(ByteArray(height))
                var t = i + 1
                for (j in 0 until height) {
                    matrix[i][j] = (t % 2).toByte()
                    t = t shr 2
                }
            }

            for (i in 0 until width) {
                for (j in 0 until height) {
                    val t = matrix[i][j]
                    matrix[i][j] = matrix[j][i]
                    matrix[j][i] = t
                }
            }

            return matrix
        }

        private fun binaryToInt(binary: ArrayList<Int>): Int {
            var code = 0
            for (i in 0 until binary.size) {
                code += i and (2 shl (binary.size - i))
            }
            return code
        }

        private fun checkDecode(binary: ArrayList<Int>, matrix: ArrayList<ByteArray>): ArrayList<Int> {
            val check = ArrayList<Int>(matrix.size)
            var decodeOk = true
            for (i in 0 until matrix.size) {
                var sum = 0
                for (j in 0 until binary.size) {
                    sum += matrix[i][j] and binary[j].toByte()
                }
                check[i] = (sum % 2)
                if (check[i] != 0)
                    decodeOk = false
            }

            if (!decodeOk)
                return check
            return arrayListOf()
        }

        fun decode(binary: ArrayList<Int>): Pair<Boolean, Int> {
            val matrix = getMatrix(binary.size)

            val error = checkDecode(binary, matrix)

            if (error.size != 0) {
                val index = binaryToInt(error)
                if (binary[index] == 0)
                    binary[index] = 1
                else
                    binary[index] = 0
                val error1 = checkDecode(binary, matrix)
                if (error1.size != 0)
                    return Pair(false, -1)
            }

            val binaryCode = arrayListOf<Int>()
            var twoPow = 1

            for (i in 0 until binary.size) {
                if (i + 1 == twoPow) {
                    twoPow *= 2
                    continue
                }
                binaryCode.add(binary[i])
            }

            return Pair(error.size == 0, binaryToInt(binaryCode))
        }
    }
}