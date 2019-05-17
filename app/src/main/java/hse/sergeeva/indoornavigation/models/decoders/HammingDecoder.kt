package hse.sergeeva.indoornavigation.models.decoders

import kotlin.experimental.and
import kotlin.math.ceil
import kotlin.math.log2

class HammingDecoder {

    companion object {
        private fun getMatrix(width: Int): List<ByteArray> {
            val matrix = arrayListOf<ByteArray>()

            val height = ceil(log2(width.toDouble())).toInt()

            for (i in 0 until width) {
                matrix.add(ByteArray(height))
                var t = i + 1
                for (j in 0 until height) {
                    matrix[i][j] = (t % 2).toByte()
                    t = t shr 1
                }
            }

            val newMatrix = arrayListOf<ByteArray>()

            for (i in 0 until height)
                newMatrix.add(ByteArray(width))

            for (i in 0 until width) {
                for (j in 0 until height) {
                    newMatrix[j][i] = matrix[i][j]
                }
            }

            return newMatrix
        }

        private fun binaryToInt(binary: List<Int>): Int {
            var code = 0
            for (i in 0 until binary.size) {
                code += binary[i] * (1 shl (binary.size - i - 1))
            }
            return code
        }

        private fun checkDecode(binary: List<Int>, matrix: List<ByteArray>): List<Int> {
            val check = arrayListOf<Int>()
            var decodeOk = true
            for (i in 0 until matrix.size) {
                var sum = 0
                for (j in 0 until binary.size) {
                    sum += matrix[i][j] and binary[j].toByte()
                }
                check.add(sum % 2)
                if (check[i] != 0)
                    decodeOk = false
            }

            if (!decodeOk)
                return check
            return arrayListOf()
        }

        fun decode(binary: List<Int>): Pair<Boolean, Int> {
            val binaryCopy = binary.toMutableList()
            val matrix = getMatrix(binaryCopy.size)
            val error = checkDecode(binaryCopy, matrix)

            if (error.isNotEmpty()) {
                val index = binaryToInt(error)
                if (binaryCopy[index] == 0)
                    binaryCopy[index] = 1
                else
                    binaryCopy[index] = 0
                val error1 = checkDecode(binaryCopy, matrix)
                if (error1.isNotEmpty())
                    return Pair(false, -1)
            }

            val binaryCode = arrayListOf<Int>()
            var twoPow = 1

            for (i in 0 until binaryCopy.size) {
                if (i + 1 == twoPow) {
                    twoPow *= 2
                    continue
                }
                binaryCode.add(binaryCopy[i])
            }

            return Pair(error.isEmpty(), binaryToInt(binaryCode))
        }
    }
}