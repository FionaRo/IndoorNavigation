package hse.sergeeva.indoornavigation.algorithms.kalmanFilter

class Matrix(private var rows: Int, private var cols: Int) {
    var data: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }

    fun setData(args: DoubleArray) {
        if (args.size != rows * cols) {
            throw AssertionError("Assertion failed")
        }
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                data[r][c] = args[r * cols + c]
            }
        }
    }

    fun setData(args: FloatArray) {
        if (args.size != rows * cols) {
            throw AssertionError("Assertion failed")
        }
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                data[r][c] = args[r * cols + c].toDouble()
            }
        }
    }

    fun setIdentityDiag() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                data[r][c] = 0.0
            }
            data[r][r] = 1.0
        }
    }

    fun setIdentity() {
        if (rows != cols) {
            throw AssertionError("Assertion failed")
        }
        setIdentityDiag()
    }

    fun subtractFromIdentity() {
        for (r in 0 until rows) {
            for (c in 0 until r)
                data[r][c] = -data[r][c]
            data[r][r] = 1.0 - data[r][r]
            for (c in r + 1 until cols)
                data[r][c] = -data[r][c]
        }
    }

    fun scale(scalar: Double) {
        for (r in 0 until rows) {
            for (c in 0 until cols)
                data[r][c] *= scalar
        }
    }

    private fun swapRows(r1: Int, r2: Int) {
        if (r1 == r2) {
            throw AssertionError("Assertion failed")
        }
        val tmp = data[r1]
        data[r1] = data[r2]
        data[r2] = tmp
    }

    private fun scaleRow(r: Int, scalar: Double) {
        if (r >= rows) {
            throw AssertionError("Assertion failed")
        }
        for (c in 0 until cols)
            data[r][c] *= scalar
    }

    internal fun shearRow(r1: Int, r2: Int, scalar: Double) {
        if (r1 == r2) {
            throw AssertionError("Assertion failed")
        }
        if (!(r1 < rows && r2 < rows)) {
            throw AssertionError("Assertion failed")
        }
        for (c in 0 until cols)
            data[r1][c] += data[r2][c] * scalar
    }

    companion object {
        fun matrixAdd(ma: Matrix, mb: Matrix, mc: Matrix) {
            if (!(ma.cols == mb.cols && mb.cols == mc.cols)) {
                throw AssertionError("Assertion failed")
            }
            if (!(ma.rows == mb.rows && mb.rows == mc.rows)) {
                throw AssertionError("Assertion failed")
            }

            for (r in 0 until ma.rows) {
                for (c in 0 until ma.cols) {
                    mc.data[r][c] = ma.data[r][c] + mb.data[r][c]
                }
            }
        }

        fun matrixSubtract(ma: Matrix, mb: Matrix, mc: Matrix) {
            if (!(ma.cols == mb.cols && mb.cols == mc.cols)) {
                throw AssertionError("Assertion failed")
            }
            if (!(ma.rows == mb.rows && mb.rows == mc.rows)) {
                throw AssertionError("Assertion failed")
            }

            for (r in 0 until ma.rows) {
                for (c in 0 until ma.cols) {
                    mc.data[r][c] = ma.data[r][c] - mb.data[r][c]
                }
            }
        }

        fun matrixMultiply(ma: Matrix, mb: Matrix, mc: Matrix) {
            if (ma.cols != mb.rows) {
                throw AssertionError("Assertion failed")
            }
            if (ma.rows != mc.rows) {
                throw AssertionError("Assertion failed")
            }
            if (mb.cols != mc.cols) {
                throw AssertionError("Assertion failed")
            }

            for (r in 0 until mc.rows) {
                for (c in 0 until mc.cols) {
                    mc.data[r][c] = 0.0
                    for (rc in 0 until ma.cols)
                        mc.data[r][c] += ma.data[r][rc] * mb.data[rc][c]
                } //for col
            } //for row
        }

        fun matrixMultiplyByTranspose(ma: Matrix, mb: Matrix, mc: Matrix) {
            if (ma.cols != mb.cols) {
                throw AssertionError("Assertion failed")
            }
            if (ma.rows != mc.rows) {
                throw AssertionError("Assertion failed")
            }
            if (mb.rows != mc.cols) {
                throw AssertionError("Assertion failed")
            }
            for (r in 0 until mc.rows) {
                for (c in 0 until mc.cols) {
                    mc.data[r][c] = 0.0
                    for (rc in 0 until ma.cols)
                        mc.data[r][c] += ma.data[r][rc] * mb.data[c][rc]
                } //for col
            } //for row
        }

        fun matrixTranspose(mtxin: Matrix, mtxout: Matrix) {
            if (mtxin.rows != mtxout.cols) {
                throw AssertionError("Assertion failed")
            }
            if (mtxin.cols != mtxout.rows) {
                throw AssertionError("Assertion failed")
            }
            for (r in 0 until mtxin.rows) {
                for (c in 0 until mtxin.cols) {
                    mtxout.data[c][r] = mtxin.data[r][c]
                } //for col
            } //for row
        }

        fun matrixEq(ma: Matrix, mb: Matrix, eps: Double): Boolean {
            if (ma.rows != mb.rows || ma.cols != mb.cols)
                return false
            for (r in 0 until ma.rows) {
                for (c in 0 until ma.cols) {
                    if (Math.abs(ma.data[r][c] - mb.data[r][c]) <= eps)
                        continue
                    return false
                }
            }
            return true
        }

        fun matrixCopy(mSrc: Matrix, mDst: Matrix) {
            if (!(mSrc.rows == mDst.rows && mSrc.cols == mDst.cols)) {
                throw AssertionError("Assertion failed")
            }
            for (r in 0 until mSrc.rows) {
                for (c in 0 until mSrc.cols) {
                    mDst.data[r][c] = mSrc.data[r][c]
                }
            }
        }

        fun matrixDestructiveInvert(mtxin: Matrix, mtxout: Matrix): Boolean {
            if (mtxin.cols != mtxin.rows) {
                throw AssertionError("Assertion failed")
            }
            if (mtxout.cols != mtxin.cols) {
                throw AssertionError("Assertion failed")
            }
            if (mtxout.rows != mtxin.rows) {
                throw AssertionError("Assertion failed")
            }

            var ri: Int
            var scalar: Double
            mtxout.setIdentity()

            for (r in 0 until mtxin.rows) {
                if (mtxin.data[r][r] == 0.0) { //we have to swap rows here to make nonzero diagonal
                    ri = r
                    while (ri < mtxin.rows) {
                        if (mtxin.data[ri][ri] != 0.0)
                            break
                        ri++
                    }

                    if (ri == mtxin.rows)
                        return false  //can't get inverse matrix

                    mtxin.swapRows(r, ri)
                    mtxout.swapRows(r, ri)
                } //if mtxin.data[r][r] == 0.0

                scalar = 1.0 / mtxin.data[r][r]
                mtxin.scaleRow(r, scalar)
                mtxout.scaleRow(r, scalar)

                for (rr in 0 until r) {
                    scalar = -mtxin.data[rr][r]
                    mtxin.shearRow(rr, r, scalar)
                    mtxout.shearRow(rr, r, scalar)
                }

                for (rr in r + 1 until mtxin.rows) {
                    scalar = -mtxin.data[rr][r]
                    mtxin.shearRow(rr, r, scalar)
                    mtxout.shearRow(rr, r, scalar)
                }
            } //for r < mtxin.rows
            return true
        }

        fun matrixSubtractFromIdentity(m: Matrix) {
            for (r in 0 until m.rows) {
                for (c in 0 until r)
                    m.data[r][c] = -m.data[r][c]
                m.data[r][r] = 1.0 - m.data[r][r]
                for (c in r + 1 until m.cols)
                    m.data[r][c] = -m.data[r][c]
            }
        }
    }
}